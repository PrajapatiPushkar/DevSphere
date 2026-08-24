package com.devsphere.user.event;

import com.devsphere.user.entity.ProcessedEvent;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.ProcessedEventRepository;
import com.devsphere.user.repository.UserProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRegisteredEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventConsumer.class);
    public static final String USER_EVENTS_TOPIC = "devsphere.user.v1";
    public static final String USER_SERVICE_GROUP_ID = "devsphere-user-service";

    private final UserProfileRepository userProfileRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public UserRegisteredEventConsumer(UserProfileRepository userProfileRepository,
                                       ProcessedEventRepository processedEventRepository) {
        this(userProfileRepository, processedEventRepository, new SimpleMeterRegistry(), null);
    }

    public UserRegisteredEventConsumer(UserProfileRepository userProfileRepository,
                                       ProcessedEventRepository processedEventRepository,
                                       MeterRegistry meterRegistry) {
        this(userProfileRepository, processedEventRepository, meterRegistry, null);
    }

    @Autowired(required = false)
    public UserRegisteredEventConsumer(UserProfileRepository userProfileRepository,
                                       ProcessedEventRepository processedEventRepository,
                                       MeterRegistry meterRegistry,
                                       Tracer tracer) {
        this.userProfileRepository = userProfileRepository;
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    @KafkaListener(topics = USER_EVENTS_TOPIC, groupId = USER_SERVICE_GROUP_ID)
    @Transactional
    public void consumeUserRegisteredEvent(UserRegisteredEvent event) {
        ScopedSpan span = tracer != null ? tracer.startScopedSpan("kafka.user-registered.process") : null;
        if (span != null) {
            span.tag("event.type", event != null && event.getEventType() != null ? event.getEventType() : "USER_REGISTERED");
            span.tag("service.operation", "consumeUserRegisteredEvent");
        }

        try {
            if (event == null) {
                log.warn("Received null Kafka message in UserRegisteredEventConsumer");
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", "Unknown", "status", "failure").increment();
                throw new IllegalArgumentException("Received null Kafka message");
            }

            log.info("Received UserRegisteredEvent - eventId: {}, eventType: {}, userId: {}",
                    event.getEventId(), event.getEventType(), event.getUserId());

            if (!isValidEvent(event)) {
                log.error("Processing failure: invalid or unsupported event payload with eventId: {}, eventType: {}, version: {}, userId: {}",
                        event.getEventId(), event.getEventType(), event.getEventVersion(), event.getUserId());
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", event.getEventType() != null ? event.getEventType() : "Unknown", "status", "failure").increment();
                throw new IllegalArgumentException("Invalid or unsupported UserRegisteredEvent payload");
            }

            String eventId = event.getEventId();
            Long userId = event.getUserId();
            String eventType = event.getEventType();

            // 1. Idempotency Check using eventId
            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("Event already processed for eventId: {} (userId: {}). Safely acknowledging duplicate event.",
                        eventId, userId);
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", eventType, "status", "duplicate").increment();
                meterRegistry.counter("devsphere.kafka.duplicate.events.total", "event_type", eventType).increment();
                return;
            }

            try {
                // 2. Business Processing & Processed Event Persistence within atomic transaction
                boolean profileExists = userProfileRepository.findByUserId(userId).isPresent();
                if (!profileExists) {
                    UserProfile newProfile = new UserProfile(userId);
                    userProfileRepository.save(newProfile);
                    meterRegistry.counter("devsphere.user.profile.created.total", "source", "kafka").increment();
                    log.info("Created user profile for userId: {} (eventId: {})", userId, eventId);
                } else {
                    log.info("User profile already exists for userId: {}, recording processed eventId: {}", userId, eventId);
                }

                ProcessedEvent processedEvent = new ProcessedEvent(eventId, eventType);
                processedEventRepository.saveAndFlush(processedEvent);
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", eventType, "status", "success").increment();
                log.info("Successfully recorded processed eventId: {} for userId: {}", eventId, userId);
            } catch (DataIntegrityViolationException e) {
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", eventType, "status", "duplicate").increment();
                meterRegistry.counter("devsphere.kafka.duplicate.events.total", "event_type", eventType).increment();
                log.warn("Concurrent duplicate event detected via DB unique constraint for eventId: {}. Safely acknowledging.", eventId);
            } catch (Exception e) {
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", eventType, "status", "failure").increment();
                throw e;
            }
        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
        }
    }

    private boolean isValidEvent(UserRegisteredEvent event) {
        if (event.getUserId() == null) {
            return false;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            return false;
        }
        if (event.getEventType() == null || !"USER_REGISTERED".equalsIgnoreCase(event.getEventType())) {
            return false;
        }
        return event.getEventVersion() != null && event.getEventVersion() == 1;
    }
}

