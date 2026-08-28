package com.devsphere.user.service;

import com.devsphere.user.dto.DocxExportResult;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.renderer.DocxResumeRenderer;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeDocxRenderingServiceTest {

    @Mock
    private ResumeCompilationService resumeCompilationService;

    @Mock
    private DocxResumeRenderer docxResumeRenderer;

    private MeterRegistry meterRegistry;
    private ResumeDocxRenderingService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new ResumeDocxRenderingService(resumeCompilationService, docxResumeRenderer, meterRegistry);
    }

    @Test
    void exportDocxResume_success_compilesRendersAndRecordsSuccessMetric() {
        Long resumeId = 50L;
        Long userId = 100L;
        byte[] mockBytes = "PK\u0003\u0004 mock docx binary".getBytes();

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                resumeId, resumeId, "Pushkar Resume", "Lead Engineer", ResumeTemplate.MODERN, List.of()
        );

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(docxResumeRenderer.render(compiled)).thenReturn(mockBytes);

        DocxExportResult result = service.exportDocxResume(resumeId, userId);

        assertThat(result.getFilename()).isEqualTo("Pushkar Resume.docx");
        assertThat(result.getDocxBytes()).isEqualTo(mockBytes);

        assertThat(meterRegistry.find("devsphere_resume_docx_export_total")
                .tag("status", "success")
                .tag("format", "docx")
                .tag("template", "modern")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void exportDocxResume_failureInCompilation_recordsFailureMetricAndThrowsException() {
        Long resumeId = 99L;
        Long userId = 100L;

        when(resumeCompilationService.compileResume(resumeId, userId))
                .thenThrow(new ResourceNotFoundException("Resume profile not found"));

        assertThatThrownBy(() -> service.exportDocxResume(resumeId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");

        assertThat(meterRegistry.find("devsphere_resume_docx_export_total")
                .tag("status", "failure")
                .tag("format", "docx")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void renderDocxResume_returnsBytesDirectly() {
        Long resumeId = 50L;
        Long userId = 100L;
        byte[] mockBytes = "PK\u0003\u0004 mock docx".getBytes();

        CompiledResumeResponse compiled = new CompiledResumeResponse(
                resumeId, resumeId, "Test Resume", "Engineer", ResumeTemplate.PROFESSIONAL, List.of()
        );

        when(resumeCompilationService.compileResume(resumeId, userId)).thenReturn(compiled);
        when(docxResumeRenderer.render(compiled)).thenReturn(mockBytes);

        byte[] bytes = service.renderDocxResume(resumeId, userId);

        assertThat(bytes).isEqualTo(mockBytes);
    }
}
