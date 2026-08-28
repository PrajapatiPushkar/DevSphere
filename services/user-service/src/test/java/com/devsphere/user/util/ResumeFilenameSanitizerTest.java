package com.devsphere.user.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeFilenameSanitizerTest {

    @Test
    void sanitizeFilename_nullOrBlankInput_returnsDefault() {
        assertThat(ResumeFilenameSanitizer.sanitizeFilename(null)).isEqualTo("resume.pdf");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("   ")).isEqualTo("resume.pdf");
    }

    @Test
    void sanitizeFilename_pathTraversalAndControlChars_sanitizesSafely() {
        String input = "../../evil\r\nContent-Disposition: malicious";
        String result = ResumeFilenameSanitizer.sanitizeFilename(input);

        assertThat(result).doesNotContain("..");
        assertThat(result).doesNotContain("/");
        assertThat(result).doesNotContain("\\");
        assertThat(result).doesNotContain("\r");
        assertThat(result).doesNotContain("\n");
        assertThat(result).endsWith(".pdf");
    }

    @Test
    void sanitizeFilename_scriptTags_removesTagsAndFallback() {
        String input = "<script>alert('xss')</script>";
        String result = ResumeFilenameSanitizer.sanitizeFilename(input);

        assertThat(result).doesNotContain("<script>");
        assertThat(result).endsWith(".pdf");
    }

    @Test
    void sanitizeFilename_normalResumeName_formatsCorrectly() {
        String input = "Pushkar Prajapati Resume / Java Developer";
        String result = ResumeFilenameSanitizer.sanitizeFilename(input);

        assertThat(result).isEqualTo("Pushkar Prajapati Resume _ Java Developer.pdf");
    }
}
