# DevSphere Architecture — Production Resilience & Fault Tolerance

## Overview

DevSphere implements production-grade resilience and fault tolerance using **Spring Cloud Circuit Breaker** and **Resilience4j**. The goal of resilience in DevSphere is failure isolation: a temporary network glitch, slow database query, or downstream service outage must **never** cause a cascading failure across the entire platform.

---

## Target Resilience Architecture

```
                    ┌─────────────────────┐
                    │    API Gateway      │
                    └──────────┬──────────┘
                               │
                       TimeLimiter / CB
                               │
              ┌────────────────┴────────────────┐
              │                                 │
              ▼                                 ▼
        Auth Service                       User Service
              │                                 │
              │                                 ├── Redis (Cache)
              │                                 │     ↓ fallback
              │                                 └── MySQL (Source of Truth)
              │
              └──────── Kafka / Outbox ─────────┘
```

---

## Core Resilience Patterns

### 1. Bounded Timeouts (First Line of Defense)
- Every network request has a bounded execution window to prevent thread starvation and indefinite hanging.
- **Gateway Downstream Timeout**: Response timeout `5s`, connection timeout `3s`, TimeLimiter `3s`.
- **Redis Timeout**: Redis operations execute with fast timeouts. Redis unavailability logs a warning and falls back to MySQL.

### 2. Circuit Breakers (`Resilience4j`)
- Applied at remote service boundaries (`API Gateway -> Auth Service` and `API Gateway -> User Service`).
- **State Machine**:
  - **CLOSED**: Normal operation. All requests pass through.
  - **OPEN**: Failure rate exceeds threshold (50% failure rate over sliding window of 5 calls). Subsequent calls fail fast and trigger fallback.
  - **HALF_OPEN**: After `5000ms` wait duration, trial calls probe downstream recovery. If successful, transitions to `CLOSED`; if failing, transitions back to `OPEN`.

### 3. Graceful Fallbacks (Truthful Degradation)
- Gateway returns a standard HTTP 503 Service Unavailable JSON payload when downstream services are unavailable or timing out:
  ```json
  {
    "status": 503,
    "error": "SERVICE_UNAVAILABLE",
    "code": "SERVICE_UNAVAILABLE",
    "message": "The requested service is temporarily unavailable. Please try again later."
  }
  ```
- **Rule of Truthful Fallbacks**: Fallbacks **never** return fake successful business data (e.g., fake user profiles or fake authentication tokens). A downstream outage remains an explicit service unavailable response.

### 4. Selective Retries & Write Protection
- Retries are strictly reserved for **transient network failures** (e.g. connection refused, network timeout).
- **Non-Idempotent Write Protection**: Automatic retries are **never** placed around non-idempotent operations like `POST /api/v1/auth/register` to prevent duplicate database inserts or side-effects. Exactly-once business semantics are preserved by the Transactional Outbox pattern.
- **Client & Security Error Exclusion**: HTTP 400 Bad Request, 401 Unauthorized, and 403 Forbidden exceptions are classified as client/security outcomes and are **never** retried or counted towards circuit breaker failure thresholds.

### 5. Bulkhead Resource Isolation
- Bounds maximum concurrent calls to intensive internal operations (e.g., Semaphore bulkhead `maxConcurrentCalls: 10`). Prevents single heavy operations from exhausting thread pools.

---

## Separation of Kafka Consumer Reliability & Resilience4j

Kafka asynchronous messaging uses Spring Kafka consumer retries, backoff, and Dead Letter Topics (DLT) established in Lesson 11. Resilience4j circuit breakers are **not** wrapped around Kafka listeners, keeping event-driven retry logic distinct from synchronous HTTP client resilience.

---

## Observability & Resilience Metrics

Micrometer and Prometheus track resilience status via standard and low-cardinality custom metrics:
- `resilience4j.circuitbreaker.state{name="authServiceCircuitBreaker|userServiceCircuitBreaker",state="closed|open|half_open"}`
- `resilience4j.circuitbreaker.calls{name=...,kind="successful|failed|ignored|not_permitted"}`
- `devsphere_resilience_fallback_total{service="auth-service|user-service|gateway",dependency="http|redis"}`
