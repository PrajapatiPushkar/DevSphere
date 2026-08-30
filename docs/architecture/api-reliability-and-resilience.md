# API Reliability & Resilience Architecture

This document describes the reliability goals, Resilience4j integration, circuit breaker state machine, retry semantics, bulkhead isolation, timeout strategies, fallback mechanics, and observability standards across DevSphere microservices (`user-service`, `auth-service`, `api-gateway`).

---

## 1. Executive Summary

Lesson 51 establishes an enterprise-grade **API Reliability & Resilience Foundation** across the DevSphere microservices platform.

The strategy protects DevSphere against:
- Downstream service delays and cascading timeouts.
- Transient network glitches and temporary infrastructure failures.
- Overloaded CPU/memory resources during high-concurrency operations.
- Repeated failure storms.

Resilience is built using **Resilience4j** integrated into Spring Boot 3, externalized through Spring Cloud Config Server (`config-repo/`), and monitored via Micrometer metrics.

---

## 2. Core Resilience Strategies

### A. Timeout Protection
- **Goal**: Prevent indefinitely hanging HTTP requests and connection-pool thread starvation.
- **Configured Timeouts**:
  - API Gateway TimeLimiter: `3000ms`
  - User Service TimeLimiter: `3000ms`
  - Auth Service TimeLimiter: `3000ms`
  - Database Hikari connection timeout: `20000ms`
  - Redis connection timeout: `2000ms`
- **Failure Contract**: Exceeded time limits throw `TimeoutException`, mapped by `GlobalExceptionHandler` to HTTP `504 Gateway Timeout` (`DOWNSTREAM_TIMEOUT`).

### B. Circuit Breaker (`Resilience4j`)
- **State Machine**:
  ```text
  CLOSED
     ↓ (Failure Rate >= 50% or Slow Call Rate >= 50%)
  OPEN (Rejects calls with CallNotPermittedException -> HTTP 503)
     ↓ (Wait Duration = 5000ms)
  HALF_OPEN (Permits 3 probe calls)
     ├── (All 3 succeed) ──► CLOSED
     └── (Any probe fails) ─► OPEN
  ```
- **Configuration Defaults**:
  - `slidingWindowType`: `COUNT_BASED`
  - `slidingWindowSize`: `10`
  - `minimumNumberOfCalls`: `5`
  - `failureRateThreshold`: `50%`
  - `slowCallRateThreshold`: `50%`
  - `slowCallDurationThreshold`: `2000ms`
  - `waitDurationInOpenState`: `5000ms`
  - `permittedNumberOfCallsInHalfOpenState`: `3`
  - `automaticTransitionFromOpenToHalfOpenEnabled`: `true`
- **Ignored Business Exceptions**: 4xx exceptions (`ResourceNotFoundException`, `UnauthorizedException`, `ForbiddenException`, `InvalidCredentialsException`, `EmailAlreadyExistsException`, `IllegalArgumentException`) do **NOT** count as circuit breaker failures.

### C. Retry Strategy
- **Transient Failures Only**: Retries are restricted to genuine transient errors (`java.io.IOException`, `java.util.concurrent.TimeoutException`).
- **Idempotency Guarantee**: Non-idempotent write operations are excluded from retries to prevent duplicate entity creation or state transitions.
- **Bounded Retry Parameters**:
  - `maxAttempts`: `3`
  - `waitDuration`: `500ms`
  - `enableExponentialBackoff`: `true` (2x multiplier)

### D. Bulkhead Concurrency Isolation
- **Goal**: Isolate CPU/memory-intensive tasks (such as resume compilation and PDF/DOCX rendering) to prevent thread pool exhaustion.
- **Configuration**:
  - `userProfileBulkhead` / `resumeExportBulkhead`: `maxConcurrentCalls = 10`, `maxWaitDuration = 100ms`
  - `authBulkhead`: `maxConcurrentCalls = 10`, `maxWaitDuration = 100ms`
- **Failure Contract**: Excess calls exceeding max capacity are rejected immediately with `BulkheadFullException`, mapped by `GlobalExceptionHandler` to HTTP `503 Service Unavailable` (`BULKHEAD_LIMIT_EXCEEDED`).

### E. Graceful Degradation & Fallback
- **Read Fallback**: Redis cache failures degrade gracefully to PostgreSQL/MySQL queries without throwing user-facing errors, while recording the `devsphere_resilience_fallback_total` metric.
- **Zero Fake Business Data**: Serious data failures return clean HTTP 503/504 error responses. Fake business data or silent data corruption masks are strictly forbidden.

---

## 3. Externalized Configuration Schema (`config-repo/`)

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 2000ms
        waitDurationInOpenState: 5000ms
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        ignoreExceptions:
          - com.devsphere.user.exception.ResourceNotFoundException
          - com.devsphere.user.exception.UnauthorizedException
          - com.devsphere.user.exception.ForbiddenException
          - java.lang.IllegalArgumentException
    instances:
      userServiceCircuitBreaker:
        baseConfig: default
      resumeExportCircuitBreaker:
        baseConfig: default
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.devsphere.user.exception.ResourceNotFoundException
          - com.devsphere.user.exception.UnauthorizedException
          - com.devsphere.user.exception.ForbiddenException
          - java.lang.IllegalArgumentException
    instances:
      userServiceRetry:
        baseConfig: default
      resumeExportRetry:
        baseConfig: default
  bulkhead:
    configs:
      default:
        maxConcurrentCalls: 10
        maxWaitDuration: 100ms
    instances:
      userProfileBulkhead:
        baseConfig: default
      resumeExportBulkhead:
        baseConfig: default
  timelimiter:
    configs:
      default:
        timeoutDuration: 3s
    instances:
      userProfileTimeLimiter:
        baseConfig: default
      resumeExportTimeLimiter:
        baseConfig: default
```

---

## 4. Observability & Metrics

- **Fallback Counter**: `devsphere_resilience_fallback_total{service="...",dependency="..."}`
- **Resilience4j Micrometer Auto-Metrics**:
  - `resilience4j.circuitbreaker.calls` (`kind=successful|failed|ignored`, `state=closed|open|half_open`)
  - `resilience4j.retry.calls` (`kind=successful_without_retry|successful_with_retry|failed_with_retry`)
  - `resilience4j.bulkhead.available.concurrent.calls`
- **Low-Cardinality Rule**: No user IDs, JWT tokens, public resume UUIDs, or PII are used as metric tags.
