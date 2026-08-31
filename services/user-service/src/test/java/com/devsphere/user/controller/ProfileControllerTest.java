package com.devsphere.user.controller;

import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ProfileController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void getProfile_withAuthHeader_returns200AndProfile() throws Exception {
        Long userId = 101L;
        UserProfileResponse response = new UserProfileResponse(
                userId, "Pushkar", "Prajapati", "Pushkar P", "Java backend dev", "1234567890", Instant.now(), Instant.now()
        );
        response.setHeadline("Software Engineer");
        response.setGithubUrl("https://github.com/example");

        when(userProfileService.getOrCreateProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/profile")
                        .header("X-Authenticated-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.displayName").value("Pushkar P"))
                .andExpect(jsonPath("$.headline").value("Software Engineer"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/example"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void getProfile_withoutAuthHeader_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void updateProfile_withValidRequest_returns200Ok() throws Exception {
        Long userId = 101L;
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setDisplayName("Pushkar Dev");
        request.setHeadline("Backend Lead");
        request.setGithubUrl("https://github.com/pushkar");
        request.setYearsOfExperience(3);

        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userId);
        response.setDisplayName("Pushkar Dev");
        response.setHeadline("Backend Lead");
        response.setGithubUrl("https://github.com/pushkar");
        response.setYearsOfExperience(3);

        when(userProfileService.updateProfile(eq(userId), any(UpdateUserProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/profile")
                        .header("X-Authenticated-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Pushkar Dev"))
                .andExpect(jsonPath("$.headline").value("Backend Lead"))
                .andExpect(jsonPath("$.yearsOfExperience").value(3));
    }

    @Test
    void updateProfile_withInvalidUrl_returns400BadRequest() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setGithubUrl("not-a-valid-url");

        mockMvc.perform(put("/api/v1/profile")
                        .header("X-Authenticated-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("GitHub URL must be a valid HTTP or HTTPS URL")));
    }
}
