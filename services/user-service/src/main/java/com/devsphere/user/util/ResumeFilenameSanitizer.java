package com.devsphere.user.util;

import java.util.Locale;

public class ResumeFilenameSanitizer {

    private static final String DEFAULT_FILENAME = "resume.pdf";
    private static final int MAX_LENGTH = 100;

    private ResumeFilenameSanitizer() {
        // Utility class
    }

    public static String sanitizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return DEFAULT_FILENAME;
        }

        // Remove control characters, CR, LF, null bytes
        String sanitized = input.replaceAll("[\\r\\n\\t\\u0000]", "");

        // Replace path traversal / directory separators (\ and /) with underscore
        sanitized = sanitized.replaceAll("[\\\\/]", "_");

        // Remove path traversal dot sequences (..)
        sanitized = sanitized.replaceAll("\\.{2,}", "");

        // Remove HTML tags / script tags if any
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Remove unsafe characters (keep alphanumeric, space, underscore, hyphen, dot)
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_\\-\\s.]", "");

        // Trim leading and trailing whitespace or dots
        sanitized = sanitized.trim().replaceAll("^[.\\s]+|[.\\s]+$", "");

        if (sanitized.isBlank()) {
            return DEFAULT_FILENAME;
        }

        // Strip existing .pdf if present before enforcing max length
        if (sanitized.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
            sanitized = sanitized.substring(0, sanitized.length() - 4).trim();
        }

        if (sanitized.isBlank()) {
            return DEFAULT_FILENAME;
        }

        // Truncate if exceeds max length
        if (sanitized.length() > MAX_LENGTH) {
            sanitized = sanitized.substring(0, MAX_LENGTH).trim();
        }

        return sanitized + ".pdf";
    }
}
