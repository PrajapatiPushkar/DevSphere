# 8. User Profile Caching with Redis

Date: 2026-08-23

## Status

Accepted

## Context

User profile reads (`GET /api/v1/users/me`) are a read-heavy path in DevSphere. Querying MySQL on every single request introduces unnecessary database load as traffic scales. We need a high-performance distributed caching mechanism for `User Service` that improves read throughput while maintaining MySQL as the system's single source of truth.

## Decision

We introduce **Redis** as a distributed cache in `User Service` using the **Cache-Aside (Lazy Caching)** pattern:

1. **MySQL Source of Truth**: MySQL remains the authoritative database. Redis stores temporary, derived JSON DTO objects (`UserProfileResponse`).
2. **Read Path**: Check Redis key `user-profile:{userId}`. On hit, return cached DTO. On miss, read from MySQL, populate Redis with configurable TTL (default 5 minutes), and return response.
3. **Write Path**: `PUT /api/v1/users/me` updates MySQL first. Upon DB success, the cache key `user-profile:{userId}` is evicted from Redis.
4. **Resilience & Fallback**: All Redis calls in `RedisUserProfileCache` catch infrastructure exceptions (`DataAccessException`, connection errors) gracefully, logging warnings and falling back to MySQL. Redis failures do NOT trigger HTTP 500 errors or rollback successful DB updates.
5. **No Credential Exposure**: Cached data is limited strictly to `UserProfileResponse` DTO. No secrets, credentials, or JPA entities are cached.

## Consequences

### Positive
- Substantially reduced MySQL read query volume for frequent profile accesses.
- Lower response latency for cached profile requests.
- High resilience: Service remains fully functional if Redis crashes or restarts.

### Negative / Trade-offs
- Eventual consistency during brief invalidation lag windows.
- Additional operational component (Redis instance) in infrastructure.
