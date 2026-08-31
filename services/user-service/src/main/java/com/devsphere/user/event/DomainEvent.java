package com.devsphere.user.event;

import java.time.Instant;

public interface DomainEvent {

    String getEventId();

    String getEventType();

    Integer getEventVersion();

    Instant getOccurredAt();

    String getTraceId();
}
