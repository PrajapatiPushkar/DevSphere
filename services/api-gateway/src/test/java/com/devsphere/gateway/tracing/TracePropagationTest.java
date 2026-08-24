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
    @DisplayName("Should accept incoming W3C traceparent header without breaking request execution")
    void testIncomingW3CTraceparentHeader() {
        String sampleTraceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        webTestClient.get()
                .uri("/fallback/auth-service")
                .header("traceparent", sampleTraceparent)
                .exchange()
                .expectStatus().isEqualTo(503);
    }
}
