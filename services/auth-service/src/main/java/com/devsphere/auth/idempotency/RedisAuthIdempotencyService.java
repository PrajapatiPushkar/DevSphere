package com.devsphere.auth.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAuthIdempotencyService implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisAuthIdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:auth:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public RedisAuthIdempotencyService() {
        this(null, new SimpleMeterRegistry(), new ObjectMapper());
    }

    public RedisAuthIdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, new SimpleMeterRegistry(), new ObjectMapper());
    }

    @Autowired(required = false)
    public RedisAuthIdempotencyService(
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public IdempotencyLockResult tryAcquireLock(String cacheKey, String fingerprint, Duration lockTtl) {
        if (cacheKey == null || cacheKey.isBlank() || redisTemplate == null) {
            log.warn("Redis unavailable or key invalid. Bypassing auth idempotency lock for key={}", cacheKey);
            recordFallback();
            return IdempotencyLockResult.BYPASSED;
        }

        String redisKey = KEY_PREFIX + cacheKey;
        try {
            IdempotencyRecord inProgressRecord = new IdempotencyRecord(IdempotencyRecord.Status.IN_PROGRESS, fingerprint);
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, inProgressRecord, lockTtl != null ? lockTtl : Duration.ofSeconds(30));

            if (Boolean.TRUE.equals(acquired)) {
                log.info("Auth idempotency lock acquired for key={}", cacheKey);
                meterRegistry.counter("devsphere_idempotency_requests_total", "service", "auth-service", "result", "acquired").increment();
                return IdempotencyLockResult.ACQUIRED;
            }

            Object existingValue = redisTemplate.opsForValue().get(redisKey);
            IdempotencyRecord existingRecord = parseRecord(existingValue);

            if (existingRecord == null) {
                log.warn("Failed to parse existing auth idempotency record for key={}. Re-acquiring lock.", cacheKey);
                redisTemplate.opsForValue().set(redisKey, inProgressRecord, lockTtl != null ? lockTtl : Duration.ofSeconds(30));
                return IdempotencyLockResult.ACQUIRED;
            }

            if (!fingerprint.equals(existingRecord.getFingerprint())) {
                log.warn("Auth idempotency fingerprint mismatch for key={}: expected={}, actual={}", cacheKey, existingRecord.getFingerprint(), fingerprint);
                meterRegistry.counter("devsphere_idempotency_requests_total", "service", "auth-service", "result", "fingerprint_mismatch").increment();
                return IdempotencyLockResult.FINGERPRINT_MISMATCH;
            }

            if (existingRecord.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
                log.warn("Auth idempotency lock currently IN_PROGRESS for key={}", cacheKey);
                meterRegistry.counter("devsphere_idempotency_requests_total", "service", "auth-service", "result", "in_progress_conflict").increment();
                return IdempotencyLockResult.IN_PROGRESS;
            }

            if (existingRecord.getStatus() == IdempotencyRecord.Status.COMPLETED) {
                log.info("Auth idempotency lock already COMPLETED for key={}. Replaying response.", cacheKey);
                meterRegistry.counter("devsphere_idempotency_requests_total", "service", "auth-service", "result", "completed_replay").increment();
                return IdempotencyLockResult.COMPLETED;
            }

            return IdempotencyLockResult.ACQUIRED;
        } catch (Exception e) {
            recordFallback();
            log.warn("Redis error during auth idempotency lock acquisition for key={}: {}. Bypassing.", cacheKey, e.getMessage());
            return IdempotencyLockResult.BYPASSED;
        }
    }

    @Override
    public void recordCompletion(String cacheKey, String fingerprint, int httpStatus, String responseBody, String contentType, Duration recordTtl) {
        if (cacheKey == null || cacheKey.isBlank() || redisTemplate == null) {
            return;
        }

        String redisKey = KEY_PREFIX + cacheKey;
        try {
            IdempotencyRecord completedRecord = new IdempotencyRecord(
                    IdempotencyRecord.Status.COMPLETED,
                    fingerprint,
                    httpStatus,
                    responseBody,
                    contentType
            );
            Duration ttl = recordTtl != null ? recordTtl : Duration.ofHours(24);
            redisTemplate.opsForValue().set(redisKey, completedRecord, ttl);
            log.info("Auth idempotency completion recorded for key={}, httpStatus={}", cacheKey, httpStatus);
        } catch (Exception e) {
            recordFallback();
            log.warn("Redis error during auth idempotency completion recording for key={}: {}", cacheKey, e.getMessage());
        }
    }

    @Override
    public void evictLock(String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank() || redisTemplate == null) {
            return;
        }

        String redisKey = KEY_PREFIX + cacheKey;
        try {
            redisTemplate.delete(redisKey);
            log.info("Auth idempotency lock evicted for key={}", cacheKey);
        } catch (Exception e) {
            recordFallback();
            log.warn("Redis error during auth idempotency lock eviction for key={}: {}", cacheKey, e.getMessage());
        }
    }

    @Override
    public IdempotencyRecord getRecord(String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank() || redisTemplate == null) {
            return null;
        }

        String redisKey = KEY_PREFIX + cacheKey;
        try {
            Object val = redisTemplate.opsForValue().get(redisKey);
            return parseRecord(val);
        } catch (Exception e) {
            recordFallback();
            log.warn("Redis error fetching auth idempotency record for key={}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private IdempotencyRecord parseRecord(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof IdempotencyRecord rec) {
            return rec;
        }
        try {
            return objectMapper.convertValue(obj, IdempotencyRecord.class);
        } catch (Exception e) {
            log.warn("Failed to convert object to IdempotencyRecord in auth-service: {}", e.getMessage());
            return null;
        }
    }

    private void recordFallback() {
        meterRegistry.counter("devsphere_resilience_fallback_total", "service", "auth-service", "dependency", "redis").increment();
    }
}
