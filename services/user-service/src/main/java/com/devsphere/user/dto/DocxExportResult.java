package com.devsphere.user.dto;

public class DocxExportResult {

    private final byte[] docxBytes;
    private final String filename;

    public DocxExportResult(byte[] docxBytes, String filename) {
        this.docxBytes = docxBytes;
        this.filename = filename;
    }

    public byte[] getDocxBytes() {
        return docxBytes;
    }

    public String getFilename() {
        return filename;
    }
}
