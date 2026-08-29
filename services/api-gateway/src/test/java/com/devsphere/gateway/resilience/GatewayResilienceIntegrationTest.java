package com.devsphere.gateway.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayResilienceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Auth Service Fallback returns 503 with standardized DOWNSTREAM_SERVICE_UNAVAILABLE error contract")
    void authServiceFallbackReturnsStandardizedErrorContract() {
        webTestClient.get()
                .uri("/fallback/auth-service")
                .header("X-Trace-Id", "trace-cb-test-123")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.code").isEqualTo("DOWNSTREAM_SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isEqualTo("Auth Service is temporarily unavailable. Please try again later.")
                .jsonPath("$.path").isEqualTo("/fallback/auth-service")
                .jsonPath("$.traceId").isEqualTo("trace-cb-test-123");
    }

    @Test
    @DisplayName("User Service Fallback returns 503 with standardized DOWNSTREAM_SERVICE_UNAVAILABLE error contract")
    void userServiceFallbackReturnsStandardizedErrorContract() {
        webTestClient.get()
                .uri("/fallback/user-service")
                .header("X-Trace-Id", "trace-user-fallback-999")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.code").isEqualTo("DOWNSTREAM_SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isEqualTo("User Service is temporarily unavailable. Please try again later.")
                .jsonPath("$.path").isEqualTo("/fallback/user-service")
                .jsonPath("$.traceId").isEqualTo("trace-user-fallback-999");
    }

    @Test
    @DisplayName("Generic Fallback returns 503 with standardized DOWNSTREAM_SERVICE_UNAVAILABLE error contract")
    void genericFallbackReturnsStandardizedErrorContract() {
        webTestClient.get()
                .uri("/fallback/service-unavailable")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.code").isEqualTo("DOWNSTREAM_SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isEqualTo("The requested service is temporarily unavailable. Please try again later.")
                .jsonPath("$.path").isEqualTo("/fallback/service-unavailable");
    }
}
