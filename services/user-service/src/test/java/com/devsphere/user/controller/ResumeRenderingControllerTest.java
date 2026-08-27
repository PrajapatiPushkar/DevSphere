package com.devsphere.user.controller;

import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.service.ResumeRenderingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ResumeRenderingController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeRenderingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeRenderingService resumeRenderingService;

    @MockBean(name = "userSecurity")
    private com.devsphere.user.security.UserSecurity userSecurity;

    @MockBean
    private com.devsphere.user.security.JwtValidator jwtValidator;

    @Test
    void renderHtml_returns200OKAndTextHtml() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;
        String mockHtml = "<!DOCTYPE html><html><body><h1>Mock Resume</h1></body></html>";

        when(resumeRenderingService.renderHtmlResume(resumeId, userId)).thenReturn(mockHtml);

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/render/html", resumeId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(mockHtml));
    }
}
