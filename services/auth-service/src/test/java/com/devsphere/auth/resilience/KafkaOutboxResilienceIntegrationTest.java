package com.devsphere.auth.resilience;

import com.devsphere.auth.event.UserRegisteredEvent;
import com.devsphere.auth.outbox.OutboxEvent;
import com.devsphere.auth.outbox.OutboxEventRepository;
import com.devsphere.auth.outbox.OutboxPublisher;
import com.devsphere.auth.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaOutboxResilienceIntegrationTest {

    private OutboxEventRepository outboxEventRepository;
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper, meterRegistry, null, 50, 5);
    }

    @Test
    @DisplayName("Kafka failure keeps Outbox event in PENDING state with incremented retry count")
    void kafkaFailureKeepsOutboxEventPendingForRetry() {
        OutboxEvent event = new OutboxEvent("evt-100", "User", "100", "UserRegisteredEvent", 1, "{\"eventId\":\"evt-100\",\"eventType\":\"UserRegisteredEvent\",\"eventVersion\":1,\"userId\":100}");
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);

        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka cluster connection refused"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        outboxPublisher.processEvent(event);

        assertEquals(1, event.getRetryCount());
        assertEquals("Kafka cluster connection refused", event.getLastError());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        verify(outboxEventRepository, times(1)).save(event);
        assertEquals(1.0, meterRegistry.counter("devsphere.outbox.publish.failures.total", "event_type", "UserRegisteredEvent").count());
    }

    @Test
    @DisplayName("Kafka failure reaching max retries marks Outbox event FAILED")
    void kafkaFailureReachingMaxRetriesMarksEventFailed() {
        OutboxEvent event = new OutboxEvent("evt-101", "User", "101", "UserRegisteredEvent", 1, "{\"eventId\":\"evt-101\",\"eventType\":\"UserRegisteredEvent\",\"eventVersion\":1,\"userId\":101}");
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(4);

        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka broker timeout"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        outboxPublisher.processEvent(event);

        assertEquals(5, event.getRetryCount());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        verify(outboxEventRepository, times(1)).save(event);
    }
}
