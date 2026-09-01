package com.devsphere.user.outbox;

import com.devsphere.user.event.DomainEventPublisher;
import com.devsphere.user.event.ResumeVersionPublishedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"devsphere.domain.events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=false",
        "app.outbox.polling-interval=500",
        "app.outbox.max-retries=3",
        "app.outbox.batch-size=10"
})
@DirtiesContext
class TransactionalOutboxIntegrationTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, Object> mockKafkaTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        outboxEventRepository.deleteAll();
    }

    @Test
    void businessTransactionCommit_persistsOutboxEventAtomically() {
        Long resumeProfileId = 5001L;

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                        resumeProfileId, 6001L, 1, 7001L
                );
                domainEventPublisher.publish(event);
            }
        });

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent outboxEvent = events.get(0);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("RESUME_PROFILE");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(resumeProfileId.toString());
        assertThat(outboxEvent.getEventType()).isEqualTo("ResumeVersionPublished");
    }

    @Test
    void businessTransactionRollback_rollsBackOutboxEvent() {
        try {
            transactionTemplate.execute(status -> {
                ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                        5002L, 6002L, 1, 7002L
                );
                domainEventPublisher.publish(event);
                throw new RuntimeException("Simulated business transaction failure");
            });
        } catch (RuntimeException ignored) {
        }

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).isEmpty();
    }

    @Test
    void pendingOutboxEvent_isPublishedToKafka_andMarkedPublished() {
        Long resumeProfileId = 5003L;
        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                resumeProfileId, 6003L, 2, 7003L
        );

        transactionTemplate.executeWithoutResult(status -> domainEventPublisher.publish(event));

        when(mockKafkaTemplate.send(any(org.apache.kafka.clients.producer.ProducerRecord.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<OutboxEvent> opt = outboxEventRepository.findByEventId(event.getEventId());
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(opt.get().getPublishedAt()).isNotNull();
        });
    }

    @Test
    void kafkaFailure_preservesPendingStatusAndIncrementsRetryCount() {
        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                5004L, 6004L, 1, 7004L
        );

        transactionTemplate.executeWithoutResult(status -> domainEventPublisher.publish(event));

        when(mockKafkaTemplate.send(any(org.apache.kafka.clients.producer.ProducerRecord.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("Kafka connection refused")));

        outboxPublisher.publishPendingEvents();

        Optional<OutboxEvent> opt = outboxEventRepository.findByEventId(event.getEventId());
        assertThat(opt).isPresent();
        assertThat(opt.get().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(opt.get().getRetryCount()).isEqualTo(1);
        assertThat(opt.get().getLastError()).contains("Kafka connection refused");
    }

    @Test
    void maxRetriesExhaustion_updatesOutboxStatusToFailed() {
        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                5005L, 6005L, 1, 7005L
        );

        transactionTemplate.executeWithoutResult(status -> domainEventPublisher.publish(event));

        when(mockKafkaTemplate.send(any(org.apache.kafka.clients.producer.ProducerRecord.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("Persistent Kafka Error")));

        outboxPublisher.publishPendingEvents();
        outboxPublisher.publishPendingEvents();
        outboxPublisher.publishPendingEvents();

        Optional<OutboxEvent> opt = outboxEventRepository.findByEventId(event.getEventId());
        assertThat(opt).isPresent();
        assertThat(opt.get().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(opt.get().getRetryCount()).isEqualTo(3);
    }

    @Test
    void outboxEventPayload_isDeserializableAndRetainsTraceIdAndKey() throws Exception {
        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                5006L, 6006L, 3, 7006L
        );

        transactionTemplate.executeWithoutResult(status -> domainEventPublisher.publish(event));

        OutboxEvent outboxEvent = outboxEventRepository.findByEventId(event.getEventId()).orElseThrow();
        assertThat(outboxEvent.getPayload()).contains(event.getEventId());
        assertThat(outboxEvent.getAggregateId()).isEqualTo("5006");

        ResumeVersionPublishedEvent deserialized = objectMapper.readValue(outboxEvent.getPayload(), ResumeVersionPublishedEvent.class);
        assertThat(deserialized.getResumeProfileId()).isEqualTo(5006L);
        assertThat(deserialized.getResumeVersionId()).isEqualTo(6006L);
        assertThat(deserialized.getUserId()).isEqualTo(7006L);
    }
}
