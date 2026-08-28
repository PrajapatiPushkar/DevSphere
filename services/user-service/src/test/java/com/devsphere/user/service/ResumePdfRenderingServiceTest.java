package com.devsphere.user.service;

import com.devsphere.user.dto.PdfExportResult;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.renderer.PdfResumeRenderer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumePdfRenderingServiceTest {

    @Mock
    private ResumeCompilationService resumeCompilationService;
    @Mock
    private PdfResumeRenderer pdfResumeRenderer;

    private MeterRegistry meterRegistry;
    private ResumePdfRenderingService pdfRenderingService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        pdfRenderingService = new ResumePdfRenderingService(resumeCompilationService, pdfResumeRenderer, meterRegistry);
    }

    @Test
    void exportPdfResume_success_incrementsSuccessCounterAndReturnsResult() {
        Long resumeId = 1L;
        Long userId = 100L;

        CompiledResumeResponse compiled = new CompiledResumeResponse();
        compiled.setId(resumeId);
        compiled.setResumeProfileId(resumeId);
        compiled.setName("Pushkar Prajapati");
        compiled.setTemplate(ResumeTemplate.MODERN);

        byte[] fakeBytes = "%PDF-1.4 test bytes".getBytes();

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(pdfResumeRenderer.render(compiled)).thenReturn(fakeBytes);

        PdfExportResult result = pdfRenderingService.exportPdfResume(resumeId, userId);

        assertThat(result.getPdfBytes()).isEqualTo(fakeBytes);
        assertThat(result.getFilename()).isEqualTo("Pushkar Prajapati.pdf");

        double count = meterRegistry.counter("devsphere_resume_pdf_export_total",
                "status", "success", "format", "pdf", "template", "modern").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void exportPdfResume_notFoundOrIdor_incrementsFailureCounterAndRethrows() {
        Long resumeId = 99L;
        Long userId = 100L;

        when(resumeCompilationService.compileResume(resumeId, userId))
                .thenThrow(new ResourceNotFoundException("Resume profile not found"));

        assertThatThrownBy(() -> pdfRenderingService.exportPdfResume(resumeId, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        double count = meterRegistry.find("devsphere_resume_pdf_export_total")
                .tag("status", "failure")
                .counter()
                .count();
        assertThat(count).isEqualTo(1.0);
    }
}
