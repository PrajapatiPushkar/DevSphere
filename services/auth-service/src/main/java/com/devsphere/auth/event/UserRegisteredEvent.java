package com.devsphere.auth.event;

import java.time.Instant;
import java.util.UUID;

public class UserRegisteredEvent {

    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private Instant occurredAt;
    private Long userId;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(Long userId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "USER_REGISTERED";
        this.eventVersion = 1;
        this.occurredAt = Instant.now();
        this.userId = userId;
    }

    public UserRegisteredEvent(String eventId, String eventType, Integer eventVersion, Instant occurredAt, Long userId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.occurredAt = occurredAt;
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Integer eventVersion) {
        this.eventVersion = eventVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
