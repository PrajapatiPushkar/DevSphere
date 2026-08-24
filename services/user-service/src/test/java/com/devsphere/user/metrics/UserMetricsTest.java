package com.devsphere.user.metrics;

import com.devsphere.user.cache.RedisUserProfileCache;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.event.UserRegisteredEvent;
import com.devsphere.user.event.UserRegisteredEventConsumer;
import com.devsphere.user.repository.ProcessedEventRepository;
import com.devsphere.user.repository.UserProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserMetricsTest {

    private UserProfileRepository userProfileRepository;
    private ProcessedEventRepository processedEventRepository;
    private MeterRegistry meterRegistry;
    private UserRegisteredEventConsumer consumer;

    @BeforeEach
    void setUp() {
        userProfileRepository = Mockito.mock(UserProfileRepository.class);
        processedEventRepository = Mockito.mock(ProcessedEventRepository.class);
        meterRegistry = new SimpleMeterRegistry();

        consumer = new UserRegisteredEventConsumer(userProfileRepository, processedEventRepository, meterRegistry);
    }

    @Test
    void consumeEvent_IncrementsProcessedAndProfileCreatedMetrics() {
        UserRegisteredEvent event = new UserRegisteredEvent("evt-100", "USER_REGISTERED", 1, Instant.now(), 101L);

        when(processedEventRepository.existsByEventId("evt-100")).thenReturn(false);
        when(userProfileRepository.findByUserId(101L)).thenReturn(Optional.empty());

        consumer.consumeUserRegisteredEvent(event);

        double processedCount = meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", "USER_REGISTERED", "status", "success").count();
        double profileCreatedCount = meterRegistry.counter("devsphere.user.profile.created.total", "source", "kafka").count();

        assertThat(processedCount).isEqualTo(1.0);
        assertThat(profileCreatedCount).isEqualTo(1.0);
    }

    @Test
    void consumeDuplicateEvent_IncrementsDuplicateMetric() {
        UserRegisteredEvent event = new UserRegisteredEvent("evt-duplicate", "USER_REGISTERED", 1, Instant.now(), 101L);

        when(processedEventRepository.existsByEventId("evt-duplicate")).thenReturn(true);

        consumer.consumeUserRegisteredEvent(event);

        double duplicateCount = meterRegistry.counter("devsphere.kafka.duplicate.events.total", "event_type", "USER_REGISTERED").count();
        double processedDuplicateCount = meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", "USER_REGISTERED", "status", "duplicate").count();

        assertThat(duplicateCount).isEqualTo(1.0);
        assertThat(processedDuplicateCount).isEqualTo(1.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisCache_IncrementsHitAndMissMetrics() {
        RedisTemplate<String, UserProfileResponse> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, UserProfileResponse> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        RedisUserProfileCache cache = new RedisUserProfileCache(redisTemplate, meterRegistry, Duration.ofMinutes(5));

        // Test Miss
        when(valueOps.get(anyString())).thenReturn(null);
        cache.get(101L);
        double missCount = meterRegistry.counter("devsphere.cache.misses.total", "cache", "user_profile").count();
        assertThat(missCount).isEqualTo(1.0);

        // Test Hit
        UserProfileResponse response = new UserProfileResponse(101L, "John", "Doe", "johndoe", "Bio", "123", Instant.now(), Instant.now());
        when(valueOps.get(anyString())).thenReturn(response);
        cache.get(101L);
        double hitCount = meterRegistry.counter("devsphere.cache.hits.total", "cache", "user_profile").count();
        assertThat(hitCount).isEqualTo(1.0);
    }
}
