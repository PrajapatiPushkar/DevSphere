package com.devsphere.user.service;

import com.devsphere.user.cache.PublicResumeCache;
import com.devsphere.user.cache.TransactionAwareCacheInvalidator;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.dto.publicresume.PublicShareStatusResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicResumeService {

    private static final Logger log = LoggerFactory.getLogger(PublicResumeService.class);

    private final ResumeProfileRepository resumeProfileRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeVersionService resumeVersionService;
    private final PublicResumeCache publicResumeCache;
    private final MeterRegistry meterRegistry;

    public PublicResumeService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeVersionRepository resumeVersionRepository,
            ResumeVersionService resumeVersionService) {
        this(resumeProfileRepository, resumeVersionRepository, resumeVersionService, null, new SimpleMeterRegistry());
    }

    public PublicResumeService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeVersionRepository resumeVersionRepository,
            ResumeVersionService resumeVersionService,
            PublicResumeCache publicResumeCache) {
        this(resumeProfileRepository, resumeVersionRepository, resumeVersionService, publicResumeCache, new SimpleMeterRegistry());
    }

    @Autowired(required = false)
    public PublicResumeService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeVersionRepository resumeVersionRepository,
            ResumeVersionService resumeVersionService,
            PublicResumeCache publicResumeCache,
            MeterRegistry meterRegistry) {
        this.resumeProfileRepository = resumeProfileRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.resumeVersionService = resumeVersionService;
        this.publicResumeCache = publicResumeCache;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Transactional(readOnly = true)
    public PublicResumeResponse getPublicResume(String publicResumeId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            if (publicResumeId == null || publicResumeId.isBlank()) {
                meterRegistry.counter("devsphere_public_resume_access_total", "status", "not_found").increment();
                throw new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found");
            }

            if (publicResumeCache != null) {
                Optional<PublicResumeResponse> cached = publicResumeCache.get(publicResumeId);
                if (cached.isPresent()) {
                    meterRegistry.counter("devsphere_public_resume_access_total", "status", "success").increment();
                    return cached.get();
                }
            }

            ResumeProfile profile = resumeProfileRepository.findByPublicId(publicResumeId)
                    .orElseThrow(() -> {
                        meterRegistry.counter("devsphere_public_resume_access_total", "status", "not_found").increment();
                        return new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found");
                    });

            if (!profile.isPublicEnabled() || profile.getStatus() == ResumeStatus.ARCHIVED) {
                meterRegistry.counter("devsphere_public_resume_access_total", "status", "not_found").increment();
                throw new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found");
            }

            ResumeVersion publishedVersion = resumeVersionRepository.findByResumeProfileIdAndStatus(profile.getId(), ResumeVersionStatus.PUBLISHED)
                    .orElseThrow(() -> {
                        meterRegistry.counter("devsphere_public_resume_access_total", "status", "not_found").increment();
                        return new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found");
                    });

            CompiledResumeResponse snapshot = resumeVersionService.compileVersion(profile.getId(), publishedVersion.getId(), profile.getUserId());
            PublicResumeResponse response = new PublicResumeResponse(snapshot);

            if (publicResumeCache != null) {
                publicResumeCache.put(publicResumeId, response);
            }

            meterRegistry.counter("devsphere_public_resume_access_total", "status", "success").increment();
            log.info("Successfully resolved public resume for publicId: {} (profileId: {}, versionNumber: {})",
                    publicResumeId, profile.getId(), publishedVersion.getVersionNumber());

            return response;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            meterRegistry.counter("devsphere_public_resume_access_total", "status", "failure").increment();
            log.error("Error resolving public resume for publicId: {}", publicResumeId, e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("devsphere_public_resume_access_duration"));
        }
    }

    @Transactional
    public PublicShareStatusResponse enablePublicSharing(Long resumeId, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        if (profile.getStatus() == ResumeStatus.ARCHIVED) {
            throw new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found");
        }

        resumeVersionRepository.findByResumeProfileIdAndStatus(resumeId, ResumeVersionStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Cannot enable public sharing without an active published resume version"));

        profile.setPublicEnabled(true);
        profile.setPublicEnabledAt(Instant.now());
        ResumeProfile saved = resumeProfileRepository.save(profile);

        if (publicResumeCache != null && saved.getPublicId() != null) {
            String publicId = saved.getPublicId();
            TransactionAwareCacheInvalidator.executeAfterCommit(() -> publicResumeCache.evict(publicId));
        }

        meterRegistry.counter("devsphere_public_resume_sharing_total", "action", "enable").increment();
        log.info("Public resume sharing enabled for resumeId: {} (publicId: {})", resumeId, saved.getPublicId());

        return new PublicShareStatusResponse(saved);
    }

    @Transactional
    public PublicShareStatusResponse revokePublicSharing(Long resumeId, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        String publicId = profile.getPublicId();
        profile.setPublicEnabled(false);
        profile.setPublicEnabledAt(null);
        ResumeProfile saved = resumeProfileRepository.save(profile);

        if (publicResumeCache != null && publicId != null) {
            TransactionAwareCacheInvalidator.executeAfterCommit(() -> publicResumeCache.evict(publicId));
        }

        meterRegistry.counter("devsphere_public_resume_sharing_total", "action", "revoke").increment();
        log.info("Public resume sharing revoked for resumeId: {} (publicId: {})", resumeId, publicId);

        return new PublicShareStatusResponse(saved);
    }

    @Transactional(readOnly = true)
    public PublicShareStatusResponse getPublicSharingStatus(Long resumeId, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        return new PublicShareStatusResponse(profile);
    }

    @Transactional
    public PublicShareStatusResponse rotatePublicToken(Long resumeId, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        String oldPublicId = profile.getPublicId();
        String newPublicId = UUID.randomUUID().toString();
        profile.setPublicId(newPublicId);
        ResumeProfile saved = resumeProfileRepository.save(profile);

        if (publicResumeCache != null) {
            TransactionAwareCacheInvalidator.executeAfterCommit(() -> {
                if (oldPublicId != null) {
                    publicResumeCache.evict(oldPublicId);
                }
                publicResumeCache.evict(newPublicId);
            });
        }

        meterRegistry.counter("devsphere_public_resume_sharing_total", "action", "rotate").increment();
        log.info("Public resume token rotated for resumeId: {}: oldPublicId={}, newPublicId={}", resumeId, oldPublicId, newPublicId);

        return new PublicShareStatusResponse(saved);
    }
}


