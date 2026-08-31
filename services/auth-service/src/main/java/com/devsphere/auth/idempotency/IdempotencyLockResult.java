package com.devsphere.auth.idempotency;

public enum IdempotencyLockResult {
    ACQUIRED,
    IN_PROGRESS,
    COMPLETED,
    FINGERPRINT_MISMATCH,
    BYPASSED
}
