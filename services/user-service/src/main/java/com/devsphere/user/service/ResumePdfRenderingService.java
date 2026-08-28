package com.devsphere.user.service;

import com.devsphere.user.dto.PdfExportResult;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.renderer.PdfResumeRenderer;
import com.devsphere.user.util.ResumeFilenameSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResumePdfRenderingService {

    private static final Logger log = LoggerFactory.getLogger(ResumePdfRenderingService.class);

    private final ResumeCompilationService resumeCompilationService;
    private final PdfResumeRenderer pdfResumeRenderer;
    private final MeterRegistry meterRegistry;

    public ResumePdfRenderingService(ResumeCompilationService resumeCompilationService,
                                     PdfResumeRenderer pdfResumeRenderer) {
        this(resumeCompilationService, pdfResumeRenderer, new SimpleMeterRegistry());
    }

    @Autowired
    public ResumePdfRenderingService(ResumeCompilationService resumeCompilationService,
                                     PdfResumeRenderer pdfResumeRenderer,
                                     MeterRegistry meterRegistry) {
        this.resumeCompilationService = resumeCompilationService;
        this.pdfResumeRenderer = pdfResumeRenderer;
        this.meterRegistry = meterRegistry;
    }

    public byte[] renderPdfResume(Long resumeId, Long userId) {
        return exportPdfResume(resumeId, userId).getPdfBytes();
    }

    public PdfExportResult exportPdfResume(Long resumeId, Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String templateName = "professional";
        try {
            CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);
            templateName = compiled.getTemplate() != null ? compiled.getTemplate().name().toLowerCase() : "professional";

            byte[] pdfBytes = pdfResumeRenderer.render(compiled);
            String filename = ResumeFilenameSanitizer.sanitizeFilename(compiled.getName());

            meterRegistry.counter("devsphere_resume_pdf_export_total",
                    "status", "success",
                    "format", "pdf",
                    "template", templateName
            ).increment();

            log.info("Successfully exported PDF resume ID: {} for userId: {} (size: {} bytes)", resumeId, userId, pdfBytes.length);
            return new PdfExportResult(pdfBytes, filename);
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_pdf_export_total",
                    "status", "failure",
                    "format", "pdf",
                    "template", templateName
            ).increment();
            log.error("Failed to export PDF resume ID: {} for userId: {}", resumeId, userId, e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("devsphere_resume_pdf_export_duration"));
        }
    }
}
