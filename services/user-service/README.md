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
- **401 vs 403 HTTP Semantics**: Unauthenticated requests return `401 Unauthorized`. Accessing another user's profile without `ROLE_ADMIN` returns `403 Forbidden`.
- **Admin Endpoint Security**: Administrative endpoints (`/api/v1/users/admin/**`) require `ROLE_ADMIN` authority.

---

## Observability & Custom Metrics

- **Prometheus Metrics Endpoint**: `/actuator/prometheus`
- **Custom Business Metrics**:
  - `devsphere_kafka_events_processed_total{event_type="UserRegisteredEvent",status="success|duplicate|failure"}`
  - `devsphere_kafka_duplicate_events_total{event_type="UserRegisteredEvent"}`
  - `devsphere_kafka_events_retry_total{event_type="UserRegisteredEvent"}`
  - `devsphere_kafka_events_dlt_total{event_type="UserRegisteredEvent"}`
  - `devsphere_user_profile_created_total{source="kafka|http"}`
  - `devsphere_cache_hits_total{cache="user_profile"}`
  - `devsphere_cache_misses_total{cache="user_profile"}`

---

## Running Tests

```powershell
# Run unit and integration tests
mvn test
```
