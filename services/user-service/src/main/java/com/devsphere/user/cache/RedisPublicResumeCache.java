package com.devsphere.user.cache;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPublicResumeCache implements PublicResumeCache {

    private static final Logger log = LoggerFactory.getLogger(RedisPublicResumeCache.class);
    private static final String CACHE_KEY_PREFIX = "public-resume:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Duration ttl;

    public RedisPublicResumeCache() {
        this(null, new SimpleMeterRegistry(), Duration.ofMinutes(10));
    }

    public RedisPublicResumeCache(RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, new SimpleMeterRegistry(), Duration.ofMinutes(10));
    }

    public RedisPublicResumeCache(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.cache.public-resume-ttl:10m}") Duration ttl) {
        this(redisTemplate, new SimpleMeterRegistry(), ttl);
    }

    @Autowired(required = false)
    public RedisPublicResumeCache(
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry,
            @Value("${app.cache.public-resume-ttl:10m}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.ttl = ttl != null ? ttl : Duration.ofMinutes(10);
    }

    @Override
    public Optional<PublicResumeResponse> get(String publicResumeId) {
        if (publicResumeId == null || publicResumeId.isBlank() || redisTemplate == null) {
            return Optional.empty();
        }

        String key = buildCacheKey(publicResumeId);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PublicResumeResponse response) {
                meterRegistry.counter("devsphere.cache.hits.total", "cache", "public_resume").increment();
                log.info("Public resume cache hit: publicResumeId={}", publicResumeId);
                return Optional.of(response);
            }
            meterRegistry.counter("devsphere.cache.misses.total", "cache", "public_resume").increment();
            log.info("Public resume cache miss: publicResumeId={}", publicResumeId);
        } catch (Exception e) {
            meterRegistry.counter("devsphere.cache.misses.total", "cache", "public_resume").increment();
            meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").increment();
            log.warn("Redis unavailable during public resume cache get for publicResumeId={}: {}", publicResumeId, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void put(String publicResumeId, PublicResumeResponse response) {
        if (publicResumeId == null || publicResumeId.isBlank() || response == null || redisTemplate == null) {
            return;
        }

        String key = buildCacheKey(publicResumeId);
        try {
            redisTemplate.opsForValue().set(key, response, ttl);
            log.info("Public resume cache put: publicResumeId={}", publicResumeId);
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").increment();
            log.warn("Redis unavailable during public resume cache put for publicResumeId={}: {}", publicResumeId, e.getMessage());
        }
    }

    @Override
    public void evict(String publicResumeId) {
        if (publicResumeId == null || publicResumeId.isBlank()) {
            return;
        }

        meterRegistry.counter("devsphere.cache.invalidation.attempts.total", "cache", "public_resume").increment();
        if (redisTemplate == null) {
            meterRegistry.counter("devsphere.cache.invalidation.failures.total", "cache", "public_resume").increment();
            return;
        }

        String key = buildCacheKey(publicResumeId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            meterRegistry.counter("devsphere.cache.invalidation.success.total", "cache", "public_resume").increment();
            log.info("Public resume cache eviction: publicResumeId={}, deleted={}", publicResumeId, deleted);
        } catch (Exception e) {
            meterRegistry.counter("devsphere.cache.invalidation.failures.total", "cache", "public_resume").increment();
            meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").increment();
            log.warn("Redis unavailable during public resume cache eviction for publicResumeId={}: {}", publicResumeId, e.getMessage());
        }
    }

    private String buildCacheKey(String publicResumeId) {
        return CACHE_KEY_PREFIX + publicResumeId;
    }
}
