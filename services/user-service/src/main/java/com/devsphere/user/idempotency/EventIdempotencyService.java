package com.devsphere.user.idempotency;

import com.devsphere.user.entity.ProcessedEvent;
import com.devsphere.user.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(EventIdempotencyService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;

    public EventIdempotencyService(ProcessedEventRepository processedEventRepository) {
        this(processedEventRepository, new SimpleMeterRegistry());
    }

    @Autowired(required = false)
    public EventIdempotencyService(ProcessedEventRepository processedEventRepository,
                                   MeterRegistry meterRegistry) {
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        String group = consumerGroup != null && !consumerGroup.isBlank() ? consumerGroup : "default";
        return processedEventRepository.existsByEventIdAndConsumerGroup(eventId, group)
                || processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public <T> EventProcessingResult<T> executeIdempotent(
            String eventId,
            String eventType,
            String consumerGroup,
            Supplier<T> businessAction) {

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank for idempotent processing");
        }

        String group = consumerGroup != null && !consumerGroup.isBlank() ? consumerGroup : "default";
        String type = eventType != null ? eventType : "Unknown";

        // 1. Check if already processed by this consumer group or legacy event_id
        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, group)
                || processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed for eventId: {}, consumerGroup: {}. Skipping business processing.", eventId, group);
            meterRegistry.counter("devsphere.events.idempotency.total", "event_type", type, "result", "duplicate").increment();
            return EventProcessingResult.duplicate();
        }

        try {
            // 2. Persist ProcessedEvent marker first inside the transaction to claim event identity
            ProcessedEvent processedEvent = new ProcessedEvent(eventId, type, group);
            processedEventRepository.saveAndFlush(processedEvent);

            // 3. Execute business side-effect
            T result = businessAction != null ? businessAction.get() : null;

            meterRegistry.counter("devsphere.events.idempotency.total", "event_type", type, "result", "processed").increment();
            log.info("Successfully processed and recorded idempotent event [eventId={}, eventType={}, consumerGroup={}]",
                    eventId, type, group);
            return EventProcessingResult.success(result);

        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate processing caught by DB unique constraint
            meterRegistry.counter("devsphere.events.idempotency.total", "event_type", type, "result", "duplicate").increment();
            log.warn("Concurrent duplicate event detected via DB unique constraint for eventId: {}, consumerGroup: {}. Safely ignoring duplicate.",
                    eventId, group);
            return EventProcessingResult.duplicate();
        } catch (Exception e) {
            meterRegistry.counter("devsphere.events.idempotency.total", "event_type", type, "result", "failed").increment();
            log.error("Failed to process event [eventId={}, eventType={}, consumerGroup={}]: {}",
                    eventId, type, group, e.getMessage(), e);
            throw e; // RETHROW so transaction rolls back, leaving NO processed_events marker
        }
    }
}
