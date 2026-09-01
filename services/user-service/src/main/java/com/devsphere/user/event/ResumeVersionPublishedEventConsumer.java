package com.devsphere.user.event;

import com.devsphere.user.idempotency.EventIdempotencyService;
import com.devsphere.user.idempotency.EventProcessingResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ResumeVersionPublishedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ResumeVersionPublishedEventConsumer.class);
    public static final String DOMAIN_EVENTS_TOPIC = "devsphere.domain.events";
    public static final String RESUME_ACTIVITY_GROUP_ID = "devsphere-resume-activity-group";

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    private final ResumeActivityEventListener resumeActivityEventListener;
    private final EventIdempotencyService idempotencyService;

    public ResumeVersionPublishedEventConsumer() {
        this(new SimpleMeterRegistry(), null, null, null);
    }

    public ResumeVersionPublishedEventConsumer(MeterRegistry meterRegistry,
                                               ResumeActivityEventListener resumeActivityEventListener,
                                               EventIdempotencyService idempotencyService) {
        this(meterRegistry, null, resumeActivityEventListener, idempotencyService);
    }

    @Autowired(required = false)
    public ResumeVersionPublishedEventConsumer(MeterRegistry meterRegistry,
                                               Tracer tracer,
                                               ResumeActivityEventListener resumeActivityEventListener,
                                               EventIdempotencyService idempotencyService) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.tracer = tracer;
        this.resumeActivityEventListener = resumeActivityEventListener;
        this.idempotencyService = idempotencyService;
    }

    @KafkaListener(topics = DOMAIN_EVENTS_TOPIC, groupId = RESUME_ACTIVITY_GROUP_ID)
    public void consumeResumeVersionPublishedEvent(
            ResumeVersionPublishedEvent event,
            @Header(name = "X-Trace-Id", required = false) String headerTraceId) {

        ScopedSpan span = tracer != null ? tracer.startScopedSpan("kafka.resume-version-published.process") : null;
        if (span != null) {
            span.tag("event.type", event != null && event.getEventType() != null ? event.getEventType() : "ResumeVersionPublished");
            span.tag("service.operation", "consumeResumeVersionPublishedEvent");
        }

        String previousTrace = MDC.get("traceId");
        try {
            if (event == null) {
                log.warn("Received null Kafka message in ResumeVersionPublishedEventConsumer");
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", "Unknown", "status", "failure").increment();
                throw new IllegalArgumentException("Received null Kafka message");
            }

            String effectiveTraceId = event.getTraceId();
            if ((effectiveTraceId == null || effectiveTraceId.isBlank()) && headerTraceId != null && !headerTraceId.isBlank()) {
                effectiveTraceId = headerTraceId;
            }
            if (effectiveTraceId != null && !effectiveTraceId.isBlank()) {
                MDC.put("traceId", effectiveTraceId);
            }

            log.info("Received Kafka ResumeVersionPublishedEvent - eventId: {}, eventType: {}, resumeProfileId: {}, versionNumber: {}, userId: {}, traceId: {}",
                    event.getEventId(), event.getEventType(), event.getResumeProfileId(), event.getVersionNumber(), event.getUserId(), effectiveTraceId);

            if (!isValidEvent(event)) {
                log.error("Processing failure: invalid or unsupported ResumeVersionPublishedEvent payload with eventId: {}, eventType: {}, version: {}, resumeProfileId: {}",
                        event.getEventId(), event.getEventType(), event.getEventVersion(), event.getResumeProfileId());
                meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", event.getEventType() != null ? event.getEventType() : "Unknown", "status", "failure").increment();
                throw new IllegalArgumentException("Invalid or unsupported ResumeVersionPublishedEvent payload");
            }

            if (idempotencyService != null) {
                EventProcessingResult<Void> result = idempotencyService.executeIdempotent(
                        event.getEventId(),
                        event.getEventType(),
                        RESUME_ACTIVITY_GROUP_ID,
                        () -> {
                            if (resumeActivityEventListener != null) {
                                resumeActivityEventListener.onResumeVersionPublished(event);
                            }
                            return null;
                        }
                );

                if (result.isDuplicate()) {
                    log.info("Safely ignored duplicate ResumeVersionPublishedEvent [eventId={}] for consumerGroup={}",
                            event.getEventId(), RESUME_ACTIVITY_GROUP_ID);
                    return;
                }
            } else if (resumeActivityEventListener != null) {
                resumeActivityEventListener.onResumeVersionPublished(event);
            }

            meterRegistry.counter("devsphere.kafka.events.processed.total", "event_type", event.getEventType(), "status", "success").increment();
            log.info("Successfully processed Kafka ResumeVersionPublishedEvent [eventId={}, resumeProfileId={}]",
                    event.getEventId(), event.getResumeProfileId());

        } catch (Exception e) {
            if (span != null) {
                span.error(e);
            }
            meterRegistry.counter("devsphere.kafka.events.processed.total",
                    "event_type", event != null && event.getEventType() != null ? event.getEventType() : "Unknown",
                    "status", "failure").increment();
            throw e;
        } finally {
            if (span != null) {
                span.end();
            }
            if (previousTrace != null) {
                MDC.put("traceId", previousTrace);
            } else {
                MDC.remove("traceId");
            }
        }
    }

    private boolean isValidEvent(ResumeVersionPublishedEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            return false;
        }
        if (event.getEventType() == null || !"ResumeVersionPublished".equalsIgnoreCase(event.getEventType())) {
            return false;
        }
        if (event.getEventVersion() == null || event.getEventVersion() != 1) {
            return false;
        }
        if (event.getResumeProfileId() == null || event.getResumeVersionId() == null || event.getUserId() == null) {
            return false;
        }
        return true;
    }
}
