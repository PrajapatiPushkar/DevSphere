package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateResumeVersionRequest;
import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.ResumeVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ResumeVersionController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeVersionService resumeVersionService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    @DisplayName("POST create version returns 201 Created")
    void createVersion_Returns201Created() throws Exception {
        Long userId = 100L;
        Long resumeId = 1L;
        CreateResumeVersionRequest request = new CreateResumeVersionRequest("Backend Engineer V1");

        ResumeVersion version = new ResumeVersion(resumeId, userId, 1, "Backend Engineer V1", "{}");
        version.setId(10L);

        when(resumeVersionService.createVersion(eq(resumeId), eq(userId), any(CreateResumeVersionRequest.class)))
                .thenReturn(new ResumeVersionResponse(version));

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/versions", resumeId)
                        .header("X-Authenticated-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.name").value("Backend Engineer V1"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET list versions returns 200 OK")
    void listVersions_Returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 1L;

        ResumeVersion v1 = new ResumeVersion(resumeId, userId, 1, "V1", "{}");
        v1.setId(10L);

        when(resumeVersionService.listVersions(resumeId, userId))
                .thenReturn(List.of(new ResumeVersionResponse(v1)));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/versions", resumeId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].versionNumber").value(1));
    }

    @Test
    @DisplayName("GET version detail returns 200 OK")
    void getVersion_Returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 1L;
        Long versionId = 10L;

        ResumeVersion v = new ResumeVersion(resumeId, userId, 1, "V1", "{}");
        v.setId(versionId);

        when(resumeVersionService.getVersion(resumeId, versionId, userId))
                .thenReturn(new ResumeVersionResponse(v));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/versions/{versionId}", resumeId, versionId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(versionId));
    }

    @Test
    @DisplayName("POST publish version returns 200 OK with PUBLISHED status")
    void publishVersion_Returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 1L;
        Long versionId = 10L;

        ResumeVersion v = new ResumeVersion(resumeId, userId, 1, "V1", "{}");
        v.setId(versionId);
        v.setStatus(ResumeVersionStatus.PUBLISHED);

        when(resumeVersionService.publishVersion(resumeId, versionId, userId))
                .thenReturn(new ResumeVersionResponse(v));

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/versions/{versionId}/publish", resumeId, versionId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("POST archive version returns 200 OK with ARCHIVED status")
    void archiveVersion_Returns200OK() throws Exception {
        Long userId = 100L;
        Long resumeId = 1L;
        Long versionId = 10L;

        ResumeVersion v = new ResumeVersion(resumeId, userId, 1, "V1", "{}");
        v.setId(versionId);
        v.setStatus(ResumeVersionStatus.ARCHIVED);

        when(resumeVersionService.archiveVersion(resumeId, versionId, userId))
                .thenReturn(new ResumeVersionResponse(v));

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/versions/{versionId}/archive", resumeId, versionId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("Cross-user access returns 404 Not Found (IDOR protection)")
    void getVersion_CrossUser_Returns404() throws Exception {
        Long userId = 200L; // User B
        Long resumeId = 1L; // User A's resume
        Long versionId = 10L;

        when(resumeVersionService.getVersion(resumeId, versionId, userId))
                .thenThrow(new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/versions/{versionId}", resumeId, versionId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void getVersion_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/1/versions/10"))
                .andExpect(status().isUnauthorized());
    }
}
