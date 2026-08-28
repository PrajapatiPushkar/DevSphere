package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;

public interface DocxResumeRenderer {

    byte[] render(CompiledResumeResponse compiledResume);
}
