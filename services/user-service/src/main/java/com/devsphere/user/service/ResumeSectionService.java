package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeSectionResponse;
import com.devsphere.user.dto.UpdateResumeSectionRequest;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeSectionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeSectionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeSectionService.class);

    private final ResumeProfileRepository resumeProfileRepository;
    private final ResumeSectionRepository resumeSectionRepository;
    private final MeterRegistry meterRegistry;

    public ResumeSectionService(ResumeProfileRepository resumeProfileRepository, ResumeSectionRepository resumeSectionRepository) {
        this(resumeProfileRepository, resumeSectionRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public ResumeSectionService(ResumeProfileRepository resumeProfileRepository,
                                ResumeSectionRepository resumeSectionRepository,
                                MeterRegistry meterRegistry) {
        this.resumeProfileRepository = resumeProfileRepository;
        this.resumeSectionRepository = resumeSectionRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<ResumeSectionResponse> listSections(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);
        return resumeSectionRepository.findAllByResumeProfileIdOrderByDisplayOrderAscIdAsc(resumeId)
                .stream()
                .map(ResumeSectionResponse::new)
                .toList();
    }

    @Transactional
    public ResumeSectionResponse updateSection(Long resumeId, Long sectionId, Long userId, UpdateResumeSectionRequest request) {
        verifyResumeOwnership(resumeId, userId);

        ResumeSection section = resumeSectionRepository.findByIdAndResumeProfileId(sectionId, resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_SECTION_NOT_FOUND", "Resume section not found"));

        if (request.getDisplayOrder() != null) {
            if (request.getDisplayOrder() < 1) {
                throw new IllegalArgumentException("displayOrder must be at least 1");
            }
            section.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getVisible() != null) {
            section.setVisible(request.getVisible());
        }

        ResumeSection updated = resumeSectionRepository.save(section);
        meterRegistry.counter("devsphere_resume_section_updated_total").increment();
        log.info("Updated section ID: {} on resumeId: {}", sectionId, resumeId);

        return new ResumeSectionResponse(updated);
    }

    private void verifyResumeOwnership(Long resumeId, Long userId) {
        if (!resumeProfileRepository.findByIdAndUserId(resumeId, userId).isPresent()) {
            throw new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found");
        }
    }
}
