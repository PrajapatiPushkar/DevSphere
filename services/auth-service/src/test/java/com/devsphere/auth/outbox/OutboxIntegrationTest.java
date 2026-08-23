package com.devsphere.auth.outbox;

import com.devsphere.auth.dto.RegisterRequest;
import com.devsphere.auth.dto.RegisterResponse;
import com.devsphere.auth.event.UserRegisteredEvent;
import com.devsphere.auth.repository.UserCredentialRepository;
import com.devsphere.auth.service.AuthService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @MockBean
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        userCredentialRepository.deleteAll();
    }

    @Test
    void registerUser_createsUserAndPendingOutboxEventInSameDatabaseTransaction() {
        RegisterRequest request = new RegisterRequest("outboxuser@example.com", "Password123!");

        RegisterResponse response = authService.register(request);

        assertThat(response.getId()).isNotNull();
        assertThat(userCredentialRepository.findByEmail("outboxuser@example.com")).isPresent();

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        OutboxEvent outboxEvent = outboxEvents.get(0);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(String.valueOf(response.getId()));
        assertThat(outboxEvent.getAggregateType()).isEqualTo("USER");
        assertThat(outboxEvent.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(outboxEvent.getEventVersion()).isEqualTo(1);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getPayload()).contains("\"userId\":" + response.getId());
        assertThat(outboxEvent.getPayload()).doesNotContain("Password123!");
    }

    @Test
    void outboxPublisher_processesPendingEvent_andUpdatesStatusToPublished() {
        RegisterRequest request = new RegisterRequest("kafkaoutbox@example.com", "Password123!");
        RegisterResponse response = authService.register(request);

        when(kafkaTemplate.send(eq("devsphere.user.v1"), eq(String.valueOf(response.getId())), any(UserRegisteredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        OutboxEvent publishedEvent = outboxEvents.get(0);

        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
    }
}
