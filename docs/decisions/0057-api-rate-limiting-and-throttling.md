# 57. Distributed API Rate Limiting and Throttling Architecture

* **Status**: Accepted
* **Impacted Components**: `api-gateway`
* **Date**: 2026-09-01

---

## Context

As DevSphere scales microservice operations, unprotected API endpoints are susceptible to traffic bursts, abusive client behavior, and brute-force authentication attacks. We required a production-grade, distributed rate-limiting mechanism at the API Gateway boundary to reject abusive requests before they consume downstream compute, database, or cache resources across `user-service` and `auth-service`.

---

## Decision

1. **Distributed Gateway Enforcement**:
   - Implemented `DistributedRateLimiterGatewayFilterFactory` at the API Gateway entry point.
   - Leveraged Spring Cloud Gateway's reactive `RedisRateLimiter` (`spring-boot-starter-data-redis-reactive`) backed by Redis Lua scripts.
   - Ensured all gateway instances share token bucket state in Redis without in-memory state drift.

2. **Client Identity Resolution**:
   - Registered `userKeyResolver` (`rate_limit:user:{userId}`) using trusted `X-Authenticated-User-Id` headers set by `JwtAuthenticationFilter` post-JWT validation.
   - Registered `ipKeyResolver` (`rate_limit:ip:{clientIp}`) for anonymous, login, and registration routes using normalized remote client IP address.

3. **Configurable Endpoint Rate Limit Policies**:
   - Centralized policy limits in `config-repo/api-gateway.yml`:
     - Auth Registration (`/api/v1/auth/register`): 5 req/s replenish, 10 burst capacity
     - Auth Login (`/api/v1/auth/login`): 5 req/s replenish, 10 burst capacity
     - Authenticated APIs (`/api/v1/users/**`, `/api/v1/resumes/**`, etc.): 20 req/s replenish, 40 burst capacity
     - Public Default (`/api/v1/auth/**`, `/api/v1/public/**`): 10 req/s replenish, 20 burst capacity

4. **HTTP 429 Error Contract**:
   - Rejections return `HTTP 429 TOO_MANY_REQUESTS` with `Retry-After: 1` header, `RATE_LIMIT_EXCEEDED` error code, JSON payload, and `X-Trace-Id` trace correlation.

5. **Redis Failure Resilience (`fail-open` / `fail-closed`)**:
   - Configured `app.rate-limit.fail-open=true` by default to allow traffic during Redis outages while logging warnings and recording error metrics (`devsphere_rate_limit_requests_total`, `result=error`).

6. **Observability**:
   - Low-cardinality Prometheus metrics `devsphere_rate_limit_requests_total` (`result` tag) and `devsphere_rate_limit_rejected_total` (`route` tag).

---

## Consequences

* **Positive**:
  - Eliminates downstream microservice overload from traffic bursts and brute-force authentication attacks.
  - Multi-gateway instances share synchronized rate limit counts via Redis.
  - Preserves trace correlation (`X-Trace-Id`) and strict low-cardinality metric rules.
* **Trade-offs / Future Scope**:
  - Redis cluster availability is required for precise rate-limiting state. Fail-open mode preserves gateway availability during Redis degradation.
