package com.devsphere.user.service;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicResumeServiceTest {

    @Mock
    private ResumeProfileRepository resumeProfileRepository;

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @Mock
    private ResumeVersionService resumeVersionService;

    private PublicResumeService publicResumeService;

    @BeforeEach
    void setUp() {
        publicResumeService = new PublicResumeService(
                resumeProfileRepository,
                resumeVersionRepository,
                resumeVersionService
        );
    }

    @Test
    void getPublicResume_WhenPublishedVersionExistsAndSharingEnabled_ReturnsPublicResumeResponse() {
        String publicId = "pub-uuid-1234";
        ResumeProfile profile = new ResumeProfile(100L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(10L);
        profile.setPublicId(publicId);
        profile.setPublicEnabled(true);

        ResumeVersion version = new ResumeVersion(10L, 100L, 1, "v1", "{}");
        version.setStatus(ResumeVersionStatus.PUBLISHED);
        version.setId(50L);

        CompiledResumeResponse snapshot = new CompiledResumeResponse(50L, 10L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL, Collections.emptyList());

        when(resumeProfileRepository.findByPublicId(publicId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(10L, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.of(version));
        when(resumeVersionService.compileVersion(10L, 50L, 100L)).thenReturn(snapshot);

        PublicResumeResponse response = publicResumeService.getPublicResume(publicId);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Backend Dev");
        assertThat(response.getTargetRole()).isEqualTo("Senior Java Engineer");
        assertThat(response.getTitle()).isEqualTo("Backend Dev — Senior Java Engineer");
        assertThat(response.getPublicResumeId()).isEqualTo(publicId);
        assertThat(response.getPublishedVersion()).isEqualTo(1);
        assertThat(response.getTemplate()).isEqualTo(ResumeTemplate.PROFESSIONAL);
    }

    @Test
    void getPublicResume_WhenPublicSharingDisabled_ThrowsResourceNotFoundException() {
        String publicId = "pub-uuid-1234";
        ResumeProfile profile = new ResumeProfile(100L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(10L);
        profile.setPublicId(publicId);
        profile.setPublicEnabled(false);

        when(resumeProfileRepository.findByPublicId(publicId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> publicResumeService.getPublicResume(publicId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Public resume not found");

        verifyNoInteractions(resumeVersionService);
    }

    @Test
    void getPublicResume_WhenPublicIdNotFound_ThrowsResourceNotFoundException() {
        when(resumeProfileRepository.findByPublicId("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicResumeService.getPublicResume("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Public resume not found");

        verifyNoInteractions(resumeVersionService);
    }

    @Test
    void getPublicResume_WhenNoPublishedVersion_ThrowsResourceNotFoundException() {
        String publicId = "pub-uuid-1234";
        ResumeProfile profile = new ResumeProfile(100L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(10L);
        profile.setPublicEnabled(true);

        when(resumeProfileRepository.findByPublicId(publicId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(10L, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicResumeService.getPublicResume(publicId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Public resume not found");

        verifyNoInteractions(resumeVersionService);
    }

    @Test
    void getPublicResume_WhenPublicIdNullOrBlank_ThrowsResourceNotFoundException() {
        assertThatThrownBy(() -> publicResumeService.getPublicResume(null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Public resume not found");

        assertThatThrownBy(() -> publicResumeService.getPublicResume("   "))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Public resume not found");

        verifyNoInteractions(resumeProfileRepository, resumeVersionRepository, resumeVersionService);
    }

    @Test
    void getPublicResume_WhenPublishedVersionHasSections_SanitizesInternalIdsFromSections() {
        String publicId = "pub-uuid-5678";
        ResumeProfile profile = new ResumeProfile(100L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(10L);
        profile.setPublicId(publicId);
        profile.setPublicEnabled(true);

        ResumeVersion version = new ResumeVersion(10L, 100L, 1, "v1", "{}");
        version.setStatus(ResumeVersionStatus.PUBLISHED);
        version.setId(50L);

        com.devsphere.user.entity.Experience exp = new com.devsphere.user.entity.Experience(
                100L, "TechCorp", "Lead Architect", com.devsphere.user.entity.EmploymentType.FULL_TIME,
                java.time.LocalDate.of(2020, 1, 1)
        );
        exp.setId(999L); // Internal database ID
        exp.setLocation("San Francisco");
        exp.setCurrentlyWorking(true);
        exp.setDescription("Built scalable platforms");


        com.devsphere.user.dto.compilation.CompiledExperienceResponse compiledExp =
                new com.devsphere.user.dto.compilation.CompiledExperienceResponse(exp, 1);

        com.devsphere.user.dto.compilation.CompiledResumeSectionResponse section =
                new com.devsphere.user.dto.compilation.CompiledResumeSectionResponse(
                        com.devsphere.user.entity.ResumeSectionType.EXPERIENCE, 1, true, java.util.Map.of("items", java.util.List.of(compiledExp))
                );

        CompiledResumeResponse snapshot = new CompiledResumeResponse(50L, 10L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL, java.util.List.of(section));

        when(resumeProfileRepository.findByPublicId(publicId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(10L, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.of(version));
        when(resumeVersionService.compileVersion(10L, 50L, 100L)).thenReturn(snapshot);

        PublicResumeResponse response = publicResumeService.getPublicResume(publicId);

        assertThat(response).isNotNull();
        assertThat(response.getSections()).hasSize(1);
        assertThat(response.getSections().get(0).getSectionType()).isEqualTo(com.devsphere.user.entity.ResumeSectionType.EXPERIENCE);

        Object content = response.getSections().get(0).getContent();
        assertThat(content).isInstanceOf(java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) content;
        assertThat(map).containsKey("items");

        @SuppressWarnings("unchecked")
        java.util.List<com.devsphere.user.dto.publicresume.PublicExperienceResponse> items =
                (java.util.List<com.devsphere.user.dto.publicresume.PublicExperienceResponse>) map.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCompanyName()).isEqualTo("TechCorp");
        assertThat(items.get(0).getJobTitle()).isEqualTo("Lead Architect");
    }

    @Test
    void enablePublicSharing_WhenPublishedVersionExists_EnablesSharing() {
        Long resumeId = 10L;
        Long userId = 100L;
        ResumeProfile profile = new ResumeProfile(userId, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);
        profile.setPublicEnabled(false);

        ResumeVersion publishedVersion = new ResumeVersion(resumeId, userId, 1, "v1", "{}");
        publishedVersion.setStatus(ResumeVersionStatus.PUBLISHED);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(resumeId, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.of(publishedVersion));
        when(resumeProfileRepository.save(profile)).thenAnswer(invocation -> invocation.getArgument(0));

        com.devsphere.user.dto.publicresume.PublicShareStatusResponse status = publicResumeService.enablePublicSharing(resumeId, userId);

        assertThat(status.isPublicEnabled()).isTrue();
        assertThat(status.getPublicEnabledAt()).isNotNull();
        assertThat(status.getShareUrl()).contains("/api/v1/public/resumes/");
    }

    @Test
    void enablePublicSharing_WhenNoPublishedVersion_ThrowsIllegalArgumentException() {
        Long resumeId = 10L;
        Long userId = 100L;
        ResumeProfile profile = new ResumeProfile(userId, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(resumeId, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicResumeService.enablePublicSharing(resumeId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot enable public sharing without an active published resume version");
    }

    @Test
    void revokePublicSharing_RevokesSharing() {
        Long resumeId = 10L;
        Long userId = 100L;
        ResumeProfile profile = new ResumeProfile(userId, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);
        profile.setPublicEnabled(true);
        profile.setPublicEnabledAt(java.time.Instant.now());

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeProfileRepository.save(profile)).thenAnswer(invocation -> invocation.getArgument(0));

        com.devsphere.user.dto.publicresume.PublicShareStatusResponse status = publicResumeService.revokePublicSharing(resumeId, userId);

        assertThat(status.isPublicEnabled()).isFalse();
        assertThat(status.getPublicEnabledAt()).isNull();
    }

    @Test
    void rotatePublicToken_ChangesPublicId() {
        Long resumeId = 10L;
        Long userId = 100L;
        ResumeProfile profile = new ResumeProfile(userId, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);
        String oldPublicId = profile.getPublicId();

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeProfileRepository.save(profile)).thenAnswer(invocation -> invocation.getArgument(0));

        com.devsphere.user.dto.publicresume.PublicShareStatusResponse status = publicResumeService.rotatePublicToken(resumeId, userId);

        assertThat(status.getPublicResumeId()).isNotEqualTo(oldPublicId);
        assertThat(status.getShareUrl()).contains(status.getPublicResumeId());
    }
}


