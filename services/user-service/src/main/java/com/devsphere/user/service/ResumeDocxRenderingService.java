package com.devsphere.user.service;

import com.devsphere.user.dto.DocxExportResult;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.renderer.DocxResumeRenderer;
import com.devsphere.user.util.ResumeFilenameSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResumeDocxRenderingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeDocxRenderingService.class);

    private final ResumeCompilationService resumeCompilationService;
    private final DocxResumeRenderer docxResumeRenderer;
    private final MeterRegistry meterRegistry;

    public ResumeDocxRenderingService(ResumeCompilationService resumeCompilationService,
                                     DocxResumeRenderer docxResumeRenderer) {
        this(resumeCompilationService, docxResumeRenderer, new SimpleMeterRegistry());
    }

    @Autowired
    public ResumeDocxRenderingService(ResumeCompilationService resumeCompilationService,
                                     DocxResumeRenderer docxResumeRenderer,
                                     MeterRegistry meterRegistry) {
        this.resumeCompilationService = resumeCompilationService;
        this.docxResumeRenderer = docxResumeRenderer;
        this.meterRegistry = meterRegistry;
    }

    public byte[] renderDocxResume(Long resumeId, Long userId) {
        return exportDocxResume(resumeId, userId).getDocxBytes();
    }

    public DocxExportResult exportDocxResume(Long resumeId, Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String templateName = "professional";
        try {
            CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);
            templateName = compiled.getTemplate() != null ? compiled.getTemplate().name().toLowerCase() : "professional";

            byte[] docxBytes = docxResumeRenderer.render(compiled);
            String filename = ResumeFilenameSanitizer.sanitizeFilename(compiled.getName(), "docx");

            meterRegistry.counter("devsphere_resume_docx_export_total",
                    "status", "success",
                    "format", "docx",
                    "template", templateName
            ).increment();

            log.info("Successfully exported DOCX resume ID: {} for userId: {} (size: {} bytes)", resumeId, userId, docxBytes.length);
            return new DocxExportResult(docxBytes, filename);
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_docx_export_total",
                    "status", "failure",
                    "format", "docx",
                    "template", templateName
            ).increment();
            log.error("Failed to export DOCX resume ID: {} for userId: {}", resumeId, userId, e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("devsphere_resume_docx_export_duration"));
        }
    }
}
