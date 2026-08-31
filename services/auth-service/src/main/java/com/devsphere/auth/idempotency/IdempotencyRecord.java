package com.devsphere.auth.idempotency;

public class IdempotencyRecord {

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    private Status status;
    private String fingerprint;
    private int httpStatus;
    private String responseBody;
    private String contentType;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(Status status, String fingerprint) {
        this.status = status;
        this.fingerprint = fingerprint;
    }

    public IdempotencyRecord(Status status, String fingerprint, int httpStatus, String responseBody, String contentType) {
        this.status = status;
        this.fingerprint = fingerprint;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.contentType = contentType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
