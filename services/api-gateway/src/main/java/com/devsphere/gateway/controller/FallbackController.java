package com.devsphere.gateway.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        log.warn("Gateway fallback triggered for Auth Service (DEVSPHERE-AUTH-SERVICE)");
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "auth-service", "dependency", "http").increment();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "SERVICE_UNAVAILABLE",
                        "code", "SERVICE_UNAVAILABLE",
                        "message", "Auth Service is temporarily unavailable. Please try again later."
                ));
    }

    @RequestMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("Gateway fallback triggered for User Service (DEVSPHERE-USER-SERVICE)");
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "http").increment();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "SERVICE_UNAVAILABLE",
                        "code", "SERVICE_UNAVAILABLE",
                        "message", "User Service is temporarily unavailable. Please try again later."
                ));
    }

    @RequestMapping("/service-unavailable")
    public ResponseEntity<Map<String, Object>> genericFallback() {
        log.warn("Gateway fallback triggered for generic downstream service");
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "gateway", "dependency", "http").increment();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "SERVICE_UNAVAILABLE",
                        "code", "SERVICE_UNAVAILABLE",
                        "message", "The requested service is temporarily unavailable. Please try again later."
                ));
    }
}
