package com.devsphere.auth.event;

import com.devsphere.auth.config.KafkaProducerConfig;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    private UserRegisteredEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new UserRegisteredEventListener(kafkaTemplate);
    }

    @Test
    void handleUserRegistered_publishesUserRegisteredEventWithCorrectFields() {
        Long userId = 101L;
        UserRegisteredDomainEvent domainEvent = new UserRegisteredDomainEvent(userId);

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        eventListener.handleUserRegistered(domainEvent);

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
        UserRegisteredEvent event = new UserRegisteredEvent(101L);

        // Verify structure has no password, token, or secret fields
        assertThat(UserRegisteredEvent.class.getDeclaredFields())
                .extracting("name")
                .containsExactlyInAnyOrder("eventId", "eventType", "eventVersion", "occurredAt", "userId");
    }
}
