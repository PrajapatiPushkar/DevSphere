package com.devsphere.user.controller;

import com.devsphere.user.dto.PdfExportResult;
import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.service.ResumePdfRenderingService;
import com.devsphere.user.service.ResumeRenderingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ResumeRenderingController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ResumeRenderingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeRenderingService resumeRenderingService;

    @MockBean
    private ResumePdfRenderingService resumePdfRenderingService;

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

    @Test
    void renderPdf_returns200OKAndApplicationPdfWithAttachmentHeader() throws Exception {
        Long userId = 100L;
        Long resumeId = 50L;
        byte[] mockPdf = "%PDF-1.4 mock pdf binary content".getBytes();
        PdfExportResult exportResult = new PdfExportResult(mockPdf, "Pushkar_Resume.pdf");

        when(resumePdfRenderingService.exportPdfResume(resumeId, userId)).thenReturn(exportResult);

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/render/pdf", resumeId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Pushkar_Resume.pdf\""))
                .andExpect(content().bytes(mockPdf));
    }

    @Test
    void renderPdf_unauthenticated_returns401() throws Exception {
        Long resumeId = 50L;

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/render/pdf", resumeId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void renderPdf_crossUserOrNotFound_returns404() throws Exception {
        Long userId = 100L;
        Long resumeId = 999L;

        when(resumePdfRenderingService.exportPdfResume(resumeId, userId))
                .thenThrow(new ResourceNotFoundException("Resume profile not found"));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/render/pdf", resumeId)
                        .header("X-Authenticated-User-Id", userId))
                .andExpect(status().isNotFound());
    }
}
