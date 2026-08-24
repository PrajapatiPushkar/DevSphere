package com.devsphere.gateway.filter;

import com.devsphere.gateway.security.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTH_HEADER = HttpHeaders.AUTHORIZATION;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String AUTH_USER_ROLES_HEADER = "X-Authenticated-User-Roles";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/actuator/health",
            "/api/demo/hello",
            "/fallback/"
    );

    private static final List<String> ADMIN_PATH_PREFIXES = List.of(
            "/api/v1/admin/",
            "/api/v1/users/admin/"
    );

    private final JwtValidator jwtValidator;
    private final MeterRegistry meterRegistry;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this(jwtValidator, new SimpleMeterRegistry());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtAuthenticationFilter(JwtValidator jwtValidator, MeterRegistry meterRegistry) {
        this.jwtValidator = jwtValidator;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.remove(AUTH_USER_ID_HEADER);
                        httpHeaders.remove(AUTH_USER_ROLES_HEADER);
                        httpHeaders.remove("X-Role");
                        httpHeaders.remove("X-User-Role");
                        httpHeaders.remove("X-Admin");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(sanitizedRequest).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return onError(exchange, "UNAUTHORIZED", "UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return onError(exchange, "UNAUTHORIZED", "UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = jwtValidator.validateAndParseToken(token);
        } catch (JwtException ex) {
            return onError(exchange, "UNAUTHORIZED", "INVALID_TOKEN", "The access token is invalid or expired", HttpStatus.UNAUTHORIZED);
        }

        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            return onError(exchange, "UNAUTHORIZED", "INVALID_TOKEN", "The access token is invalid or expired", HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = jwtValidator.extractRoles(claims);

        if (isAdminPath(path) && !roles.contains("ADMIN")) {
            return onError(exchange, "FORBIDDEN", "FORBIDDEN", "You do not have permission to access this resource", HttpStatus.FORBIDDEN);
        }

        String rolesHeaderValue = String.join(",", roles);

        // Mutate request: attach trusted user ID/roles headers and strip untrusted client input
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove(AUTH_USER_ID_HEADER);
                    httpHeaders.remove(AUTH_USER_ROLES_HEADER);
                    httpHeaders.remove("X-Role");
                    httpHeaders.remove("X-User-Role");
                    httpHeaders.remove("X-Admin");
                })
                .header(AUTH_USER_ID_HEADER, userId)
                .header(AUTH_USER_ROLES_HEADER, rolesHeaderValue)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isAdminPath(String path) {
        return ADMIN_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, String code, String message, HttpStatus status) {
        String reason = status == HttpStatus.FORBIDDEN ? "forbidden" : "unauthenticated";
        meterRegistry.counter("devsphere_auth_authorization_denied_total", "reason", reason).increment();
        log.warn("Gateway security denied request for path: {}, status: {}, reason: {}", exchange.getRequest().getURI().getPath(), status.value(), reason);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = String.format("""
                {
                  "status": %d,
                  "error": "%s",
                  "code": "%s",
                  "message": "%s"
                }
                """, status.value(), error, code, message);

        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
