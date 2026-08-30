package com.devsphere.user.service;

import com.devsphere.user.cache.UserProfileCache;
import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final UserProfileCache userProfileCache;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public UserProfileService(UserProfileRepository userProfileRepository, UserProfileCache userProfileCache) {
        this(userProfileRepository, userProfileCache, new SimpleMeterRegistry(), null);
    }

    public UserProfileService(UserProfileRepository userProfileRepository,
                              UserProfileCache userProfileCache,
                              MeterRegistry meterRegistry) {
        this(userProfileRepository, userProfileCache, meterRegistry, null);
    }

    @Autowired(required = false)
    public UserProfileService(UserProfileRepository userProfileRepository,
                              UserProfileCache userProfileCache,
                              MeterRegistry meterRegistry,
                              Tracer tracer) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileCache = userProfileCache;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    public UserProfileResponse getOrCreateProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        Optional<UserProfileResponse> cached = userProfileCache.get(userId);
        if (cached.isPresent()) {
            return cached.get();
        }

        return getOrCreateProfileFromDb(userId);
    }

    @Transactional
    public UserProfileResponse getOrCreateProfileFromDb(Long userId) {
        ScopedSpan span = tracer != null ? tracer.startScopedSpan("user.profile.get") : null;
        if (span != null) {
            span.tag("service.operation", "getOrCreateProfile");
        }

        try {
            log.info("User profile requested for userId: {}", userId);

            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        ScopedSpan createSpan = tracer != null ? tracer.startScopedSpan("user.profile.create") : null;
                        if (createSpan != null) {
                            createSpan.tag("service.operation", "createProfile");
                        }
                        try {
                            log.info("User profile lazily created for userId: {}", userId);
                            UserProfile newProfile = new UserProfile(userId);
                            meterRegistry.counter("devsphere.user.profile.created.total", "source", "http").increment();
                            return userProfileRepository.save(newProfile);
                        } finally {
                            if (createSpan != null) {
                                createSpan.end();
                            }
                        }
                    });

            UserProfileResponse response = mapToResponse(profile);
            userProfileCache.put(userId, response);

            return response;
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        ScopedSpan span = tracer != null ? tracer.startScopedSpan("user.profile.update") : null;
        if (span != null) {
            span.tag("service.operation", "updateProfile");
        }

        try {
            log.info("User profile update requested for userId: {}", userId);

            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        log.info("User profile lazily created during update for userId: {}", userId);
                        meterRegistry.counter("devsphere.user.profile.created.total", "source", "http").increment();
                        return new UserProfile(userId);
                    });

            if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
            if (request.getLastName() != null) profile.setLastName(request.getLastName());
            if (request.getDisplayName() != null) profile.setDisplayName(request.getDisplayName());
            if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
            if (request.getBio() != null) profile.setBio(request.getBio());
            if (request.getLocation() != null) profile.setLocation(request.getLocation());
            if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());
            if (request.getGithubUrl() != null) profile.setGithubUrl(request.getGithubUrl());
            if (request.getLinkedinUrl() != null) profile.setLinkedinUrl(request.getLinkedinUrl());
            if (request.getPortfolioUrl() != null) profile.setPortfolioUrl(request.getPortfolioUrl());
            if (request.getCurrentRole() != null) profile.setCurrentRole(request.getCurrentRole());
            if (request.getYearsOfExperience() != null) profile.setYearsOfExperience(request.getYearsOfExperience());

            UserProfile savedProfile = userProfileRepository.save(profile);
            meterRegistry.counter("devsphere_profile_updates_total").increment();
            log.info("User profile updated successfully in database for userId: {}", userId);

            UserProfileResponse response = mapToResponse(savedProfile);
            userProfileCache.evict(userId);

            return response;
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    private UserProfileResponse mapToResponse(UserProfile entity) {
        return new UserProfileResponse(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDisplayName(),
                entity.getHeadline(),
                entity.getBio(),
                entity.getLocation(),
                entity.getPhoneNumber(),
                entity.getGithubUrl(),
                entity.getLinkedinUrl(),
                entity.getPortfolioUrl(),
                entity.getCurrentRole(),
                entity.getYearsOfExperience(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
