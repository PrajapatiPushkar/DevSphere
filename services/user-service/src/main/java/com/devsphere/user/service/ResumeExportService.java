package com.devsphere.user.service;

import com.devsphere.user.dto.ResumeExportFormat;
import com.devsphere.user.dto.ResumeExportResult;
import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.renderer.DocxResumeRenderer;
import com.devsphere.user.renderer.PdfResumeRenderer;
import com.devsphere.user.renderer.ResumeRenderer;
import com.devsphere.user.util.ResumeFilenameSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResumeExportService {

    private static final Logger log = LoggerFactory.getLogger(ResumeExportService.class);
    private static final long DEFAULT_MAX_SIZE_BYTES = 10485760L; // 10MB default

    private final ResumeCompilationService resumeCompilationService;
    private final ResumeVersionService resumeVersionService;
    private final ResumeRenderer htmlResumeRenderer;
    private final PdfResumeRenderer pdfResumeRenderer;
    private final DocxResumeRenderer docxResumeRenderer;
    private final MeterRegistry meterRegistry;
    private final long maxSizeBytes;

    public ResumeExportService(ResumeCompilationService resumeCompilationService,
                               ResumeRenderer htmlResumeRenderer,
                               PdfResumeRenderer pdfResumeRenderer,
                               DocxResumeRenderer docxResumeRenderer) {
        this(resumeCompilationService, null, htmlResumeRenderer, pdfResumeRenderer, docxResumeRenderer, new SimpleMeterRegistry(), DEFAULT_MAX_SIZE_BYTES);
    }

    public ResumeExportService(ResumeCompilationService resumeCompilationService,
                               ResumeRenderer htmlResumeRenderer,
                               PdfResumeRenderer pdfResumeRenderer,
                               DocxResumeRenderer docxResumeRenderer,
                               MeterRegistry meterRegistry,
                               long maxSizeBytes) {
        this(resumeCompilationService, null, htmlResumeRenderer, pdfResumeRenderer, docxResumeRenderer, meterRegistry, maxSizeBytes);
    }

    public ResumeExportService(ResumeCompilationService resumeCompilationService,
                               ResumeVersionService resumeVersionService,
                               ResumeRenderer htmlResumeRenderer,
                               PdfResumeRenderer pdfResumeRenderer,
                               DocxResumeRenderer docxResumeRenderer) {
        this(resumeCompilationService, resumeVersionService, htmlResumeRenderer, pdfResumeRenderer, docxResumeRenderer, new SimpleMeterRegistry(), DEFAULT_MAX_SIZE_BYTES);
    }

    @Autowired
    public ResumeExportService(ResumeCompilationService resumeCompilationService,
                               ResumeVersionService resumeVersionService,
                               ResumeRenderer htmlResumeRenderer,
                               PdfResumeRenderer pdfResumeRenderer,
                               DocxResumeRenderer docxResumeRenderer,
                               MeterRegistry meterRegistry,
                               @Value("${app.resume.export.max-size-bytes:10485760}") long maxSizeBytes) {
        this.resumeCompilationService = resumeCompilationService;
        this.resumeVersionService = resumeVersionService;
        this.htmlResumeRenderer = htmlResumeRenderer;
        this.pdfResumeRenderer = pdfResumeRenderer;
        this.docxResumeRenderer = docxResumeRenderer;
        this.meterRegistry = meterRegistry;
        this.maxSizeBytes = maxSizeBytes > 0 ? maxSizeBytes : DEFAULT_MAX_SIZE_BYTES;
    }

    public ResumeExportResult exportResume(Long resumeId, Long userId, ResumeExportFormat format) {
        ResumeExportFormat actualFormat = format != null ? format : ResumeExportFormat.PDF;
        CompiledResumeResponse compiled;
        try {
            compiled = resumeCompilationService.compileResume(resumeId, userId);
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_export_total",
                    "status", "failure",
                    "format", actualFormat.name().toLowerCase(),
                    "template", "professional"
            ).increment();
            throw e;
        }
        return doExport(compiled, actualFormat, "resumeId: " + resumeId + " for userId: " + userId);
    }

    public ResumeExportResult exportResumeVersion(Long resumeId, Long versionId, Long userId, ResumeExportFormat format) {
        if (resumeVersionService == null) {
            throw new IllegalStateException("ResumeVersionService is not configured");
        }
        ResumeExportFormat actualFormat = format != null ? format : ResumeExportFormat.PDF;
        CompiledResumeResponse compiled;
        try {
            compiled = resumeVersionService.compileVersion(resumeId, versionId, userId);
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_export_total",
                    "status", "failure",
                    "format", actualFormat.name().toLowerCase(),
                    "template", "professional"
            ).increment();
            throw e;
        }
        return doExport(compiled, actualFormat, "versionId: " + versionId + " (resumeId: " + resumeId + ") for userId: " + userId);
    }

    private ResumeExportResult doExport(CompiledResumeResponse compiled, ResumeExportFormat format, String contextInfo) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String templateName = "professional";

        try {
            if (compiled == null) {
                throw new IllegalArgumentException("Compiled resume must not be null");
            }

            templateName = compiled.getTemplate() != null ? compiled.getTemplate().name().toLowerCase() : "professional";

            byte[] content = switch (format) {
                case HTML -> {
                    String html = htmlResumeRenderer.render(compiled);
                    yield html != null ? html.getBytes(StandardCharsets.UTF_8) : new byte[0];
                }
                case PDF -> pdfResumeRenderer.render(compiled);
                case DOCX -> docxResumeRenderer.render(compiled);
            };

            if (content == null || content.length == 0) {
                throw new IllegalArgumentException("Generated export document is empty");
            }

            if (content.length > maxSizeBytes) {
                throw new IllegalArgumentException("Generated export document size (" + content.length + " bytes) exceeds maximum allowed limit (" + maxSizeBytes + " bytes)");
            }

            String filename = ResumeFilenameSanitizer.sanitizeFilename(compiled.getName(), format.getExtension());

            meterRegistry.counter("devsphere_resume_export_total",
                    "status", "success",
                    "format", format.name().toLowerCase(),
                    "template", templateName
            ).increment();

            log.info("Successfully exported {} in format: {} (size: {} bytes, filename: {})",
                    contextInfo, format.name(), content.length, filename);

            return new ResumeExportResult(content, format.getMediaType(), filename, format.isAttachment());
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_export_total",
                    "status", "failure",
                    "format", format.name().toLowerCase(),
                    "template", templateName
            ).increment();

            log.error("Failed to export {} in format: {}", contextInfo, format.name(), e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("devsphere_resume_export_duration"));
        }
    }
}
