package com.devsphere.user.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeFilenameSanitizerTest {

    @Test
    void sanitizeFilename_nullOrBlankInput_returnsDefault() {
        assertThat(ResumeFilenameSanitizer.sanitizeFilename(null)).isEqualTo("resume.pdf");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("   ")).isEqualTo("resume.pdf");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename(null, "html")).isEqualTo("resume.html");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("   ", "docx")).isEqualTo("resume.docx");
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
    void sanitizeFilename_windowsPathTraversal_sanitizesSafely() {
        String input = "C:\\Users\\John\\Documents\\resume.pdf";
        String result = ResumeFilenameSanitizer.sanitizeFilename(input, "pdf");

        assertThat(result).doesNotContain("C:");
        assertThat(result).doesNotContain("\\");
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
    void sanitizeFilename_duplicateExtensions_handlesCleanly() {
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("John_Doe_Resume.pdf", "pdf")).isEqualTo("John_Doe_Resume.pdf");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("John_Doe_Resume.docx", "docx")).isEqualTo("John_Doe_Resume.docx");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("John_Doe_Resume.html", "html")).isEqualTo("John_Doe_Resume.html");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("John_Doe_Resume.pdf.pdf", "pdf")).isEqualTo("John_Doe_Resume.pdf");
        assertThat(ResumeFilenameSanitizer.sanitizeFilename("John_Doe_Resume.pdf", "docx")).isEqualTo("John_Doe_Resume.docx");
    }

    @Test
    void sanitizeFilename_normalResumeName_formatsCorrectly() {
        String input = "Pushkar Prajapati Resume / Java Developer";
        String result = ResumeFilenameSanitizer.sanitizeFilename(input, "docx");

        assertThat(result).isEqualTo("Pushkar Prajapati Resume _ Java Developer.docx");
    }

    @Test
    void sanitizeFilename_maxLengthTruncation_truncatesBaseNameBeforeExtension() {
        String longInput = "A".repeat(150);
        String result = ResumeFilenameSanitizer.sanitizeFilename(longInput, "pdf");

        assertThat(result.length()).isEqualTo(104); // 100 base + ".pdf" (4)
        assertThat(result).endsWith(".pdf");
    }
}
