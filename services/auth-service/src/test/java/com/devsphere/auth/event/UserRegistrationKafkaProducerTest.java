package com.devsphere.auth.event;

import com.devsphere.auth.config.KafkaProducerConfig;
import com.devsphere.auth.outbox.OutboxEvent;
import com.devsphere.auth.outbox.OutboxEventRepository;
import com.devsphere.auth.outbox.OutboxPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationKafkaProducerTest {

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
    void outboxPublisher_publishesUserRegisteredEventWithCorrectFields() throws Exception {
        Long userId = 101L;
        UserRegisteredEvent event = new UserRegisteredEvent(userId);
        String payload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = new OutboxEvent(event.getEventId(), "USER", "101", "USER_REGISTERED", 1, payload);

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.processEvent(outboxEvent);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaProducerConfig.USER_EVENTS_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("101");

        UserRegisteredEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isNotNull();
        assertThat(publishedEvent.getUserId()).isEqualTo(userId);
        assertThat(publishedEvent.getEventId()).isNotBlank();
        assertThat(publishedEvent.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(publishedEvent.getEventVersion()).isEqualTo(1);
        assertThat(publishedEvent.getOccurredAt()).isNotNull();
    }

    @Test
    void userRegisteredEventPayload_doesNotContainPasswordsOrTokens() {
        assertThat(UserRegisteredEvent.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("password", "token", "jwt", "bcrypt", "secret");
    }
}
