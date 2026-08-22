package com.devsphere.auth.event;

import com.devsphere.auth.config.KafkaProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public UserRegisteredEventListener(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredDomainEvent domainEvent) {
        Long userId = domainEvent.getUserId();
        UserRegisteredEvent event = new UserRegisteredEvent(userId);

        log.info("Publishing UserRegisteredEvent for userId: {}, eventId: {}", userId, event.getEventId());

        try {
            kafkaTemplate.send(KafkaProducerConfig.USER_EVENTS_TOPIC, String.valueOf(userId), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish UserRegisteredEvent for userId: {}, eventId: {}: {}",
                                    userId, event.getEventId(), ex.getMessage(), ex);
                        } else {
                            log.info("UserRegisteredEvent published successfully for userId: {}, eventId: {}",
                                    userId, event.getEventId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending UserRegisteredEvent to Kafka for userId: {}: {}", userId, e.getMessage(), e);
        }
    }
}
