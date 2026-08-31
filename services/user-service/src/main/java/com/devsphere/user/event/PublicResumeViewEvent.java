package com.devsphere.user.event;

import java.time.Instant;

public class PublicResumeViewEvent {

    private final String publicId;
    private final Long resumeProfileId;
    private final String clientIp;
    private final String referrer;
    private final String userAgent;
    private final Instant timestamp;

    public PublicResumeViewEvent(String publicId, Long resumeProfileId, String clientIp, String referrer, String userAgent) {
        this.publicId = publicId;
        this.resumeProfileId = resumeProfileId;
        this.clientIp = clientIp;
        this.referrer = referrer;
        this.userAgent = userAgent;
        this.timestamp = Instant.now();
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
