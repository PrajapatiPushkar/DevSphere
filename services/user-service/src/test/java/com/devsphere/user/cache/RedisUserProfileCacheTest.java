package com.devsphere.user.cache;

import com.devsphere.user.dto.UserProfileResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisUserProfileCacheTest {

    @Mock
    private RedisTemplate<String, UserProfileResponse> redisTemplate;

    @Mock
    private ValueOperations<String, UserProfileResponse> valueOperations;

    private io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private RedisUserProfileCache cache;
    private final Duration ttl = Duration.ofMinutes(5);

    @BeforeEach
    void setUp() {
        meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        cache = new RedisUserProfileCache(redisTemplate, meterRegistry, ttl);
    }

    @Test
    void get_whenKeyExists_returnsCachedProfile() {
        Long userId = 101L;
        String key = "user-profile:101";
        UserProfileResponse cachedResponse = new UserProfileResponse(
                userId, "Pushkar", "Prajapati", "Pushkar", "Bio", "123", Instant.now(), Instant.now()
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(cachedResponse);

        Optional<UserProfileResponse> result = cache.get(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getFirstName()).isEqualTo("Pushkar");
    }

    @Test
    void get_whenKeyDoesNotExist_returnsEmpty() {
        Long userId = 101L;
        String key = "user-profile:101";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        Optional<UserProfileResponse> result = cache.get(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void get_whenRedisThrowsException_degradesGracefullyAndReturnsEmpty() {
        Long userId = 101L;
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        Optional<UserProfileResponse> result = cache.get(userId);

        assertThat(result).isEmpty();
        assertThat(meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count()).isEqualTo(1.0);
    }

    @Test
    void put_savesProfileWithConfiguredTtl() {
        Long userId = 101L;
        String key = "user-profile:101";
        UserProfileResponse profile = new UserProfileResponse(userId, "Pushkar", "Prajapati", null, null, null, Instant.now(), Instant.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.put(userId, profile);

        verify(valueOperations).set(eq(key), eq(profile), eq(ttl));
    }

    @Test
    void put_whenRedisThrowsException_doesNotThrow() {
        Long userId = 101L;
        UserProfileResponse profile = new UserProfileResponse(userId, "Pushkar", "Prajapati", null, null, null, Instant.now(), Instant.now());

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis down"));

        assertThatCode(() -> cache.put(userId, profile)).doesNotThrowAnyException();
        assertThat(meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count()).isEqualTo(1.0);
    }

    @Test
    void evict_deletesKeyFromRedis() {
        Long userId = 101L;
        String key = "user-profile:101";

        when(redisTemplate.delete(key)).thenReturn(true);

        cache.evict(userId);

        verify(redisTemplate).delete(key);
    }

    @Test
    void evict_whenRedisThrowsException_doesNotThrow() {
        Long userId = 101L;

        when(redisTemplate.delete("user-profile:101")).thenThrow(new RedisConnectionFailureException("Redis down"));

        assertThatCode(() -> cache.evict(userId)).doesNotThrowAnyException();
        assertThat(meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count()).isEqualTo(1.0);
    }
}
