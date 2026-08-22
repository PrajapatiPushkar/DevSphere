package com.devsphere.user.service;

import com.devsphere.user.cache.UserProfileCache;
import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileCache userProfileCache;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository, userProfileCache);
    }

    @Test
    void getOrCreateProfile_whenCacheHit_returnsCachedResponseWithoutQueryingDatabase() {
        Long userId = 101L;
        UserProfileResponse cachedResponse = new UserProfileResponse(
                userId, "CachedFirst", "CachedLast", "CachedDisplay", "CachedBio", "111", Instant.now(), Instant.now()
        );
        when(userProfileCache.get(userId)).thenReturn(Optional.of(cachedResponse));

        UserProfileResponse response = userProfileService.getOrCreateProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("CachedFirst");
        verify(userProfileRepository, never()).findByUserId(any());
        verify(userProfileCache, never()).put(any(), any());
    }

    @Test
    void getOrCreateProfile_whenCacheMiss_queriesDatabaseAndPopulatesCache() {
        Long userId = 101L;
        UserProfile existing = new UserProfile(1L, userId, "Pushkar", "Prajapati", "Pushkar P", "Developer", "1234567890", Instant.now(), Instant.now());

        when(userProfileCache.get(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        UserProfileResponse response = userProfileService.getOrCreateProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getFirstName()).isEqualTo("Pushkar");

        verify(userProfileRepository).findByUserId(userId);
        verify(userProfileCache).put(eq(userId), any(UserProfileResponse.class));
    }

    @Test
    void getOrCreateProfile_whenProfileDoesNotExist_lazilyCreatesProfileAndPopulatesCache() {
        Long userId = 101L;
        when(userProfileCache.get(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile p = invocation.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        UserProfileResponse response = userProfileService.getOrCreateProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userProfileCache).put(eq(userId), any(UserProfileResponse.class));
    }

    @Test
    void getOrCreateProfile_nullUserId_throwsException() {
        assertThatThrownBy(() -> userProfileService.getOrCreateProfile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID must not be null");
    }

    @Test
    void updateProfile_updatesDatabaseAndEvictsCache() {
        Long userId = 101L;
        UserProfile existing = new UserProfile(1L, userId, "OldFirst", "OldLast", "OldDisplay", "OldBio", "0000000000", Instant.now(), Instant.now());

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("NewFirst", "NewLast", "NewDisplay", "NewBio", "9999999999");
        UserProfileResponse response = userProfileService.updateProfile(userId, request);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getFirstName()).isEqualTo("NewFirst");
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userProfileCache).evict(userId);
    }
}
