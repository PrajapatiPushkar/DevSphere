# Distributed API Rate Limiting and Throttling Architecture

This document describes the production-grade distributed **API Rate Limiting and Throttling System** implemented at the API Gateway level in DevSphere.

---

## 1. System Overview & Protection Context

DevSphere microservices are protected at the entry point (API Gateway) against:
- Accidental traffic spikes and denial-of-service bursts
- Brute-force credential attempts on authentication routes
- Resource starvation caused by abusive anonymous or authenticated clients
- Downstream microservice overload (`user-service`, `auth-service`)

```text
Client Request
      │
      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ DevSphere API Gateway                                                  │
│                                                                        │
│ 1. JwtAuthenticationFilter (Mutates header: X-Authenticated-User-Id)  │
│ 2. KeyResolver (userKeyResolver / ipKeyResolver)                       │
│ 3. DistributedRateLimiter (Spring Cloud Gateway + Reactive Redis)     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 │                                       │
                 ▼ (Allowed)                             ▼ (Rate Limited / 429)
      Downstream Microservice                   HTTP 429 TOO_MANY_REQUESTS
    (user-service / auth-service)               + Retry-After: 1 Header
```

---

## 2. Distributed Architecture & Algorithm

- **Token Bucket Algorithm**: Implemented using Spring Cloud Gateway's `RedisRateLimiter`, supported by Redis Lua scripts for atomic token consumption across multiple gateway instances.
- **Shared State Storage**: All API Gateway instances connect to the central Redis cluster (`SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`), maintaining synchronized token bucket counts without in-memory state drift.
- **Replenish Rate & Burst Capacity**:
  - `replenishRate`: Average number of tokens added to the bucket per second.
  - `burstCapacity`: Maximum number of requests allowed in a single burst.

---

## 3. Client Identity Resolution Strategy

Key resolution is managed by `RateLimiterConfig`:

| Route Category | KeyResolver Bean | Key Pattern | Resolution Strategy |
| :--- | :--- | :--- | :--- |
| **Authenticated APIs** (`/api/v1/users/**`, `/api/v1/resumes/**`, etc.) | `userKeyResolver` | `rate_limit:user:{userId}` | Reads trusted `X-Authenticated-User-Id` populated by `JwtAuthenticationFilter`. Falls back to `rate_limit:ip:{clientIp}` for unauthenticated requests. |
| **Authentication Endpoints** (`/api/v1/auth/login`, `/api/v1/auth/register`) | `ipKeyResolver` | `rate_limit:ip:{clientIp}` | Uses client IP address extracted from `ServerWebExchange.getRemoteAddress()`. |
| **Public Endpoints** (`/api/v1/public/**`) | `ipKeyResolver` | `rate_limit:ip:{clientIp}` | Uses client IP address extracted from `ServerWebExchange.getRemoteAddress()`. |

---

## 4. Rate Limit Policy Configuration

Configured centrally in `config-repo/api-gateway.yml`:

```yaml
app:
  rate-limit:
    enabled: true
    fail-open: true
    timeout-ms: 2000
    login:
      replenish-rate: 5
      burst-capacity: 10
    registration:
      replenish-rate: 5
      burst-capacity: 10
    authenticated:
      replenish-rate: 20
      burst-capacity: 40
    public-default:
      replenish-rate: 10
      burst-capacity: 20
```

---

## 5. HTTP Response Contract (`HTTP 429`)

When a client exceeds its rate limit, the gateway rejects the request before it reaches downstream microservices:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 1
X-Trace-Id: c64f392e874f45389861e3dc821cc67e

{
  "timestamp": "2026-09-01T05:50:00.000Z",
  "status": 429,
  "error": "TOO_MANY_REQUESTS",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please try again later.",
  "path": "/api/v1/auth/login",
  "traceId": "c64f392e874f45389861e3dc821cc67e"
}
```

---

## 6. Redis Outage Policy (`fail-open` / `fail-closed`)

The rate limiter supports configurable failure handling (`app.rate-limit.fail-open`):
- **`fail-open = true` (Default)**: If Redis is unavailable or times out (`timeout-ms: 2000`), the gateway logs a warning and allows the request to pass through to preserve application availability.
- **`fail-open = false`**: If Redis is unavailable, the gateway fails closed by returning `HTTP 429 TOO_MANY_REQUESTS` to enforce strict security boundaries.

---

## 7. Metrics & Observability

Low-cardinality Prometheus metrics are published via Micrometer:
- `devsphere_rate_limit_requests_total` (Tags: `result` = `allowed` \| `rejected` \| `error`)
- `devsphere_rate_limit_rejected_total` (Tags: `route` = `auth-login` \| `auth-register` \| `user-service` \| `auth-service`)

> [!NOTE]
> High-cardinality values such as `userId`, `IP`, `email`, or `JWT` tokens are strictly excluded from metric tags. Trace IDs are preserved via MDC and `X-Trace-Id` headers.
