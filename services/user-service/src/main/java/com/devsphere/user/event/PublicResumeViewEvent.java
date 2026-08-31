package com.devsphere.user.event;

import java.time.Instant;

public class PublicResumeViewEvent extends BaseDomainEvent {

    public static final String EVENT_TYPE = "PublicResumeViewed";

    private final String publicId;
    private final Long resumeProfileId;
    private final String clientIp;
    private final String referrer;
    private final String userAgent;

    public PublicResumeViewEvent(String publicId, Long resumeProfileId, String clientIp, String referrer, String userAgent) {
        super(EVENT_TYPE, 1);
        this.publicId = publicId;
        this.resumeProfileId = resumeProfileId;
        this.clientIp = clientIp;
        this.referrer = referrer;
        this.userAgent = userAgent;
    }

    public PublicResumeViewEvent(String eventId, Integer eventVersion, Instant occurredAt, String traceId,
                                 String publicId, Long resumeProfileId, String clientIp, String referrer, String userAgent) {
        super(eventId, EVENT_TYPE, eventVersion, occurredAt, traceId);
        this.publicId = publicId;
        this.resumeProfileId = resumeProfileId;
        this.clientIp = clientIp;
        this.referrer = referrer;
        this.userAgent = userAgent;
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
        return getOccurredAt();
    }
}
