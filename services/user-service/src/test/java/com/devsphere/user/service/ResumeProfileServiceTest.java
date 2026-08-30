package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeSectionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeProfileServiceTest {

    @Mock
    private ResumeProfileRepository resumeProfileRepository;

    @Mock
    private ResumeSectionRepository resumeSectionRepository;

    private SimpleMeterRegistry meterRegistry;
    private ResumeProfileService resumeProfileService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        resumeProfileService = new ResumeProfileService(resumeProfileRepository, resumeSectionRepository, null, meterRegistry);
    }

    @Test
    void createResumeProfile_createsProfileAndPopulates6DefaultSections() {
        Long userId = 100L;
        ResumeProfileRequest request = new ResumeProfileRequest("Java Resume", "Senior Backend Eng", "Summary text", ResumeTemplate.PROFESSIONAL);

        when(resumeProfileRepository.save(any(ResumeProfile.class))).thenAnswer(inv -> {
            ResumeProfile rp = inv.getArgument(0);
            rp.setId(50L);
            return rp;
        });

        ResumeProfileResponse response = resumeProfileService.createResumeProfile(userId, request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo(ResumeStatus.DRAFT);
        assertThat(response.getTemplate()).isEqualTo(ResumeTemplate.PROFESSIONAL);

        verify(resumeSectionRepository, times(6)).save(any(ResumeSection.class));
        assertThat(meterRegistry.find("devsphere_resume_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void activateResumeProfile_archivesPreviousActiveResumeAndActivatesTarget() {
        Long userId = 100L;
        Long oldActiveId = 50L;
        Long targetId = 51L;

        ResumeProfile oldActive = new ResumeProfile(userId, "Old Active", "Role", ResumeTemplate.MODERN);
        oldActive.setId(oldActiveId);
        oldActive.setStatus(ResumeStatus.ACTIVE);

        ResumeProfile target = new ResumeProfile(userId, "Target Resume", "Role", ResumeTemplate.PROFESSIONAL);
        target.setId(targetId);
        target.setStatus(ResumeStatus.DRAFT);

        when(resumeProfileRepository.findByIdAndUserId(targetId, userId)).thenReturn(Optional.of(target));
        when(resumeProfileRepository.findAllByUserIdAndStatus(userId, ResumeStatus.ACTIVE)).thenReturn(List.of(oldActive));
        when(resumeProfileRepository.save(any(ResumeProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeProfileResponse response = resumeProfileService.activateResumeProfile(targetId, userId);

        assertThat(response.getStatus()).isEqualTo(ResumeStatus.ACTIVE);
        assertThat(oldActive.getStatus()).isEqualTo(ResumeStatus.ARCHIVED);
        assertThat(meterRegistry.find("devsphere_resume_activated_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void archiveResumeProfile_setsStatusToArchived() {
        Long userId = 100L;
        Long id = 50L;
        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.MINIMAL);
        profile.setId(id);
        profile.setStatus(ResumeStatus.DRAFT);

        when(resumeProfileRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(profile));
        when(resumeProfileRepository.save(any(ResumeProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeProfileResponse response = resumeProfileService.archiveResumeProfile(id, userId);

        assertThat(response.getStatus()).isEqualTo(ResumeStatus.ARCHIVED);
        assertThat(meterRegistry.find("devsphere_resume_archived_total").counter().count()).isEqualTo(1.0);
    }
}
