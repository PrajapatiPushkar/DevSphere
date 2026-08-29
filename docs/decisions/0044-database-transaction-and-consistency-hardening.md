# ADR 0036: Database Transaction & Consistency Hardening

## Status
Accepted

## Date
2026-08-29

## Context
Following Lesson 44's pagination, filtering, and query reliability improvements, backend services (`user-service`, `auth-service`) required production hardening around transaction boundaries, read/write consistency, atomic multi-step operations, optimistic/pessimistic locking, database constraint handling, and Outbox transactional integrity.

## Decision
We establish explicit transaction boundaries, concurrency controls, and standardized error handling for database consistency across DevSphere services.

### Key Architectural Rationale
1. **Service-Layer Transaction Scope**:
   All business transactions are declared strictly at the `@Service` layer. Controllers and repositories remain free of transaction management logic.

2. **Selective `@Version` Optimistic Locking**:
   Added `@Version private Long version;` to concurrency-sensitive entities (`ResumeProfile`, `Goal`, `Task`, `DeveloperProject`, `UserProfile`) to protect against lost updates during concurrent edits.

3. **Parent Pessimistic Lock for Resume Publishing**:
   Resume publishing (`publishVersion`) utilizes parent-row `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `ResumeProfile` to enforce that concurrent publishing attempts execute serially, guaranteeing that exactly one version is `PUBLISHED` at any given time.

4. **Transactional Outbox Consistency**:
   Business record modifications and Outbox event creation are committed together within the exact same database transaction. Outbox event publishing to Kafka executes asynchronously after commit.

5. **Read-Only Query Optimization**:
   Query methods across service components are annotated with `@Transactional(readOnly = true)` to optimize Hibernate session state tracking and memory usage.

6. **Standardized Database Error Mapping**:
   Mapped database-level exceptions in `GlobalExceptionHandler`:
   - `OptimisticLockingFailureException` $\rightarrow$ HTTP `409 CONFLICT` (`RESOURCE_VERSION_CONFLICT`)
   - `DataIntegrityViolationException` $\rightarrow$ HTTP `409 CONFLICT` (`DATABASE_CONSTRAINT_VIOLATION`)
   - `PessimisticLockingFailureException` $\rightarrow$ HTTP `409 CONFLICT` (`LOCK_ACQUISITION_TIMEOUT`)
   Raw SQL statements, database table names, and internal JPA trace details are concealed from API payloads.

7. **Database Source of Truth for Redis**:
   Redis cache operations remain secondary to primary MySQL database commits. Cache evictions occur after database mutation, and cache failures fail open safely.

## Consequences
### Positive
- Strict atomic multi-step operations prevent partial database commits or orphan records.
- Elimination of lost-update risks via optimistic and pessimistic locking.
- Complete protection against leaking internal database schemas or SQL traces in API responses.
- Outbox event creation guaranteed to commit in lockstep with business entity changes.

### Tradeoffs
- Concurrent updates to the same entity version will fail with HTTP 409 CONFLICT, requiring client retry.
