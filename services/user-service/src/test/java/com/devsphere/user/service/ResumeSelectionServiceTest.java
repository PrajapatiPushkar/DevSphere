package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeExperienceRequest;
import com.devsphere.user.dto.ResumeExperienceResponse;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.entity.ResumeExperience;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeTemplate;
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
import com.devsphere.user.repository.ResumeSkillRepository;
import com.devsphere.user.repository.SkillRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeSelectionServiceTest {

    @Mock
    private ResumeProfileRepository resumeProfileRepository;
    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private EducationRepository educationRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private DeveloperProjectRepository projectRepository;

    @Mock
    private ResumeExperienceRepository resumeExperienceRepository;
    @Mock
    private ResumeEducationRepository resumeEducationRepository;
    @Mock
    private ResumeSkillRepository resumeSkillRepository;
    @Mock
    private ResumeCertificationRepository resumeCertificationRepository;
    @Mock
    private ResumeProjectRepository resumeProjectRepository;

    private SimpleMeterRegistry meterRegistry;
    private ResumeSelectionService resumeSelectionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        resumeSelectionService = new ResumeSelectionService(
                resumeProfileRepository, experienceRepository, educationRepository, skillRepository,
                certificationRepository, projectRepository, resumeExperienceRepository, resumeEducationRepository,
                resumeSkillRepository, resumeCertificationRepository, resumeProjectRepository, meterRegistry
        );
    }

    @Test
    void addExperience_validOwnership_savesSelection() {
        Long userId = 100L;
        Long resumeId = 50L;
        Long expId = 10L;

        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        Experience exp = new Experience(userId, "Company", "Title", EmploymentType.FULL_TIME, LocalDate.now());
        exp.setId(expId);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(experienceRepository.findByIdAndUserId(expId, userId)).thenReturn(Optional.of(exp));
        when(resumeExperienceRepository.existsByResumeProfileIdAndExperienceId(resumeId, expId)).thenReturn(false);
        when(resumeExperienceRepository.save(any(ResumeExperience.class))).thenAnswer(inv -> {
            ResumeExperience re = inv.getArgument(0);
            re.setId(200L);
            return re;
        });

        ResumeExperienceResponse response = resumeSelectionService.addExperience(resumeId, userId, new ResumeExperienceRequest(expId, 1));

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getExperienceId()).isEqualTo(expId);
        assertThat(meterRegistry.find("devsphere_resume_experience_selected_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void addExperience_unownedExperience_throwsNotFound() {
        Long userId = 100L;
        Long resumeId = 50L;
        Long expId = 10L;

        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(experienceRepository.findByIdAndUserId(expId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeSelectionService.addExperience(resumeId, userId, new ResumeExperienceRequest(expId, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience record not found");
    }

    @Test
    void addExperience_duplicateSelection_throwsDuplicateResumeSelectionException() {
        Long userId = 100L;
        Long resumeId = 50L;
        Long expId = 10L;

        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        Experience exp = new Experience(userId, "Company", "Title", EmploymentType.FULL_TIME, LocalDate.now());
        exp.setId(expId);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(experienceRepository.findByIdAndUserId(expId, userId)).thenReturn(Optional.of(exp));
        when(resumeExperienceRepository.existsByResumeProfileIdAndExperienceId(resumeId, expId)).thenReturn(true);

        assertThatThrownBy(() -> resumeSelectionService.addExperience(resumeId, userId, new ResumeExperienceRequest(expId, 1)))
                .isInstanceOf(DuplicateResumeSelectionException.class)
                .hasMessageContaining("Experience is already selected in this resume");
    }
}
