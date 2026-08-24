package com.devsphere.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtValidatorTest {

    private static final String SECRET = "devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long";
    private SecretKey key;
    private JwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        this.jwtValidator = new JwtValidator(SECRET);
        this.jwtValidator.init();
    }

    @Test
    @DisplayName("Should successfully validate and parse valid JWT token")
    void validateValidToken() {
        String token = Jwts.builder()
                .subject("42")
                .claim("email", "test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        Claims claims = jwtValidator.validateAndParseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception for expired JWT token")
    void rejectExpiredToken() {
        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtValidator.validateAndParseToken(expiredToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid token signature")
    void rejectInvalidSignature() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("different-super-secret-key-that-is-at-least-256-bits-long!!".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        assertThatThrownBy(() -> jwtValidator.validateAndParseToken(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should extract roles from JWT claims list or string")
    void extractRolesFromClaims() {
        String tokenWithList = Jwts.builder()
                .subject("42")
                .claim("roles", java.util.List.of("ADMIN", "USER"))
                .signWith(key)
                .compact();

        Claims claims = jwtValidator.validateAndParseToken(tokenWithList);
        java.util.List<String> roles = jwtValidator.extractRoles(claims);
        assertThat(roles).containsExactly("ADMIN", "USER");
    }
}
