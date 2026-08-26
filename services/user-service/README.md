# DevSphere User Service

## Overview

The **User Service** is the core application domain microservice responsible for managing authenticated user profile information in DevSphere. It operates on port `8082`, enforces independent Spring Security JWT validation, role authorization (`ROLE_USER`, `ROLE_ADMIN`), resource ownership checks (`authenticatedUserId == targetUserId OR ROLE_ADMIN`), exposes Prometheus metrics (`/actuator/prometheus`), consumes centralized configuration from Spring Cloud Config Server (`http://localhost:8888`), and maintains strict database ownership over `devsphere_user` with Redis distributed caching and Kafka consumer reliability.

```
Client ──► API Gateway (:8080) ──[Gateway Security]──► User Service (:8082) ──┬──► Redis Cache (:6379)
                                                        │                     └──► MySQL (devsphere_user)
Direct Request ──────────────────[JWT Validation]───────┤
                                 [SecurityContext]      │
                                 [Ownership Check]      │
                                 [401 / 403 Handlers] ──┘

Kafka Topic (devsphere.user.v1) ──► Consumer ──[Idempotency & Retries]──► MySQL / DLT
```

---

## Security & Authorization
- **Independent JWT Validation**: Validates `Authorization: Bearer <token>` independently on incoming HTTP requests. Direct calls bypassing Gateway cannot bypass authorization.
- **Resource Ownership Enforcement**: Endpoints (`/api/v1/users/{userId}`) verify `authenticatedUserId == requestedUserId OR ROLE_ADMIN` via `@PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")`.
- **IDOR Protection**: Developer profile (`/api/v1/profile`), goal management (`/api/v1/goals/**`), and task management (`/api/v1/tasks/**`) endpoints enforce strict user identity scoping (`findByIdAndUserId`). Requests for non-owned tasks or goals return `404 Not Found` without leaking resource existence.
- **Goal & Task Domains**: Supports developer profiles, daily/weekly/long-term goals, and task management (`TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`) with optional goal association, dynamic overdue calculation, state transition guardrails, database pagination, and logical archival (`status = ARCHIVED`).
- **401 vs 403 HTTP Semantics**: Unauthenticated requests return `401 Unauthorized`. Accessing unauthorized resources returns `403 Forbidden` or `404 Not Found` for IDOR isolation.
- **Admin Endpoint Security**: Administrative endpoints (`/api/v1/users/admin/**`) require `ROLE_ADMIN` authority.

---

## Observability & Custom Metrics

- **Prometheus Metrics Endpoint**: `/actuator/prometheus`
- **Distributed Tracing (`devsphere-user-service`)**: Micrometer Tracing + OpenTelemetry bridge (`micrometer-tracing-bridge-otel`), OTLP exporter (`http://localhost:4318/v1/traces`), custom business spans (`user.profile.get`, `user.profile.create`, `user.profile.update`, `kafka.user-registered.process`), log MDC correlation, and W3C trace context extraction from incoming Kafka record headers.
- **Custom Business & Resilience Metrics**:
  - `devsphere_kafka_events_processed_total{event_type="UserRegisteredEvent",status="success|duplicate|failure"}`
  - `devsphere_kafka_duplicate_events_total{event_type="UserRegisteredEvent"}`
  - `devsphere_kafka_events_retry_total{event_type="UserRegisteredEvent"}`
  - `devsphere_kafka_events_dlt_total{event_type="UserRegisteredEvent"}`
  - `devsphere_user_profile_created_total{source="kafka|http"}`
  - `devsphere_cache_hits_total{cache="user_profile"}`
  - `devsphere_cache_misses_total{cache="user_profile"}`
  - `devsphere_resilience_fallback_total{service="user-service",dependency="redis"}`

---

## Container Registry Image

Published to GitHub Container Registry (GHCR):
- **Image URI**: `ghcr.io/<repository-owner>/devsphere-user-service`
- **Primary Tags**: `${GITHUB_SHA}`, `sha-<short-sha>`, `v1.0.0`
- **Local Build**: `docker build -t devsphere/user-service:local services/user-service`

---

## Running Tests

```powershell
# Run unit and integration tests
mvn test
```

