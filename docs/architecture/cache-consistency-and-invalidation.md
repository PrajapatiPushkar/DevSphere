# DevSphere Architecture — Reliable Cache Invalidation & Cache Consistency

## Overview

This document specifies the cache consistency policy, post-transaction-commit invalidation patterns, failure isolation, and observability model for the **DevSphere** platform, implemented in Lesson 48.

---

## 1. Cache Consistency Policy & Inventory

| Read Model | Cached Entity DTO | Cache Key Pattern | TTL | Source of Truth | Invalidation Triggers |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **User Profile** | `UserProfileResponse` | `user-profile:{userId}` | `5m` | MySQL `user_profiles` | `updateProfile` |
| **Public Resume** | `PublicResumeResponse` | `public-resume:{publicResumeId}` | `10m` | MySQL `resume_versions` (published) | `publishVersion`, `archiveVersion`, `archiveResumeProfile`, `deleteResumeProfile` |

---

## 2. Post-Transaction-Commit Invalidation

### Transaction Boundary Policy
To prevent race conditions where a concurrent read repopulates Redis with uncommitted or rolled-back data, cache invalidations are registered with Spring's `TransactionSynchronizationManager` via `TransactionAwareCacheInvalidator`:

```text
Database Transaction Starts
       │
   [MySQL Update Executed]
       │
   [Transaction Commit Requested]
       │
   Commit Succeeds? ─────────► NO ──► Skip Eviction (Cache remains safe)
       │
      YES
       │
       ▼
   [afterCommit Hook Executed]
       │
   [Targeted Redis Eviction] (`user-profile:{id}` or `public-resume:{publicId}`)
```

### Why Post-Commit?
- **Rollback Safety**: If database constraints fail or a runtime exception occurs before commit, `afterCommit()` is not triggered, preventing valid cached data from being unnecessarily wiped.
- **Race Condition Prevention**: Ensures database changes are globally visible to read queries before cache entries are removed.

---

## 3. Cache Failure Isolation & Resilience

Redis is strictly a read cache and must **never** break primary business writes or transaction outcomes.

```text
Database Write (MySQL) Succeeds
             │
             ▼
Transaction Post-Commit Invalidation Attempted
             │
             ├──► Redis Available? ──► YES ──► Evict Key + Metric `devsphere.cache.invalidation.success.total`
             │
             └──► Redis Down/Timeout?
                     │
                     ├─► Log Operational Warning (Safe, non-sensitive)
                     ├─► Metric `devsphere.cache.invalidation.failures.total`
                     ├─► Metric `devsphere_resilience_fallback_total`
                     └─► RETURN SUCCESS to Client (Database update preserved!)
                             │
                             ▼
                     TTL Fallback (5m/10m) Purges Stale Entry Automatically
```

---

## 4. Observability & Low-Cardinality Metrics

Micrometer metric counters track invalidation behavior across cache clusters using low-cardinality tags:

```text
devsphere.cache.invalidation.attempts.total  {cache="user_profile" | "public_resume"}
devsphere.cache.invalidation.success.total   {cache="user_profile" | "public_resume"}
devsphere.cache.invalidation.failures.total  {cache="user_profile" | "public_resume"}
```

- High-cardinality values (e.g. user IDs, public UUIDs, tokens) are strictly excluded from metric tag keys.

---

## 5. Handled Failure Scenarios

| Scenario | Execution Sequence | System State & Result |
| :--- | :--- | :--- |
| **Scenario A** | DB update commits ➔ Post-commit hook fires ➔ Redis eviction succeeds | New DB data saved. Old cache evicted immediately. |
| **Scenario B** | DB update commits ➔ Post-commit hook fires ➔ Redis network error | DB update succeeds. Failure logged & metric incremented. TTL purges stale cache after expiration. |
| **Scenario C** | DB update fails/rolls back ➔ `afterCommit()` skipped | DB state unchanged. Previous valid cache preserved without premature deletion. |
| **Scenario D** | Stale cache read attempted after mutation | Targeted eviction purges exact key without purging unrelated cache entries. |
