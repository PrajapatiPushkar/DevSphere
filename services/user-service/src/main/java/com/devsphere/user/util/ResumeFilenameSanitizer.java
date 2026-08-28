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
            return defaultFilename;
        }

        // Strip existing target extension if present before enforcing max length
        String extSuffix = "." + ext;
        if (sanitized.toLowerCase(Locale.ENGLISH).endsWith(extSuffix)) {
            sanitized = sanitized.substring(0, sanitized.length() - extSuffix.length()).trim();
        }

        if (sanitized.isBlank()) {
            return defaultFilename;
        }

        // Truncate if exceeds max length
        if (sanitized.length() > MAX_LENGTH) {
            sanitized = sanitized.substring(0, MAX_LENGTH).trim();
        }

        return sanitized + "." + ext;
    }
}
