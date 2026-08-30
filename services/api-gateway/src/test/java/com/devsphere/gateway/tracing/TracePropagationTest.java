package com.devsphere.gateway.tracing;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class TracePropagationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired(required = false)
    private Tracer tracer;

    @Test
    @DisplayName("Should inject Micrometer Tracer into Gateway context")
    void testTracerBeanExists() {
        assertThat(tracer).isNotNull();
    }

    @Test
    @DisplayName("Should inject X-Trace-Id header into Gateway response")
    void testInjectsXTraceIdResponseHeader() {
        webTestClient.get()
                .uri("/fallback/auth-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().exists("X-Trace-Id");
    }

    @Test
    @DisplayName("Should preserve custom incoming X-Trace-Id header in response")
    void testPreservesCustomXTraceId() {
        String customTraceId = "custom-trace-id-9999";

        webTestClient.get()
                .uri("/fallback/auth-service")
                .header("X-Trace-Id", customTraceId)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().valueEquals("X-Trace-Id", customTraceId);
    }

    @Test
    @DisplayName("Should extract traceId from W3C traceparent header and inject into X-Trace-Id response header")
    void testIncomingW3CTraceparentHeader() {
        String sampleTraceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        webTestClient.get()
                .uri("/fallback/auth-service")
                .header("traceparent", sampleTraceparent)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().valueEquals("X-Trace-Id", "4bf92f3577b34da6a3ce929d0e0e4736");
    }
}
