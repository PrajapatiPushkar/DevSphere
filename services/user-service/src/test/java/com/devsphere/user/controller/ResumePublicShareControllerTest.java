package com.devsphere.user.controller;

import com.devsphere.user.dto.publicresume.PublicShareStatusResponse;
import com.devsphere.user.security.JwtAuthenticationFilter;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.PublicResumeService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResumePublicShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResumePublicShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicResumeService publicResumeService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void enablePublicSharing_ReturnsStatus() throws Exception {
        Long resumeId = 10L;
        Long userId = 100L;
        setAuthenticatedUser(userId);

        PublicShareStatusResponse response = new PublicShareStatusResponse("pub-uuid-1234", true, Instant.now(), "/api/v1/public/resumes/pub-uuid-1234");
        when(publicResumeService.enablePublicSharing(eq(resumeId), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/public/share", resumeId)
                        .header("X-Authenticated-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicResumeId").value("pub-uuid-1234"))
                .andExpect(jsonPath("$.publicEnabled").value(true))
                .andExpect(jsonPath("$.shareUrl").value("/api/v1/public/resumes/pub-uuid-1234"));
    }

    @Test
    void revokePublicSharing_ReturnsStatus() throws Exception {
        Long resumeId = 10L;
        Long userId = 100L;
        setAuthenticatedUser(userId);

        PublicShareStatusResponse response = new PublicShareStatusResponse("pub-uuid-1234", false, null, "/api/v1/public/resumes/pub-uuid-1234");
        when(publicResumeService.revokePublicSharing(eq(resumeId), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/public/revoke", resumeId)
                        .header("X-Authenticated-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicEnabled").value(false))
                .andExpect(jsonPath("$.publicEnabledAt").doesNotExist());
    }

    @Test
    void getPublicSharingStatus_ReturnsStatus() throws Exception {
        Long resumeId = 10L;
        Long userId = 100L;
        setAuthenticatedUser(userId);

        PublicShareStatusResponse response = new PublicShareStatusResponse("pub-uuid-1234", true, Instant.now(), "/api/v1/public/resumes/pub-uuid-1234");
        when(publicResumeService.getPublicSharingStatus(eq(resumeId), eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/public/status", resumeId)
                        .header("X-Authenticated-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicResumeId").value("pub-uuid-1234"))
                .andExpect(jsonPath("$.publicEnabled").value(true));
    }

    @Test
    void rotatePublicToken_ReturnsNewStatus() throws Exception {
        Long resumeId = 10L;
        Long userId = 100L;
        setAuthenticatedUser(userId);

        PublicShareStatusResponse response = new PublicShareStatusResponse("new-pub-uuid-5678", true, Instant.now(), "/api/v1/public/resumes/new-pub-uuid-5678");
        when(publicResumeService.rotatePublicToken(eq(resumeId), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/public/rotate", resumeId)
                        .header("X-Authenticated-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicResumeId").value("new-pub-uuid-5678"))
                .andExpect(jsonPath("$.shareUrl").value("/api/v1/public/resumes/new-pub-uuid-5678"));
    }

    private void setAuthenticatedUser(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, "user@devsphere.com", java.util.Collections.emptyList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
