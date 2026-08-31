package com.devsphere.user.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null domain event");
            return;
        }

        String eventTraceId = event.getTraceId();
        if (eventTraceId != null && !eventTraceId.isBlank()) {
            if (MDC.get("traceId") == null || MDC.get("traceId").isBlank()) {
                MDC.put("traceId", eventTraceId);
            }
        }

        log.info("Publishing domain event [type={}, eventId={}, traceId={}]",
                event.getEventType(), event.getEventId(), event.getTraceId());

        applicationEventPublisher.publishEvent(event);
    }
}
