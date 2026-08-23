package com.devsphere.user.event;

import com.devsphere.user.entity.ProcessedEvent;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.ProcessedEventRepository;
import com.devsphere.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public UserRegisteredEventConsumer(UserProfileRepository userProfileRepository,
                                       ProcessedEventRepository processedEventRepository) {
        this.userProfileRepository = userProfileRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = USER_EVENTS_TOPIC, groupId = USER_SERVICE_GROUP_ID)
    @Transactional
    public void consumeUserRegisteredEvent(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("Received null Kafka message in UserRegisteredEventConsumer");
            throw new IllegalArgumentException("Received null Kafka message");
        }

        log.info("Received UserRegisteredEvent - eventId: {}, eventType: {}, userId: {}",
                event.getEventId(), event.getEventType(), event.getUserId());

        if (!isValidEvent(event)) {
            log.error("Processing failure: invalid or unsupported event payload with eventId: {}, eventType: {}, version: {}, userId: {}",
                    event.getEventId(), event.getEventType(), event.getEventVersion(), event.getUserId());
            throw new IllegalArgumentException("Invalid or unsupported UserRegisteredEvent payload");
        }

        String eventId = event.getEventId();
        Long userId = event.getUserId();

        // 1. Idempotency Check using eventId
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed for eventId: {} (userId: {}). Safely acknowledging duplicate event.",
                    eventId, userId);
            return;
        }

        // 2. Business Processing & Processed Event Persistence within atomic transaction
        boolean profileExists = userProfileRepository.findByUserId(userId).isPresent();
        if (!profileExists) {
            UserProfile newProfile = new UserProfile(userId);
            userProfileRepository.save(newProfile);
            log.info("Created user profile for userId: {} (eventId: {})", userId, eventId);
        } else {
            log.info("User profile already exists for userId: {}, recording processed eventId: {}", userId, eventId);
        }

        try {
            ProcessedEvent processedEvent = new ProcessedEvent(eventId, event.getEventType());
            processedEventRepository.saveAndFlush(processedEvent);
            log.info("Successfully recorded processed eventId: {} for userId: {}", eventId, userId);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate event detected via DB unique constraint for eventId: {}. Safely acknowledging.", eventId);
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
