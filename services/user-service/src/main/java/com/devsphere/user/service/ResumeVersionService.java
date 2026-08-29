package com.devsphere.user.service;

import com.devsphere.user.dto.CreateResumeVersionRequest;
import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import com.devsphere.user.dto.compilation.CompiledEducationResponse;
import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.dto.compilation.CompiledProjectResponse;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.dto.compilation.CompiledResumeSectionResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import com.devsphere.user.dto.compilation.CompiledSummaryResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeVersionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeVersionService.class);

    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeProfileRepository resumeProfileRepository;
    private final ResumeCompilationService resumeCompilationService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public ResumeVersionService(
            ResumeVersionRepository resumeVersionRepository,
            ResumeProfileRepository resumeProfileRepository,
            ResumeCompilationService resumeCompilationService,
            ObjectMapper objectMapper) {
        this(resumeVersionRepository, resumeProfileRepository, resumeCompilationService, objectMapper, new SimpleMeterRegistry());
    }

    @Autowired
    public ResumeVersionService(
            ResumeVersionRepository resumeVersionRepository,
            ResumeProfileRepository resumeProfileRepository,
            ResumeCompilationService resumeCompilationService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.resumeVersionRepository = resumeVersionRepository;
        this.resumeProfileRepository = resumeProfileRepository;
        this.resumeCompilationService = resumeCompilationService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ResumeVersionResponse createVersion(Long resumeId, Long userId, CreateResumeVersionRequest request) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);
        if (compiled == null) {
            throw new IllegalArgumentException("Compiled resume output must not be null");
        }

        try {
            String snapshotJson = objectMapper.writeValueAsString(compiled);
            int nextVersionNumber = resumeVersionRepository.findMaxVersionNumberByResumeProfileId(resumeId).orElse(0) + 1;

            String versionName;
            if (request != null && request.getName() != null && !request.getName().isBlank()) {
                versionName = validateAndSanitizeVersionName(request.getName());
            } else {
                versionName = "Version " + nextVersionNumber;
            }

            ResumeVersion version = new ResumeVersion(resumeId, userId, nextVersionNumber, versionName, snapshotJson);
            ResumeVersion saved = resumeVersionRepository.save(version);

            meterRegistry.counter("devsphere_resume_versions_created_total", "status", "success").increment();
            log.info("Created resume version ID: {} (versionNumber: {}) for resumeId: {} and userId: {}",
                    saved.getId(), saved.getVersionNumber(), resumeId, userId);

            return new ResumeVersionResponse(saved, compiled);
        } catch (DataIntegrityViolationException e) {
            meterRegistry.counter("devsphere_resume_versions_created_total", "status", "failure").increment();
            log.warn("Concurrency conflict creating resume version for resumeId: {}: {}", resumeId, e.getMessage());
            throw new IllegalArgumentException("A version with this version number already exists for this resume profile");
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_versions_created_total", "status", "failure").increment();
            log.error("Failed to create resume version for resumeId: {} and userId: {}", resumeId, userId, e);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Failed to create resume version snapshot", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ResumeVersionResponse> listVersions(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);

        List<ResumeVersion> versions = resumeVersionRepository
                .findAllByResumeProfileIdAndUserIdOrderByVersionNumberDesc(resumeId, userId);

        List<ResumeVersionResponse> responses = new ArrayList<>();
        for (ResumeVersion v : versions) {
            CompiledResumeResponse snapshot = deserializeSnapshot(v.getSnapshotData());
            responses.add(new ResumeVersionResponse(v, snapshot));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public ResumeVersionResponse getVersion(Long resumeId, Long versionId, Long userId) {
        verifyResumeOwnership(resumeId, userId);

        ResumeVersion version = resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(versionId, resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_VERSION_NOT_FOUND", "Resume version not found"));

        CompiledResumeResponse snapshot = deserializeSnapshot(version.getSnapshotData());
        return new ResumeVersionResponse(version, snapshot);
    }

    @Transactional
    public ResumeVersionResponse publishVersion(Long resumeId, Long versionId, Long userId) {
        resumeProfileRepository.findByIdAndUserIdForUpdate(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        ResumeVersion version = resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(versionId, resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_VERSION_NOT_FOUND", "Resume version not found"));

        if (version.getStatus() != ResumeVersionStatus.DRAFT) {
            meterRegistry.counter("devsphere_resume_version_publish_total", "status", "failure", "transition", "publish").increment();
            throw new IllegalArgumentException("Only DRAFT versions can be published. Current version status: " + version.getStatus());
        }

        Optional<ResumeVersion> currentPublishedOpt = resumeVersionRepository.findByResumeProfileIdAndStatus(resumeId, ResumeVersionStatus.PUBLISHED);
        if (currentPublishedOpt.isPresent()) {
            ResumeVersion currentPublished = currentPublishedOpt.get();
            if (!currentPublished.getId().equals(versionId)) {
                currentPublished.setStatus(ResumeVersionStatus.ARCHIVED);
                currentPublished.setArchivedAt(Instant.now());
                resumeVersionRepository.save(currentPublished);
                resumeVersionRepository.flush();
                log.info("Archived previously published resume version ID: {} for resumeId: {}", currentPublished.getId(), resumeId);
            }
        }

        version.setStatus(ResumeVersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());

        ResumeVersion saved = resumeVersionRepository.save(version);
        resumeVersionRepository.flush();

        meterRegistry.counter("devsphere_resume_version_publish_total", "status", "success", "transition", "publish").increment();
        meterRegistry.counter("devsphere_resume_versions_published_total", "status", "success").increment();
        log.info("Published resume version ID: {} (versionNumber: {}) for resumeId: {} and userId: {}",
                saved.getId(), saved.getVersionNumber(), resumeId, userId);

        CompiledResumeResponse snapshot = deserializeSnapshot(saved.getSnapshotData());
        return new ResumeVersionResponse(saved, snapshot);
    }

    @Transactional(readOnly = true)
    public ResumeVersionResponse getPublishedVersion(Long resumeId, Long userId) {
        verifyResumeOwnership(resumeId, userId);

        ResumeVersion publishedVersion = resumeVersionRepository.findByResumeProfileIdAndUserIdAndStatus(resumeId, userId, ResumeVersionStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("PUBLISHED_VERSION_NOT_FOUND", "No published resume version found for this resume profile"));

        CompiledResumeResponse snapshot = deserializeSnapshot(publishedVersion.getSnapshotData());
        return new ResumeVersionResponse(publishedVersion, snapshot);
    }

    @Transactional
    public ResumeVersionResponse archiveVersion(Long resumeId, Long versionId, Long userId) {
        verifyResumeOwnership(resumeId, userId);

        ResumeVersion version = resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(versionId, resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_VERSION_NOT_FOUND", "Resume version not found"));

        if (version.getStatus() == ResumeVersionStatus.ARCHIVED) {
            CompiledResumeResponse snapshot = deserializeSnapshot(version.getSnapshotData());
            return new ResumeVersionResponse(version, snapshot);
        }

        version.setStatus(ResumeVersionStatus.ARCHIVED);
        version.setArchivedAt(Instant.now());

        ResumeVersion saved = resumeVersionRepository.save(version);
        log.info("Archived resume version ID: {} for resumeId: {} and userId: {}", versionId, resumeId, userId);

        CompiledResumeResponse snapshot = deserializeSnapshot(saved.getSnapshotData());
        return new ResumeVersionResponse(saved, snapshot);
    }

    @Transactional(readOnly = true)
    public CompiledResumeResponse compileVersion(Long resumeId, Long versionId, Long userId) {
        verifyResumeOwnership(resumeId, userId);

        ResumeVersion version = resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(versionId, resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_VERSION_NOT_FOUND", "Resume version not found"));

        return deserializeSnapshot(version.getSnapshotData());
    }

    private void verifyResumeOwnership(Long resumeId, Long userId) {
        resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));
    }

    private CompiledResumeResponse deserializeSnapshot(String snapshotJson) {
        try {
            CompiledResumeResponse compiled = objectMapper.readValue(snapshotJson, CompiledResumeResponse.class);
            normalizeSectionContent(compiled);
            return compiled;
        } catch (Exception e) {
            log.error("Failed to deserialize snapshot JSON: {}", e.getMessage(), e);
            throw new IllegalStateException("Corrupted resume version snapshot data", e);
        }
    }

    private void normalizeSectionContent(CompiledResumeResponse compiled) {
        if (compiled == null || compiled.getSections() == null) {
            return;
        }

        for (CompiledResumeSectionResponse section : compiled.getSections()) {
            if (section.getSectionType() == null || section.getContent() == null) {
                continue;
            }

            Object raw = section.getContent();
            switch (section.getSectionType()) {
                case SUMMARY -> {
                    if (raw instanceof Map || raw instanceof String) {
                        section.setContent(objectMapper.convertValue(raw, CompiledSummaryResponse.class));
                    }
                }
                case EXPERIENCE -> {
                    if (raw instanceof Map<?, ?> map && map.containsKey("items")) {
                        Object itemsRaw = map.get("items");
                        List<CompiledExperienceResponse> items = objectMapper.convertValue(
                                itemsRaw, new TypeReference<List<CompiledExperienceResponse>>() {}
                        );
                        section.setContent(Map.of("items", items));
                    }
                }
                case EDUCATION -> {
                    if (raw instanceof Map<?, ?> map && map.containsKey("items")) {
                        Object itemsRaw = map.get("items");
                        List<CompiledEducationResponse> items = objectMapper.convertValue(
                                itemsRaw, new TypeReference<List<CompiledEducationResponse>>() {}
                        );
                        section.setContent(Map.of("items", items));
                    }
                }
                case SKILLS -> {
                    if (raw instanceof Map) {
                        section.setContent(objectMapper.convertValue(raw, CompiledSkillsResponse.class));
                    }
                }
                case CERTIFICATIONS -> {
                    if (raw instanceof Map<?, ?> map && map.containsKey("items")) {
                        Object itemsRaw = map.get("items");
                        List<CompiledCertificationResponse> items = objectMapper.convertValue(
                                itemsRaw, new TypeReference<List<CompiledCertificationResponse>>() {}
                        );
                        section.setContent(Map.of("items", items));
                    }
                }
                case PROJECTS -> {
                    if (raw instanceof Map<?, ?> map && map.containsKey("items")) {
                        Object itemsRaw = map.get("items");
                        List<CompiledProjectResponse> items = objectMapper.convertValue(
                                itemsRaw, new TypeReference<List<CompiledProjectResponse>>() {}
                        );
                        section.setContent(Map.of("items", items));
                    }
                }
            }
        }
    }

    private String validateAndSanitizeVersionName(String rawName) {
        String trimmed = rawName.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Version name cannot exceed 255 characters");
        }

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 32 || c == 127) {
                throw new IllegalArgumentException("Version name contains invalid control characters");
            }
        }

        String sanitized = trimmed.replaceAll("<[^>]*>", "").trim();
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Version name must contain valid text");
        }
        return sanitized;
    }
}
