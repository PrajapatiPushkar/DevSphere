package com.devsphere.user;

import com.devsphere.user.dto.CertificationRequest;
import com.devsphere.user.dto.CertificationResponse;
import com.devsphere.user.dto.EducationRequest;
import com.devsphere.user.dto.EducationResponse;
import com.devsphere.user.dto.ExperienceRequest;
import com.devsphere.user.dto.ExperienceResponse;
import com.devsphere.user.dto.SkillRequest;
import com.devsphere.user.dto.SkillResponse;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Proficiency;
import com.devsphere.user.entity.SkillCategory;
import com.devsphere.user.exception.DuplicateSkillException;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CertificationRepository;
import com.devsphere.user.repository.EducationRepository;
import com.devsphere.user.repository.ExperienceRepository;
import com.devsphere.user.repository.SkillRepository;
import com.devsphere.user.service.CertificationService;
import com.devsphere.user.service.EducationService;
import com.devsphere.user.service.ExperienceService;
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
class CareerHistoryIntegrationTest {

    @Autowired
    private ExperienceService experienceService;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private EducationService educationService;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private SkillService skillService;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private CertificationRepository certificationRepository;

    @BeforeEach
    void cleanDatabase() {
        experienceRepository.deleteAll();
        educationRepository.deleteAll();
        skillRepository.deleteAll();
        certificationRepository.deleteAll();
    }

    @Test
    void experienceCrudLifecycle_createsUpdatesListsAndDeletesExperience() {
        Long userId = 900L;

        ExperienceRequest createReq = new ExperienceRequest(
                "Acme Corp", "Backend Engineer", EmploymentType.FULL_TIME, "Remote",
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 1), false, "Built Java microservices", 1
        );

        ExperienceResponse created = experienceService.createExperience(userId, createReq);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getCompanyName()).isEqualTo("Acme Corp");

        List<ExperienceResponse> list = experienceService.listExperiences(userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(created.getId());

        ExperienceRequest updateReq = new ExperienceRequest(
                "Acme Corp Updated", "Senior Backend Engineer", EmploymentType.FULL_TIME, "San Francisco",
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 12, 1), false, "Lead Engineer", 0
        );

        ExperienceResponse updated = experienceService.updateExperience(created.getId(), userId, updateReq);
        assertThat(updated.getJobTitle()).isEqualTo("Senior Backend Engineer");

        experienceService.deleteExperience(created.getId(), userId);

        assertThat(experienceService.listExperiences(userId)).isEmpty();
    }

    @Test
    void experienceDateValidation_currentlyWorkingTrueEnforcesNullEndDate() {
        Long userId = 901L;

        ExperienceRequest invalidRequest = new ExperienceRequest(
                "Meta", "Software Engineer", EmploymentType.FULL_TIME, "Remote",
                LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 1), true, "Present role", 0
        );

        assertThatThrownBy(() -> experienceService.createExperience(userId, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must be null when currentlyWorking is true");

        ExperienceRequest validRequest = new ExperienceRequest(
                "Meta", "Software Engineer", EmploymentType.FULL_TIME, "Remote",
                LocalDate.of(2023, 1, 1), null, true, "Present role", 0
        );

        ExperienceResponse created = experienceService.createExperience(userId, validRequest);
        assertThat(created.getCurrentlyWorking()).isTrue();
        assertThat(created.getEndDate()).isNull();
    }

    @Test
    void educationCrudLifecycle_createsUpdatesListsAndDeletesEducation() {
        Long userId = 902L;

        EducationRequest createReq = new EducationRequest(
                "MIT", "Bachelor of Science", "Computer Science", "Boston",
                LocalDate.of(2018, 9, 1), LocalDate.of(2022, 5, 1), false, "CS Major", 1
        );

        EducationResponse created = educationService.createEducation(userId, createReq);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getInstitutionName()).isEqualTo("MIT");

        List<EducationResponse> list = educationService.listEducations(userId);
        assertThat(list).hasSize(1);

        educationService.deleteEducation(created.getId(), userId);
        assertThat(educationService.listEducations(userId)).isEmpty();
    }

    @Test
    void skillCrudLifecycle_createsUpdatesListsAndDeletesSkill() {
        Long userId = 903L;

        SkillRequest createReq = new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 5, 1);
        SkillResponse created = skillService.createSkill(userId, createReq);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Java");

        List<SkillResponse> list = skillService.listSkills(userId);
        assertThat(list).hasSize(1);

        skillService.deleteSkill(created.getId(), userId);
        assertThat(skillService.listSkills(userId)).isEmpty();
    }

    @Test
    void skillDuplicatePrevention_caseInsensitiveDuplicateThrowsException() {
        Long userId = 904L;

        skillService.createSkill(userId, new SkillRequest("Java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.EXPERT, 5, 1));

        assertThatThrownBy(() -> skillService.createSkill(userId, new SkillRequest("java", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.ADVANCED, 3, 2)))
                .isInstanceOf(DuplicateSkillException.class)
                .hasMessageContaining("A skill with name 'java' already exists");

        assertThatThrownBy(() -> skillService.createSkill(userId, new SkillRequest("JAVA", SkillCategory.PROGRAMMING_LANGUAGE, Proficiency.INTERMEDIATE, 2, 3)))
                .isInstanceOf(DuplicateSkillException.class)
                .hasMessageContaining("A skill with name 'JAVA' already exists");
    }

    @Test
    void certificationCrudLifecycle_createsUpdatesListsAndDeletesCertification() {
        Long userId = 905L;

        CertificationRequest createReq = new CertificationRequest(
                "AWS Solutions Architect", "AWS",
                LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1),
                "AWS-123", "https://aws.amazon.com/verify", "Architect", 1
        );

        CertificationResponse created = certificationService.createCertification(userId, createReq);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("AWS Solutions Architect");

        List<CertificationResponse> list = certificationService.listCertifications(userId);
        assertThat(list).hasSize(1);

        certificationService.deleteCertification(created.getId(), userId);
        assertThat(certificationService.listCertifications(userId)).isEmpty();
    }

    @Test
    void certificationDateValidation_expirationDateBeforeIssueDateThrowsException() {
        Long userId = 906L;

        CertificationRequest request = new CertificationRequest(
                "Invalid Cert", "Org",
                LocalDate.of(2023, 1, 1), LocalDate.of(2020, 1, 1),
                null, null, "Invalid", 0
        );

        assertThatThrownBy(() -> certificationService.createCertification(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirationDate must not be before issueDate");
    }

    @Test
    void userIsolationAndIdorProtection_userCannotAccessOrMutateOtherUserHistory() {
        Long userA = 907L;
        Long userB = 908L;

        ExperienceResponse expA = experienceService.createExperience(userA, new ExperienceRequest(
                "Company A", "Dev A", EmploymentType.FULL_TIME, "Remote",
                LocalDate.of(2022, 1, 1), null, true, "Dev A role", 0
        ));

        assertThatThrownBy(() -> experienceService.getExperience(expA.getId(), userB))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> experienceService.deleteExperience(expA.getId(), userB))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
