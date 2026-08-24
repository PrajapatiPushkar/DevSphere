package com.devsphere.auth.outbox;

import com.devsphere.auth.config.KafkaProducerConfig;
import com.devsphere.auth.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
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

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    private final int batchSize;
    private final int maxRetries;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry(), null, batchSize, maxRetries);
    }

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this(outboxEventRepository, kafkaTemplate, objectMapper, meterRegistry, null, batchSize, maxRetries);
    }

    @Autowired(required = false)
    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            Tracer tracer,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${app.outbox.polling-interval:1000}")
    public void publishPendingEvents() {
        Pageable pageable = PageRequest.of(0, batchSize);
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, pageable);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} PENDING outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    public void processEvent(OutboxEvent event) {
        ScopedSpan span = tracer != null ? tracer.startScopedSpan("outbox.publish") : null;
        if (span != null) {
            span.tag("event.type", event.getEventType() != null ? event.getEventType() : "UserRegisteredEvent");
            span.tag("service.operation", "outbox.publish");
        }
        try {
            UserRegisteredEvent payload = objectMapper.readValue(event.getPayload(), UserRegisteredEvent.class);
            String messageKey = event.getAggregateId();

            log.info("Outbox publisher sending eventId: {}, aggregateId: {} to Kafka", event.getEventId(), messageKey);

            kafkaTemplate.send(KafkaProducerConfig.USER_EVENTS_TOPIC, messageKey, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            handleFailure(event, ex);
                        } else {
                            handleSuccess(event);
                        }
                    });
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

