package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeSectionResponse;
import com.devsphere.user.dto.UpdateResumeSectionRequest;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeSectionServiceTest {

    @Mock
    private ResumeProfileRepository resumeProfileRepository;

    @Mock
    private ResumeSectionRepository resumeSectionRepository;

    private SimpleMeterRegistry meterRegistry;
    private ResumeSectionService resumeSectionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        resumeSectionService = new ResumeSectionService(resumeProfileRepository, resumeSectionRepository, meterRegistry);
    }

    @Test
    void listSections_returnsSectionsForOwnedResume() {
        Long userId = 100L;
        Long resumeId = 50L;
        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        ResumeSection section = new ResumeSection(resumeId, ResumeSectionType.SUMMARY, 1, true);
        section.setId(100L);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAsc(resumeId)).thenReturn(List.of(section));

        List<ResumeSectionResponse> list = resumeSectionService.listSections(resumeId, userId);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getSectionType()).isEqualTo(ResumeSectionType.SUMMARY);
    }

    @Test
    void updateSection_updatesDisplayOrderAndVisibility() {
        Long userId = 100L;
        Long resumeId = 50L;
        Long sectionId = 100L;

        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);

        ResumeSection section = new ResumeSection(resumeId, ResumeSectionType.EXPERIENCE, 2, true);
        section.setId(sectionId);

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeSectionRepository.findByIdAndResumeProfileId(sectionId, resumeId)).thenReturn(Optional.of(section));
        when(resumeSectionRepository.save(any(ResumeSection.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateResumeSectionRequest request = new UpdateResumeSectionRequest(1, false);

        ResumeSectionResponse response = resumeSectionService.updateSection(resumeId, sectionId, userId, request);

        assertThat(response.getDisplayOrder()).isEqualTo(1);
        assertThat(response.getVisible()).isFalse();
        assertThat(meterRegistry.find("devsphere_resume_section_updated_total").counter().count()).isEqualTo(1.0);
    }
}
