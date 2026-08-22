package com.devsphere.user;

import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import com.devsphere.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanDatabase() {
        userProfileRepository.deleteAll();
    }

    @Test
    void getOrCreateProfile_createsAndPersistsMinimalProfile() {
        Long userId = 200L;

        UserProfileResponse response = userProfileService.getOrCreateProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();

        assertThat(userProfileRepository.findByUserId(userId)).isPresent();
    }

    @Test
    void updateProfile_persistsAllProfileFields() {
        Long userId = 201L;
        userProfileService.getOrCreateProfile(userId);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Pushkar", "Prajapati", "Pushkar Dev", "Full stack Java developer", "+1-555-0199"
        );

        UserProfileResponse updated = userProfileService.updateProfile(userId, request);

        assertThat(updated.getUserId()).isEqualTo(userId);
        assertThat(updated.getFirstName()).isEqualTo("Pushkar");
        assertThat(updated.getLastName()).isEqualTo("Prajapati");
        assertThat(updated.getDisplayName()).isEqualTo("Pushkar Dev");
        assertThat(updated.getBio()).isEqualTo("Full stack Java developer");
        assertThat(updated.getPhoneNumber()).isEqualTo("+1-555-0199");
    }

    @Test
    void uniqueUserIdConstraint_preventsDuplicateProfilesForSameUserId() {
        Long userId = 202L;
        userProfileRepository.saveAndFlush(new UserProfile(userId));

        UserProfile duplicate = new UserProfile(userId);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
