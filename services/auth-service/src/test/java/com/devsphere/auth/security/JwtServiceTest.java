package com.devsphere.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String VALID_SECRET = "devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long";
    private static final long EXPIRATION_SECONDS = 3600;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(VALID_SECRET, EXPIRATION_SECONDS);
        jwtService.init();
    }

    @Test
    @DisplayName("Should generate valid JWT token with user ID subject and email claim")
    void generateAndParseValidToken() {
        Long userId = 42L;
        String email = "jwtuser@example.com";

        String token = jwtService.generateToken(userId, email);

        assertThat(token).isNotBlank();

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("jwtuser@example.com");
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("JWT claims should NEVER contain password or passwordHash")
    void jwtDoesNotContainSensitiveCredentials() {
        String token = jwtService.generateToken(100L, "secure@example.com");
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.get("password")).isNull();
        assertThat(claims.get("passwordHash")).isNull();
    }

    @Test
    @DisplayName("Should fail fast on startup if secret is shorter than 256 bits (32 chars)")
    void shortSecretThrowsException() {
        JwtService invalidService = new JwtService("too-short-secret", 3600);

        assertThatThrownBy(invalidService::init)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secret must be at least 32 characters");
    }
}
