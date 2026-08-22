package com.devsphere.user.cache;

import com.devsphere.user.dto.UserProfileResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUserProfileCache implements UserProfileCache {

    private static final Logger log = LoggerFactory.getLogger(RedisUserProfileCache.class);
    private static final String CACHE_KEY_PREFIX = "user-profile:";

    private final RedisTemplate<String, UserProfileResponse> redisTemplate;
    private final Duration ttl;

    public RedisUserProfileCache(
            RedisTemplate<String, UserProfileResponse> redisTemplate,
            @Value("${app.cache.user-profile-ttl:5m}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public Optional<UserProfileResponse> get(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String key = buildCacheKey(userId);
        try {
            UserProfileResponse cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("User profile cache hit: userId={}", userId);
                return Optional.of(cached);
            }
            log.info("User profile cache miss: userId={}", userId);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache get for userId={}: {}", userId, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void put(Long userId, UserProfileResponse profile) {
        if (userId == null || profile == null) {
            return;
        }

        String key = buildCacheKey(userId);
        try {
            redisTemplate.opsForValue().set(key, profile, ttl);
            log.info("User profile cache put: userId={}", userId);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache put for userId={}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }

        String key = buildCacheKey(userId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("User profile cache eviction: userId={}, deleted={}", userId, deleted);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache eviction for userId={}: {}", userId, e.getMessage());
        }
    }

    private String buildCacheKey(Long userId) {
        return CACHE_KEY_PREFIX + userId;
    }
}
