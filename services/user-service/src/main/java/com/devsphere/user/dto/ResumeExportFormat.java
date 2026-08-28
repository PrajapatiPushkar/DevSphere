package com.devsphere.user.dto;

import java.util.Locale;

public enum ResumeExportFormat {
    HTML("text/html;charset=UTF-8", "html", false),
    PDF("application/pdf", "pdf", true),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", true);

    private final String mediaType;
    private final String extension;
    private final boolean attachment;

    ResumeExportFormat(String mediaType, String extension, boolean attachment) {
        this.mediaType = mediaType;
        this.extension = extension;
        this.attachment = attachment;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isAttachment() {
        return attachment;
    }

    public static ResumeExportFormat fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return PDF;
        }
        String clean = extension.trim().toLowerCase(Locale.ENGLISH);
        if (clean.startsWith(".")) {
            clean = clean.substring(1);
        }
        for (ResumeExportFormat format : values()) {
            if (format.getExtension().equalsIgnoreCase(clean)) {
                return format;
            }
        }
        return PDF;
    }
}
