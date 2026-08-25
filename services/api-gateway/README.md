# DevSphere API Gateway

## Purpose
The API Gateway serves as the single external entry point and perimeter security boundary for all client requests in the DevSphere microservices architecture. It handles request routing, reactive JWT authentication validation, request header sanitization, distributed Redis token-bucket rate limiting, perimeter authorization, and resilience fallbacks before forwarding requests to downstream services.

---

## Current Status
> **Distributed Rate Limiting & API Protection (Lesson 18)**  
> The API Gateway enforces distributed Redis token-bucket rate limiting (`rate_limit:*`), authenticated user identity keying (`rate_limit:user:{userId}`), public client IP keying (`rate_limit:ip:{ip}`), strict login/registration rate policies, safe HTTP 429 JSON responses with `Retry-After` headers, configurable fail-open/fail-closed Redis error handling, bounded downstream timeouts, Resilience4j circuit breakers, header sanitization, and low-cardinality Prometheus metrics.

---

## Responsibilities
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Centralized configuration import from Spring Cloud Config Server (`http://localhost:8888`).
- Dynamic discovery-based routing via Netflix Eureka (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).
- Distributed Rate Limiting (`DistributedRateLimiter`): Token-bucket rate limiting backed by Redis with isolated keyspace (`rate_limit:*`).
- Context-Aware Key Resolvers: Identity-based key resolution for authenticated routes (`rate_limit:user:{userId}`) and IP-based resolution for public routes (`rate_limit:ip:{ip}`).
- Standard 429 Responses: Safe HTTP 429 JSON body and `Retry-After` response headers without leaking Redis internals.
- Configurable Redis Failure Handling: `app.rate-limit.fail-open=true|false` for availability vs protection tradeoffs.
- Distributed Tracing (`devsphere-api-gateway`): OpenTelemetry Micrometer Tracing integration, W3C `traceparent` header propagation, and span attribute `rate_limit.result`.
- Reactive JWT signature (HS256) & expiration validation.
- Role extraction from JWT claims (`roles: ["USER"]` / `["ADMIN"]`).
- Coarse route authorization (e.g. enforcing `ROLE_ADMIN` on `/api/v1/admin/**` routes).
- Bounded downstream timeouts and Spring Cloud Circuit Breaker Resilience4j protection.
- Truthful 503 Service Unavailable fallback handling (`/fallback/auth-service`, `/fallback/user-service`).
- Prometheus metrics exposure via `/actuator/prometheus` including `devsphere_rate_limit_requests_total` and `devsphere_rate_limit_rejected_total`.

---

## Actuator & Prometheus Endpoints

- `GET /actuator/health`: Service health indicator.
- `GET /actuator/prometheus`: Micrometer Prometheus metrics scrape target.

---

## Environment Variables

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `SPRING_REDIS_HOST` | Redis Server Host | `localhost` |
| `SPRING_REDIS_PORT` | Redis Server Port | `6379` |
| `APP_RATE_LIMIT_ENABLED` | Enable/Disable Rate Limiting | `true` |
| `APP_RATE_LIMIT_FAIL_OPEN` | Fail-Open on Redis Error | `true` |
| `JWT_SECRET` | HS256 Secret Key (Min 32 chars / 256 bits) | *Development fallback* |
| `CONFIG_SERVER_URL` | Spring Cloud Config Server URL | `http://localhost:8888` |
| `EUREKA_SERVER_URL` | Netflix Eureka Server URL | `http://localhost:8761/eureka/` |

---

## Route Security & Rate Limit Policies

| Route / Endpoint | Auth Policy | Rate Limit Key | Replenish Rate | Burst Capacity |
| :--- | :--- | :--- | :---: | :---: |
| `POST /api/v1/auth/register` | Public | Client IP (`rate_limit:ip:*`) | 5 / sec | 10 |
| `POST /api/v1/auth/login` | Public | Client IP (`rate_limit:ip:*`) | 5 / sec | 10 |
| `GET/POST /api/v1/auth/**` | Public | Client IP (`rate_limit:ip:*`) | 10 / sec | 20 |
| `GET/POST /api/v1/users/**` | Protected | User ID (`rate_limit:user:*`) | 20 / sec | 40 |
| `/actuator/health` | Public | Exempt | — | — |
| `/actuator/prometheus` | Public | Exempt | — | — |

---

## Container Registry Image

Published to GitHub Container Registry (GHCR):
- **Image URI**: `ghcr.io/<repository-owner>/devsphere-api-gateway`
- **Primary Tags**: `${GITHUB_SHA}`, `sha-<short-sha>`, `v1.0.0`
- **Local Build**: `docker build -t devsphere/api-gateway:local services/api-gateway`

---

## Running Automated Tests

```bash
cd services/api-gateway
mvn test
```

