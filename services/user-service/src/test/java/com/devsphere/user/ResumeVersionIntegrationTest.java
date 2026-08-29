package com.devsphere.user;

import com.devsphere.user.dto.CreateResumeVersionRequest;
import com.devsphere.user.dto.EducationRequest;
import com.devsphere.user.dto.EducationResponse;
import com.devsphere.user.dto.ExperienceRequest;
import com.devsphere.user.dto.ExperienceResponse;
import com.devsphere.user.dto.ResumeEducationRequest;
import com.devsphere.user.dto.ResumeExperienceRequest;
import com.devsphere.user.dto.ResumeExportFormat;
import com.devsphere.user.dto.ResumeExportResult;
import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.EducationRepository;
import com.devsphere.user.repository.ExperienceRepository;
import com.devsphere.user.repository.ResumeEducationRepository;
import com.devsphere.user.repository.ResumeExperienceRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeSectionRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import com.devsphere.user.service.EducationService;
import com.devsphere.user.service.ExperienceService;
import com.devsphere.user.service.ResumeExportService;
import com.devsphere.user.service.ResumeProfileService;
import com.devsphere.user.service.ResumeSelectionService;
import com.devsphere.user.service.ResumeVersionService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ResumeVersionIntegrationTest {

    @Autowired
    private ResumeProfileService resumeProfileService;
    @Autowired
    private ResumeSelectionService resumeSelectionService;
    @Autowired
    private ExperienceService experienceService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private ResumeVersionService resumeVersionService;
    @Autowired
    private ResumeExportService resumeExportService;

    @Autowired
    private ResumeVersionRepository resumeVersionRepository;
    @Autowired
    private ResumeProfileRepository resumeProfileRepository;
    @Autowired
    private ResumeSectionRepository resumeSectionRepository;
    @Autowired
    private ResumeExperienceRepository resumeExperienceRepository;
    @Autowired
    private ResumeEducationRepository resumeEducationRepository;
    @Autowired
    private ExperienceRepository experienceRepository;
    @Autowired
    private EducationRepository educationRepository;

    @BeforeEach
    void cleanDatabase() {
        resumeVersionRepository.deleteAll();
        resumeExperienceRepository.deleteAll();
        resumeEducationRepository.deleteAll();
        resumeSectionRepository.deleteAll();
        resumeProfileRepository.deleteAll();
        experienceRepository.deleteAll();
        educationRepository.deleteAll();
    }

    @Test
    @DisplayName("MANDATORY IMMUTABILITY TEST: Published version remains unchanged when career data mutates")
    void mandatoryImmutabilityTest_PublishedVersionStaysFrozen() {
        Long userId = 2001L;

        // 1. Create career experience: Company A & Role A
        ExperienceResponse expA = experienceService.createExperience(userId, new ExperienceRequest(
                "Company A", "Role A", EmploymentType.FULL_TIME, "Remote", LocalDate.of(2020, 1, 1), null, true, "Led team A", 0
        ));

        // Create Resume Profile
        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest(
                "My Developer Resume", "Senior Software Engineer", "Experienced Engineer", ResumeTemplate.PROFESSIONAL
        ));

        // Select Experience A
        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(expA.getId(), 1));

        // 2. Create Version 1
        ResumeVersionResponse v1Created = resumeVersionService.createVersion(profile.getId(), userId, new CreateResumeVersionRequest("Version 1 - Initial"));
        assertThat(v1Created.getVersionNumber()).isEqualTo(1);
        assertThat(v1Created.getStatus()).isEqualTo(ResumeVersionStatus.DRAFT);

        // 3. Publish Version 1
        ResumeVersionResponse v1Published = resumeVersionService.publishVersion(profile.getId(), v1Created.getId(), userId);
        assertThat(v1Published.getStatus()).isEqualTo(ResumeVersionStatus.PUBLISHED);
        assertThat(v1Published.getPublishedAt()).isNotNull();

        // 4. Mutate live career data in database: Change Company A & Role A to Company B & Role B
        Experience liveExp = experienceRepository.findById(expA.getId()).orElseThrow();
        liveExp.setCompanyName("Company B");
        liveExp.setJobTitle("Role B");
        experienceRepository.save(liveExp);

        // 5. Compile & Export Version 1
        ResumeExportResult v1HtmlExport = resumeExportService.exportResumeVersion(profile.getId(), v1Published.getId(), userId, ResumeExportFormat.HTML);
        String v1Html = new String(v1HtmlExport.getContent(), java.nio.charset.StandardCharsets.UTF_8);

        // Expected: Version 1 MUST still contain Company A and Role A, and MUST NOT contain Company B or Role B
        assertThat(v1Html).contains("Company A");
        assertThat(v1Html).contains("Role A");
        assertThat(v1Html).doesNotContain("Company B");
        assertThat(v1Html).doesNotContain("Role B");

        // 6. Create Version 2 (which captures the updated live data Company B & Role B)
        ResumeVersionResponse v2Created = resumeVersionService.createVersion(profile.getId(), userId, new CreateResumeVersionRequest("Version 2 - Updated"));
        assertThat(v2Created.getVersionNumber()).isEqualTo(2);

        ResumeExportResult v2HtmlExport = resumeExportService.exportResumeVersion(profile.getId(), v2Created.getId(), userId, ResumeExportFormat.HTML);
        String v2Html = new String(v2HtmlExport.getContent(), java.nio.charset.StandardCharsets.UTF_8);

        // Version 2 should contain Company B and Role B
        assertThat(v2Html).contains("Company B");
        assertThat(v2Html).contains("Role B");
        assertThat(v2Html).doesNotContain("Company A");
        assertThat(v2Html).doesNotContain("Role A");
    }

    @Test
    @DisplayName("CROSS-FORMAT VERSION TEST: HTML, PDF, and DOCX exports for a published version render same snapshot")
    void crossFormatVersionExportTest_RendersIdenticalSnapshot() throws Exception {
        Long userId = 2002L;

        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest(
                "Acme Corp", "Backend Architect", EmploymentType.FULL_TIME, "San Francisco", LocalDate.of(2019, 1, 1), null, true, "Engineered cloud microservices", 0
        ));
        EducationResponse edu = educationService.createEducation(userId, new EducationRequest(
                "Stanford University", "B.S.", "Computer Science", "Stanford", LocalDate.of(2015, 9, 1), LocalDate.of(2019, 6, 1), false, "GPA 3.9", 0
        ));

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest(
                "Architect Resume", "Lead Architect", "Cloud Systems Specialist", ResumeTemplate.MODERN
        ));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        resumeSelectionService.addEducation(profile.getId(), userId, new ResumeEducationRequest(edu.getId(), 2));

        ResumeVersionResponse version = resumeVersionService.createVersion(profile.getId(), userId, new CreateResumeVersionRequest("Published Version"));
        resumeVersionService.publishVersion(profile.getId(), version.getId(), userId);

        // 1. HTML Version Export
        ResumeExportResult htmlResult = resumeExportService.exportResumeVersion(profile.getId(), version.getId(), userId, ResumeExportFormat.HTML);
        assertThat(htmlResult.getContentType()).contains("text/html");
        String htmlText = new String(htmlResult.getContent(), java.nio.charset.StandardCharsets.UTF_8);

        // 2. PDF Version Export
        ResumeExportResult pdfResult = resumeExportService.exportResumeVersion(profile.getId(), version.getId(), userId, ResumeExportFormat.PDF);
        assertThat(pdfResult.getContentType()).isEqualTo("application/pdf");
        String pdfText;
        try (org.apache.pdfbox.pdmodel.PDDocument pdfDoc = org.apache.pdfbox.pdmodel.PDDocument.load(pdfResult.getContent())) {
            pdfText = new org.apache.pdfbox.text.PDFTextStripper().getText(pdfDoc);
        }

        // 3. DOCX Version Export
        ResumeExportResult docxResult = resumeExportService.exportResumeVersion(profile.getId(), version.getId(), userId, ResumeExportFormat.DOCX);
        assertThat(docxResult.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        String docxText;
        try (org.apache.poi.xwpf.usermodel.XWPFDocument docxDoc = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(docxResult.getContent()))) {
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : docxDoc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            docxText = sb.toString();
        }

        // Verify key representative fields in all 3 formats
        List<String> keyStrings = List.of(
                "Architect Resume",
                "Lead Architect",
                "Cloud Systems Specialist",
                "Acme Corp",
                "Backend Architect",
                "Stanford University"
        );

        for (String key : keyStrings) {
            assertThat(htmlText).as("HTML version export should contain: " + key).contains(key);
            assertThat(pdfText).as("PDF version export should contain: " + key).contains(key);
            assertThat(docxText).as("DOCX version export should contain: " + key).contains(key);
        }
    }

    @Test
    @DisplayName("Cross-user access control: User B cannot access or render User A's version")
    void crossUserVersionAccess_ReturnsNotFound() {
        Long userA = 2003L;
        Long userB = 2004L;

        ResumeProfileResponse profileA = resumeProfileService.createResumeProfile(userA, new ResumeProfileRequest(
                "User A Resume", "Developer", null, ResumeTemplate.PROFESSIONAL
        ));
        ResumeVersionResponse vA = resumeVersionService.createVersion(profileA.getId(), userA, new CreateResumeVersionRequest("User A Version"));

        // User B tries to get version detail
        assertThatThrownBy(() -> resumeVersionService.getVersion(profileA.getId(), vA.getId(), userB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");

        // User B tries to export version
        assertThatThrownBy(() -> resumeExportService.exportResumeVersion(profileA.getId(), vA.getId(), userB, ResumeExportFormat.HTML))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");
    }
}
