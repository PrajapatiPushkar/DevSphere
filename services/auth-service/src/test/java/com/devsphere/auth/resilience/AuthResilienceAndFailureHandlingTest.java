package com.devsphere.auth.resilience;

import com.devsphere.auth.exception.GlobalExceptionHandler;
import com.devsphere.auth.exception.InvalidCredentialsException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthResilienceAndFailureHandlingTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Auth CircuitBreaker transitions CLOSED -> OPEN on failures and ignores InvalidCredentialsException")
    void authCircuitBreaker_IgnoresInvalidCredentialsException() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50.0f)
                .ignoreExceptions(InvalidCredentialsException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("authCircuitBreaker", config);

        // Perform 4 calls with InvalidCredentialsException
        for (int i = 0; i < 4; i++) {
            try {
                circuitBreaker.executeRunnable(() -> {
                    throw new InvalidCredentialsException("Invalid username or password");
                });
            } catch (Exception ignored) {
            }
        }

        // Circuit breaker remains CLOSED because 401 InvalidCredentialsException is ignored
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        // Perform 4 calls with IOException -> failure rate 100% -> OPEN
        for (int i = 0; i < 4; i++) {
            try {
                circuitBreaker.executeRunnable(() -> {
                    throw new RuntimeException(new IOException("Database connection timeout"));
                });
            } catch (Exception ignored) {
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Auth GlobalExceptionHandler maps BulkheadFullException to 503 SERVICE_UNAVAILABLE")
    void handleBulkheadFull_Returns503() {
        Bulkhead bulkhead = Bulkhead.ofDefaults("authBulkhead");
        BulkheadFullException ex = BulkheadFullException.createBulkheadFullException(bulkhead);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

        ResponseEntity<?> response = exceptionHandler.handleBulkheadFull(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Auth GlobalExceptionHandler maps CallNotPermittedException to 503 SERVICE_UNAVAILABLE")
    void handleCallNotPermitted_Returns503() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("authCircuitBreaker");
        CallNotPermittedException ex = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");

        ResponseEntity<?> response = exceptionHandler.handleCallNotPermitted(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Auth GlobalExceptionHandler maps RequestNotPermitted to 429 TOO_MANY_REQUESTS")
    void handleRequestNotPermitted_Returns429() {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("authLimiter");
        RequestNotPermitted ex = RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

        ResponseEntity<?> response = exceptionHandler.handleRequestNotPermitted(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Auth GlobalExceptionHandler maps TimeoutException to 504 GATEWAY_TIMEOUT")
    void handleTimeout_Returns504() {
        TimeoutException ex = new TimeoutException("Database auth query timed out");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

        ResponseEntity<?> response = exceptionHandler.handleTimeout(ex, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
