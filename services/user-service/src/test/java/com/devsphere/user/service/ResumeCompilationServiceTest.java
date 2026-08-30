package com.devsphere.user.service;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.CareerProfile;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.entity.ResumeExperience;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CareerProfileRepository;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeCompilationServiceTest {

    @Mock
    private ResumeProfileRepository resumeProfileRepository;
    @Mock
    private ResumeSectionRepository resumeSectionRepository;
    @Mock
    private CareerProfileRepository careerProfileRepository;

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
    private ResumeCompilationService resumeCompilationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        resumeCompilationService = new ResumeCompilationService(
                resumeProfileRepository, resumeSectionRepository, careerProfileRepository,
                experienceRepository, educationRepository, skillRepository, certificationRepository, projectRepository,
                resumeExperienceRepository, resumeEducationRepository, resumeSkillRepository,
                resumeCertificationRepository, resumeProjectRepository, meterRegistry
        );
    }

    @Test
    void compileResume_ownedResume_returnsCompiledResponse() {
        Long userId = 100L;
        Long resumeId = 50L;

        ResumeProfile profile = new ResumeProfile(userId, "Java Resume", "Senior Dev", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);
        profile.setSummaryOverride("Override Summary");

        ResumeSection summarySec = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 1, true);
        ResumeSection expSec = new ResumeSection(resumeId, ResumeSectionType.EXPERIENCE, 2, true);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId)).thenReturn(List.of(summarySec, expSec));

        CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);

        assertThat(compiled.getName()).isEqualTo("Java Resume");
        assertThat(compiled.getSections()).hasSize(2);
        assertThat(compiled.getSections().get(0).getSectionType()).isEqualTo(ResumeSectionType.SUMMARY);

        CompiledSummaryResponse sumContent = (CompiledSummaryResponse) compiled.getSections().get(0).getContent();
        assertThat(sumContent.getText()).isEqualTo("Override Summary");
        assertThat(meterRegistry.find("devsphere_resume_compilation_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void compileResume_summaryOverrideAbsent_usesCareerProfileSummary() {
        Long userId = 100L;
        Long resumeId = 50L;

        ResumeProfile profile = new ResumeProfile(userId, "Java Resume", "Senior Dev", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);
        profile.setSummaryOverride(null);

        CareerProfile cp = new CareerProfile(userId);
        cp.setProfessionalSummary("Career Profile Summary");

        ResumeSection summarySec = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 1, true);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId)).thenReturn(List.of(summarySec));
        when(careerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(cp));

        CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);

        CompiledSummaryResponse sumContent = (CompiledSummaryResponse) compiled.getSections().get(0).getContent();
        assertThat(sumContent.getText()).isEqualTo("Career Profile Summary");
    }

    @Test
    void compileResume_invisibleSections_excludedFromCompiledResponse() {
        Long userId = 100L;
        Long resumeId = 50L;

        ResumeProfile profile = new ResumeProfile(userId, "Java Resume", "Senior Dev", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        ResumeSection summarySec = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 1, true);
        ResumeSection eduSec = new ResumeSection(resumeId, ResumeSectionType.EDUCATION, 2, false);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId)).thenReturn(List.of(summarySec, eduSec));

        CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);

        assertThat(compiled.getSections()).hasSize(1);
        assertThat(compiled.getSections().get(0).getSectionType()).isEqualTo(ResumeSectionType.SUMMARY);
    }

    @Test
    void compileResume_missingSourceRecord_skipsMissingRecordWithoutCrash() {
        Long userId = 100L;
        Long resumeId = 50L;
        Long missingExpId = 999L;

        ResumeProfile profile = new ResumeProfile(userId, "Java Resume", "Senior Dev", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        ResumeSection expSec = new ResumeSection(resumeId, ResumeSectionType.EXPERIENCE, 1, true);
        ResumeExperience ref = new ResumeExperience(resumeId, missingExpId, 1);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId)).thenReturn(List.of(expSec));
        when(resumeExperienceRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)).thenReturn(List.of(ref));
        when(experienceRepository.findAllByIdInAndUserId(List.of(missingExpId), userId)).thenReturn(List.of());

        CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);

        assertThat(compiled.getSections()).hasSize(1);
        CompiledResumeSectionResponse section = compiled.getSections().get(0);
        assertThat(section.getSectionType()).isEqualTo(ResumeSectionType.EXPERIENCE);
    }

    @Test
    void compileResume_unownedResume_throwsNotFound() {
        Long userId = 100L;
        Long resumeId = 50L;

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeCompilationService.compileResume(resumeId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");
    }
}
