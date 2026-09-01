package com.devsphere.user.event;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    public static final String DEFAULT_DOMAIN_EVENTS_TOPIC = "devsphere.domain.events";
    public static final String USER_EVENTS_TOPIC = "devsphere.user.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    public KafkaDomainEventPublisher() {
        this(null, null);
    }

    @Autowired
    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.kafkaTemplate = kafkaTemplate;
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

        log.info("Publishing domain event [type={}, eventId={}, traceId={}]",
                event.getEventType(), event.getEventId(), event.getTraceId());

        // 1. Publish locally via Spring ApplicationEventPublisher for in-memory listeners
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(event);
        }

        // 2. Transmit to Kafka topic with MDC trace header and business message key
        if (kafkaTemplate != null) {
            String topic = resolveTopic(event);
            String messageKey = resolveMessageKey(event);

            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, messageKey, event);
            if (traceId != null && !traceId.isBlank()) {
                record.headers().add("X-Trace-Id", traceId.getBytes(StandardCharsets.UTF_8));
                record.headers().add("traceId", traceId.getBytes(StandardCharsets.UTF_8));
            }
            if (event.getEventType() != null) {
                record.headers().add("event_type", event.getEventType().getBytes(StandardCharsets.UTF_8));
            }

            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish domain event to Kafka [topic={}, key={}, eventId={}]: {}",
                            topic, messageKey, event.getEventId(), ex.getMessage(), ex);
                } else {
                    log.info("Successfully published domain event to Kafka [topic={}, partition={}, offset={}, key={}, eventId={}]",
                            topic,
                            result != null && result.getRecordMetadata() != null ? result.getRecordMetadata().partition() : -1,
                            result != null && result.getRecordMetadata() != null ? result.getRecordMetadata().offset() : -1,
                            messageKey,
                            event.getEventId());
                }
            });
        }
    }

    private String resolveTopic(DomainEvent event) {
        if ("UserRegistered".equalsIgnoreCase(event.getEventType()) || "USER_REGISTERED".equalsIgnoreCase(event.getEventType())) {
            return USER_EVENTS_TOPIC;
        }
        return DEFAULT_DOMAIN_EVENTS_TOPIC;
    }

    private String resolveMessageKey(DomainEvent event) {
        if (event instanceof ResumeVersionPublishedEvent rve) {
            return rve.getResumeProfileId() != null ? rve.getResumeProfileId().toString() : rve.getUserId().toString();
        } else if (event instanceof PublicResumeViewEvent pve) {
            return pve.getResumeProfileId() != null ? pve.getResumeProfileId().toString() : (pve.getPublicId() != null ? pve.getPublicId() : event.getEventId());
        } else if (event instanceof PublicResumeShareRevokedEvent pre) {
            return pre.getResumeProfileId() != null ? pre.getResumeProfileId().toString() : (pre.getPublicId() != null ? pre.getPublicId() : event.getEventId());
        } else if (event instanceof UserRegisteredEvent ure) {
            return ure.getUserId() != null ? ure.getUserId().toString() : ure.getEventId();
        }
        return event.getEventId();
    }
}
