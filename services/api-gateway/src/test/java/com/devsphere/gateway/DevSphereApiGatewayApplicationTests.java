package com.devsphere.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DevSphereApiGatewayApplicationTests {

    private static final String SECRET = "devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long";

    @Autowired
    private WebTestClient webTestClient;

    private SecretKey key;

    @BeforeEach
    void setUp() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Health endpoint /actuator/health is publicly accessible")
    void healthEndpointReturnsStatusUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Public endpoint /api/demo/hello routes without JWT")
    void gatewayRoutesToTemporaryDemoStub() {
        webTestClient.get()
                .uri("/api/demo/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("temporary-demo-service")
                .jsonPath("$.message").isEqualTo("Request successfully routed through DevSphere API Gateway");
    }

    @Test
    @DisplayName("Protected endpoint /api/demo/protected without token returns 401 Unauthorized")
    void protectedEndpointWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/demo/protected")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Authentication is required");
    }

    @Test
    @DisplayName("Protected endpoint with malformed Authorization header returns 401 Unauthorized")
    void protectedEndpointWithMalformedHeaderReturns401() {
        webTestClient.get()
                .uri("/api/demo/protected")
                .header(HttpHeaders.AUTHORIZATION, "Basic invalidHeader")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Protected endpoint with invalid JWT signature returns 401 Unauthorized")
    void protectedEndpointWithInvalidSignatureReturns401() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("different-super-secret-key-that-is-at-least-256-bits-long!!".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        webTestClient.get()
                .uri("/api/demo/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN")
                .jsonPath("$.message").isEqualTo("The access token is invalid or expired");
    }

    @Test
    @DisplayName("Protected endpoint with expired JWT returns 401 Unauthorized")
    void protectedEndpointWithExpiredTokenReturns401() {
        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        webTestClient.get()
                .uri("/api/demo/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Protected endpoint with valid JWT returns 200 OK and downstream response")
    void protectedEndpointWithValidJwtReturnsSuccess() {
        String validToken = Jwts.builder()
                .subject("100")
                .claim("email", "testuser@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        webTestClient.get()
                .uri("/api/demo/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("temporary-demo-service")
                .jsonPath("$.message").isEqualTo("Authenticated request successfully reached downstream service");
    }

    @Test
    @DisplayName("Protected User Service endpoint /api/v1/users/me without token returns 401 Unauthorized")
    void userServiceEndpointWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Authentication is required");
    }

    @Test
    @DisplayName("Protected User Service endpoint /api/v1/users/me with spoofed header but no valid JWT returns 401 Unauthorized")
    void userServiceEndpointWithSpoofedHeaderWithoutJwtReturns401() {
        webTestClient.get()
                .uri("/api/v1/users/me")
                .header("X-Authenticated-User-Id", "999")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }
}
