package com.devsphere.user.resilience;

import com.devsphere.user.cache.RedisUserProfileCache;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.exception.GlobalExceptionHandler;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RedisFailureAndResilienceIntegrationTest {

    private RedisTemplate<String, UserProfileResponse> redisTemplate;
    private ValueOperations<String, UserProfileResponse> valueOperations;
    private MeterRegistry meterRegistry;
    private RedisUserProfileCache cache;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        meterRegistry = new SimpleMeterRegistry();
        cache = new RedisUserProfileCache(redisTemplate, meterRegistry, Duration.ofMinutes(5));
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Redis get failure falls back safely and increments fallback metric")
    void redisGetFailureFallsBackSafely() {
        when(valueOperations.get(any())).thenThrow(new RedisConnectionFailureException("Redis connection lost"));

        Optional<UserProfileResponse> result = cache.get(100L);

        assertTrue(result.isEmpty());
        assertEquals(1.0, meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count());
    }

    @Test
    @DisplayName("Redis put failure logs warning without throwing exception")
    void redisPutFailureHandledSafely() {
        doThrow(new RedisConnectionFailureException("Redis write timeout"))
                .when(valueOperations).set(any(), any(), any());

        assertDoesNotThrow(() -> cache.put(100L, new UserProfileResponse()));
        assertEquals(1.0, meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count());
    }

    @Test
    @DisplayName("Redis evict failure logs warning without throwing exception")
    void redisEvictFailureHandledSafely() {
        when(redisTemplate.delete(anyString())).thenThrow(new RedisConnectionFailureException("Redis connection error"));

        assertDoesNotThrow(() -> cache.evict(100L));
        assertEquals(1.0, meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count());
    }

    @Test
    @DisplayName("GlobalExceptionHandler maps BulkheadFullException to 503 SERVICE_UNAVAILABLE with BULKHEAD_LIMIT_EXCEEDED code")
    void handleBulkheadFullExceptionReturns503() {
        BulkheadFullException ex = BulkheadFullException.createBulkheadFullException(
                io.github.resilience4j.bulkhead.Bulkhead.ofDefaults("userProfileBulkhead")
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/resumes/1/render");

        ResponseEntity<?> response = exceptionHandler.handleBulkheadFull(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
