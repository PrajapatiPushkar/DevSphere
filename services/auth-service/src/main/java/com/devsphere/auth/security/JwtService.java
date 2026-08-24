package com.devsphere.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret;
    private final long expirationSeconds;
    private SecretKey key;

    public JwtService(
            @Value("${jwt.secret:${JWT_SECRET:devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long}}") String secret,
            @Value("${jwt.expiration-seconds:${JWT_EXPIRATION_SECONDS:3600}}") long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalArgumentException("JWT secret configuration error: Secret must be at least 32 characters (256 bits) long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, List.of("USER"));
    }

    public String generateToken(Long userId, String email, String role) {
        return generateToken(userId, email, List.of(role != null && !role.isBlank() ? role : "USER"));
    }

    public String generateToken(Long userId, String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000));

        List<String> effectiveRoles = (roles != null && !roles.isEmpty()) ? roles : List.of("USER");

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", effectiveRoles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
