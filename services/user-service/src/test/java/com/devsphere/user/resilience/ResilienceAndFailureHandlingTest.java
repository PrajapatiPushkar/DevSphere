package com.devsphere.user.resilience;

import com.devsphere.user.exception.GlobalExceptionHandler;
import com.devsphere.user.exception.ResourceNotFoundException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResilienceAndFailureHandlingTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("CircuitBreaker transitions CLOSED -> OPEN after repeated failures, then HALF_OPEN probe -> CLOSED")
    void circuitBreaker_LifecycleAndStateTransitions() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreExceptions(ResourceNotFoundException.class, IllegalArgumentException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("testServiceCircuitBreaker", config);

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        // Perform 4 failing calls -> Failure rate 100% >= 50% -> OPEN
        for (int i = 0; i < 4; i++) {
            try {
                circuitBreaker.executeRunnable(() -> {
                    throw new RuntimeException("Downstream dependency failure");
                });
            } catch (Exception ignored) {
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Call while OPEN should throw CallNotPermittedException
        assertThrows(CallNotPermittedException.class, () ->
                circuitBreaker.executeRunnable(() -> {}));

        // Wait for open state duration to elapse -> Transition to HALF_OPEN
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }

        // Execute successful calls in HALF_OPEN to recover back to CLOSED
        circuitBreaker.executeRunnable(() -> {});
        circuitBreaker.executeRunnable(() -> {});

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("CircuitBreaker ignores non-transient 4xx business exceptions")
    void circuitBreaker_IgnoresBusinessExceptions() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50.0f)
                .ignoreExceptions(ResourceNotFoundException.class, IllegalArgumentException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("businessIgnoreCircuitBreaker", config);

        // Perform 4 calls throwing ignored ResourceNotFoundException
        for (int i = 0; i < 4; i++) {
            try {
                circuitBreaker.executeRunnable(() -> {
                    throw new ResourceNotFoundException("NOT_FOUND", "Resource missing");
                });
            } catch (Exception ignored) {
            }
        }

        // Circuit breaker remains CLOSED because 4xx business exceptions are ignored
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Retry retries transient IOException up to maxAttempts")
    void retry_RetriesTransientException() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(IOException.class)
                .ignoreExceptions(ResourceNotFoundException.class)
                .build();

        Retry retry = Retry.of("testRetry", config);
        AtomicInteger callCount = new AtomicInteger(0);

        assertThrows(IOException.class, () ->
                retry.executeCheckedSupplier(() -> {
                    callCount.incrementAndGet();
                    throw new IOException("Transient network error");
                }));

        assertEquals(3, callCount.get());
    }

    @Test
    @DisplayName("Retry skips non-retryable ResourceNotFoundException without retrying")
    void retry_SkipsNonRetryableException() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(IOException.class)
                .ignoreExceptions(ResourceNotFoundException.class)
                .build();

        Retry retry = Retry.of("testSkipRetry", config);
        AtomicInteger callCount = new AtomicInteger(0);

        assertThrows(ResourceNotFoundException.class, () ->
                retry.executeCheckedSupplier(() -> {
                    callCount.incrementAndGet();
                    throw new ResourceNotFoundException("NOT_FOUND", "User profile not found");
                }));

        assertEquals(1, callCount.get());
    }

    @Test
    @DisplayName("Bulkhead limits concurrent calls and rejects excess calls with BulkheadFullException")
    void bulkhead_LimitsConcurrency() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build();

        Bulkhead bulkhead = Bulkhead.of("testBulkhead", config);

        // Acquire single permit
        assertTrue(bulkhead.tryAcquirePermission());

        // Second permit attempt fails -> BulkheadFullException
        assertThrows(BulkheadFullException.class, () -> {
            if (!bulkhead.tryAcquirePermission()) {
                throw BulkheadFullException.createBulkheadFullException(bulkhead);
            }
        });

        bulkhead.onComplete();
    }

    @Test
    @DisplayName("GlobalExceptionHandler maps CallNotPermittedException to 503 SERVICE_UNAVAILABLE")
    void handleCallNotPermittedException_Returns503() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testBreaker");
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/resumes/1");

        ResponseEntity<?> response = exceptionHandler.handleCallNotPermitted(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("GlobalExceptionHandler maps RequestNotPermitted to 429 TOO_MANY_REQUESTS")
    void handleRequestNotPermitted_Returns429() {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("testLimiter");
        RequestNotPermitted ex = RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/resumes/pub-123");

        ResponseEntity<?> response = exceptionHandler.handleRequestNotPermitted(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("GlobalExceptionHandler maps TimeoutException to 504 GATEWAY_TIMEOUT")
    void handleTimeoutException_Returns504() {
        TimeoutException ex = new TimeoutException("Operation timed out after 3000ms");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/100");

        ResponseEntity<?> response = exceptionHandler.handleTimeout(ex, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
