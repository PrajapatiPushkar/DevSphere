package com.devsphere.user;

import com.devsphere.user.dto.CertificationRequest;
import com.devsphere.user.dto.CertificationResponse;
import com.devsphere.user.dto.CreateProjectRequest;
import com.devsphere.user.dto.EducationRequest;
import com.devsphere.user.dto.EducationResponse;
import com.devsphere.user.dto.ExperienceRequest;
import com.devsphere.user.dto.ExperienceResponse;
import com.devsphere.user.dto.ProjectResponse;
import com.devsphere.user.dto.ResumeCertificationRequest;
import com.devsphere.user.dto.ResumeCertificationResponse;
import com.devsphere.user.dto.ResumeEducationRequest;
import com.devsphere.user.dto.ResumeEducationResponse;
import com.devsphere.user.dto.ResumeExportFormat;
import com.devsphere.user.dto.ResumeExportResult;
import com.devsphere.user.dto.ResumeExperienceRequest;
import com.devsphere.user.dto.ResumeExperienceResponse;
import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.dto.ResumeProjectRequest;
import com.devsphere.user.dto.ResumeProjectResponse;
import com.devsphere.user.dto.ResumeSectionResponse;
import com.devsphere.user.dto.ResumeSkillRequest;
import com.devsphere.user.dto.ResumeSkillResponse;
import com.devsphere.user.dto.SkillRequest;
import com.devsphere.user.dto.SkillResponse;
import com.devsphere.user.dto.UpdateResumeSectionRequest;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.SkillCategory;
import com.devsphere.user.exception.DuplicateResumeSelectionException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CertificationRepository;
import com.devsphere.user.repository.DeveloperProjectRepository;
import com.devsphere.user.repository.EducationRepository;
import com.devsphere.user.repository.ExperienceRepository;
import com.devsphere.user.repository.ResumeCertificationRepository;
import com.devsphere.user.repository.ResumeEducationRepository;
import com.devsphere.user.repository.ResumeExperienceRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeProjectRepository;
import com.devsphere.user.repository.ResumeSectionRepository;
import com.devsphere.user.repository.ResumeSkillRepository;
import com.devsphere.user.repository.SkillRepository;
import com.devsphere.user.service.CertificationService;
import com.devsphere.user.service.EducationService;
import com.devsphere.user.service.ExperienceService;
import com.devsphere.user.service.ProjectService;
import com.devsphere.user.service.ResumeExportService;
import com.devsphere.user.service.ResumeProfileService;
import com.devsphere.user.service.ResumeSectionService;
import com.devsphere.user.service.ResumeSelectionService;
import com.devsphere.user.service.SkillService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ResumeIntegrationTest {

    @Autowired
    private ResumeProfileService resumeProfileService;
    @Autowired
    private ResumeSectionService resumeSectionService;
    @Autowired
    private ResumeSelectionService resumeSelectionService;
    @Autowired
    private com.devsphere.user.service.ResumeCompilationService resumeCompilationService;
    @Autowired
    private com.devsphere.user.service.ResumeRenderingService resumeRenderingService;
    @Autowired
    private com.devsphere.user.service.ResumePdfRenderingService resumePdfRenderingService;
    @Autowired
    private com.devsphere.user.service.ResumeDocxRenderingService resumeDocxRenderingService;
    @Autowired
    private ResumeExportService resumeExportService;

    @Autowired
    private ExperienceService experienceService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private SkillService skillService;
    @Autowired
    private CertificationService certificationService;
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ResumeProfileRepository resumeProfileRepository;
    @Autowired
    private ResumeSectionRepository resumeSectionRepository;
    @Autowired
    private ResumeExperienceRepository resumeExperienceRepository;
    @Autowired
    private ResumeEducationRepository resumeEducationRepository;
    @Autowired
    private ResumeSkillRepository resumeSkillRepository;
    @Autowired
    private ResumeCertificationRepository resumeCertificationRepository;
    @Autowired
    private ResumeProjectRepository resumeProjectRepository;

    @Autowired
    private ExperienceRepository experienceRepository;
    @Autowired
    private EducationRepository educationRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private CertificationRepository certificationRepository;
    @Autowired
    private DeveloperProjectRepository projectRepository;

    @BeforeEach
    void cleanDatabase() {
        resumeExperienceRepository.deleteAll();
        resumeEducationRepository.deleteAll();
        resumeSkillRepository.deleteAll();
        resumeCertificationRepository.deleteAll();
        resumeProjectRepository.deleteAll();
        resumeSectionRepository.deleteAll();
        resumeProfileRepository.deleteAll();

        experienceRepository.deleteAll();
        educationRepository.deleteAll();
        skillRepository.deleteAll();
        certificationRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void resumeLifecycle_createsUpdatesArchivesActivatesAndDeletesResume() {
        Long userId = 1000L;

        ResumeProfileRequest createReq = new ResumeProfileRequest("Full Stack Resume", "Lead Full Stack Engineer", "Experienced Dev", ResumeTemplate.PROFESSIONAL);
        ResumeProfileResponse created = resumeProfileService.createResumeProfile(userId, createReq);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ResumeStatus.DRAFT);
        assertThat(created.getTemplate()).isEqualTo(ResumeTemplate.PROFESSIONAL);

        // Verify default 6 sections created
        List<ResumeSectionResponse> sections = resumeSectionService.listSections(created.getId(), userId);
        assertThat(sections).hasSize(6);

        // Activate resume
        ResumeProfileResponse activated = resumeProfileService.activateResumeProfile(created.getId(), userId);
        assertThat(activated.getStatus()).isEqualTo(ResumeStatus.ACTIVE);

        // Archive resume
        ResumeProfileResponse archived = resumeProfileService.archiveResumeProfile(created.getId(), userId);
        assertThat(archived.getStatus()).isEqualTo(ResumeStatus.ARCHIVED);

        // Delete (logical archival)
        resumeProfileService.deleteResumeProfile(created.getId(), userId);
        ResumeProfileResponse fetched = resumeProfileService.getResumeProfile(created.getId(), userId);
        assertThat(fetched.getStatus()).isEqualTo(ResumeStatus.ARCHIVED);
    }

    @Test
    void singleActiveResumeRule_activatingNewResumeArchivesPreviousActive() {
        Long userId = 1001L;

        ResumeProfileResponse r1 = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Resume 1", "Role 1", null, ResumeTemplate.MODERN));
        ResumeProfileResponse r2 = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Resume 2", "Role 2", null, ResumeTemplate.MINIMAL));

        resumeProfileService.activateResumeProfile(r1.getId(), userId);
        assertThat(resumeProfileService.getResumeProfile(r1.getId(), userId).getStatus()).isEqualTo(ResumeStatus.ACTIVE);

        // Activate r2
        resumeProfileService.activateResumeProfile(r2.getId(), userId);
        assertThat(resumeProfileService.getResumeProfile(r2.getId(), userId).getStatus()).isEqualTo(ResumeStatus.ACTIVE);
        assertThat(resumeProfileService.getResumeProfile(r1.getId(), userId).getStatus()).isEqualTo(ResumeStatus.ARCHIVED);

        List<ResumeProfileResponse> activeList = resumeProfileService.listResumeProfiles(userId)
                .stream()
                .filter(r -> r.getStatus() == ResumeStatus.ACTIVE)
                .toList();
        assertThat(activeList).hasSize(1);
        assertThat(activeList.get(0).getId()).isEqualTo(r2.getId());
    }

    @Test
    void sectionManagement_listsAndUpdatesSectionVisibilityAndOrdering() {
        Long userId = 1002L;
        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Resume", "Role", null, ResumeTemplate.PROFESSIONAL));

        List<ResumeSectionResponse> sections = resumeSectionService.listSections(profile.getId(), userId);
        Long summarySectionId = sections.get(0).getId();

        ResumeSectionResponse updated = resumeSectionService.updateSection(profile.getId(), summarySectionId, userId, new UpdateResumeSectionRequest(10, false));
        assertThat(updated.getDisplayOrder()).isEqualTo(10);
        assertThat(updated.getVisible()).isFalse();
    }

    @Test
    void selections_experienceEducationSkillCertificationProject() {
        Long userId = 1003L;
        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Resume", "Role", null, ResumeTemplate.PROFESSIONAL));

        // Create source entities
        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Co", "Dev", EmploymentType.FULL_TIME, "Loc", LocalDate.of(2022, 1, 1), null, true, "Desc", 0));
        EducationResponse edu = educationService.createEducation(userId, new EducationRequest("Uni", "BS", "CS", "Loc", LocalDate.of(2018, 9, 1), LocalDate.of(2022, 5, 1), false, "Desc", 0));
        SkillResponse skill = skillService.createSkill(userId, new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 5, 0));
        CertificationResponse cert = certificationService.createCertification(userId, new CertificationRequest("AWS", "Amazon", LocalDate.now(), null, null, null, null, 0));
        ProjectResponse proj = projectService.createProject(userId, new CreateProjectRequest("DevSphere", "Description", ProjectType.PERSONAL, "https://github.com", null, null, List.of("Java"), null, null));

        // Add selections
        ResumeExperienceResponse selExp = resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        ResumeEducationResponse selEdu = resumeSelectionService.addEducation(profile.getId(), userId, new ResumeEducationRequest(edu.getId(), 2));
        ResumeSkillResponse selSkill = resumeSelectionService.addSkill(profile.getId(), userId, new ResumeSkillRequest(skill.getId(), 3));
        ResumeCertificationResponse selCert = resumeSelectionService.addCertification(profile.getId(), userId, new ResumeCertificationRequest(cert.getId(), 4));
        ResumeProjectResponse selProj = resumeSelectionService.addProject(profile.getId(), userId, new ResumeProjectRequest(proj.getId(), 5));

        assertThat(selExp.getExperienceId()).isEqualTo(exp.getId());
        assertThat(selEdu.getEducationId()).isEqualTo(edu.getId());
        assertThat(selSkill.getSkillId()).isEqualTo(skill.getId());
        assertThat(selCert.getCertificationId()).isEqualTo(cert.getId());
        assertThat(selProj.getProjectId()).isEqualTo(proj.getId());

        // Remove selections
        resumeSelectionService.removeExperience(profile.getId(), exp.getId(), userId);
        assertThat(resumeSelectionService.listExperiences(profile.getId(), userId)).isEmpty();
    }

    @Test
    void duplicateSelectionPrevention_addingSameItemTwiceReturnsConflict() {
        Long userId = 1004L;
        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Resume", "Role", null, ResumeTemplate.PROFESSIONAL));
        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Co", "Dev", EmploymentType.FULL_TIME, "Loc", LocalDate.of(2022, 1, 1), null, true, "Desc", 0));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));

        assertThatThrownBy(() -> resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 2)))
                .isInstanceOf(DuplicateResumeSelectionException.class)
                .hasMessageContaining("Experience is already selected in this resume");
    }

    @Test
    void crossUserOwnershipAndIdorIsolation_preventsSelectingOtherUserSourceEntities() {
        Long userA = 1005L;
        Long userB = 1006L;

        ExperienceResponse expA = experienceService.createExperience(userA, new ExperienceRequest("Co A", "Dev A", EmploymentType.FULL_TIME, "Loc", LocalDate.of(2022, 1, 1), null, true, "Desc", 0));
        ResumeProfileResponse profileB = resumeProfileService.createResumeProfile(userB, new ResumeProfileRequest("Resume B", "Role B", null, ResumeTemplate.PROFESSIONAL));

        assertThatThrownBy(() -> resumeSelectionService.addExperience(profileB.getId(), userB, new ResumeExperienceRequest(expA.getId(), 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience record not found");

        assertThatThrownBy(() -> resumeProfileService.getResumeProfile(profileB.getId(), userA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");
    }

    @Test
    void compileResumePipeline_fullCompilationWithOrderingVisibilityAndSelections() {
        Long userId = 1007L;

        // Create source entities
        ExperienceResponse exp1 = experienceService.createExperience(userId, new ExperienceRequest("Tech Corp", "Backend Engineer", EmploymentType.FULL_TIME, "Remote", LocalDate.of(2021, 1, 1), null, true, "Java & Spring Boot", 0));
        SkillResponse skill1 = skillService.createSkill(userId, new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 5, 0));

        // Create resume profile with summary override
        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Java Backend Resume", "Senior Java Engineer", "Overridden Executive Summary", ResumeTemplate.PROFESSIONAL));

        // Add selections
        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp1.getId(), 1));
        resumeSelectionService.addSkill(profile.getId(), userId, new ResumeSkillRequest(skill1.getId(), 1));

        // Modify section visibility & display order (make EDUCATION invisible, set PROJECTS to displayOrder 1, SUMMARY to displayOrder 2)
        List<ResumeSectionResponse> sections = resumeSectionService.listSections(profile.getId(), userId);
        for (ResumeSectionResponse sec : sections) {
            if (sec.getSectionType() == com.devsphere.user.entity.ResumeSectionType.EDUCATION) {
                resumeSectionService.updateSection(profile.getId(), sec.getId(), userId, new UpdateResumeSectionRequest(sec.getDisplayOrder(), false));
            } else if (sec.getSectionType() == com.devsphere.user.entity.ResumeSectionType.PROJECTS) {
                resumeSectionService.updateSection(profile.getId(), sec.getId(), userId, new UpdateResumeSectionRequest(1, true));
            } else if (sec.getSectionType() == com.devsphere.user.entity.ResumeSectionType.SUMMARY) {
                resumeSectionService.updateSection(profile.getId(), sec.getId(), userId, new UpdateResumeSectionRequest(2, true));
            }
        }

        // Compile resume
        CompiledResumeResponse compiled = resumeCompilationService.compileResume(profile.getId(), userId);

        assertThat(compiled.getName()).isEqualTo("Java Backend Resume");
        assertThat(compiled.getTargetRole()).isEqualTo("Senior Java Engineer");

        // Verify EDUCATION is excluded (invisible)
        boolean hasEducation = compiled.getSections().stream().anyMatch(s -> s.getSectionType() == com.devsphere.user.entity.ResumeSectionType.EDUCATION);
        assertThat(hasEducation).isFalse();

        // Verify PROJECTS is now first section (displayOrder 1)
        assertThat(compiled.getSections().get(0).getSectionType()).isEqualTo(com.devsphere.user.entity.ResumeSectionType.PROJECTS);

        // Verify summary override text
        CompiledResumeSectionResponse summarySec = compiled.getSections().stream()
                .filter(s -> s.getSectionType() == com.devsphere.user.entity.ResumeSectionType.SUMMARY)
                .findFirst().orElseThrow();
        CompiledSummaryResponse summaryContent = (CompiledSummaryResponse) summarySec.getContent();
        assertThat(summaryContent.getText()).isEqualTo("Overridden Executive Summary");
    }

    @Test
    void compileResumePipeline_crossUserCompilationReturnsNotFound() {
        Long userA = 1008L;
        Long userB = 1009L;

        ResumeProfileResponse profileA = resumeProfileService.createResumeProfile(userA, new ResumeProfileRequest("Resume A", "Role A", null, ResumeTemplate.MINIMAL));

        assertThatThrownBy(() -> resumeCompilationService.compileResume(profileA.getId(), userB))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");
    }

    @Test
    void htmlRenderingPipeline_rendersHtmlDocumentEndToEnd() {
        Long userId = 1010L;

        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Tech Corp", "Full Stack Developer", EmploymentType.FULL_TIME, "San Francisco", LocalDate.of(2020, 5, 1), null, true, "Built microservices", 0));
        SkillResponse skill = skillService.createSkill(userId, new SkillRequest("Spring Boot", SkillCategory.FRAMEWORK, Proficiency.EXPERT, 4, 0));

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Full Stack Resume", "Senior Developer", "Experienced Architect", ResumeTemplate.MODERN));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        resumeSelectionService.addSkill(profile.getId(), userId, new ResumeSkillRequest(skill.getId(), 1));

        String html = resumeRenderingService.renderHtmlResume(profile.getId(), userId);

        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html).contains("<title>Full Stack Resume</title>");
        assertThat(html).contains("class=\"template-modern\"");
        assertThat(html).contains("Full Stack Developer");
        assertThat(html).contains("Spring Boot");
        assertThat(html).contains("Experienced Architect");
    }

    @Test
    void pdfRenderingPipeline_rendersPdfDocumentEndToEnd() throws Exception {
        Long userId = 1013L;

        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Tech Solutions", "Lead Backend Engineer", EmploymentType.FULL_TIME, "New York", LocalDate.of(2021, 1, 1), null, true, "Designed cloud services", 0));
        EducationResponse edu = educationService.createEducation(userId, new EducationRequest("MIT", "Master of Science", "Computer Science", "Cambridge", LocalDate.of(2019, 9, 1), LocalDate.of(2021, 5, 1), false, "Thesis on distributed systems", 0));
        SkillResponse skill = skillService.createSkill(userId, new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 6, 0));
        CertificationResponse cert = certificationService.createCertification(userId, new CertificationRequest("AWS Security", "Amazon", LocalDate.now(), null, null, null, null, 0));
        ProjectResponse proj = projectService.createProject(userId, new CreateProjectRequest("DevSphere Engine", "Platform Engine", ProjectType.PERSONAL, "https://github.com/devsphere", null, null, List.of("Java"), null, null));

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Java Resume", "Principal Architect", "Extensive Enterprise Experience", ResumeTemplate.PROFESSIONAL));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        resumeSelectionService.addEducation(profile.getId(), userId, new ResumeEducationRequest(edu.getId(), 2));
        resumeSelectionService.addSkill(profile.getId(), userId, new ResumeSkillRequest(skill.getId(), 3));
        resumeSelectionService.addCertification(profile.getId(), userId, new ResumeCertificationRequest(cert.getId(), 4));
        resumeSelectionService.addProject(profile.getId(), userId, new ResumeProjectRequest(proj.getId(), 5));

        com.devsphere.user.dto.PdfExportResult exportResult = resumePdfRenderingService.exportPdfResume(profile.getId(), userId);

        assertThat(exportResult.getFilename()).isEqualTo("Java Resume.pdf");
        byte[] pdfBytes = exportResult.getPdfBytes();

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);

            assertThat(text).contains("Java Resume");
            assertThat(text).contains("Principal Architect");
            assertThat(text).contains("Extensive Enterprise Experience");
            assertThat(text).contains("Tech Solutions");
            assertThat(text).contains("MIT");
            assertThat(text).contains("Java");
            assertThat(text).contains("AWS Security");
            assertThat(text).contains("DevSphere Engine");
        }
    }

    @Test
    void docxRenderingPipeline_rendersDocxDocumentEndToEnd() throws Exception {
        Long userId = 1016L;

        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Tech Solutions", "Lead Backend Engineer", EmploymentType.FULL_TIME, "New York", LocalDate.of(2021, 1, 1), null, true, "Designed cloud services", 0));
        EducationResponse edu = educationService.createEducation(userId, new EducationRequest("MIT", "Master of Science", "Computer Science", "Cambridge", LocalDate.of(2019, 9, 1), LocalDate.of(2021, 5, 1), false, "Thesis on distributed systems", 0));
        SkillResponse skill = skillService.createSkill(userId, new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 6, 0));
        CertificationResponse cert = certificationService.createCertification(userId, new CertificationRequest("AWS Security", "Amazon", LocalDate.now(), null, null, null, null, 0));
        ProjectResponse proj = projectService.createProject(userId, new CreateProjectRequest("DevSphere Engine", "Platform Engine", ProjectType.PERSONAL, "https://github.com/devsphere", null, null, List.of("Java"), null, null));

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Java Resume", "Principal Architect", "Extensive Enterprise Experience", ResumeTemplate.PROFESSIONAL));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        resumeSelectionService.addEducation(profile.getId(), userId, new ResumeEducationRequest(edu.getId(), 2));
        resumeSelectionService.addSkill(profile.getId(), userId, new ResumeSkillRequest(skill.getId(), 3));
        resumeSelectionService.addCertification(profile.getId(), userId, new ResumeCertificationRequest(cert.getId(), 4));
        resumeSelectionService.addProject(profile.getId(), userId, new ResumeProjectRequest(proj.getId(), 5));

        com.devsphere.user.dto.DocxExportResult exportResult = resumeDocxRenderingService.exportDocxResume(profile.getId(), userId);

        assertThat(exportResult.getFilename()).isEqualTo("Java Resume.docx");
        byte[] docxBytes = exportResult.getDocxBytes();

        assertThat(docxBytes).isNotNull();
        assertThat(docxBytes.length).isGreaterThan(0);

        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(docxBytes))) {
            assertThat(document.getParagraphs()).isNotEmpty();

            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : document.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            String text = sb.toString();

            assertThat(text).contains("Java Resume");
            assertThat(text).contains("Principal Architect");
            assertThat(text).contains("Extensive Enterprise Experience");
            assertThat(text).contains("Tech Solutions");
            assertThat(text).contains("MIT");
            assertThat(text).contains("Java");
            assertThat(text).contains("AWS Security");
            assertThat(text).contains("DevSphere Engine");
        }
    }

    @Test
    void exportServiceOrchestration_exportsHtmlPdfDocxConsistently() throws Exception {
        Long userId = 1020L;

        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Unified Core", "Principal Engineer", EmploymentType.FULL_TIME, "Boston", LocalDate.of(2021, 1, 1), null, true, "Engineered export platform", 0));
        EducationResponse edu = educationService.createEducation(userId, new EducationRequest("Harvard", "Bachelor of Science", "Computer Science", "Cambridge", LocalDate.of(2017, 9, 1), LocalDate.of(2021, 5, 1), false, "Graduated Magna Cum Laude", 0));

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Unified Resume", "Distinguished Engineer", "Unified multi-format exporter", ResumeTemplate.PROFESSIONAL));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        resumeSelectionService.addEducation(profile.getId(), userId, new ResumeEducationRequest(edu.getId(), 2));

        // HTML Export
        ResumeExportResult htmlResult = resumeExportService.exportResume(profile.getId(), userId, ResumeExportFormat.HTML);
        assertThat(htmlResult.getContentType()).contains("text/html");
        assertThat(htmlResult.getFilename()).isEqualTo("Unified Resume.html");
        assertThat(htmlResult.isAttachment()).isFalse();

        // PDF Export
        ResumeExportResult pdfResult = resumeExportService.exportResume(profile.getId(), userId, ResumeExportFormat.PDF);
        assertThat(pdfResult.getContentType()).isEqualTo("application/pdf");
        assertThat(pdfResult.getFilename()).isEqualTo("Unified Resume.pdf");
        assertThat(pdfResult.isAttachment()).isTrue();

        // DOCX Export
        ResumeExportResult docxResult = resumeExportService.exportResume(profile.getId(), userId, ResumeExportFormat.DOCX);
        assertThat(docxResult.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(docxResult.getFilename()).isEqualTo("Unified Resume.docx");
        assertThat(docxResult.isAttachment()).isTrue();
    }

    @Test
    void documentFormatsContentConsistency_htmlPdfDocxContainSameCompiledData() throws Exception {
        Long userId = 1019L;

        ExperienceResponse exp = experienceService.createExperience(userId, new ExperienceRequest("Cloud Core Corp", "Lead Backend Architect", EmploymentType.FULL_TIME, "Seattle", LocalDate.of(2020, 1, 1), null, true, "Designed multi-tenant microservices", 0));
        ProjectResponse proj = projectService.createProject(userId, new CreateProjectRequest("DevSphere Platform", "Developer Platform", ProjectType.OPEN_SOURCE, "https://github.com/devsphere/platform", null, null, List.of("Java", "Spring Boot"), null, null));

        ResumeProfileResponse profile = resumeProfileService.createResumeProfile(userId, new ResumeProfileRequest("Pushkar Consistency Test", "Staff Systems Engineer", "Architecting multi-format document engine", ResumeTemplate.MODERN));

        resumeSelectionService.addExperience(profile.getId(), userId, new ResumeExperienceRequest(exp.getId(), 1));
        resumeSelectionService.addProject(profile.getId(), userId, new ResumeProjectRequest(proj.getId(), 2));

        // 1. HTML Rendering
        String htmlText = resumeRenderingService.renderHtmlResume(profile.getId(), userId);

        // 2. PDF Rendering
        byte[] pdfBytes = resumePdfRenderingService.renderPdfResume(profile.getId(), userId);
        String pdfText;
        try (org.apache.pdfbox.pdmodel.PDDocument pdfDoc = org.apache.pdfbox.pdmodel.PDDocument.load(pdfBytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            pdfText = stripper.getText(pdfDoc);
        }

        // 3. DOCX Rendering
        byte[] docxBytes = resumeDocxRenderingService.renderDocxResume(profile.getId(), userId);
        String docxText;
        try (org.apache.poi.xwpf.usermodel.XWPFDocument docxDoc = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(docxBytes))) {
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : docxDoc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            docxText = sb.toString();
        }

        // Verify key representative fields exist in all 3 formats
        List<String> keyStrings = List.of(
                "Pushkar Consistency Test",
                "Staff Systems Engineer",
                "Architecting multi-format document engine",
                "Cloud Core Corp",
                "Lead Backend Architect",
                "DevSphere Platform"
        );

        for (String key : keyStrings) {
            assertThat(htmlText).as("HTML format should contain: " + key).contains(key);
            assertThat(pdfText).as("PDF format should contain: " + key).contains(key);
            assertThat(docxText).as("DOCX format should contain: " + key).contains(key);
        }
    }
}
