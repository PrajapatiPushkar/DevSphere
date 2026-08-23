package com.devsphere.user.event;

import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.ProcessedEventRepository;
import com.devsphere.user.repository.UserProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"devsphere.user.v1", "devsphere.user.v1.DLT"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "app.kafka.consumer.max-attempts=3",
        "app.kafka.consumer.retry-backoff-ms=200"
})
@DirtiesContext
class UserKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> dltConsumer;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
        processedEventRepository.deleteAll();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-dlt-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        dltConsumer = consumerFactory.createConsumer();
        dltConsumer.subscribe(Collections.singletonList("devsphere.user.v1.DLT"));
    }

    @AfterEach
    void tearDown() {
        if (dltConsumer != null) {
            dltConsumer.close();
        }
    }

    @Test
    void publishedUserRegisteredEvent_isConsumedAndInitializesUserProfileAndProcessedEvent() {
        Long userId = 501L;
        String eventId = UUID.randomUUID().toString();
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        kafkaTemplate.send("devsphere.user.v1", String.valueOf(userId), event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            assertThat(profileOpt).isPresent();
            assertThat(profileOpt.get().getUserId()).isEqualTo(userId);
            assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
        });
    }

    @Test
    void duplicateUserRegisteredEvent_isProcessedIdempotently() {
        Long userId = 502L;
        String eventId = "evt-duplicate-idempotency-test";
        UserRegisteredEvent event1 = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );
        UserRegisteredEvent event2 = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        kafkaTemplate.send("devsphere.user.v1", String.valueOf(userId), event1);
        kafkaTemplate.send("devsphere.user.v1", String.valueOf(userId), event2);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            assertThat(profileOpt).isPresent();
            assertThat(processedEventRepository.count()).isEqualTo(1);
        });
    }

    @Test
    void invalidUserRegisteredEvent_routesToDLT_afterRetryExhaustion() {
        Long userId = 503L;
        String eventId = "evt-invalid-version";
        UserRegisteredEvent invalidEvent = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                99, // unsupported version
                Instant.now(),
                userId
        );

        kafkaTemplate.send("devsphere.user.v1", String.valueOf(userId), invalidEvent);

        await().atMost(Duration.ofSeconds(10))
                .ignoreException(IllegalStateException.class)
                .untilAsserted(() -> {
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(dltConsumer, "devsphere.user.v1.DLT", Duration.ofSeconds(1));
            assertThat(record).isNotNull();
            assertThat(record.topic()).isEqualTo("devsphere.user.v1.DLT");
            assertThat(new String(record.headers().lastHeader("kafka_dlt-original-topic").value())).isEqualTo("devsphere.user.v1");
        });
    }

    @Test
    void malformedJsonPayload_routesToDLT() {
        kafkaTemplate.send("devsphere.user.v1", "key-malformed", "{ malformed_json_payload }");

        await().atMost(Duration.ofSeconds(10))
                .ignoreException(IllegalStateException.class)
                .untilAsserted(() -> {
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(dltConsumer, "devsphere.user.v1.DLT", Duration.ofSeconds(1));
            assertThat(record).isNotNull();
            assertThat(record.topic()).isEqualTo("devsphere.user.v1.DLT");
            assertThat(new String(record.headers().lastHeader("kafka_dlt-original-topic").value())).isEqualTo("devsphere.user.v1");
        });
    }
}
