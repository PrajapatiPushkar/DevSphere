# DevSphere — Distributed Rate Limiting and API Protection Architecture

> **Status**: Production Architecture (Lesson 18)  
> *Distributed API Protection & Redis Token-Bucket Rate Limiting for DevSphere API Gateway*

---

## 1. Executive Overview & Problem Statement

In a distributed multi-instance microservices platform, single-client abuse (whether malicious, automated bot traffic, or run-away client loops) can exhaust Gateway resources, downstream microservice thread pools, and database connections. Local in-memory rate counters in each Gateway instance are insufficient because traffic distributed across $N$ Gateway instances effectively multiplies the global rate limit by $N$.

DevSphere implements **Distributed Rate Limiting at the API Gateway layer** backed by **Redis**. Every Gateway instance shares rate-limiting state in Redis using atomic Lua token-bucket evaluations.

---

## 2. Distributed Target Architecture

```
                         ┌─────────────────┐
                         │     Clients     │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │
                         │                 │
                         │ JWT Validation  │
                         │ Rate Limiting   │
                         │ RBAC            │
                         │ Resilience      │
                         └────────┬────────┘
                                  │
                         ┌────────┴────────┐
                         │                 │
                         ▼                 ▼
                    ┌──────────┐      ┌──────────────┐
                    │  Redis   │      │   Eureka     │
                    │          │      │  Discovery   │
                    │ Rate     │      └──────┬───────┘
                    │ Limits   │             │
                    │ + Cache  │             ▼
                    └──────────┘       ┌─────────────┐
                                       │ Microservices│
                                       └─────────────┘
```

### Key Components

1. **API Gateway Entry Point**: Enforces early request rejection before forwarding traffic to downstream services (`DEVSPHERE-AUTH-SERVICE`, `DEVSPHERE-USER-SERVICE`).
2. **Redis Distributed State Store**: Manages token-bucket counters atomically across all Gateway instances.
3. **Namespace Isolation**: Rate limiting operational state uses `rate_limit:*` keys, completely isolated from User Profile caching (`user_profile:*`).

---

## 3. Token Bucket Algorithm

DevSphere uses Spring Cloud Gateway's token bucket algorithm executed via Redis Lua scripts:

- **Bucket Capacity**: Maximum token burst allowance.
- **Refill Rate**: Number of tokens replenished per second.
- **Request Cost**: 1 token per HTTP request.

When tokens are available, the request proceeds down the filter chain. When tokens are exhausted, the Gateway immediately returns **HTTP 429 Too Many Requests**.

---

## 4. Key Resolution Strategy

### Authenticated Users (`userKeyResolver`)
- **Key Format**: `rate_limit:user:{userId}`
- **Source**: Derived strictly from `X-Authenticated-User-Id`, which is validated and populated by `JwtAuthenticationFilter` after cryptographically verifying the incoming JWT.
- **Security Guarantee**: Client-supplied headers like `X-Authenticated-User-Id`, `X-Role`, or `X-Admin` are automatically stripped before authentication, preventing key spoofing.

### Unauthenticated & Public Requests (`ipKeyResolver`)
- **Key Format**: `rate_limit:ip:{normalizedIp}`
- **Source**: Extracted from client socket remote address or validated trusted proxies.
- **Support**: Handles both IPv4 (`127.0.0.1`) and IPv6 (`::1`) address formats.

---

## 5. Endpoint-Specific Rate Limit Policies

| Endpoint Path | Rate Limit Key | Replenish Rate | Burst Capacity | Description |
| :--- | :--- | :---: | :---: | :--- |
| `POST /api/v1/auth/register` | Client IP (`rate_limit:ip:*`) | 5 / sec | 10 | Strict bot & registration abuse protection |
| `POST /api/v1/auth/login` | Client IP (`rate_limit:ip:*`) | 5 / sec | 10 | Brute force & credential stuffing protection |
| `GET/POST /api/v1/auth/**` | Client IP (`rate_limit:ip:*`) | 10 / sec | 20 | Public route default protection |
| `GET/POST /api/v1/users/**` | User ID (`rate_limit:user:*`) | 20 / sec | 40 | Authenticated user profile quota |
| `/actuator/health` | Exempt | — | — | Health probes preserved for monitoring |
| `/actuator/prometheus` | Exempt | — | — | Prometheus scraping endpoint preserved |

---

## 6. Safe 429 Response & Retry-After Header

When a client exceeds its allowed rate limit, the Gateway returns a standardized JSON response:

**HTTP Status**: `429 Too Many Requests`  
**Headers**: `Retry-After: 1`

```json
{
  "status": 429,
  "error": "TOO_MANY_REQUESTS",
  "message": "Rate limit exceeded. Please try again later."
}
```

No internal Redis keys, bucket state, or stack traces are leaked to the client.

---

## 7. Redis Failure Policy (Fail-Open vs Fail-Closed)

Rate limiting operations against Redis execute with a strict bounded timeout (`2000ms`). If Redis is unavailable or times out:

- **Fail-Open (`app.rate-limit.fail-open=true`)** *(Default for Local Dev)*:
  Logs a warning and allows requests to pass through to prioritize application availability.
- **Fail-Closed (`app.rate-limit.fail-open=false`)**:
  Logs an error and returns HTTP 429 to prioritize downstream infrastructure protection.

---

## 8. Observability: Metrics & Tracing

### Prometheus Metrics
Exposed at `/actuator/prometheus`:

- `devsphere_rate_limit_requests_total{result="allowed|rejected|error"}`
- `devsphere_rate_limit_rejected_total{route="auth-register|auth-login|auth-service|user-service"}`

> [!IMPORTANT]
> **Low-Cardinality Rule**: Metric tags are strictly bounded. Dynamic identifiers (`userId`, `email`, `IP`, `JWT`) are NEVER included as metric tags.

### Distributed Tracing
OpenTelemetry trace spans record a safe attribute:
- `rate_limit.result` = `allowed` | `rejected` | `error_fail_open` | `error_fail_closed`
