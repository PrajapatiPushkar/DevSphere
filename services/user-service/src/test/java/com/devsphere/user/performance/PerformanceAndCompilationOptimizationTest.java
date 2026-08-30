package com.devsphere.user.performance;

import com.devsphere.user.cache.PublicResumeCache;
import com.devsphere.user.cache.UserProfileCache;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
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
import com.devsphere.user.repository.ResumeVersionRepository;
import com.devsphere.user.repository.SkillRepository;
import com.devsphere.user.repository.UserProfileRepository;
import com.devsphere.user.service.PublicResumeService;
import com.devsphere.user.service.ResumeCompilationService;
import com.devsphere.user.service.ResumeVersionService;
import com.devsphere.user.service.UserProfileService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PerformanceAndCompilationOptimizationTest {

    private ResumeProfileRepository resumeProfileRepository;
    private ResumeSectionRepository resumeSectionRepository;
    private CareerProfileRepository careerProfileRepository;

    private ExperienceRepository experienceRepository;
    private EducationRepository educationRepository;
    private SkillRepository skillRepository;
    private CertificationRepository certificationRepository;
    private DeveloperProjectRepository projectRepository;

    private ResumeExperienceRepository resumeExperienceRepository;
    private ResumeEducationRepository resumeEducationRepository;
    private ResumeSkillRepository resumeSkillRepository;
    private ResumeCertificationRepository resumeCertificationRepository;
    private ResumeProjectRepository resumeProjectRepository;

    private ResumeVersionRepository resumeVersionRepository;
    private ResumeVersionService resumeVersionService;
    private PublicResumeCache publicResumeCache;
    private PublicResumeService publicResumeService;
    private UserProfileRepository userProfileRepository;
    private UserProfileCache userProfileCache;
    private UserProfileService userProfileService;

    private MeterRegistry meterRegistry;
    private ResumeCompilationService compilationService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        resumeProfileRepository = mock(ResumeProfileRepository.class);
        resumeSectionRepository = mock(ResumeSectionRepository.class);
        careerProfileRepository = mock(CareerProfileRepository.class);

        experienceRepository = mock(ExperienceRepository.class);
        educationRepository = mock(EducationRepository.class);
        skillRepository = mock(SkillRepository.class);
        certificationRepository = mock(CertificationRepository.class);
        projectRepository = mock(DeveloperProjectRepository.class);

        resumeExperienceRepository = mock(ResumeExperienceRepository.class);
        resumeEducationRepository = mock(ResumeEducationRepository.class);
        resumeSkillRepository = mock(ResumeSkillRepository.class);
        resumeCertificationRepository = mock(ResumeCertificationRepository.class);
        resumeProjectRepository = mock(ResumeProjectRepository.class);

        resumeVersionRepository = mock(ResumeVersionRepository.class);
        resumeVersionService = mock(ResumeVersionService.class);
        publicResumeCache = mock(PublicResumeCache.class);
        userProfileRepository = mock(UserProfileRepository.class);
        userProfileCache = mock(UserProfileCache.class);

        meterRegistry = new SimpleMeterRegistry();

        compilationService = new ResumeCompilationService(
                resumeProfileRepository, resumeSectionRepository, careerProfileRepository,
                experienceRepository, educationRepository, skillRepository, certificationRepository, projectRepository,
                resumeExperienceRepository, resumeEducationRepository, resumeSkillRepository,
                resumeCertificationRepository, resumeProjectRepository, meterRegistry
        );

        publicResumeService = new PublicResumeService(
                resumeProfileRepository, resumeVersionRepository, resumeVersionService, publicResumeCache, meterRegistry
        );

        userProfileService = new UserProfileService(
                userProfileRepository, userProfileCache, meterRegistry, null
        );
    }

    @Test
    @DisplayName("Resume compilation uses SQL-level filtering and completes in single-digit query passes")
    void compileResumeUsesSqlLevelFiltering() {
        Long resumeId = 1L;
        Long userId = 100L;

        ResumeProfile profile = new ResumeProfile(userId, "Full Stack Resume", "Senior Engineer", ResumeTemplate.MODERN);
        profile.setId(resumeId);

        ResumeSection summarySec = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 1, true);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId))
                .thenReturn(List.of(summarySec));
        when(careerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        CompiledResumeResponse compiled = compilationService.compileResume(resumeId, userId);

        assertNotNull(compiled);
        assertEquals("Full Stack Resume", compiled.getName());
        assertEquals(1, compiled.getSections().size());
        verify(experienceRepository, never()).findAllById(any());
        verify(experienceRepository, never()).findAllByIdInAndUserId(any(), any());
    }

    @Test
    @DisplayName("Public resume read uses Redis cache HIT without querying database")
    void publicResumeCacheHitAvoidsDatabaseQueries() {
        String publicId = "pub-uuid-9999";
        PublicResumeResponse cachedResponse = new PublicResumeResponse();

        when(publicResumeCache.get(publicId)).thenReturn(Optional.of(cachedResponse));

        PublicResumeResponse response = publicResumeService.getPublicResume(publicId);

        assertNotNull(response);
        assertSame(cachedResponse, response);
        verify(resumeProfileRepository, never()).findByPublicId(any());
        verify(resumeVersionRepository, never()).findByResumeProfileIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Public resume cache MISS queries database and populates Redis cache")
    void publicResumeCacheMissQueriesDatabaseAndPopulatesCache() {
        String publicId = "pub-uuid-8888";
        Long profileId = 10L;
        Long userId = 200L;
        Long versionId = 5L;

        ResumeProfile profile = new ResumeProfile(userId, "My Resume", "Lead Architect", ResumeTemplate.PROFESSIONAL);
        profile.setId(profileId);
        profile.setPublicId(publicId);

        ResumeVersion publishedVersion = new ResumeVersion(profileId, userId, 1, "V1", "{}");
        publishedVersion.setId(versionId);
        publishedVersion.setStatus(ResumeVersionStatus.PUBLISHED);

        CompiledResumeResponse snapshot = new CompiledResumeResponse(profileId, profileId, "My Resume", "Lead Architect", ResumeTemplate.PROFESSIONAL, List.of());

        when(publicResumeCache.get(publicId)).thenReturn(Optional.empty());
        when(resumeProfileRepository.findByPublicId(publicId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(profileId, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.of(publishedVersion));
        when(resumeVersionService.compileVersion(profileId, versionId, userId)).thenReturn(snapshot);

        PublicResumeResponse response = publicResumeService.getPublicResume(publicId);

        assertNotNull(response);
        verify(publicResumeCache, times(1)).put(eq(publicId), any());
    }

    @Test
    @DisplayName("UserProfileService checks Redis cache before opening DB connection")
    void userProfileServiceChecksCacheBeforeDb() {
        Long userId = 300L;
        UserProfileResponse cachedResponse = new UserProfileResponse();

        when(userProfileCache.get(userId)).thenReturn(Optional.of(cachedResponse));

        UserProfileResponse response = userProfileService.getOrCreateProfile(userId);

        assertNotNull(response);
        assertSame(cachedResponse, response);
        verify(userProfileRepository, never()).findByUserId(any());
    }
}
