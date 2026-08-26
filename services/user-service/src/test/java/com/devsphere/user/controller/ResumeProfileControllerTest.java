package com.devsphere.user.controller;

import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.ResumeProfileService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ResumeProfileController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeProfileService resumeProfileService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void createResumeProfile_returns201Created() throws Exception {
        Long userId = 100L;
        ResumeProfileRequest request = new ResumeProfileRequest("Java Backend Resume", "Backend Engineer", "Summary", ResumeTemplate.PROFESSIONAL);

        ResumeProfile profile = new ResumeProfile(userId, "Java Backend Resume", "Backend Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(50L);

        when(resumeProfileService.createResumeProfile(eq(userId), any(ResumeProfileRequest.class)))
                .thenReturn(new ResumeProfileResponse(profile));

        mockMvc.perform(post("/api/v1/resumes")
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.name").value("Java Backend Resume"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void activateResumeProfile_returns200OK() throws Exception {
        Long userId = 100L;
        Long id = 50L;

        ResumeProfile profile = new ResumeProfile(userId, "Resume", "Role", ResumeTemplate.PROFESSIONAL);
        profile.setId(id);
        profile.setStatus(ResumeStatus.ACTIVE);

        when(resumeProfileService.activateResumeProfile(id, userId)).thenReturn(new ResumeProfileResponse(profile));

        mockMvc.perform(post("/api/v1/resumes/{id}/activate", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteResumeProfile_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long id = 50L;

        doNothing().when(resumeProfileService).deleteResumeProfile(id, userId);

        mockMvc.perform(delete("/api/v1/resumes/{id}", id)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(resumeProfileService).deleteResumeProfile(id, userId);
    }
}
