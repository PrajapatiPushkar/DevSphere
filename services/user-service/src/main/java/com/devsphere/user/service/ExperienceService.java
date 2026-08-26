package com.devsphere.user.service;

import com.devsphere.user.dto.ExperienceRequest;
import com.devsphere.user.dto.ExperienceResponse;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ExperienceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {

    private static final Logger log = LoggerFactory.getLogger(ExperienceService.class);

    private final ExperienceRepository experienceRepository;
    private final MeterRegistry meterRegistry;

    public ExperienceService(ExperienceRepository experienceRepository) {
        this(experienceRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public ExperienceService(ExperienceRepository experienceRepository, MeterRegistry meterRegistry) {
        this.experienceRepository = experienceRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ExperienceResponse createExperience(Long userId, ExperienceRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        validateExperienceDates(request);

        Experience experience = new Experience(
                userId,
                request.getCompanyName().trim(),
                request.getJobTitle().trim(),
                request.getEmploymentType(),
                request.getStartDate()
        );
        experience.setLocation(trimToNull(request.getLocation()));
        experience.setCurrentlyWorking(request.getCurrentlyWorking() != null ? request.getCurrentlyWorking() : false);
        experience.setEndDate(Boolean.TRUE.equals(request.getCurrentlyWorking()) ? null : request.getEndDate());
        experience.setDescription(trimToNull(request.getDescription()));
        experience.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        Experience saved = experienceRepository.save(experience);
        meterRegistry.counter("devsphere_experience_created_total").increment();
        log.info("Created experience ID: {} for userId: {}", saved.getId(), userId);

        return new ExperienceResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExperienceResponse getExperience(Long id, Long userId) {
        Experience experience = experienceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("EXPERIENCE_NOT_FOUND", "Experience record not found"));
        return new ExperienceResponse(experience);
    }

    @Transactional(readOnly = true)
    public List<ExperienceResponse> listExperiences(Long userId) {
        return experienceRepository.findAllByUserIdOrderByDisplayOrderAscStartDateDesc(userId)
                .stream()
                .map(ExperienceResponse::new)
                .toList();
    }

    @Transactional
    public ExperienceResponse updateExperience(Long id, Long userId, ExperienceRequest request) {
        Experience experience = experienceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("EXPERIENCE_NOT_FOUND", "Experience record not found"));

        validateExperienceDates(request);

        experience.setCompanyName(request.getCompanyName().trim());
        experience.setJobTitle(request.getJobTitle().trim());
        experience.setEmploymentType(request.getEmploymentType());
        experience.setLocation(trimToNull(request.getLocation()));
        experience.setStartDate(request.getStartDate());
        experience.setCurrentlyWorking(request.getCurrentlyWorking() != null ? request.getCurrentlyWorking() : false);
        experience.setEndDate(Boolean.TRUE.equals(request.getCurrentlyWorking()) ? null : request.getEndDate());
        experience.setDescription(trimToNull(request.getDescription()));
        if (request.getDisplayOrder() != null) {
            experience.setDisplayOrder(request.getDisplayOrder());
        }

        Experience updated = experienceRepository.save(experience);
        meterRegistry.counter("devsphere_experience_updated_total").increment();
        log.info("Updated experience ID: {} for userId: {}", updated.getId(), userId);

        return new ExperienceResponse(updated);
    }

    @Transactional
    public void deleteExperience(Long id, Long userId) {
        Experience experience = experienceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("EXPERIENCE_NOT_FOUND", "Experience record not found"));

        experienceRepository.delete(experience);
        meterRegistry.counter("devsphere_experience_deleted_total").increment();
        log.info("Deleted experience ID: {} for userId: {}", id, userId);
    }

    private void validateExperienceDates(ExperienceRequest request) {
        boolean currentlyWorking = Boolean.TRUE.equals(request.getCurrentlyWorking());
        if (currentlyWorking && request.getEndDate() != null) {
            throw new IllegalArgumentException("endDate must be null when currentlyWorking is true");
        }
        if (!currentlyWorking && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
        }
    }

    private String trimToNull(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }
}
