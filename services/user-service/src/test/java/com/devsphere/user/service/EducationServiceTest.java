package com.devsphere.user.service;

import com.devsphere.user.dto.EducationRequest;
import com.devsphere.user.dto.EducationResponse;
import com.devsphere.user.entity.Education;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.EducationRepository;
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
class EducationServiceTest {

    @Mock
    private EducationRepository educationRepository;

    private SimpleMeterRegistry meterRegistry;
    private EducationService educationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        educationService = new EducationService(educationRepository, meterRegistry);
    }

    @Test
    void createEducation_validRequest_createsAndIncrementsCounter() {
        Long userId = 100L;
        EducationRequest request = new EducationRequest(
                "Stanford University", "Bachelor of Science", "Computer Science", "California",
                LocalDate.of(2018, 9, 1), LocalDate.of(2022, 6, 1), false, "CS degree", 1
        );

        when(educationRepository.save(any(Education.class))).thenAnswer(inv -> {
            Education edu = inv.getArgument(0);
            edu.setId(20L);
            return edu;
        });

        EducationResponse response = educationService.createEducation(userId, request);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getInstitutionName()).isEqualTo("Stanford University");
        assertThat(meterRegistry.find("devsphere_education_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void createEducation_endDateBeforeStartDate_throwsIllegalArgumentException() {
        Long userId = 100L;
        EducationRequest request = new EducationRequest(
                "Invalid Uni", "Degree", "Major", "Location",
                LocalDate.of(2022, 9, 1), LocalDate.of(2018, 6, 1), false, "Invalid dates", 0
        );

        assertThatThrownBy(() -> educationService.createEducation(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must not be before startDate");
    }

    @Test
    void getEducation_found_returnsResponse() {
        Long id = 20L;
        Long userId = 100L;
        Education edu = new Education(userId, "MIT", "BS", LocalDate.now());
        edu.setId(id);

        when(educationRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(edu));

        EducationResponse response = educationService.getEducation(id, userId);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getInstitutionName()).isEqualTo("MIT");
    }

    @Test
    void deleteEducation_found_deletesRecord() {
        Long id = 20L;
        Long userId = 100L;
        Education edu = new Education(userId, "MIT", "BS", LocalDate.now());
        edu.setId(id);

        when(educationRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(edu));

        educationService.deleteEducation(id, userId);

        verify(educationRepository).delete(edu);
        assertThat(meterRegistry.find("devsphere_education_deleted_total").counter().count()).isEqualTo(1.0);
    }
}
