package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;

public interface PdfResumeRenderer {

    byte[] render(String html);

    byte[] render(CompiledResumeResponse compiledResume);
}
