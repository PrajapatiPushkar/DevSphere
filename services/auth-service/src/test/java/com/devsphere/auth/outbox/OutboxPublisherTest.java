package com.devsphere.auth.outbox;

import com.devsphere.auth.config.KafkaProducerConfig;
import com.devsphere.auth.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    private OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper, 50, 5);
    }

    @Test
    void publishPendingEvents_successfulKafkaSend_marksEventPublished() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent(101L);
        String payload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = new OutboxEvent(event.getEventId(), "USER", "101", "USER_REGISTERED", 1, payload);

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(eq(KafkaProducerConfig.USER_EVENTS_TOPIC), eq("101"), any(UserRegisteredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    void processEvent_whenKafkaFails_incrementsRetryCountAndSavesError() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent(101L);
        String payload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = new OutboxEvent(event.getEventId(), "USER", "101", "USER_REGISTERED", 1, payload);
        outboxEvent.setRetryCount(0);

        CompletableFuture<org.springframework.kafka.support.SendResult<String, UserRegisteredEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker down"));

        when(kafkaTemplate.send(eq(KafkaProducerConfig.USER_EVENTS_TOPIC), eq("101"), any(UserRegisteredEvent.class)))
                .thenReturn(failedFuture);

        outboxPublisher.processEvent(outboxEvent);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getLastError()).contains("Kafka broker down");
    }

    @Test
    void processEvent_whenMaxRetriesReached_marksStatusFailed() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent(101L);
        String payload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = new OutboxEvent(event.getEventId(), "USER", "101", "USER_REGISTERED", 1, payload);
        outboxEvent.setRetryCount(4); // 4 + 1 = 5 (maxRetries)

        CompletableFuture<org.springframework.kafka.support.SendResult<String, UserRegisteredEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Connection timeout"));

        when(kafkaTemplate.send(eq(KafkaProducerConfig.USER_EVENTS_TOPIC), eq("101"), any(UserRegisteredEvent.class)))
                .thenReturn(failedFuture);

        outboxPublisher.processEvent(outboxEvent);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(saved.getRetryCount()).isEqualTo(5);
        assertThat(saved.getLastError()).contains("Connection timeout");
    }
}
