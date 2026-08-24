package com.devsphere.user.service;

import com.devsphere.user.cache.UserProfileCache;
import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

    public UserProfileService(UserProfileRepository userProfileRepository, UserProfileCache userProfileCache) {
        this(userProfileRepository, userProfileCache, new SimpleMeterRegistry());
    }

    @Autowired
    public UserProfileService(UserProfileRepository userProfileRepository,
                              UserProfileCache userProfileCache,
                              MeterRegistry meterRegistry) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileCache = userProfileCache;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public UserProfileResponse getOrCreateProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        log.info("User profile requested for userId: {}", userId);

        Optional<UserProfileResponse> cached = userProfileCache.get(userId);
        if (cached.isPresent()) {
            return cached.get();
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("User profile lazily created for userId: {}", userId);
                    UserProfile newProfile = new UserProfile(userId);
                    meterRegistry.counter("devsphere.user.profile.created.total", "source", "http").increment();
                    return userProfileRepository.save(newProfile);
                });

        UserProfileResponse response = mapToResponse(profile);
        userProfileCache.put(userId, response);

        return response;
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        log.info("User profile update requested for userId: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("User profile lazily created during update for userId: {}", userId);
                    meterRegistry.counter("devsphere.user.profile.created.total", "source", "http").increment();
                    return new UserProfile(userId);
                });

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setPhoneNumber(request.getPhoneNumber());

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("User profile updated successfully in database for userId: {}", userId);

        UserProfileResponse response = mapToResponse(savedProfile);
        userProfileCache.evict(userId);

        return response;
    }

    private UserProfileResponse mapToResponse(UserProfile entity) {
        return new UserProfileResponse(
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDisplayName(),
                entity.getBio(),
                entity.getPhoneNumber(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
