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
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenHtmlToPdfResumeRendererTest {

    private HtmlResumeRenderer htmlResumeRenderer;
    private OpenHtmlToPdfResumeRenderer pdfResumeRenderer;

    @BeforeEach
    void setUp() {
        htmlResumeRenderer = new HtmlResumeRenderer();
        pdfResumeRenderer = new OpenHtmlToPdfResumeRenderer(htmlResumeRenderer);
    }

    @Test
    void render_nullInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> pdfResumeRenderer.render((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pdfResumeRenderer.render((CompiledResumeResponse) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void render_validCompiledResume_producesValidParseablePdf() throws IOException {
        CompiledResumeResponse compiled = createSampleCompiledResume(ResumeTemplate.PROFESSIONAL);

        byte[] pdfBytes = pdfResumeRenderer.render(compiled);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        // Verify PDF signature (%PDF-)
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");

        // Parse PDF using PDFBox
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            assertThat(extractedText).contains("Pushkar Prajapati");
            assertThat(extractedText).contains("Senior Backend Engineer");
            assertThat(extractedText).contains("Experienced software engineer with backend expertise.");
            assertThat(extractedText).contains("Tech Corp");
            assertThat(extractedText).contains("University of Tech");
            assertThat(extractedText).contains("Java");
            assertThat(extractedText).contains("AWS Certified Solutions Architect");
            assertThat(extractedText).contains("DevSphere Platform");
        }
    }

    @Test
    void render_multipleTemplates_exportSuccessfully() throws IOException {
        for (ResumeTemplate template : ResumeTemplate.values()) {
            CompiledResumeResponse compiled = createSampleCompiledResume(template);
            byte[] pdfBytes = pdfResumeRenderer.render(compiled);

            assertThat(pdfBytes).isNotNull();
            assertThat(pdfBytes.length).isGreaterThan(0);

            try (PDDocument document = PDDocument.load(pdfBytes)) {
                assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            }
        }
    }

    @Test
    void render_xssPayload_isEscapedAsPlainTextNotExecutedMarkup() throws IOException {
        CompiledResumeResponse compiled = new CompiledResumeResponse();
        compiled.setName("<script>alert('xss-name')</script>");
        compiled.setTargetRole("<img src=x onerror=alert('xss-role')>");
        compiled.setTemplate(ResumeTemplate.PROFESSIONAL);

        CompiledSummaryResponse summary = new CompiledSummaryResponse("<iframe src=\"javascript:alert('xss')\"></iframe>");
        CompiledResumeSectionResponse summarySection = new CompiledResumeSectionResponse(
                ResumeSectionType.SUMMARY, 1, true, summary);
        compiled.setSections(List.of(summarySection));

        byte[] pdfBytes = pdfResumeRenderer.render(compiled);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Escaped HTML tags should be rendered as plain text in the PDF document
            assertThat(text).contains("<script>alert('xss-name')</script>");
            assertThat(text).contains("<img src=x onerror=alert('xss-role')>");
        }
    }

    @Test
    void render_unsafeUrl_doesNotRenderUnsafeLink() throws IOException {
        CompiledCertificationResponse cert = new CompiledCertificationResponse();
        cert.setName("Cert");
        cert.setIssuingOrganization("Org");
        cert.setCredentialUrl("javascript:alert('xss')");

        CompiledResumeSectionResponse certSection = new CompiledResumeSectionResponse(
                ResumeSectionType.CERTIFICATIONS, 1, true, List.of(cert));

        CompiledResumeResponse compiled = new CompiledResumeResponse();
        compiled.setName("Developer");
        compiled.setTemplate(ResumeTemplate.PROFESSIONAL);
        compiled.setSections(List.of(certSection));

        byte[] pdfBytes = pdfResumeRenderer.render(compiled);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertThat(text).doesNotContain("Verify Credential");
        }
    }

    private CompiledResumeResponse createSampleCompiledResume(ResumeTemplate template) {
        CompiledResumeResponse compiled = new CompiledResumeResponse();
        compiled.setId(1L);
        compiled.setResumeProfileId(1L);
        compiled.setName("Pushkar Prajapati");
        compiled.setTargetRole("Senior Backend Engineer");
        compiled.setTemplate(template);

        CompiledSummaryResponse summary = new CompiledSummaryResponse("Experienced software engineer with backend expertise.");
        CompiledResumeSectionResponse summarySec = new CompiledResumeSectionResponse(ResumeSectionType.SUMMARY, 1, true, summary);

        CompiledExperienceResponse exp = new CompiledExperienceResponse();
        exp.setJobTitle("Lead Engineer");
        exp.setCompanyName("Tech Corp");
        exp.setLocation("Remote");
        exp.setEmploymentType(EmploymentType.FULL_TIME);
        exp.setStartDate(LocalDate.of(2021, 1, 1));
        exp.setCurrentlyWorking(true);
        exp.setDescription("Building high performance microservices");
        CompiledResumeSectionResponse expSec = new CompiledResumeSectionResponse(ResumeSectionType.EXPERIENCE, 2, true, List.of(exp));

        CompiledEducationResponse edu = new CompiledEducationResponse();
        edu.setDegree("Bachelor of Science");
        edu.setInstitutionName("University of Tech");
        edu.setFieldOfStudy("Computer Science");
        edu.setStartDate(LocalDate.of(2017, 9, 1));
        edu.setEndDate(LocalDate.of(2021, 5, 1));
        CompiledResumeSectionResponse eduSec = new CompiledResumeSectionResponse(ResumeSectionType.EDUCATION, 3, true, List.of(edu));

        CompiledSkillItemResponse skill = new CompiledSkillItemResponse();
        skill.setName("Java");
        skill.setProficiency(Proficiency.EXPERT);
        CompiledSkillsResponse skillsResp = new CompiledSkillsResponse(List.of(skill));
        CompiledResumeSectionResponse skillSec = new CompiledResumeSectionResponse(ResumeSectionType.SKILLS, 4, true, skillsResp);

        CompiledCertificationResponse cert = new CompiledCertificationResponse();
        cert.setName("AWS Certified Solutions Architect");
        cert.setIssuingOrganization("Amazon Web Services");
        cert.setIssueDate(LocalDate.of(2022, 6, 1));
        cert.setCredentialUrl("https://aws.amazon.com/verify/12345");
        CompiledResumeSectionResponse certSec = new CompiledResumeSectionResponse(ResumeSectionType.CERTIFICATIONS, 5, true, List.of(cert));

        CompiledProjectResponse proj = new CompiledProjectResponse();
        proj.setName("DevSphere Platform");
        proj.setProjectType(ProjectType.PERSONAL);
        proj.setDescription("Developer ecosystem microservices platform");
        proj.setTechStack(List.of("Java", "Spring Boot", "Kafka"));
        proj.setRepositoryUrl("https://github.com/devsphere/platform");
        CompiledResumeSectionResponse projSec = new CompiledResumeSectionResponse(ResumeSectionType.PROJECTS, 6, true, List.of(proj));

        compiled.setSections(List.of(summarySec, expSec, eduSec, skillSec, certSec, projSec));
        return compiled;
    }
}
