package com.devsphere.user.service;

import com.devsphere.user.dto.CareerProfileRequest;
import com.devsphere.user.dto.CareerProfileResponse;
import com.devsphere.user.entity.Availability;
import com.devsphere.user.entity.CareerProfile;
import com.devsphere.user.entity.WorkPreference;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CareerProfileRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerProfileServiceTest {

    @Mock
    private CareerProfileRepository careerProfileRepository;

    private SimpleMeterRegistry meterRegistry;
    private CareerProfileService careerProfileService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        careerProfileService = new CareerProfileService(careerProfileRepository, meterRegistry);
    }

    @Test
    void getCareerProfile_whenProfileExists_returnsResponse() {
        Long userId = 100L;
        CareerProfile profile = new CareerProfile(userId);
        profile.setId(1L);
        profile.setCurrentTitle("Senior Java Developer");
        profile.setTargetRole("Lead Backend Engineer");

        when(careerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        CareerProfileResponse response = careerProfileService.getCareerProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getCurrentTitle()).isEqualTo("Senior Java Developer");
        assertThat(response.getTargetRole()).isEqualTo("Lead Backend Engineer");
    }

    @Test
    void getCareerProfile_whenProfileNotFound_throwsResourceNotFoundException() {
        Long userId = 100L;
        when(careerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerProfileService.getCareerProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Career profile not found for user");
    }

    @Test
    void upsertCareerProfile_createsNewProfile_whenNoneExists() {
        Long userId = 100L;
        CareerProfileRequest request = new CareerProfileRequest(
                "Experienced backend engineer with Spring Boot expertise",
                "Java Backend Engineer", "Lead Architect", 5, "Bangalore",
                WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        when(careerProfileRepository.existsByUserId(userId)).thenReturn(false);
        when(careerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> {
            CareerProfile cp = invocation.getArgument(0);
            cp.setId(1L);
            return cp;
        });

        CareerProfileResponse response = careerProfileService.upsertCareerProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCurrentTitle()).isEqualTo("Java Backend Engineer");
        assertThat(response.getWorkPreference()).isEqualTo(WorkPreference.REMOTE);
        assertThat(response.getAvailability()).isEqualTo(Availability.OPEN_TO_WORK);

        assertThat(meterRegistry.find("devsphere_career_profile_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void upsertCareerProfile_updatesExistingProfile_whenAlreadyExists() {
        Long userId = 100L;
        CareerProfile existing = new CareerProfile(userId);
        existing.setId(1L);
        existing.setCurrentTitle("Java Dev");

        CareerProfileRequest request = new CareerProfileRequest(
                "Updated summary", "Senior Java Dev", "Staff Engineer", 8, "Remote",
                WorkPreference.FLEXIBLE, Availability.ACTIVELY_LOOKING
        );

        when(careerProfileRepository.existsByUserId(userId)).thenReturn(true);
        when(careerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(careerProfileRepository.save(any(CareerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerProfileResponse response = careerProfileService.upsertCareerProfile(userId, request);

        assertThat(response.getCurrentTitle()).isEqualTo("Senior Java Dev");
        assertThat(response.getTargetRole()).isEqualTo("Staff Engineer");
        assertThat(response.getYearsOfExperience()).isEqualTo(8);

        assertThat(meterRegistry.find("devsphere_career_profile_updated_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void upsertCareerProfile_withNegativeYears_throwsIllegalArgumentException() {
        Long userId = 100L;
        CareerProfileRequest request = new CareerProfileRequest(
                "Summary", "Dev", "Lead", -2, "Location", WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        assertThatThrownBy(() -> careerProfileService.upsertCareerProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yearsOfExperience must not be negative");
    }

    @Test
    void upsertCareerProfile_withExcessiveYears_throwsIllegalArgumentException() {
        Long userId = 100L;
        CareerProfileRequest request = new CareerProfileRequest(
                "Summary", "Dev", "Lead", 100, "Location", WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        assertThatThrownBy(() -> careerProfileService.upsertCareerProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yearsOfExperience cannot exceed 70 years");
    }

    @Test
    void deleteCareerProfile_deletesProfile_whenExists() {
        Long userId = 100L;
        when(careerProfileRepository.existsByUserId(userId)).thenReturn(true);

        careerProfileService.deleteCareerProfile(userId);

        verify(careerProfileRepository).deleteByUserId(userId);
        assertThat(meterRegistry.find("devsphere_career_profile_deleted_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void deleteCareerProfile_throwsNotFound_whenNotExists() {
        Long userId = 100L;
        when(careerProfileRepository.existsByUserId(userId)).thenReturn(false);

        assertThatThrownBy(() -> careerProfileService.deleteCareerProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Career profile not found for user");
    }
}
