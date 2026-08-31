package com.devsphere.user.event;

public class ResumeVersionPublishedEvent extends BaseDomainEvent {

    public static final String EVENT_TYPE = "ResumeVersionPublished";

    private final Long resumeProfileId;
    private final Long resumeVersionId;
    private final Integer versionNumber;
    private final Long userId;

    public ResumeVersionPublishedEvent(Long resumeProfileId, Long resumeVersionId, Integer versionNumber, Long userId) {
        super(EVENT_TYPE, 1);
        this.resumeProfileId = resumeProfileId;
        this.resumeVersionId = resumeVersionId;
        this.versionNumber = versionNumber;
        this.userId = userId;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public Long getResumeVersionId() {
        return resumeVersionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public Long getUserId() {
        return userId;
    }
}
