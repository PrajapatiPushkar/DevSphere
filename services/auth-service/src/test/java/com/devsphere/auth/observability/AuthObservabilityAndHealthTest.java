package com.devsphere.auth.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthObservabilityAndHealthTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private static final Set<String> DANGEROUS_HIGH_CARDINALITY_TAG_KEYS = Set.of(
            "userId", "user_id", "email", "jwt", "token", "password", "requestId", "traceId"
    );

    @Test
    @DisplayName("Auth Actuator /actuator/health/liveness returns 200 OK with STATUS UP")
    void livenessProbe_Returns200UP() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Auth Actuator /actuator/health/readiness returns 200 OK with STATUS UP")
    void readinessProbe_Returns200UP() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Unauthenticated Auth Actuator health check hides sensitive DB/JWT details")
    void unauthenticatedHealthCheck_HidesSensitiveDetails() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("password");
        assertThat(response.getBody()).doesNotContain("secret");
        assertThat(response.getBody()).doesNotContain("jdbcUrl");
    }

    @Test
    @DisplayName("Auth MeterRegistry tag keys adhere to low-cardinality invariant")
    void meterRegistry_EnforcesLowCardinalityTagKeys() {
        meterRegistry.getMeters().forEach(meter -> {
            for (Tag tag : meter.getId().getTags()) {
                assertThat(DANGEROUS_HIGH_CARDINALITY_TAG_KEYS)
                        .withFailMessage("Dangerous high-cardinality tag key found in meter %s: %s", meter.getId().getName(), tag.getKey())
                        .doesNotContain(tag.getKey());
            }
        });
    }
}
