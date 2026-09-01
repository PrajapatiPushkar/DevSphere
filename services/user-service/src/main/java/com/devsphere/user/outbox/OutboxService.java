package com.devsphere.user.outbox;

import com.devsphere.user.event.DomainEvent;
import com.devsphere.user.event.PublicResumeShareRevokedEvent;
import com.devsphere.user.event.PublicResumeViewEvent;
import com.devsphere.user.event.ResumeVersionPublishedEvent;
import com.devsphere.user.event.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent saveDomainEvent(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to save null domain event to outbox");
            return null;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            String aggregateType = resolveAggregateType(event);
            String aggregateId = resolveAggregateId(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    event.getEventId(),
                    aggregateType,
                    aggregateId,
                    event.getEventType(),
                    event.getEventVersion() != null ? event.getEventVersion() : 1,
                    payloadJson
            );

            OutboxEvent saved = outboxEventRepository.save(outboxEvent);
            log.info("Persisted OutboxEvent [eventId={}, aggregateType={}, aggregateId={}, eventType={}] to outbox",
                    saved.getEventId(), saved.getAggregateType(), saved.getAggregateId(), saved.getEventType());
            return saved;
        } catch (Exception e) {
            log.error("Failed to serialize and save domain event [eventId={}, eventType={}] to outbox: {}",
                    event.getEventId(), event.getEventType(), e.getMessage(), e);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Failed to save outbox event", e);
        }
    }

    private String resolveAggregateType(DomainEvent event) {
        if (event instanceof ResumeVersionPublishedEvent || event instanceof PublicResumeViewEvent || event instanceof PublicResumeShareRevokedEvent) {
            return "RESUME_PROFILE";
        } else if (event instanceof UserRegisteredEvent) {
            return "USER";
        }
        return "DOMAIN_EVENT";
    }

    private String resolveAggregateId(DomainEvent event) {
        if (event instanceof ResumeVersionPublishedEvent rve) {
            return rve.getResumeProfileId() != null ? rve.getResumeProfileId().toString() : (rve.getUserId() != null ? rve.getUserId().toString() : event.getEventId());
        } else if (event instanceof PublicResumeViewEvent pve) {
            return pve.getResumeProfileId() != null ? pve.getResumeProfileId().toString() : (pve.getPublicId() != null ? pve.getPublicId() : event.getEventId());
        } else if (event instanceof PublicResumeShareRevokedEvent pre) {
            return pre.getResumeProfileId() != null ? pre.getResumeProfileId().toString() : (pre.getPublicId() != null ? pre.getPublicId() : event.getEventId());
        } else if (event instanceof UserRegisteredEvent ure) {
            return ure.getUserId() != null ? ure.getUserId().toString() : event.getEventId();
        }
        return event.getEventId();
    }
}
