# 50. API Reliability & Resilience Foundation

* **Status**: Accepted
* **Impacted Components**: `user-service`, `auth-service`, `api-gateway`, `config-repo`
* **Date**: 2026-08-30

---

## Context

DevSphere operates as a production microservices platform. As traffic grows, remote service calls, database connections, Redis caches, and resource-heavy operations (e.g. resume rendering and PDF/DOCX compilation) can experience transient network glitches, resource exhaustion, or cascading downstream delays.

We needed a standardized, production-grade reliability foundation to protect APIs against:
1. Indefinitely hanging requests.
2. Cascading failures across microservices.
3. Thread pool exhaustion during high-concurrency export/rendering operations.
4. Unnecessary retries on 4xx business/validation failures.
5. Inconsistent error formats during system degradation.

---

## Decision

1. **Standardized Resilience4j Architecture**:
   - Integrated Resilience4j Circuit Breakers, Retries, Bulkheads, and Time Limiters across `user-service`, `auth-service`, and `api-gateway`.
   - Externalized operational parameters into `config-repo/` (`user-service.yml`, `auth-service.yml`, `api-gateway.yml`).

2. **Circuit Breaker Policy**:
   - `slidingWindowSize = 10`, `minimumNumberOfCalls = 5`, `failureRateThreshold = 50%`, `slowCallRateThreshold = 50%`, `slowCallDurationThreshold = 2000ms`, `waitDurationInOpenState = 5000ms`, `permittedNumberOfCallsInHalfOpenState = 3`.
   - Ignored non-transient 4xx business exceptions (`ResourceNotFoundException`, `UnauthorizedException`, `ForbiddenException`, `InvalidCredentialsException`, `EmailAlreadyExistsException`, `IllegalArgumentException`).

3. **Retry Policy**:
   - Restricted retries strictly to transient IO/network exceptions (`IOException`, `TimeoutException`) with max 3 attempts and exponential backoff (500ms initial, 2x multiplier).
   - Excluded non-idempotent write operations and business validation errors from retry policies.

4. **Bulkhead Concurrency Isolation**:
   - Applied `@Bulkhead(name = "userProfileBulkhead")` and `@Bulkhead(name = "resumeExportBulkhead")` (max 10 concurrent calls, 100ms max wait) to prevent rendering and compilation from starving web container threads.

5. **Error Contract & Global Exception Mapping**:
   - Standardized exception mapping in `GlobalExceptionHandler`:
     - `BulkheadFullException` $\rightarrow$ HTTP 503 `BULKHEAD_LIMIT_EXCEEDED`
     - `CallNotPermittedException` $\rightarrow$ HTTP 503 `DOWNSTREAM_SERVICE_UNAVAILABLE`
     - `RequestNotPermitted` $\rightarrow$ HTTP 429 `RATE_LIMIT_EXCEEDED`
     - `TimeoutException` $\rightarrow$ HTTP 504 `DOWNSTREAM_TIMEOUT`

6. **Observability**:
   - Recorded low-cardinality Micrometer counters (`devsphere_resilience_fallback_total{service="...",dependency="..."}`) and auto-configured Resilience4j metrics.

---

## Consequences

* **Positive**:
  - Complete protection against cascading failures and thread pool starvation.
  - Consistent HTTP 503/504/429 error contracts without internal stack trace leakage.
  - Zero fake data returned during system degradation.
* **Negative / Trade-offs**:
  - Adds minor processing overhead for Resilience4j aspect execution and sliding window tracking.
