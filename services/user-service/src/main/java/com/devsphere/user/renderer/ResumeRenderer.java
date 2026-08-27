package com.devsphere.user.renderer;

import com.devsphere.user.dto.compilation.CompiledResumeResponse;

public interface ResumeRenderer {

    String render(CompiledResumeResponse compiledResume);
}
