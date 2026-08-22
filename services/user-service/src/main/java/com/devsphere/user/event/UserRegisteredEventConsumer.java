package com.devsphere.user.event;

import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRegisteredEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventConsumer.class);
    public static final String USER_EVENTS_TOPIC = "devsphere.user.v1";
    public static final String USER_SERVICE_GROUP_ID = "devsphere-user-service";

    private final UserProfileRepository userProfileRepository;

    public UserRegisteredEventConsumer(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(topics = USER_EVENTS_TOPIC, groupId = USER_SERVICE_GROUP_ID)
    @Transactional
    public void consumeUserRegisteredEvent(UserRegisteredEvent event) {
        if (event == null) {
            log.warn("Received null Kafka message in UserRegisteredEventConsumer");
            return;
        }

        log.info("Received UserRegisteredEvent for userId: {}, eventId: {}, eventType: {}",
                event.getUserId(), event.getEventId(), event.getEventType());

        if (!isValidEvent(event)) {
            log.warn("Skipping invalid or unsupported event payload with eventId: {}, eventType: {}, version: {}, userId: {}",
                    event.getEventId(), event.getEventType(), event.getEventVersion(), event.getUserId());
            return;
        }

        Long userId = event.getUserId();

        // Idempotency check: Ensure profile is not duplicated if event is delivered multiple times
        boolean exists = userProfileRepository.findByUserId(userId).isPresent();
        if (exists) {
            log.info("User profile already exists for userId: {} (eventId: {}). Skipping duplicate event execution.",
                    userId, event.getEventId());
            return;
        }

        UserProfile newProfile = new UserProfile(userId);
        userProfileRepository.save(newProfile);
        log.info("Successfully created initial user profile for userId: {} via UserRegisteredEvent (eventId: {})",
                userId, event.getEventId());
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
