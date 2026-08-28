package com.devsphere.user.util;

import java.util.Locale;

public class ResumeFilenameSanitizer {

    private static final int MAX_LENGTH = 100;

    private ResumeFilenameSanitizer() {
        // Utility class
    }

    public static String sanitizeFilename(String input) {
        return sanitizeFilename(input, "pdf");
    }

    public static String sanitizeFilename(String input, String extension) {
        String ext = (extension != null && !extension.isBlank()) ? extension.trim().toLowerCase(Locale.ENGLISH) : "pdf";
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        String defaultFilename = "resume." + ext;

        if (input == null || input.isBlank()) {
            return defaultFilename;
        }

        // Remove control characters, CR, LF, null bytes
        String sanitized = input.replaceAll("[\\r\\n\\t\\u0000-\\u001F\\u007F]", "");

        // Remove Windows drive letters (e.g. C:, D:)
        sanitized = sanitized.replaceAll("^[a-zA-Z]:", "");

        // Replace path separators (\ and /) with underscore
        sanitized = sanitized.replaceAll("[\\\\/]", "_");

        // Remove path traversal dot sequences (..)
        sanitized = sanitized.replaceAll("\\.{2,}", "");

        // Remove HTML / script tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Keep alphanumeric, spaces, underscores, hyphens, and dots
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_\\-\\s.]", "");

        // Trim leading and trailing whitespace or dots
        sanitized = sanitized.trim().replaceAll("^[.\\s]+|[.\\s]+$", "");

        if (sanitized.isBlank()) {
            return defaultFilename;
        }

        // Strip existing matching target extension (e.g. .pdf, .docx, .html)
        String extSuffix = "." + ext;
        if (sanitized.toLowerCase(Locale.ENGLISH).endsWith(extSuffix)) {
            sanitized = sanitized.substring(0, sanitized.length() - extSuffix.length()).trim();
        }

        // Also strip other known extensions if present at the end to prevent double extension confusion (e.g. resume.pdf.docx -> resume.docx)
        for (String knownExt : new String[]{"pdf", "docx", "html"}) {
            String knownSuffix = "." + knownExt;
            if (sanitized.toLowerCase(Locale.ENGLISH).endsWith(knownSuffix)) {
                sanitized = sanitized.substring(0, sanitized.length() - knownSuffix.length()).trim();
            }
        }

        // Trim dots or spaces after stripping extension
        sanitized = sanitized.trim().replaceAll("^[.\\s]+|[.\\s]+$", "");

        if (sanitized.isBlank()) {
            return defaultFilename;
        }

        // Truncate base filename if it exceeds max length
        if (sanitized.length() > MAX_LENGTH) {
            sanitized = sanitized.substring(0, MAX_LENGTH).trim();
        }

        return sanitized + "." + ext;
    }
}
