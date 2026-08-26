package com.devsphere.user.service;

import com.devsphere.user.dto.EducationRequest;
import com.devsphere.user.dto.EducationResponse;
import com.devsphere.user.entity.Education;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.EducationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationService {

    private static final Logger log = LoggerFactory.getLogger(EducationService.class);

    private final EducationRepository educationRepository;
    private final MeterRegistry meterRegistry;

    public EducationService(EducationRepository educationRepository) {
        this(educationRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public EducationService(EducationRepository educationRepository, MeterRegistry meterRegistry) {
        this.educationRepository = educationRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public EducationResponse createEducation(Long userId, EducationRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        validateEducationDates(request);

        Education education = new Education(
                userId,
                request.getInstitutionName().trim(),
                request.getDegree().trim(),
                request.getStartDate()
        );
        education.setFieldOfStudy(trimToNull(request.getFieldOfStudy()));
        education.setLocation(trimToNull(request.getLocation()));
        education.setCurrentlyStudying(request.getCurrentlyStudying() != null ? request.getCurrentlyStudying() : false);
        education.setEndDate(Boolean.TRUE.equals(request.getCurrentlyStudying()) ? null : request.getEndDate());
        education.setDescription(trimToNull(request.getDescription()));
        education.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        Education saved = educationRepository.save(education);
        meterRegistry.counter("devsphere_education_created_total").increment();
        log.info("Created education ID: {} for userId: {}", saved.getId(), userId);

        return new EducationResponse(saved);
    }

    @Transactional(readOnly = true)
    public EducationResponse getEducation(Long id, Long userId) {
        Education education = educationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("EDUCATION_NOT_FOUND", "Education record not found"));
        return new EducationResponse(education);
    }

    @Transactional(readOnly = true)
    public List<EducationResponse> listEducations(Long userId) {
        return educationRepository.findAllByUserIdOrderByDisplayOrderAscStartDateDesc(userId)
                .stream()
                .map(EducationResponse::new)
                .toList();
    }

    @Transactional
    public EducationResponse updateEducation(Long id, Long userId, EducationRequest request) {
        Education education = educationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("EDUCATION_NOT_FOUND", "Education record not found"));

        validateEducationDates(request);

        education.setInstitutionName(request.getInstitutionName().trim());
        education.setDegree(request.getDegree().trim());
        education.setFieldOfStudy(trimToNull(request.getFieldOfStudy()));
        education.setLocation(trimToNull(request.getLocation()));
        education.setStartDate(request.getStartDate());
        education.setCurrentlyStudying(request.getCurrentlyStudying() != null ? request.getCurrentlyStudying() : false);
        education.setEndDate(Boolean.TRUE.equals(request.getCurrentlyStudying()) ? null : request.getEndDate());
        education.setDescription(trimToNull(request.getDescription()));
        if (request.getDisplayOrder() != null) {
            education.setDisplayOrder(request.getDisplayOrder());
        }

        Education updated = educationRepository.save(education);
        meterRegistry.counter("devsphere_education_updated_total").increment();
        log.info("Updated education ID: {} for userId: {}", updated.getId(), userId);

        return new EducationResponse(updated);
    }

    @Transactional
    public void deleteEducation(Long id, Long userId) {
        Education education = educationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("EDUCATION_NOT_FOUND", "Education record not found"));

        educationRepository.delete(education);
        meterRegistry.counter("devsphere_education_deleted_total").increment();
        log.info("Deleted education ID: {} for userId: {}", id, userId);
    }

    private void validateEducationDates(EducationRequest request) {
        boolean currentlyStudying = Boolean.TRUE.equals(request.getCurrentlyStudying());
        if (currentlyStudying && request.getEndDate() != null) {
            throw new IllegalArgumentException("endDate must be null when currentlyStudying is true");
        }
        if (!currentlyStudying && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
        }
    }

    private String trimToNull(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }
}
