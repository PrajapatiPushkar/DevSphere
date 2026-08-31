package com.devsphere.user.cache;

import com.devsphere.user.dto.publicresume.PublicResumeAnalyticsResponse;
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
public class RedisPublicResumeAnalyticsCache implements PublicResumeAnalyticsCache {

    private static final Logger log = LoggerFactory.getLogger(RedisPublicResumeAnalyticsCache.class);
    private static final String CACHE_KEY_PREFIX = "public-resume-analytics:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Duration ttl;

    public RedisPublicResumeAnalyticsCache() {
        this(null, new SimpleMeterRegistry(), Duration.ofMinutes(5));
    }

    public RedisPublicResumeAnalyticsCache(RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, new SimpleMeterRegistry(), Duration.ofMinutes(5));
    }

    public RedisPublicResumeAnalyticsCache(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.cache.public-resume-analytics-ttl:5m}") Duration ttl) {
        this(redisTemplate, new SimpleMeterRegistry(), ttl);
    }

    @Autowired(required = false)
    public RedisPublicResumeAnalyticsCache(
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry,
            @Value("${app.cache.public-resume-analytics-ttl:5m}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.ttl = ttl != null ? ttl : Duration.ofMinutes(5);
    }

    @Override
    public Optional<PublicResumeAnalyticsResponse> get(Long resumeId) {
        if (resumeId == null || redisTemplate == null) {
            return Optional.empty();
        }

        String key = buildCacheKey(resumeId);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PublicResumeAnalyticsResponse response) {
                meterRegistry.counter("devsphere.cache.hits.total", "cache", "public_resume_analytics").increment();
                log.info("Public resume analytics cache hit: resumeId={}", resumeId);
                return Optional.of(response);
            }
            meterRegistry.counter("devsphere.cache.misses.total", "cache", "public_resume_analytics").increment();
            log.info("Public resume analytics cache miss: resumeId={}", resumeId);
        } catch (Exception e) {
            meterRegistry.counter("devsphere.cache.misses.total", "cache", "public_resume_analytics").increment();
            meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").increment();
            log.warn("Redis unavailable during public resume analytics cache get for resumeId={}: {}", resumeId, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void put(Long resumeId, PublicResumeAnalyticsResponse response) {
        if (resumeId == null || response == null || redisTemplate == null) {
            return;
        }

        String key = buildCacheKey(resumeId);
        try {
            redisTemplate.opsForValue().set(key, response, ttl);
            log.info("Public resume analytics cache put: resumeId={}", resumeId);
        } catch (Exception e) {
            meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").increment();
            log.warn("Redis unavailable during public resume analytics cache put for resumeId={}: {}", resumeId, e.getMessage());
        }
    }

    @Override
    public void evict(Long resumeId) {
        if (resumeId == null) {
            return;
        }

        meterRegistry.counter("devsphere.cache.invalidation.attempts.total", "cache", "public_resume_analytics").increment();
        if (redisTemplate == null) {
            meterRegistry.counter("devsphere.cache.invalidation.failures.total", "cache", "public_resume_analytics").increment();
            return;
        }

        String key = buildCacheKey(resumeId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            meterRegistry.counter("devsphere.cache.invalidation.success.total", "cache", "public_resume_analytics").increment();
            log.info("Public resume analytics cache eviction: resumeId={}, deleted={}", resumeId, deleted);
        } catch (Exception e) {
            meterRegistry.counter("devsphere.cache.invalidation.failures.total", "cache", "public_resume_analytics").increment();
            meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").increment();
            log.warn("Redis unavailable during public resume analytics cache eviction for resumeId={}: {}", resumeId, e.getMessage());
        }
    }

    private String buildCacheKey(Long resumeId) {
        return CACHE_KEY_PREFIX + resumeId;
    }
}
