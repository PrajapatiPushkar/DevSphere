package com.devsphere.user.service;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
    private final MeterRegistry meterRegistry;

    public PublicResumeService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeVersionRepository resumeVersionRepository,
            ResumeVersionService resumeVersionService) {
        this(resumeProfileRepository, resumeVersionRepository, resumeVersionService, new SimpleMeterRegistry());
    }

    @Autowired
    public PublicResumeService(
            ResumeProfileRepository resumeProfileRepository,
            ResumeVersionRepository resumeVersionRepository,
            ResumeVersionService resumeVersionService,
            MeterRegistry meterRegistry) {
        this.resumeProfileRepository = resumeProfileRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.resumeVersionService = resumeVersionService;
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

            ResumeProfile profile = resumeProfileRepository.findByPublicId(publicResumeId)
                    .orElseThrow(() -> {
                        meterRegistry.counter("devsphere_public_resume_access_total", "status", "not_found").increment();
                        return new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found");
                    });

            ResumeVersion publishedVersion = resumeVersionRepository.findByResumeProfileIdAndStatus(profile.getId(), ResumeVersionStatus.PUBLISHED)
                    .orElseThrow(() -> {
                        meterRegistry.counter("devsphere_public_resume_access_total", "status", "not_found").increment();
                        return new ResourceNotFoundException("PUBLIC_RESUME_NOT_FOUND", "Public resume not found");
                    });

            CompiledResumeResponse snapshot = resumeVersionService.compileVersion(profile.getId(), publishedVersion.getId(), profile.getUserId());
            meterRegistry.counter("devsphere_public_resume_access_total", "status", "success").increment();
            log.info("Successfully resolved public resume for publicId: {} (profileId: {}, versionNumber: {})",
                    publicResumeId, profile.getId(), publishedVersion.getVersionNumber());

            return new PublicResumeResponse(snapshot);
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
}

