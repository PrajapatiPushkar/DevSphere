package com.devsphere.user.controller;

import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(UserProfileController.class)
@Import(GlobalExceptionHandler.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    @Test
    void getMyProfile_withValidAuthHeader_returns200AndProfile() throws Exception {
        Long userId = 101L;
        UserProfileResponse response = new UserProfileResponse(
                userId, "Pushkar", "Prajapati", "Pushkar P", "Java backend dev", "1234567890", Instant.now(), Instant.now()
        );
        when(userProfileService.getOrCreateProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Authenticated-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.firstName").value("Pushkar"))
                .andExpect(jsonPath("$.lastName").value("Prajapati"))
                .andExpect(jsonPath("$.displayName").value("Pushkar P"))
                .andExpect(jsonPath("$.bio").value("Java backend dev"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.jwtSecret").doesNotExist());
    }

    @Test
    void getMyProfile_withoutAuthHeader_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authenticated user identity is required"));
    }

    @Test
    void getMyProfile_withInvalidAuthHeaderFormat_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Authenticated-User-Id", "invalid-id"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid authenticated user identity format"));
    }

    @Test
    void updateMyProfile_withValidRequest_returns200AndUpdatedProfile() throws Exception {
        Long userId = 101L;
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Pushkar", "Prajapati", "Pushkar", "Updated bio", "9876543210");
        UserProfileResponse response = new UserProfileResponse(
                userId, "Pushkar", "Prajapati", "Pushkar", "Updated bio", "9876543210", Instant.now(), Instant.now()
        );
        when(userProfileService.updateProfile(eq(userId), any(UpdateUserProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("X-Authenticated-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.firstName").value("Pushkar"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));
    }

    @Test
    void updateMyProfile_withFirstNameExceedingMaxLength_returns400BadRequest() throws Exception {
        String longFirstName = "A".repeat(101);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(longFirstName, "Prajapati", "Pushkar", "Bio", "12345");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("X-Authenticated-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message", containsString("First name must not exceed 100 characters")));
    }
}
