package com.devsphere.user.service;

import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfileResponse getOrCreateProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        log.info("User profile requested for userId: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("User profile lazily created for userId: {}", userId);
                    UserProfile newProfile = new UserProfile(userId);
                    return userProfileRepository.save(newProfile);
                });

        return mapToResponse(profile);
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
                    return new UserProfile(userId);
                });

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setPhoneNumber(request.getPhoneNumber());

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("User profile updated successfully for userId: {}", userId);

        return mapToResponse(savedProfile);
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
