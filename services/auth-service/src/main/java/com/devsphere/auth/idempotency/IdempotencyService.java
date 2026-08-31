package com.devsphere.auth.idempotency;

import java.time.Duration;

public interface IdempotencyService {

    IdempotencyLockResult tryAcquireLock(String cacheKey, String fingerprint, Duration lockTtl);

    void recordCompletion(String cacheKey, String fingerprint, int httpStatus, String responseBody, String contentType, Duration recordTtl);

    void evictLock(String cacheKey);

    IdempotencyRecord getRecord(String cacheKey);
}
