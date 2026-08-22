package com.devsphere.user.event;

import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"devsphere.user.v1"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@DirtiesContext
class UserKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void cleanUp() {
        userProfileRepository.deleteAll();
    }

    @Test
    void publishedUserRegisteredEvent_isConsumedAndInitializesUserProfile() {
        Long userId = 501L;
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
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
        });
    }

    @Test
    void duplicateUserRegisteredEvent_isProcessedIdempotently() {
        Long userId = 502L;
        UserRegisteredEvent event1 = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );
        UserRegisteredEvent event2 = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
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
        });
    }
}
