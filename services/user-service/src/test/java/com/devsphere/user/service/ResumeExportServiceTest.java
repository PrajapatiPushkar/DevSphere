package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeExportFormat;
import com.devsphere.user.dto.ResumeExportResult;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.renderer.DocxResumeRenderer;
import com.devsphere.user.renderer.PdfResumeRenderer;
import com.devsphere.user.renderer.ResumeRenderer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeExportServiceTest {

    @Mock
    private ResumeCompilationService resumeCompilationService;

    @Mock
    private ResumeRenderer htmlResumeRenderer;

    @Mock
    private PdfResumeRenderer pdfResumeRenderer;

    @Mock
    private DocxResumeRenderer docxResumeRenderer;

    private MeterRegistry meterRegistry;
    private ResumeExportService exportService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        exportService = new ResumeExportService(
                resumeCompilationService,
                htmlResumeRenderer,
                pdfResumeRenderer,
                docxResumeRenderer,
                meterRegistry,
                1000L // 1000 bytes max limit for testing
        );
    }

    @Test
    void exportResume_htmlFormat_success() {
        Long resumeId = 1L;
        Long userId = 100L;
        CompiledResumeResponse compiled = new CompiledResumeResponse(resumeId, resumeId, "Pushkar Resume", "Engineer", ResumeTemplate.PROFESSIONAL, List.of());
        String mockHtml = "<html><body>Resume</body></html>";

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(htmlResumeRenderer.render(compiled)).thenReturn(mockHtml);

        ResumeExportResult result = exportService.exportResume(resumeId, userId, ResumeExportFormat.HTML);

        assertThat(result.getContentType()).contains("text/html");
        assertThat(result.getFilename()).isEqualTo("Pushkar Resume.html");
        assertThat(result.isAttachment()).isFalse();
        assertThat(new String(result.getContent())).isEqualTo(mockHtml);
    }

    @Test
    void exportResume_pdfFormat_success() {
        Long resumeId = 1L;
        Long userId = 100L;
        CompiledResumeResponse compiled = new CompiledResumeResponse(resumeId, resumeId, "Pushkar Resume", "Engineer", ResumeTemplate.MODERN, List.of());
        byte[] mockPdf = "%PDF-1.4 binary content".getBytes();

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(pdfResumeRenderer.render(compiled)).thenReturn(mockPdf);

        ResumeExportResult result = exportService.exportResume(resumeId, userId, ResumeExportFormat.PDF);

        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getFilename()).isEqualTo("Pushkar Resume.pdf");
        assertThat(result.isAttachment()).isTrue();
        assertThat(result.getContent()).isEqualTo(mockPdf);
    }

    @Test
    void exportResume_docxFormat_success() {
        Long resumeId = 1L;
        Long userId = 100L;
        CompiledResumeResponse compiled = new CompiledResumeResponse(resumeId, resumeId, "Pushkar Resume", "Engineer", ResumeTemplate.MINIMAL, List.of());
        byte[] mockDocx = "PK\u0003\u0004 docx content".getBytes();

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(docxResumeRenderer.render(compiled)).thenReturn(mockDocx);

        ResumeExportResult result = exportService.exportResume(resumeId, userId, ResumeExportFormat.DOCX);

        assertThat(result.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(result.getFilename()).isEqualTo("Pushkar Resume.docx");
        assertThat(result.isAttachment()).isTrue();
        assertThat(result.getContent()).isEqualTo(mockDocx);
    }

    @Test
    void exportResume_exceedsMaxSizeGuardrail_throwsIllegalArgumentException() {
        Long resumeId = 1L;
        Long userId = 100L;
        CompiledResumeResponse compiled = new CompiledResumeResponse(resumeId, resumeId, "Pushkar Resume", "Engineer", ResumeTemplate.PROFESSIONAL, List.of());
        byte[] hugeContent = new byte[2000]; // 2000 bytes > 1000 bytes limit

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(pdfResumeRenderer.render(compiled)).thenReturn(hugeContent);

        assertThatThrownBy(() -> exportService.exportResume(resumeId, userId, ResumeExportFormat.PDF))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    void exportResume_emptyOutput_throwsIllegalArgumentException() {
        Long resumeId = 1L;
        Long userId = 100L;
        CompiledResumeResponse compiled = new CompiledResumeResponse(resumeId, resumeId, "Pushkar Resume", "Engineer", ResumeTemplate.PROFESSIONAL, List.of());

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(pdfResumeRenderer.render(compiled)).thenReturn(new byte[0]);

        assertThatThrownBy(() -> exportService.exportResume(resumeId, userId, ResumeExportFormat.PDF))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Generated export document is empty");
    }

    @Test
    void exportResume_compilationFailure_recordsFailureMetricAndRethrows() {
        Long resumeId = 1L;
        Long userId = 100L;

        when(resumeCompilationService.compileResume(resumeId, userId))
                .thenThrow(new ResourceNotFoundException("Resume profile not found"));

        assertThatThrownBy(() -> exportService.exportResume(resumeId, userId, ResumeExportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(meterRegistry.counter("devsphere_resume_export_total", "status", "failure", "format", "pdf", "template", "professional").count()).isEqualTo(1.0);
    }
}
