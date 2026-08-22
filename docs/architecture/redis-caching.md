# Redis Distributed Caching Architecture

This document describes the Redis caching architecture introduced in **Lesson 9** for `User Service`.

---

## 1. Overview

To optimize performance and reduce read pressure on MySQL database instances, `User Service` employs Redis as a distributed cache for user profiles.

```
Request (GET /api/v1/users/me)
        │
        ▼
 ┌──────────────┐
 │ User Service │ (Port 8082)
 └──────┬───────┘
        │
        ▼
 ┌──────────────┐  CACHE HIT
 │    Redis     ├───────────────► Return UserProfileResponse DTO
 └──────┬───────┘
        │ CACHE MISS / UNHEALTHY
        ▼
 ┌──────────────┐
 │    MySQL     │ (devsphere_user database)
 └──────┬───────┘
        │
        ├───────────────────────► Populate Redis Cache (TTL: 5m)
        │
        ▼
  Return UserProfileResponse DTO
```

---

## 2. Core Architectural Principles

1. **MySQL is the Source of Truth**:
   - MySQL (`devsphere_user` database) is the absolute, persistent source of truth.
   - Redis holds temporary, derived data. If Redis is flushed, restarted, or unavailable, the system continues to operate seamlessly by reading directly from MySQL.

2. **Cache-Aside (Lazy Caching) Pattern**:
   - **Read Flow (`GET /api/v1/users/me`)**:
     1. User Service checks Redis for key `user-profile:{userId}`.
     2. **Cache Hit**: Returns cached `UserProfileResponse` DTO directly.
     3. **Cache Miss**: Queries MySQL, populates Redis cache key with configurable TTL (default 5 minutes), and returns `UserProfileResponse`.
   - **Write Flow (`PUT /api/v1/users/me`)**:
     1. User Service updates MySQL first.
     2. Upon successful DB transaction commit, User Service evicts cache key `user-profile:{userId}` from Redis.
     3. The subsequent `GET` request will trigger a cache miss and reload the fresh profile from MySQL.

3. **Demand-Driven Cache Population**:
   - Asynchronous registration profile creation (`UserRegisteredEvent` from Kafka) writes to MySQL ONLY.
   - Redis cache is populated on-demand when the user makes their first `GET /api/v1/users/me` request.

4. **DTO-Only Caching**:
   - Only `UserProfileResponse` DTO (JSON serialized with `Jackson2JsonRedisSerializer`) is cached in Redis.
   - JPA entities (`UserProfile`) are never stored in Redis to prevent persistence coupling, lazy-loading proxy errors, and schema leakage.
   - Passwords, hashes, JWTs, and internal database credentials are NEVER cached.

---

## 3. Cache Key Strategy & TTL Configuration

- **Key Pattern**: `user-profile:{userId}` (e.g. `user-profile:101`).
- **TTL**: Configurable in `application.yml` via `app.cache.user-profile-ttl: 5m`.
- **TTL Rationale**:
  - Bounded memory usage.
  - Automatic stale data eviction even if cache eviction calls fail.
  - Defense against missed invalidation edge cases.

---

## 4. Resilience & Fallback Behavior

`RedisUserProfileCache` wraps all Redis operations in exception try-catch blocks:
- **Redis Outage during `GET`**: Logs a warning (`"Redis unavailable during cache get for userId=..."`) and returns `Optional.empty()`, triggering seamless fallback to MySQL.
- **Redis Outage during `PUT` / Eviction**: Logs a warning (`"Redis unavailable during cache eviction for userId=..."`) without rolling back the successful MySQL transaction.
- **Actuator Health Integration**: `management.health.redis.enabled: false` ensures `/actuator/health` remains `UP` (200 OK) when Redis is down, reflecting the application's true operational status powered by MySQL.
