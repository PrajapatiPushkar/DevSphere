package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;

@Component
public class OpenHtmlToPdfResumeRenderer implements PdfResumeRenderer {

    private final ResumeRenderer htmlResumeRenderer;

    public OpenHtmlToPdfResumeRenderer(ResumeRenderer htmlResumeRenderer) {
        this.htmlResumeRenderer = htmlResumeRenderer;
    }

    @Override
    public byte[] render(CompiledResumeResponse compiledResume) {
        if (compiledResume == null) {
            throw new IllegalArgumentException("Compiled resume must not be null");
        }
        String html = htmlResumeRenderer.render(compiledResume);
        return render(html);
    }

    @Override
    public byte[] render(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("HTML content must not be null or blank");
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF from HTML", e);
        }
    }
}
