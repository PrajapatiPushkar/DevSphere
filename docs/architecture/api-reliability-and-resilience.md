# DevSphere Architecture — API Reliability & Resilience Hardening

## Overview

This document defines the production resilience architecture for the **DevSphere** microservices platform, hardened in Lesson 46. The platform isolates downstream dependency failures, bounds timeouts, enforces safe retry policies, tests circuit breaker states, protects thread pools via bulkheads, guarantees Redis and Kafka failure isolation, and standardizes error contracts.

---

## 1. Resilience Architecture Overview

```text
 Client
   │
   ▼
┌─────────────────────────────────────────────────────────┐
│                      API Gateway                        │
│  - Distributed Rate Limiter (Redis fail-open)           │
│  - Circuit Breakers (Auth & User Services)              │
│  - TimeLimiter (3s bounded timeout)                     │
│  - FallbackController (Standardized 503 error contract) │
└──────────────┬────────────────────────────┬─────────────┘
               │                            │
               ▼                            ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│       Auth Service       │   │       User Service       │
│  - Outbox Pattern        │   │  - Redis Cache Fallback │
│  - Bounded Retries (5x)  │   │  - Heavy Export Bulkhead │
└──────────────┬───────────┘   └────────────┬─────────────┘
               │                            │
               ▼                            ▼
         Apache Kafka                  Redis Cache
     (Durable Event Queue)         (Optional Read Cache)
```

---

## 2. Bounded Timeout Strategy

All network and dependency calls are strictly bounded to prevent thread pool exhaustion and hung connections:

| Boundary / Dependency | Connection Timeout | Read / Request Timeout | Fallback Behavior |
| :--- | :--- | :--- | :--- |
| Gateway → Auth Service | 2000ms | 3000ms | HTTP 503 `DOWNSTREAM_SERVICE_UNAVAILABLE` |
| Gateway → User Service | 2000ms | 3000ms | HTTP 503 `DOWNSTREAM_SERVICE_UNAVAILABLE` |
| User Service → Redis | 1000ms | 2000ms | Fallback to MySQL DB query |
| Auth Service → Kafka | 2000ms | 3000ms | Outbox event retained in `PENDING` state |
| User Service → Database | 2000ms | 5000ms | HTTP 409 / 503 Transaction Rollback |

---

## 3. Targeted Retry Policy

Retries are bounded, short, and applied exclusively to transient network/infrastructure errors.

### Safe to Retry
- Transient TCP connection resets
- Temporary network timeouts
- Downstream HTTP 503 (Service Unavailable)
- Transient Outbox publish errors (bounded up to 5 attempts)

### NOT Retried
- 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict
- Non-idempotent HTTP POST mutations (e.g. user registration, profile creation) without explicit idempotency guarantees
- Business constraint and validation violations

---

## 4. Resilience4j Circuit Breaker Strategy

Circuit breakers protect downstream microservices from cascading failures.

### Configuration Parameters
- **Sliding Window**: Count-based, size = `5` calls
- **Minimum Calls**: `3` calls
- **Failure Threshold**: `50%`
- **Wait Duration in Open State**: `5000ms`
- **Permitted Calls in Half-Open State**: `2` calls
- **Automatic Transition Open → Half-Open**: `Enabled`

### State Transitions
```text
┌────────┐  50% failures  ┌──────┐  5s wait duration  ┌───────────┐
│ CLOSED │───────────────►│ OPEN │───────────────────►│ HALF_OPEN │
└────────┘                └──────┘                    └─────┬─────┘
    ▲                        │                              │
    │                        └─► Fast 503 Fallback          │
    │                                                       │
    └────────────────── 2 successful probes ────────────────┘
```

---

## 5. Bulkhead Concurrency Protection

Heavy CPU-bound or memory-intensive rendering operations (such as HTML/PDF/DOCX resume export) are isolated using Resilience4j Bulkheads:

- **Instance Name**: `userProfileBulkhead`
- **Max Concurrent Calls**: `10`
- **Max Wait Duration**: `100ms`
- **Rejection Exception**: `BulkheadFullException` mapped to HTTP 503 `BULKHEAD_LIMIT_EXCEEDED`

---

## 6. Dependency Isolation & Failure Behavior

### Redis Cache Failure (Optional Dependency)
- **Get Failure**: Returns `Optional.empty()`, increments `devsphere_resilience_fallback_total{dependency="redis"}` counter, logs warning, and queries MySQL directly.
- **Put / Evict Failure**: Logs warning without throwing, allowing the primary business transaction to complete successfully. Database remains the single source of truth.

### Kafka Broker Failure (Asynchronous Dependency)
- **Registration Flow**: User registration writes user entity + Outbox event atomically to MySQL.
- **Outbox Publisher**: If Kafka is unreachable during outbox polling, event retry count is incremented and status remains `PENDING`. Kafka outage does not break HTTP registration.

---

## 7. Standardized Error Contract

All resilience failures conform to the platform's standardized error JSON schema:

```json
{
  "timestamp": "2026-08-30T05:18:27Z",
  "status": 503,
  "error": "SERVICE_UNAVAILABLE",
  "code": "DOWNSTREAM_SERVICE_UNAVAILABLE",
  "message": "Auth Service is temporarily unavailable. Please try again later.",
  "path": "/api/v1/auth/login",
  "traceId": "trace-987654321"
}
```

---

## 8. Observability & Tracing

- **Micrometer Counters**:
  - `devsphere_resilience_fallback_total{service, dependency}`
  - `devsphere.outbox.publish.failures.total{event_type}`
  - `resilience4j.circuitbreaker.calls`
  - `resilience4j.bulkhead.calls`
- **Distributed Tracing**: OpenTelemetry / Micrometer trace IDs (`X-Trace-Id`, `traceparent`) are propagated through Gateway, service execution, fallback handlers, and MDC logs. High-cardinality tags (userId, request body) are strictly excluded from metrics.
