package com.devsphere.auth.event;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;

public abstract class BaseDomainEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Integer eventVersion;
    private final Instant occurredAt;
    private final String traceId;

    protected BaseDomainEvent(String eventType) {
        this(UUID.randomUUID().toString(), eventType, 1, Instant.now(), captureTraceId());
    }

    protected BaseDomainEvent(String eventType, Integer eventVersion) {
        this(UUID.randomUUID().toString(), eventType, eventVersion, Instant.now(), captureTraceId());
    }

    protected BaseDomainEvent(String eventId, String eventType, Integer eventVersion, Instant occurredAt, String traceId) {
        this.eventId = eventId != null && !eventId.isBlank() ? eventId : UUID.randomUUID().toString();
        this.eventType = eventType;
        this.eventVersion = eventVersion != null ? eventVersion : 1;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.traceId = traceId != null ? traceId : captureTraceId();
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

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public Integer getEventVersion() {
        return eventVersion;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }
}
