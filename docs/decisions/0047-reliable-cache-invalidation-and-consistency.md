# 47. Reliable Cache Invalidation & Cache Consistency

* **Status**: Accepted
* **Date**: 2026-08-30
* **Deciders**: DevSphere Engineering Team

## Context & Problem Statement

In Lesson 47, we introduced Redis read model caching for `user-profile:{userId}` and `public-resume:{publicResumeId}`. However, performing inline cache invalidation *during* active database transactions risks race conditions (e.g. concurrent read re-populating pre-commit state) or purging valid cache entries when database transactions subsequently roll back.

We needed a reliable, production-oriented cache consistency strategy that ensures database state changes cleanly invalidate affected cache entries post-commit while strictly isolating primary database writes from Redis outages.

## Decision Drivers

* **Post-Commit Synchronization**: Invalidate cache entries strictly *after* MySQL database transactions successfully commit (`afterCommit()`).
* **Cache Failure Isolation**: Redis outages must log warnings and increment Micrometer metrics without rolling back successful database mutations.
* **Targeted Invalidation**: Only purge the exact cache key (`user-profile:{userId}` or `public-resume:{publicResumeId}`) associated with the mutated entity.
* **TTL Fallback Preservation**: Maintain 5-minute (user profile) and 10-minute (public resume) TTLs as safety nets against transient invalidation misses.
* **Low-Cardinality Observability**: Track invalidation attempts, successes, and failures without leaking high-cardinality IDs into metric tags.

## Considered Options

1. **Option 1**: Use distributed locks or 2-phase commit (2PC) between Redis and MySQL.
2. **Option 2**: Invalidate cache inline *before* transaction commit.
3. **Option 3**: Use Spring `TransactionSynchronizationManager` post-commit synchronization with Redis failure isolation and TTL fallbacks (Chosen).

## Decision Outcome

Chosen Option: **Option 3**. We implemented post-transaction-commit cache invalidation via `TransactionAwareCacheInvalidator`.

### Key Implementation Details:
1. **`TransactionAwareCacheInvalidator` Utility**:
   - Registers an `afterCommit()` callback via Spring's `TransactionSynchronizationManager`. If the database transaction rolls back, eviction is skipped. If no transaction is active, eviction runs immediately.

2. **Service Layer Invalidation Wiring**:
   - `UserProfileService.updateProfile`: Evicts `user-profile:{userId}` post-commit.
   - `ResumeVersionService.publishVersion` & `archiveVersion`: Evicts `public-resume:{publicId}` post-commit.
   - `ResumeProfileService.archiveResumeProfile` & `deleteResumeProfile`: Evicts `public-resume:{publicId}` post-commit.

3. **Metrics & Resilience**:
   - Added `devsphere.cache.invalidation.attempts.total`, `devsphere.cache.invalidation.success.total`, and `devsphere.cache.invalidation.failures.total`.
   - Redis exceptions during eviction are caught and logged without propagating runtime exceptions to callers.

4. **Testing**:
   - Added `CacheConsistencyAndInvalidationTest.java` verifying Scenarios A, B, C, and D.

## Pros and Cons of the Option

### Positive Consequences
* **Consistency Guarantee**: Read models in Redis are only purged after MySQL commits the new state.
* **Zero Transaction Blockage**: Redis downtime does not fail database writes.
* **Rollback Safety**: Transaction rollbacks keep valid cached data intact.

### Negative Consequences / Trade-offs
* **Transient Asymmetry Window**: A microsecond delay between DB commit and Redis delete execution exists, mitigated by post-commit execution speed (~1ms).

## Compliance & Verification

* All test suites (`auth-service`: 36, `api-gateway`: 31, `user-service`: 360+) verified green.
* `CacheConsistencyAndInvalidationTest` confirms post-commit invalidation, failure isolation, and targeted key eviction.
