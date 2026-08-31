package com.devsphere.user.controller;

import com.devsphere.user.dto.publicresume.PublicResumeAnalyticsResponse;
import com.devsphere.user.exception.ResourceNotFoundException;

import com.devsphere.user.service.PublicResumeAnalyticsService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeAnalyticsControllerTest {

    private PublicResumeAnalyticsService analyticsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        analyticsService = mock(PublicResumeAnalyticsService.class);
        ResumeAnalyticsController controller = new ResumeAnalyticsController(analyticsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.devsphere.user.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void getResumeAnalytics_whenOwner_returns200OkWithAnalyticsPayload() throws Exception {
        Long userId = 100L;
        Long resumeId = 10L;

        PublicResumeAnalyticsResponse response = new PublicResumeAnalyticsResponse(
                resumeId, "pub-uuid-100", 50L, 35L, Instant.now(),
                Map.of("2026-08-31", 50L), Map.of("linkedin.com", 30L)
        );

        when(analyticsService.getResumeAnalytics(eq(resumeId), eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/analytics", resumeId)
                        .header("X-Authenticated-User-Id", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value(10))
                .andExpect(jsonPath("$.publicId").value("pub-uuid-100"))
                .andExpect(jsonPath("$.totalViews").value(50))
                .andExpect(jsonPath("$.uniqueVisitors").value(35))
                .andExpect(jsonPath("$.topReferrers['linkedin.com']").value(30));
    }

    @Test
    void getResumeAnalytics_whenNotOwner_returns404NotFound() throws Exception {
        Long userId = 100L;
        Long resumeId = 999L;

        when(analyticsService.getResumeAnalytics(eq(resumeId), eq(userId)))
                .thenThrow(new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/analytics", resumeId)
                        .header("X-Authenticated-User-Id", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    @Test
    void getResumeAnalytics_withoutUserHeader_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/10/analytics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
