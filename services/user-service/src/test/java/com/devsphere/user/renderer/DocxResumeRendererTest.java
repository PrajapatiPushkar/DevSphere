package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import com.devsphere.user.dto.compilation.CompiledEducationResponse;
import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSkillItemResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.Certification;
import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.Education;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.Skill;
import com.devsphere.user.entity.SkillCategory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocxResumeRendererTest {

    private ApachePoiDocxResumeRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ApachePoiDocxResumeRenderer();
    }

    @Test
    void render_nullCompiledResume_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> renderer.render(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Compiled resume must not be null");
    }

    @Test
    void render_validCompiledResume_returnsNonEmptyDocxBytesAndReopensWithPoi() throws Exception {
        CompiledResumeResponse compiled = buildSampleCompiledResume(ResumeTemplate.PROFESSIONAL);

        byte[] docxBytes = renderer.render(compiled);

        assertThat(docxBytes).isNotNull();
        assertThat(docxBytes.length).isGreaterThan(0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertThat(doc.getParagraphs()).isNotEmpty();

            String fullText = extractFullText(doc);
            assertThat(fullText).contains("Pushkar Developer");
            assertThat(fullText).contains("Senior Software Engineer");
            assertThat(fullText).contains("Experienced software architect with expertise in Java microservices.");
            assertThat(fullText).contains("Backend Tech Lead");
            assertThat(fullText).contains("DevSphere Corp");
            assertThat(fullText).contains("Master of Science");
            assertThat(fullText).contains("Tech University");
            assertThat(fullText).contains("Java");
            assertThat(fullText).contains("AWS Certified Security");
            assertThat(fullText).contains("Amazon Web Services");
            assertThat(fullText).contains("DevSphere Engine");
        }
    }

    @Test
    void render_allTemplates_succeedAndFormatCorrectly() throws Exception {
        for (ResumeTemplate template : ResumeTemplate.values()) {
            CompiledResumeResponse compiled = buildSampleCompiledResume(template);
            byte[] bytes = renderer.render(compiled);

            assertThat(bytes).isNotNull();
            assertThat(bytes.length).isGreaterThan(0);

            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                assertThat(doc.getParagraphs()).isNotEmpty();
                assertThat(extractFullText(doc)).contains("Pushkar Developer");
            }
        }
    }

    @Test
    void render_invisibleSectionExcluded() throws Exception {
        CompiledResumeResponse compiled = buildSampleCompiledResume(ResumeTemplate.PROFESSIONAL);

        // Hide EDUCATION section
        compiled.getSections().stream()
                .filter(s -> s.getSectionType() == ResumeSectionType.EDUCATION)
                .findFirst()
                .ifPresent(s -> s.setVisible(false));

        byte[] bytes = renderer.render(compiled);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = extractFullText(doc);
            assertThat(text).contains("EXPERIENCE");
            assertThat(text).doesNotContain("Tech University");
        }
    }

    @Test
    void render_sectionOrderingIsPreserved() throws Exception {
        CompiledResumeResponse compiled = buildSampleCompiledResume(ResumeTemplate.PROFESSIONAL);

        // Reverse order of sections: PROJECTS first, SUMMARY second
        CompiledResumeSectionResponse projSec = compiled.getSections().stream()
                .filter(s -> s.getSectionType() == ResumeSectionType.PROJECTS)
                .findFirst().orElseThrow();
        CompiledResumeSectionResponse sumSec = compiled.getSections().stream()
                .filter(s -> s.getSectionType() == ResumeSectionType.SUMMARY)
                .findFirst().orElseThrow();

        compiled.setSections(List.of(projSec, sumSec));

        byte[] bytes = renderer.render(compiled);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = extractFullText(doc);
            int projIdx = text.indexOf("PROJECTS");
            int sumIdx = text.indexOf("SUMMARY");

            assertThat(projIdx).isGreaterThan(-1);
            assertThat(sumIdx).isGreaterThan(-1);
            assertThat(projIdx).isLessThan(sumIdx);
        }
    }

    @Test
    void render_xssPayload_renderedAsLiteralPlainText() throws Exception {
        CompiledResumeResponse compiled = buildSampleCompiledResume(ResumeTemplate.PROFESSIONAL);
        String xssPayload = "<script>alert('xss')</script>";
        compiled.setName(xssPayload);

        byte[] bytes = renderer.render(compiled);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = extractFullText(doc);
            assertThat(text).contains(xssPayload);
        }
    }

    @Test
    void render_unsafeUrls_suppressedOrHandledSafely() throws Exception {
        CompiledResumeResponse compiled = buildSampleCompiledResume(ResumeTemplate.PROFESSIONAL);

        DeveloperProject projEntity = new DeveloperProject(100L, "Unsafe Project", ProjectType.PERSONAL);
        projEntity.setDescription("Desc");
        projEntity.setTechStack(List.of("Java"));
        projEntity.setRepositoryUrl("javascript:alert(1)");
        projEntity.setLiveUrl("file:///etc/passwd");
        projEntity.setDocumentationUrl("https://safe-docs.example.com");

        CompiledProjectResponse projResp = new CompiledProjectResponse(projEntity, 1);
        CompiledResumeSectionResponse projSec = new CompiledResumeSectionResponse(
                ResumeSectionType.PROJECTS, 1, true, List.of(projResp)
        );
        compiled.setSections(List.of(projSec));

        byte[] bytes = renderer.render(compiled);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = extractFullText(doc);
            assertThat(text).contains("Unsafe Project");
            assertThat(text).contains("Docs"); // Safe URL rendered
            assertThat(text).doesNotContain("javascript:alert(1)");
            assertThat(text).doesNotContain("file:///etc/passwd");
        }
    }

    @Test
    void render_rendererDoesNotMutateCompiledData() {
        CompiledResumeResponse compiled = buildSampleCompiledResume(ResumeTemplate.PROFESSIONAL);
        String originalName = compiled.getName();

        renderer.render(compiled);

        assertThat(compiled.getName()).isEqualTo(originalName);
    }

    private String extractFullText(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : doc.getParagraphs()) {
            sb.append(p.getText()).append("\n");
        }
        return sb.toString();
    }

    private CompiledResumeResponse buildSampleCompiledResume(ResumeTemplate template) {
        CompiledSummaryResponse summary = new CompiledSummaryResponse("Experienced software architect with expertise in Java microservices.");

        Experience expEntity = new Experience(100L, "DevSphere Corp", "Backend Tech Lead", EmploymentType.FULL_TIME, LocalDate.of(2021, 1, 1));
        expEntity.setLocation("San Francisco, CA");
        expEntity.setCurrentlyWorking(true);
        expEntity.setDescription("Led team of 6 engineers building cloud platforms.");
        CompiledExperienceResponse exp = new CompiledExperienceResponse(expEntity, 1);

        Education eduEntity = new Education(100L, "Tech University", "Master of Science", LocalDate.of(2018, 9, 1));
        eduEntity.setFieldOfStudy("Computer Science");
        eduEntity.setLocation("Boston, MA");
        eduEntity.setEndDate(LocalDate.of(2020, 5, 1));
        eduEntity.setCurrentlyStudying(false);
        eduEntity.setDescription("Focused on distributed systems.");
        CompiledEducationResponse edu = new CompiledEducationResponse(eduEntity, 1);

        Skill skill1Entity = new Skill(100L, "Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT);
        Skill skill2Entity = new Skill(100L, "Spring Boot", SkillCategory.FRAMEWORK, Proficiency.EXPERT);
        CompiledSkillItemResponse skill1 = new CompiledSkillItemResponse(skill1Entity, 1);
        CompiledSkillItemResponse skill2 = new CompiledSkillItemResponse(skill2Entity, 2);
        CompiledSkillsResponse skills = new CompiledSkillsResponse(List.of(skill1, skill2));

        Certification certEntity = new Certification(100L, "AWS Certified Security", "Amazon Web Services");
        certEntity.setIssueDate(LocalDate.of(2022, 6, 1));
        certEntity.setExpirationDate(LocalDate.of(2025, 6, 1));
        certEntity.setCredentialUrl("https://aws.amazon.com/verify/123");
        certEntity.setDescription("Cloud Security Specialty");
        CompiledCertificationResponse cert = new CompiledCertificationResponse(certEntity, 1);

        DeveloperProject projEntity = new DeveloperProject(100L, "DevSphere Engine", ProjectType.OPEN_SOURCE);
        projEntity.setDescription("Developer ecosystem engine.");
        projEntity.setTechStack(List.of("Java", "Spring Boot"));
        projEntity.setRepositoryUrl("https://github.com/devsphere/engine");
        projEntity.setLiveUrl("https://devsphere.example.com");
        projEntity.setDocumentationUrl("https://docs.devsphere.example.com");
        CompiledProjectResponse proj = new CompiledProjectResponse(projEntity, 1);

        CompiledResumeSectionResponse sumSec = new CompiledResumeSectionResponse(ResumeSectionType.SUMMARY, 1, true, summary);
        CompiledResumeSectionResponse expSec = new CompiledResumeSectionResponse(ResumeSectionType.EXPERIENCE, 2, true, List.of(exp));
        CompiledResumeSectionResponse eduSec = new CompiledResumeSectionResponse(ResumeSectionType.EDUCATION, 3, true, List.of(edu));
        CompiledResumeSectionResponse skillSec = new CompiledResumeSectionResponse(ResumeSectionType.SKILLS, 4, true, skills);
        CompiledResumeSectionResponse certSec = new CompiledResumeSectionResponse(ResumeSectionType.CERTIFICATIONS, 5, true, List.of(cert));
        CompiledResumeSectionResponse projSec = new CompiledResumeSectionResponse(ResumeSectionType.PROJECTS, 6, true, List.of(proj));

        return new CompiledResumeResponse(
                100L, 100L, "Pushkar Developer", "Senior Software Engineer", template,
                List.of(sumSec, expSec, eduSec, skillSec, certSec, projSec)
        );
    }
}
