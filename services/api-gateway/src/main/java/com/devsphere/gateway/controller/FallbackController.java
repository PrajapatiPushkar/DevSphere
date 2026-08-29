package com.devsphere.gateway.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    private final MeterRegistry meterRegistry;

    public FallbackController() {
        this(new SimpleMeterRegistry());
    }

    public FallbackController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @RequestMapping("/auth-service")
    public ResponseEntity<Map<String, Object>> authServiceFallback(ServerHttpRequest request) {
        log.warn("Gateway fallback triggered for Auth Service (DEVSPHERE-AUTH-SERVICE)");
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "auth-service", "dependency", "http").increment();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponseBody("Auth Service is temporarily unavailable. Please try again later.", request));
    }

    @RequestMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback(ServerHttpRequest request) {
        log.warn("Gateway fallback triggered for User Service (DEVSPHERE-USER-SERVICE)");
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "http").increment();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponseBody("User Service is temporarily unavailable. Please try again later.", request));
    }

    @RequestMapping("/service-unavailable")
    public ResponseEntity<Map<String, Object>> genericFallback(ServerHttpRequest request) {
        log.warn("Gateway fallback triggered for generic downstream service");
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "gateway", "dependency", "http").increment();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponseBody("The requested service is temporarily unavailable. Please try again later.", request));
    }

    private Map<String, Object> buildErrorResponseBody(String message, ServerHttpRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 503);
        body.put("error", "SERVICE_UNAVAILABLE");
        body.put("code", "DOWNSTREAM_SERVICE_UNAVAILABLE");
        body.put("message", message);

        String path = request != null ? request.getPath().value() : null;
        if (path != null) {
            body.put("path", path);
        }

        String traceId = null;
        if (request != null) {
            traceId = request.getHeaders().getFirst("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = request.getHeaders().getFirst("traceparent");
            }
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId") != null ? MDC.get("traceId") : MDC.get("trace_id");
        }
        if (traceId != null && !traceId.isBlank()) {
            body.put("traceId", traceId);
        }

        return body;
    }
}

