package com.devsphere.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String AUTH_USER_ROLES_HEADER = "X-Authenticated-User-Roles";

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                try {
                    Claims claims = jwtValidator.validateAndParseToken(token);
                    String userIdStr = claims.getSubject();
                    if (userIdStr != null && !userIdStr.isBlank()) {
                        Long userId = Long.parseLong(userIdStr);
                        String email = claims.get("email", String.class);
                        List<GrantedAuthority> authorities = jwtValidator.extractAuthorities(claims);

                        UserPrincipal principal = new UserPrincipal(userId, email, authorities);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (JwtException | NumberFormatException e) {
                    // Clear security context on invalid token
                    SecurityContextHolder.clearContext();
                }
            }
        } else {
            // Fallback for internal gateway forwarding if valid header exists AND request comes through gateway
            String userIdHeader = request.getHeader(AUTH_USER_ID_HEADER);
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    Long userId = Long.parseLong(userIdHeader.trim());
                    String rolesHeader = request.getHeader(AUTH_USER_ROLES_HEADER);
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (rolesHeader != null && !rolesHeader.isBlank()) {
                        for (String r : rolesHeader.split(",")) {
                            String role = r.trim();
                            if (!role.isEmpty()) {
                                String authName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                authorities.add(new SimpleGrantedAuthority(authName));
                            }
                        }
                    }
                    if (authorities.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }
                    UserPrincipal principal = new UserPrincipal(userId, null, authorities);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (NumberFormatException e) {
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
