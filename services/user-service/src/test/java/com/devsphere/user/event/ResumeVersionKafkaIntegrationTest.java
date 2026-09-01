package com.devsphere.user.event;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"devsphere.domain.events", "devsphere.domain.events.DLT"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "app.kafka.consumer.max-attempts=3",
        "app.kafka.consumer.retry-backoff-ms=200"
})
@DirtiesContext
class ResumeVersionKafkaIntegrationTest {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    private ResumeActivityEventListener resumeActivityEventListener;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> dltConsumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-resume-dlt-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        dltConsumer = consumerFactory.createConsumer();
        dltConsumer.subscribe(Collections.singletonList("devsphere.domain.events.DLT"));
    }

    @AfterEach
    void tearDown() {
        if (dltConsumer != null) {
            dltConsumer.close();
        }
    }

    @Test
    void domainEventPublisher_publishesResumeVersionPublishedEventToKafka_andConsumerInvokesHandler() {
        Long resumeProfileId = 1001L;
        Long resumeVersionId = 2001L;
        Integer versionNumber = 1;
        Long userId = 3001L;
        String testTraceId = "trace-kafka-test-1234";

        MDC.put("traceId", testTraceId);

        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(
                resumeProfileId, resumeVersionId, versionNumber, userId
        );

        // Verify DomainEventPublisher abstraction remains intact while publishing to Kafka
        domainEventPublisher.publish(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(resumeActivityEventListener, atLeastOnce()).onResumeVersionPublished(any(ResumeVersionPublishedEvent.class));
        });

        MDC.clear();
    }

    @Test
    void malformedEvent_onDomainEventsTopic_routesToDLT_afterRetryExhaustion() {
        kafkaTemplate.send("devsphere.domain.events", "invalid-key", "{ malformed_json_payload }");

        await().atMost(Duration.ofSeconds(10))
                .ignoreException(IllegalStateException.class)
                .untilAsserted(() -> {
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(dltConsumer, "devsphere.domain.events.DLT", Duration.ofSeconds(1));
            assertThat(record).isNotNull();
            assertThat(record.topic()).isEqualTo("devsphere.domain.events.DLT");
            assertThat(new String(record.headers().lastHeader("kafka_dlt-original-topic").value())).isEqualTo("devsphere.domain.events");
        });
    }

    @Test
    void domainEventPublisherAbstraction_isKafkaDomainEventPublisher() {
        assertThat(domainEventPublisher).isInstanceOf(KafkaDomainEventPublisher.class);
    }
}
