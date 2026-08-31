package com.devsphere.user.event;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;

public class UserRegisteredEvent implements DomainEvent {

    public static final String EVENT_TYPE = "USER_REGISTERED";

    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private Instant occurredAt;
    private String traceId;
    private Long userId;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(Long userId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = EVENT_TYPE;
        this.eventVersion = 1;
        this.occurredAt = Instant.now();
        this.traceId = captureTraceId();
        this.userId = userId;
    }

    public UserRegisteredEvent(String eventId, String eventType, Integer eventVersion, Instant occurredAt, Long userId) {
        this(eventId, eventType, eventVersion, occurredAt, captureTraceId(), userId);
    }

    public UserRegisteredEvent(String eventId, String eventType, Integer eventVersion, Instant occurredAt, String traceId, Long userId) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.eventType = eventType != null ? eventType : EVENT_TYPE;
        this.eventVersion = eventVersion != null ? eventVersion : 1;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.traceId = traceId != null ? traceId : captureTraceId();
        this.userId = userId;
    }

    private static String captureTraceId() {
        String mdcTrace = MDC.get("traceId");
        if (mdcTrace != null && !mdcTrace.isBlank()) {
            return mdcTrace;
        }
        String altTrace = MDC.get("trace_id");
        return altTrace != null ? altTrace : "";
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public Integer getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Integer eventVersion) {
        this.eventVersion = eventVersion;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
