package com.devsphere.user.event;

import com.devsphere.user.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    private final OutboxService outboxService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public KafkaDomainEventPublisher() {
        this(null, null);
    }

    @Autowired
    public KafkaDomainEventPublisher(OutboxService outboxService,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.outboxService = outboxService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null domain event");
            return;
        }

        String traceId = event.getTraceId();
        if (traceId != null && !traceId.isBlank()) {
            if (MDC.get("traceId") == null || MDC.get("traceId").isBlank()) {
                MDC.put("traceId", traceId);
            }
        }

        log.info("Publishing domain event via Transactional Outbox [type={}, eventId={}, traceId={}]",
                event.getEventType(), event.getEventId(), event.getTraceId());

        // 1. Publish locally via Spring ApplicationEventPublisher for in-memory listeners
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(event);
        }

        // 2. Persist outbox event inside the active database transaction
        if (outboxService != null) {
            outboxService.saveDomainEvent(event);
        }
    }
}
