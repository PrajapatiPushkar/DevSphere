package com.devsphere.user.service;

import com.devsphere.user.dto.CareerProfileRequest;
import com.devsphere.user.dto.CareerProfileResponse;
import com.devsphere.user.entity.CareerProfile;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CareerProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerProfileService {

    private static final Logger log = LoggerFactory.getLogger(CareerProfileService.class);

    private final CareerProfileRepository careerProfileRepository;
    private final MeterRegistry meterRegistry;

    public CareerProfileService(CareerProfileRepository careerProfileRepository) {
        this(careerProfileRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public CareerProfileService(CareerProfileRepository careerProfileRepository, MeterRegistry meterRegistry) {
        this.careerProfileRepository = careerProfileRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public CareerProfileResponse getCareerProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        CareerProfile profile = careerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CAREER_PROFILE_NOT_FOUND", "Career profile not found for user"));
        return new CareerProfileResponse(profile);
    }

    @Transactional
    public CareerProfileResponse upsertCareerProfile(Long userId, CareerProfileRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        if (request.getYearsOfExperience() != null) {
            if (request.getYearsOfExperience() < 0) {
                throw new IllegalArgumentException("yearsOfExperience must not be negative");
            }
            if (request.getYearsOfExperience() > 70) {
                throw new IllegalArgumentException("yearsOfExperience cannot exceed 70 years");
            }
        }

        log.info("Upserting career profile for userId: {}", userId);

        boolean isNew = !careerProfileRepository.existsByUserId(userId);
        CareerProfile profile = careerProfileRepository.findByUserId(userId)
                .orElseGet(() -> new CareerProfile(userId));

        profile.setProfessionalSummary(trimToNull(request.getProfessionalSummary()));
        profile.setCurrentTitle(trimToNull(request.getCurrentTitle()));
        profile.setTargetRole(trimToNull(request.getTargetRole()));
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setPreferredLocation(trimToNull(request.getPreferredLocation()));
        profile.setWorkPreference(request.getWorkPreference());
        profile.setAvailability(request.getAvailability());

        CareerProfile saved = careerProfileRepository.save(profile);

        if (isNew) {
            meterRegistry.counter("devsphere_career_profile_created_total").increment();
        } else {
            meterRegistry.counter("devsphere_career_profile_updated_total").increment();
        }

        return new CareerProfileResponse(saved);
    }

    @Transactional
    public void deleteCareerProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (!careerProfileRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("CAREER_PROFILE_NOT_FOUND", "Career profile not found for user");
        }

        log.info("Deleting career profile for userId: {}", userId);
        careerProfileRepository.deleteByUserId(userId);
        meterRegistry.counter("devsphere_career_profile_deleted_total").increment();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
