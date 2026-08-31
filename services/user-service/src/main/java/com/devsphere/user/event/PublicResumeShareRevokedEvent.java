package com.devsphere.user.event;

public class PublicResumeShareRevokedEvent extends BaseDomainEvent {

    public static final String EVENT_TYPE = "PublicResumeShareRevoked";

    private final Long resumeProfileId;
    private final String publicId;
    private final Long userId;

    public PublicResumeShareRevokedEvent(Long resumeProfileId, String publicId, Long userId) {
        super(EVENT_TYPE, 1);
        this.resumeProfileId = resumeProfileId;
        this.publicId = publicId;
        this.userId = userId;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getUserId() {
        return userId;
    }
}
