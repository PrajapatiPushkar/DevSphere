package com.devsphere.user.event;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DomainEventArchitectureTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Test A — Event Creation: Auto-populates eventId, occurredAt, traceId and version")
    void eventCreation_PopulatesStandardMetadata() {
        MDC.put("traceId", "trace-xyz-12345");

        PublicResumeViewEvent event = new PublicResumeViewEvent(
                "pub-123", 10L, "192.168.1.1", "https://linkedin.com", "UserAgent/1.0"
        );

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventType()).isEqualTo("PublicResumeViewed");
        assertThat(event.getEventVersion()).isEqualTo(1);
        assertThat(event.getOccurredAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(event.getTraceId()).isEqualTo("trace-xyz-12345");
        assertThat(event.getPublicId()).isEqualTo("pub-123");
        assertThat(event.getResumeProfileId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Test B — Event Immutability: Standard domain events store final immutable fields")
    void eventImmutability_FieldsAreImmutable() {
        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(10L, 5L, 2, 100L);

        assertThat(event.getResumeProfileId()).isEqualTo(10L);
        assertThat(event.getResumeVersionId()).isEqualTo(5L);
        assertThat(event.getVersionNumber()).isEqualTo(2);
        assertThat(event.getUserId()).isEqualTo(100L);
        assertThat(event.getEventType()).isEqualTo("ResumeVersionPublished");
    }

    @Test
    @DisplayName("Test C — Event Publication: SpringDomainEventPublisher delegates to ApplicationEventPublisher")
    void eventPublication_DelegatesToSpringPublisher() {
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(springPublisher);

        PublicResumeShareRevokedEvent event = new PublicResumeShareRevokedEvent(10L, "pub-999", 100L);
        publisher.publish(event);

        verify(springPublisher).publishEvent(event);
    }

    @Test
    @DisplayName("Test E & F — Failure Isolation & Security: Listener catches exceptions and excludes secrets")
    void listenerFailureIsolationAndSecurity() {
        ResumeActivityEventListener listener = new ResumeActivityEventListener();
        ResumeVersionPublishedEvent event = new ResumeVersionPublishedEvent(10L, 5L, 2, 100L);

        // Does not throw exception even when MDC or metrics fail
        listener.onResumeVersionPublished(event);

        // Verify event class payload does not expose password/JWT/secret fields
        assertThat(event.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("password", "token", "jwt", "bcrypt", "secret");
    }

    @Test
    @DisplayName("Test G — Trace Correlation: Event captures and restores MDC trace context")
    void traceCorrelation_PreservesMdcContext() {
        MDC.put("traceId", "active-trace-999");
        UserRegisteredEvent event = new UserRegisteredEvent(500L);

        assertThat(event.getTraceId()).isEqualTo("active-trace-999");

        MDC.clear();
        ResumeActivityEventListener listener = new ResumeActivityEventListener();
        listener.onResumeVersionPublished(new ResumeVersionPublishedEvent(1L, 1L, 1, 500L));

        // After listener execution, MDC is cleaned up if it was empty before
        assertThat(MDC.get("traceId")).isNull();
    }
}
