package com.devsphere.user.controller;

import com.devsphere.user.dto.CareerProfileRequest;
import com.devsphere.user.dto.CareerProfileResponse;
import com.devsphere.user.entity.Availability;
import com.devsphere.user.entity.CareerProfile;
import com.devsphere.user.entity.WorkPreference;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.CareerProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CareerProfileController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class CareerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CareerProfileService careerProfileService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void getCareerProfile_returns200OK_whenProfileExists() throws Exception {
        Long userId = 100L;
        CareerProfile profile = new CareerProfile(userId);
        profile.setId(1L);
        profile.setCurrentTitle("Full Stack Developer");
        profile.setTargetRole("Tech Lead");
        profile.setWorkPreference(WorkPreference.REMOTE);

        when(careerProfileService.getCareerProfile(userId)).thenReturn(new CareerProfileResponse(profile));

        mockMvc.perform(get("/api/v1/career-profile")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.currentTitle").value("Full Stack Developer"))
                .andExpect(jsonPath("$.workPreference").value("REMOTE"));
    }

    @Test
    void getCareerProfile_returns404NotFound_whenNoProfileExists() throws Exception {
        Long userId = 100L;
        when(careerProfileService.getCareerProfile(userId))
                .thenThrow(new ResourceNotFoundException("CAREER_PROFILE_NOT_FOUND", "Career profile not found for user"));

        mockMvc.perform(get("/api/v1/career-profile")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAREER_PROFILE_NOT_FOUND"));
    }

    @Test
    void upsertCareerProfile_withValidRequest_returns200OK() throws Exception {
        Long userId = 100L;
        CareerProfileRequest request = new CareerProfileRequest(
                "Passionate Java Developer", "Java Developer", "Senior Java Engineer",
                4, "Remote", WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        CareerProfile profile = new CareerProfile(userId);
        profile.setId(1L);
        profile.setProfessionalSummary("Passionate Java Developer");
        profile.setCurrentTitle("Java Developer");
        profile.setTargetRole("Senior Java Engineer");
        profile.setYearsOfExperience(4);
        profile.setWorkPreference(WorkPreference.REMOTE);

        when(careerProfileService.upsertCareerProfile(eq(userId), any(CareerProfileRequest.class)))
                .thenReturn(new CareerProfileResponse(profile));

        mockMvc.perform(put("/api/v1/career-profile")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.currentTitle").value("Java Developer"))
                .andExpect(jsonPath("$.yearsOfExperience").value(4));
    }

    @Test
    void upsertCareerProfile_withNegativeYears_returns400BadRequest() throws Exception {
        Long userId = 100L;
        CareerProfileRequest request = new CareerProfileRequest(
                "Summary", "Title", "Target", -5, "Location", WorkPreference.REMOTE, Availability.OPEN_TO_WORK
        );

        mockMvc.perform(put("/api/v1/career-profile")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void deleteCareerProfile_returns204NoContent() throws Exception {
        Long userId = 100L;
        doNothing().when(careerProfileService).deleteCareerProfile(userId);

        mockMvc.perform(delete("/api/v1/career-profile")
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(careerProfileService).deleteCareerProfile(userId);
    }
}
