package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
import com.devsphere.user.entity.ResumeStatus;
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

import com.devsphere.user.cache.PublicResumeCache;
import com.devsphere.user.cache.TransactionAwareCacheInvalidator;

@Service
public class ResumeProfileService {

    private static final Logger log = LoggerFactory.getLogger(ResumeProfileService.class);

    private final ResumeProfileRepository resumeProfileRepository;
    private final ResumeSectionRepository resumeSectionRepository;
    private final PublicResumeCache publicResumeCache;
    private final MeterRegistry meterRegistry;

    public ResumeProfileService(ResumeProfileRepository resumeProfileRepository, ResumeSectionRepository resumeSectionRepository) {
        this(resumeProfileRepository, resumeSectionRepository, null, new SimpleMeterRegistry());
    }

    public ResumeProfileService(ResumeProfileRepository resumeProfileRepository, ResumeSectionRepository resumeSectionRepository, PublicResumeCache publicResumeCache) {
        this(resumeProfileRepository, resumeSectionRepository, publicResumeCache, new SimpleMeterRegistry());
    }

    @Autowired(required = false)
    public ResumeProfileService(ResumeProfileRepository resumeProfileRepository,
                                ResumeSectionRepository resumeSectionRepository,
                                PublicResumeCache publicResumeCache,
                                MeterRegistry meterRegistry) {
        this.resumeProfileRepository = resumeProfileRepository;
        this.resumeSectionRepository = resumeSectionRepository;
        this.publicResumeCache = publicResumeCache;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Transactional
    public ResumeProfileResponse createResumeProfile(Long userId, ResumeProfileRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        ResumeProfile profile = new ResumeProfile(
                userId,
                request.getName().trim(),
                request.getTargetRole().trim(),
                request.getTemplate()
        );
        profile.setSummaryOverride(trimToNull(request.getSummaryOverride()));
        profile.setStatus(ResumeStatus.DRAFT);

        ResumeProfile saved = resumeProfileRepository.save(profile);

        // Populate default 6 sections
        ResumeSectionType[] defaultSections = ResumeSectionType.values();
        for (int i = 0; i < defaultSections.length; i++) {
            ResumeSection section = new ResumeSection(saved.getId(), defaultSections[i], i + 1, true);
            resumeSectionRepository.save(section);
        }

        meterRegistry.counter("devsphere_resume_created_total").increment();
        log.info("Created resume profile ID: {} for userId: {}", saved.getId(), userId);

        return new ResumeProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResumeProfileResponse getResumeProfile(Long id, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));
        return new ResumeProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<ResumeProfileResponse> listResumeProfiles(Long userId) {
        return resumeProfileRepository.findAllByUserIdOrderByIdDesc(userId)
                .stream()
                .map(ResumeProfileResponse::new)
                .toList();
    }

    @Transactional
    public ResumeProfileResponse updateResumeProfile(Long id, Long userId, ResumeProfileRequest request) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        profile.setName(request.getName().trim());
        profile.setTargetRole(request.getTargetRole().trim());
        profile.setSummaryOverride(trimToNull(request.getSummaryOverride()));
        profile.setTemplate(request.getTemplate());

        ResumeProfile updated = resumeProfileRepository.save(profile);
        meterRegistry.counter("devsphere_resume_updated_total").increment();
        log.info("Updated resume profile ID: {} for userId: {}", updated.getId(), userId);

        return new ResumeProfileResponse(updated);
    }

    @Transactional
    public ResumeProfileResponse archiveResumeProfile(Long id, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        profile.setStatus(ResumeStatus.ARCHIVED);
        ResumeProfile updated = resumeProfileRepository.save(profile);

        if (publicResumeCache != null && profile.getPublicId() != null) {
            String publicId = profile.getPublicId();
            TransactionAwareCacheInvalidator.executeAfterCommit(() -> publicResumeCache.evict(publicId));
        }

        meterRegistry.counter("devsphere_resume_archived_total").increment();
        log.info("Archived resume profile ID: {} for userId: {}", updated.getId(), userId);

        return new ResumeProfileResponse(updated);
    }

    @Transactional
    public void deleteResumeProfile(Long id, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        profile.setStatus(ResumeStatus.ARCHIVED);
        resumeProfileRepository.save(profile);

        if (publicResumeCache != null && profile.getPublicId() != null) {
            String publicId = profile.getPublicId();
            TransactionAwareCacheInvalidator.executeAfterCommit(() -> publicResumeCache.evict(publicId));
        }

        meterRegistry.counter("devsphere_resume_deleted_total").increment();
        log.info("Logically archived/deleted resume profile ID: {} for userId: {}", id, userId);
    }

    @Transactional
    public ResumeProfileResponse activateResumeProfile(Long id, Long userId) {
        ResumeProfile targetProfile = resumeProfileRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        // Atomically archive any currently ACTIVE resume for this user
        List<ResumeProfile> activeResumes = resumeProfileRepository.findAllByUserIdAndStatus(userId, ResumeStatus.ACTIVE);
        for (ResumeProfile active : activeResumes) {
            if (!active.getId().equals(id)) {
                active.setStatus(ResumeStatus.ARCHIVED);
                resumeProfileRepository.save(active);
                log.info("Deactivated previous active resume profile ID: {} for userId: {}", active.getId(), userId);
            }
        }

        targetProfile.setStatus(ResumeStatus.ACTIVE);
        ResumeProfile activated = resumeProfileRepository.save(targetProfile);
        meterRegistry.counter("devsphere_resume_activated_total").increment();
        log.info("Activated resume profile ID: {} for userId: {}", activated.getId(), userId);

        return new ResumeProfileResponse(activated);
    }

    private String trimToNull(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }
}
