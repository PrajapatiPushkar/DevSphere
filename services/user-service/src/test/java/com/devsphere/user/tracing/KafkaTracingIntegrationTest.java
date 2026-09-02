package com.devsphere.user.tracing;

import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.event.UserRegisteredEvent;
import com.devsphere.user.event.UserRegisteredEventConsumer;
import com.devsphere.user.repository.ProcessedEventRepository;
import com.devsphere.user.repository.UserProfileRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaTracingIntegrationTest {

    private UserProfileRepository userProfileRepository;
    private ProcessedEventRepository processedEventRepository;
    private SimpleMeterRegistry meterRegistry;
    private Tracer tracer;
    private ScopedSpan scopedSpan;
    private UserRegisteredEventConsumer consumer;

    @BeforeEach
    void setUp() {
        userProfileRepository = Mockito.mock(UserProfileRepository.class);
        processedEventRepository = Mockito.mock(ProcessedEventRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        tracer = Mockito.mock(Tracer.class);
        scopedSpan = Mockito.mock(ScopedSpan.class);

        when(tracer.startScopedSpan(anyString())).thenReturn(scopedSpan);

        consumer = new UserRegisteredEventConsumer(
                userProfileRepository,
                processedEventRepository,
                meterRegistry,
                tracer
        );
    }

    @Test
    @DisplayName("Should create kafka.user-registered.process span when processing Kafka event")
    void testKafkaConsumerCreatesSpan() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-trace-101",
                "USER_REGISTERED",
                1,
                java.time.Instant.now(),
                300L
        );

        when(processedEventRepository.existsByEventIdAndConsumerGroup(anyString(), anyString())).thenReturn(false);
        when(userProfileRepository.findByUserId(300L)).thenReturn(Optional.empty());

        consumer.consumeUserRegisteredEvent(event);

        verify(tracer).startScopedSpan("kafka.user-registered.process");
        verify(scopedSpan).tag("event.type", "USER_REGISTERED");
        verify(scopedSpan).tag("service.operation", "consumeUserRegisteredEvent");
        verify(scopedSpan).end();

        verify(userProfileRepository).save(any(UserProfile.class));
        verify(processedEventRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should preserve idempotency check when processing duplicate event with tracing active")
    void testDuplicateEventIdempotencyWithTracing() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-trace-dup-101",
                "USER_REGISTERED",
                1,
                java.time.Instant.now(),
                300L
        );

        when(processedEventRepository.existsByEventIdAndConsumerGroup(anyString(), anyString())).thenReturn(true);

        consumer.consumeUserRegisteredEvent(event);

        verify(tracer).startScopedSpan("kafka.user-registered.process");
        verify(scopedSpan).end();

        // Idempotency: user profile should NOT be saved again
        verify(userProfileRepository, never()).save(any());
    }
}
