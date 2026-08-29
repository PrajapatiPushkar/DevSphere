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
                resumeVersionService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void getPublicResume_WhenPublishedVersionExists_ReturnsPublicResumeResponse() {
        String publicId = "pub-uuid-1234";
        ResumeProfile profile = new ResumeProfile(100L, "Backend Dev", "Senior Java Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(10L);
        profile.setPublicId(publicId);

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
        assertThat(response.getTemplate()).isEqualTo(ResumeTemplate.PROFESSIONAL);
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

        when(resumeProfileRepository.findByPublicId(publicId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(10L, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicResumeService.getPublicResume(publicId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Public resume not found");

        verifyNoInteractions(resumeVersionService);
    }
}
