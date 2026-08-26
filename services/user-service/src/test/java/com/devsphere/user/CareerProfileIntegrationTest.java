package com.devsphere.user;

import com.devsphere.user.dto.CareerProfileRequest;
import com.devsphere.user.dto.CareerProfileResponse;
import com.devsphere.user.entity.Availability;
import com.devsphere.user.entity.CareerProfile;
import com.devsphere.user.entity.WorkPreference;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.CareerProfileRepository;
import com.devsphere.user.service.CareerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CareerProfileIntegrationTest {

    @Autowired
    private CareerProfileService careerProfileService;

    @Autowired
    private CareerProfileRepository careerProfileRepository;

    @BeforeEach
    void cleanDatabase() {
        careerProfileRepository.deleteAll();
    }

    @Test
    void careerProfileCrudLifecycle_createsUpdatesGetsAndDeletesProfile() {
        Long userId = 800L;

        CareerProfileRequest createReq = new CareerProfileRequest(
                "Passionate Java Developer building microservices",
                "Backend Engineer", "Senior Backend Engineer", 3, "Remote",
                WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        CareerProfileResponse created = careerProfileService.upsertCareerProfile(userId, createReq);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getCurrentTitle()).isEqualTo("Backend Engineer");
        assertThat(created.getWorkPreference()).isEqualTo(WorkPreference.REMOTE);

        CareerProfileResponse fetched = careerProfileService.getCareerProfile(userId);
        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getProfessionalSummary()).isEqualTo("Passionate Java Developer building microservices");

        CareerProfileRequest updateReq = new CareerProfileRequest(
                "Updated Summary", "Senior Backend Engineer", "Lead Architect", 5, "Bangalore",
                WorkPreference.HYBRID, Availability.ACTIVELY_LOOKING
        );

        CareerProfileResponse updated = careerProfileService.upsertCareerProfile(userId, updateReq);
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getCurrentTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(updated.getTargetRole()).isEqualTo("Lead Architect");
        assertThat(updated.getYearsOfExperience()).isEqualTo(5);
        assertThat(updated.getWorkPreference()).isEqualTo(WorkPreference.HYBRID);

        careerProfileService.deleteCareerProfile(userId);

        assertThatThrownBy(() -> careerProfileService.getCareerProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Career profile not found for user");
    }

    @Test
    void putIdempotency_multipleCallsDoNotCreateDuplicateRecords() {
        Long userId = 801L;

        CareerProfileRequest req = new CareerProfileRequest(
                "Idempotent Test", "Dev", "Lead", 2, "Remote",
                WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        CareerProfileResponse r1 = careerProfileService.upsertCareerProfile(userId, req);
        CareerProfileResponse r2 = careerProfileService.upsertCareerProfile(userId, req);
        CareerProfileResponse r3 = careerProfileService.upsertCareerProfile(userId, req);

        assertThat(r1.getId()).isEqualTo(r2.getId());
        assertThat(r2.getId()).isEqualTo(r3.getId());
        assertThat(careerProfileRepository.count()).isEqualTo(1);
    }

    @Test
    void singletonConstraint_preventsMultipleProfilesPerUserInDatabase() {
        Long userId = 802L;

        CareerProfile cp1 = new CareerProfile(userId);
        cp1.setCurrentTitle("Title 1");
        careerProfileRepository.save(cp1);

        CareerProfile cp2 = new CareerProfile(userId);
        cp2.setCurrentTitle("Title 2");

        assertThatThrownBy(() -> careerProfileRepository.saveAndFlush(cp2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ownershipAndUserIsolation_userCannotAccessOrMutateOtherUserProfiles() {
        Long userA = 803L;
        Long userB = 804L;

        careerProfileService.upsertCareerProfile(userA, new CareerProfileRequest(
                "User A Profile", "Dev A", "Lead A", 4, "Location A",
                WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        ));

        assertThatThrownBy(() -> careerProfileService.getCareerProfile(userB))
                .isInstanceOf(ResourceNotFoundException.class);

        CareerProfileResponse userBProfile = careerProfileService.upsertCareerProfile(userB, new CareerProfileRequest(
                "User B Profile", "Dev B", "Lead B", 2, "Location B",
                WorkPreference.ONSITE, Availability.NOT_LOOKING
        ));

        assertThat(userBProfile.getCurrentTitle()).isEqualTo("Dev B");

        CareerProfileResponse userAProfile = careerProfileService.getCareerProfile(userA);
        assertThat(userAProfile.getCurrentTitle()).isEqualTo("Dev A");
    }
}
