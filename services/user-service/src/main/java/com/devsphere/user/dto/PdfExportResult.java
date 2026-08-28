package com.devsphere.user.dto;

public class PdfExportResult {

    private final byte[] pdfBytes;
    private final String filename;

    public PdfExportResult(byte[] pdfBytes, String filename) {
        this.pdfBytes = pdfBytes;
        this.filename = filename;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public String getFilename() {
        return filename;
    }
}
