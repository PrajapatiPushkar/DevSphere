package com.devsphere.user.service;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.devsphere.user.renderer.ResumeRenderer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResumeRenderingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeRenderingService.class);

    private final ResumeCompilationService resumeCompilationService;
    private final ResumeRenderer resumeRenderer;
    private final MeterRegistry meterRegistry;

    public ResumeRenderingService(ResumeCompilationService resumeCompilationService, ResumeRenderer resumeRenderer) {
        this(resumeCompilationService, resumeRenderer, new SimpleMeterRegistry());
    }

    @Autowired
    public ResumeRenderingService(ResumeCompilationService resumeCompilationService,
                                  ResumeRenderer resumeRenderer,
                                  MeterRegistry meterRegistry) {
        this.resumeCompilationService = resumeCompilationService;
        this.resumeRenderer = resumeRenderer;
        this.meterRegistry = meterRegistry;
    }

    public String renderHtmlResume(Long resumeId, Long userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            CompiledResumeResponse compiled = resumeCompilationService.compileResume(resumeId, userId);
            String html = resumeRenderer.render(compiled);

            meterRegistry.counter("devsphere_resume_render_total",
                    "status", "success",
                    "format", "html",
                    "template", compiled.getTemplate() != null ? compiled.getTemplate().name().toLowerCase() : "professional"
            ).increment();

            log.info("Successfully rendered HTML resume ID: {} for userId: {}", resumeId, userId);
            return html;
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resume_render_total",
                    "status", "failure",
                    "format", "html"
            ).increment();
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("devsphere_resume_render_duration"));
        }
    }
}
