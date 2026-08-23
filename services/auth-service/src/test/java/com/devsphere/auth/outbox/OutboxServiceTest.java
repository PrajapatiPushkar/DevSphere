package com.devsphere.auth.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    void saveUserRegisteredOutboxEvent_createsPendingOutboxRecord() {
        Long userId = 101L;

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxEvent saved = outboxService.saveUserRegisteredOutboxEvent(userId);

        assertThat(saved).isNotNull();
        assertThat(saved.getEventId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("USER");
        assertThat(saved.getAggregateId()).isEqualTo("101");
        assertThat(saved.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(saved.getEventVersion()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getPayload()).contains("\"userId\":101");
        assertThat(saved.getPayload()).contains("\"eventType\":\"USER_REGISTERED\"");

        // Ensure zero credential/secret exposure in outbox payload
        assertThat(saved.getPayload()).doesNotContain("password");
        assertThat(saved.getPayload()).doesNotContain("passwordHash");
        assertThat(saved.getPayload()).doesNotContain("jwtSecret");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getAggregateId()).isEqualTo("101");
    }

    @Test
    void saveUserRegisteredOutboxEvent_nullUserId_throwsException() {
        assertThatThrownBy(() -> outboxService.saveUserRegisteredOutboxEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID must not be null");
    }
}
