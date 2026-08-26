package com.devsphere.user.service;

import com.devsphere.user.dto.ExperienceRequest;
import com.devsphere.user.dto.ExperienceResponse;
import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.ExperienceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
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
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;

    private SimpleMeterRegistry meterRegistry;
    private ExperienceService experienceService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        experienceService = new ExperienceService(experienceRepository, meterRegistry);
    }

    @Test
    void createExperience_validRequest_createsAndIncrementsCounter() {
        Long userId = 100L;
        ExperienceRequest request = new ExperienceRequest(
                "Acme Corp", "Backend Engineer", EmploymentType.FULL_TIME, "San Francisco",
                LocalDate.of(2022, 1, 1), LocalDate.of(2023, 6, 1), false, "Built microservices", 1
        );

        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> {
            Experience exp = inv.getArgument(0);
            exp.setId(10L);
            return exp;
        });

        ExperienceResponse response = experienceService.createExperience(userId, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(meterRegistry.find("devsphere_experience_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void createExperience_currentlyWorkingTrue_setsEndDateToNull() {
        Long userId = 100L;
        ExperienceRequest request = new ExperienceRequest(
                "Tech Inc", "Senior Engineer", EmploymentType.FULL_TIME, "Remote",
                LocalDate.of(2023, 1, 1), null, true, "Present role", 0
        );

        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> {
            Experience exp = inv.getArgument(0);
            exp.setId(11L);
            return exp;
        });

        ExperienceResponse response = experienceService.createExperience(userId, request);

        assertThat(response.getCurrentlyWorking()).isTrue();
        assertThat(response.getEndDate()).isNull();
    }

    @Test
    void createExperience_endDateBeforeStartDate_throwsIllegalArgumentException() {
        Long userId = 100L;
        ExperienceRequest request = new ExperienceRequest(
                "Invalid Inc", "Dev", EmploymentType.FULL_TIME, "Remote",
                LocalDate.of(2023, 6, 1), LocalDate.of(2022, 1, 1), false, "Invalid dates", 0
        );

        assertThatThrownBy(() -> experienceService.createExperience(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must not be before startDate");
    }

    @Test
    void getExperience_found_returnsResponse() {
        Long id = 10L;
        Long userId = 100L;
        Experience exp = new Experience(userId, "Acme", "Dev", EmploymentType.FULL_TIME, LocalDate.now());
        exp.setId(id);

        when(experienceRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(exp));

        ExperienceResponse response = experienceService.getExperience(id, userId);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getCompanyName()).isEqualTo("Acme");
    }

    @Test
    void getExperience_notFound_throwsException() {
        when(experienceRepository.findByIdAndUserId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experienceService.getExperience(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listExperiences_returnsList() {
        Long userId = 100L;
        Experience e1 = new Experience(userId, "Company A", "Dev A", EmploymentType.FULL_TIME, LocalDate.now());
        e1.setId(1L);

        when(experienceRepository.findAllByUserIdOrderByDisplayOrderAscStartDateDesc(userId)).thenReturn(List.of(e1));

        List<ExperienceResponse> list = experienceService.listExperiences(userId);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCompanyName()).isEqualTo("Company A");
    }

    @Test
    void deleteExperience_found_deletesRecord() {
        Long id = 10L;
        Long userId = 100L;
        Experience exp = new Experience(userId, "Acme", "Dev", EmploymentType.FULL_TIME, LocalDate.now());
        exp.setId(id);

        when(experienceRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(exp));

        experienceService.deleteExperience(id, userId);

        verify(experienceRepository).delete(exp);
        assertThat(meterRegistry.find("devsphere_experience_deleted_total").counter().count()).isEqualTo(1.0);
    }
}
