package com.devsphere.auth.outbox;

import com.devsphere.auth.event.UserRegisteredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public OutboxEvent saveUserRegisteredOutboxEvent(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null for outbox event creation");
        }

        UserRegisteredEvent event = new UserRegisteredEvent(userId);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserRegisteredEvent for userId: {}", userId, e);
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                event.getEventId(),
                "USER",
                String.valueOf(userId),
                "USER_REGISTERED",
                1,
                jsonPayload
        );

        OutboxEvent saved = outboxEventRepository.save(outboxEvent);
        log.info("Outbox event created with status PENDING for userId: {}, eventId: {}", userId, event.getEventId());
        return saved;
    }
}
