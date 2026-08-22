package com.devsphere.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtValidator {

    private final String secret;
    private SecretKey key;

    public JwtValidator(@Value("${jwt.secret:${JWT_SECRET:devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long}}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalArgumentException("JWT secret configuration error: Secret must be at least 32 characters (256 bits) long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateAndParseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
