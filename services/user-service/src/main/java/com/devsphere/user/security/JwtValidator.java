package com.devsphere.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtValidator {

    private final String secret;
    private SecretKey key;

    public JwtValidator(
            @Value("${jwt.secret:${JWT_SECRET:devsphere-super-secret-jwt-signing-key-for-local-development-must-be-at-least-256-bits-long}}") String secret) {
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

    @SuppressWarnings("unchecked")
    public List<GrantedAuthority> extractAuthorities(Claims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (claims == null) {
            return authorities;
        }

        Object rolesObj = claims.get("roles");
        List<String> roleStrings = new ArrayList<>();

        if (rolesObj instanceof List<?> list) {
            for (Object obj : list) {
                if (obj != null) {
                    roleStrings.add(obj.toString());
                }
            }
        } else if (rolesObj instanceof String roleStr) {
            roleStrings.add(roleStr);
        }

        if (roleStrings.isEmpty()) {
            roleStrings.add("USER");
        }

        for (String role : roleStrings) {
            String authorityName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(authorityName));
        }

        return authorities;
    }
}
