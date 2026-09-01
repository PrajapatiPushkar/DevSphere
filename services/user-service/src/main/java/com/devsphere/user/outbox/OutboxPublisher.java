package com.devsphere.user.outbox;

import com.devsphere.user.event.DomainEvent;
import com.devsphere.user.event.PublicResumeShareRevokedEvent;
import com.devsphere.user.event.PublicResumeViewEvent;
import com.devsphere.user.event.ResumeVersionPublishedEvent;
import com.devsphere.user.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    public static final String DEFAULT_DOMAIN_EVENTS_TOPIC = "devsphere.domain.events";
    public static final String USER_EVENTS_TOPIC = "devsphere.user.v1";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    private final int batchSize;
    private final int maxRetries;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry(), null, batchSize, maxRetries);
    }

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this(outboxEventRepository, kafkaTemplate, objectMapper, meterRegistry, null, batchSize, maxRetries);
    }

    @Autowired(required = false)
    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            Tracer tracer,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.tracer = tracer;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${app.outbox.polling-interval:1000}")
    public void publishPendingEvents() {
        if (outboxEventRepository == null) {
            return;
        }

        Pageable pageable = PageRequest.of(0, batchSize);
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, pageable);

        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} PENDING outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    public void processEvent(OutboxEvent event) {
        if (event == null) {
            return;
        }

        ScopedSpan span = tracer != null ? tracer.startScopedSpan("outbox.publish") : null;
        if (span != null) {
            span.tag("event.type", event.getEventType() != null ? event.getEventType() : "DomainEvent");
            span.tag("service.operation", "outbox.publish");
        }

        try {
            Object payloadObj = deserializePayload(event);
            String topic = resolveTopic(event);
            String messageKey = event.getAggregateId();
            String traceId = extractTraceId(payloadObj);

            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, messageKey, payloadObj);
            if (traceId != null && !traceId.isBlank()) {
                record.headers().add("X-Trace-Id", traceId.getBytes(StandardCharsets.UTF_8));
                record.headers().add("traceId", traceId.getBytes(StandardCharsets.UTF_8));
            }
            if (event.getEventType() != null) {
                record.headers().add("event_type", event.getEventType().getBytes(StandardCharsets.UTF_8));
            }

            log.info("Outbox publisher sending eventId: {}, aggregateId: {}, topic: {} to Kafka",
                    event.getEventId(), messageKey, topic);

            if (kafkaTemplate != null) {
                kafkaTemplate.send(record).whenComplete((result, ex) -> {
                    if (ex != null) {
                        handleFailure(event, ex);
                    } else {
                        handleSuccess(event);
                    }
                });
            } else {
                handleSuccess(event);
            }
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            handleFailure(event, e);
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    private Object deserializePayload(OutboxEvent event) throws Exception {
        String type = event.getEventType();
        String payloadJson = event.getPayload();

        if ("ResumeVersionPublished".equalsIgnoreCase(type)) {
            return objectMapper.readValue(payloadJson, ResumeVersionPublishedEvent.class);
        } else if ("PublicResumeViewed".equalsIgnoreCase(type)) {
            return objectMapper.readValue(payloadJson, PublicResumeViewEvent.class);
        } else if ("PublicResumeShareRevoked".equalsIgnoreCase(type)) {
            return objectMapper.readValue(payloadJson, PublicResumeShareRevokedEvent.class);
        } else if ("USER_REGISTERED".equalsIgnoreCase(type) || "UserRegistered".equalsIgnoreCase(type)) {
            return objectMapper.readValue(payloadJson, UserRegisteredEvent.class);
        }
        return objectMapper.readValue(payloadJson, JsonNode.class);
    }

    private String resolveTopic(OutboxEvent event) {
        if ("USER_REGISTERED".equalsIgnoreCase(event.getEventType()) || "UserRegistered".equalsIgnoreCase(event.getEventType())) {
            return USER_EVENTS_TOPIC;
        }
        return DEFAULT_DOMAIN_EVENTS_TOPIC;
    }

    private String extractTraceId(Object payloadObj) {
        if (payloadObj instanceof DomainEvent de) {
            return de.getTraceId();
        } else if (payloadObj instanceof JsonNode node) {
            if (node.has("traceId")) {
                return node.get("traceId").asText();
            }
        }
        return null;
    }

    private void handleSuccess(OutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        outboxEventRepository.save(event);
        meterRegistry.counter("devsphere.outbox.events.published.total", "event_type", event.getEventType(), "status", "success").increment();
        log.info("Outbox event marked PUBLISHED for eventId: {}, aggregateId: {}", event.getEventId(), event.getAggregateId());
    }

    private void handleFailure(OutboxEvent event, Throwable ex) {
        int newRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(newRetryCount);
        event.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        meterRegistry.counter("devsphere.outbox.publish.failures.total", "event_type", event.getEventType()).increment();

        if (newRetryCount >= maxRetries) {
            event.setStatus(OutboxStatus.FAILED);
            meterRegistry.counter("devsphere.outbox.events.published.total", "event_type", event.getEventType(), "status", "failed").increment();
            log.error("Outbox event eventId: {} reached max retries ({}) and is marked FAILED: {}",
                    event.getEventId(), maxRetries, ex.getMessage());
        } else {
            log.warn("Outbox event eventId: {} publish failed (attempt {}/{}): {}",
                    event.getEventId(), newRetryCount, maxRetries, ex.getMessage());
        }

        outboxEventRepository.save(event);
    }
}
