package com.devsphere.user.event;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ResumeActivityEventListener {

    private static final Logger log = LoggerFactory.getLogger(ResumeActivityEventListener.class);

    private final MeterRegistry meterRegistry;

    public ResumeActivityEventListener() {
        this(new SimpleMeterRegistry());
    }

    @Autowired(required = false)
    public ResumeActivityEventListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onResumeVersionPublished(ResumeVersionPublishedEvent event) {
        if (event == null) {
            return;
        }

        String previousTrace = MDC.get("traceId");
        try {
            if (event.getTraceId() != null && !event.getTraceId().isBlank()) {
                MDC.put("traceId", event.getTraceId());
            }

            meterRegistry.counter("devsphere_domain_events_total", "event_type", event.getEventType(), "status", "success").increment();
            log.info("[AFTER_COMMIT] Processed ResumeVersionPublishedEvent for resumeId={}, version={}, userId={}, eventId={}",
                    event.getResumeProfileId(), event.getVersionNumber(), event.getUserId(), event.getEventId());
        } catch (Exception e) {
            meterRegistry.counter("devsphere_domain_events_total", "event_type", event.getEventType(), "status", "error").increment();
            log.error("Failed to process ResumeVersionPublishedEvent [eventId={}]: {}", event.getEventId(), e.getMessage(), e);
        } finally {
            if (previousTrace != null) {
                MDC.put("traceId", previousTrace);
            } else {
                MDC.remove("traceId");
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPublicResumeShareRevoked(PublicResumeShareRevokedEvent event) {
        if (event == null) {
            return;
        }

        String previousTrace = MDC.get("traceId");
        try {
            if (event.getTraceId() != null && !event.getTraceId().isBlank()) {
                MDC.put("traceId", event.getTraceId());
            }

            meterRegistry.counter("devsphere_domain_events_total", "event_type", event.getEventType(), "status", "success").increment();
            log.info("[AFTER_COMMIT] Processed PublicResumeShareRevokedEvent for resumeId={}, publicId={}, userId={}, eventId={}",
                    event.getResumeProfileId(), event.getPublicId(), event.getUserId(), event.getEventId());
        } catch (Exception e) {
            meterRegistry.counter("devsphere_domain_events_total", "event_type", event.getEventType(), "status", "error").increment();
            log.error("Failed to process PublicResumeShareRevokedEvent [eventId={}]: {}", event.getEventId(), e.getMessage(), e);
        } finally {
            if (previousTrace != null) {
                MDC.put("traceId", previousTrace);
            } else {
                MDC.remove("traceId");
            }
        }
    }
}
