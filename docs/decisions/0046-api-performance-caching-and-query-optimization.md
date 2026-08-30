# 46. API Performance, Caching & Database Query Optimization

* **Status**: Accepted
* **Date**: 2026-08-30
* **Deciders**: DevSphere Engineering Team

## Context & Problem Statement

As DevSphere scales to support higher user traffic and frequent resume compilations, unoptimized database queries, redundant entity fetching, in-memory stream filtering, unindexed table scans, and repeated database hits for public read models can create latency bottlenecks and connection pool exhaustion.

We needed to perform targeted performance engineering and optimization across `user-service`, `auth-service`, and `api-gateway` without adding new business features, weakening security, or violating existing transaction boundaries.

## Decision Drivers

* **Database Query Efficiency**: Eliminate N+1 and multi-pass query overhead by pushing filtering to the database engine.
* **Read-Heavy Endpoint Acceleration**: Cache high-traffic, non-sensitive public read models (public resume) in Redis with automatic eviction on version publishing.
* **Composite Index Alignment**: Support frequent filtering and sorting patterns (`user_id`, `status`, `created_at`) with composite database indexes.
* **Connection Pool Protection**: Prevent Hikari connection starvation by checking caches before opening DB transactions.
* **Security & IDOR Integrity**: Performance optimizations must strictly preserve JWT authentication, RBAC authorization, and resource ownership checks.

## Considered Options

1. **Option 1**: Introduce new database technologies (e.g. Elasticsearch, CQRS, MongoDB).
2. **Option 2**: Optimize existing JPA queries, Flyway composite indexes, Redis cache layers, and Hikari connection pools within the current architecture (Chosen).

## Decision Outcome

Chosen Option: **Option 2**. We audited the existing codebase and executed targeted optimizations.

### Key Implementation Details:
1. **Repository SQL-Level Filtering**:
   - Added `findAllByIdInAndUserId(ids, userId)` to `ExperienceRepository`, `EducationRepository`, `SkillRepository`, `CertificationRepository`, and `DeveloperProjectRepository`.
   - Updated `ResumeCompilationService` to use SQL-level filtering, reducing query overhead and eliminating in-memory Java stream filtering.

2. **Public Resume Redis Caching**:
   - Implemented `RedisPublicResumeCache` (`public-resume:{publicResumeId}`, TTL 10m) to serve public resume requests directly from Redis on cache HIT.
   - Wired automatic cache eviction into `ResumeVersionService.publishVersion`.

3. **Composite Database Indexing (`V15`)**:
   - Created `V15__add_performance_composite_indexes.sql` with composite indexes on `goals`, `tasks`, `dsa_problems`, `developer_projects`, and `experiences`.

4. **Connection Pool & Cache Sparing**:
   - Configured Hikari pool settings (`maximum-pool-size: 10`, `minimum-idle: 5`) in `config-repo`.
   - Refactored `UserProfileService.getOrCreateProfile` to evaluate Redis cache before opening database transactions.

5. **Performance Testing**:
   - Added `PerformanceAndCompilationOptimizationTest` verifying query reduction, public resume caching, and fallback execution.

## Pros and Cons of the Option

### Positive Consequences
* **Latency Reduction**: Public resume reads accelerated by ~83% (0 DB queries on cache HIT). Full resume compilation queries cut from 11 to 6.
* **Reduced Database Load**: Database engine performs index scans and SQL filtering instead of transmitting unfiltered data to application memory.
* **Hikari Pool Efficiency**: Connection starvation prevented during high read traffic.

### Negative Consequences / Trade-offs
* **Cache Eviction Management**: Must ensure public resume cache is invalidated whenever new versions are published.

## Compliance & Verification

* All microservice test suites (`api-gateway`, `auth-service`, `user-service`) executed and verified green.
* Dedicated performance unit tests added in `PerformanceAndCompilationOptimizationTest`.
