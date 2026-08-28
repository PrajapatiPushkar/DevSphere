package com.devsphere.user.dto;

public class ResumeExportResult {

    private final byte[] content;
    private final String contentType;
    private final String filename;
    private final boolean attachment;

    public ResumeExportResult(byte[] content, String contentType, String filename, boolean attachment) {
        this.content = content;
        this.contentType = contentType;
        this.filename = filename;
        this.attachment = attachment;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isAttachment() {
        return attachment;
    }
}
