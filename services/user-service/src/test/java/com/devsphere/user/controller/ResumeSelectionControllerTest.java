package com.devsphere.user.controller;

import com.devsphere.user.dto.ResumeExperienceRequest;
import com.devsphere.user.dto.ResumeExperienceResponse;
import com.devsphere.user.entity.ResumeExperience;
import com.devsphere.user.exception.DuplicateResumeSelectionException;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.service.ResumeSelectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

@WebMvcTest(value = ResumeSelectionController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeSelectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeSelectionService resumeSelectionService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void addExperience_returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;
        Long expId = 10L;

        ResumeExperienceRequest request = new ResumeExperienceRequest(expId, 1);

        ResumeExperience re = new ResumeExperience(resumeId, expId, 1);
        re.setId(200L);

        when(resumeSelectionService.addExperience(eq(resumeId), eq(userId), any(ResumeExperienceRequest.class)))
                .thenReturn(new ResumeExperienceResponse(re));

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/experiences", resumeId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200L))
                .andExpect(jsonPath("$.experienceId").value(expId));
    }

    @Test
    void addExperience_duplicate_returns409Conflict() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;
        Long expId = 10L;

        ResumeExperienceRequest request = new ResumeExperienceRequest(expId, 1);

        when(resumeSelectionService.addExperience(eq(resumeId), eq(userId), any(ResumeExperienceRequest.class)))
                .thenThrow(new DuplicateResumeSelectionException("DUPLICATE_SELECTION", "Experience is already selected in this resume"));

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/experiences", resumeId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SELECTION"));
    }

    @Test
    void removeExperience_returns204NoContent() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;
        Long expId = 10L;

        doNothing().when(resumeSelectionService).removeExperience(resumeId, expId, userId);

        mockMvc.perform(delete("/api/v1/resumes/{resumeId}/experiences/{experienceId}", resumeId, expId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(resumeSelectionService).removeExperience(resumeId, expId, userId);
    }
}
