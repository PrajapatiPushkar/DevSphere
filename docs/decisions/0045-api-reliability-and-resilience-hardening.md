# 45. API Reliability & Resilience Hardening

* **Status**: Accepted
* **Date**: 2026-08-30
* **Deciders**: DevSphere Engineering Team

## Context & Problem Statement

DevSphere's microservice architecture relies on inter-service HTTP routing through API Gateway, asynchronous messaging via Kafka with the Transactional Outbox pattern, and read caching via Redis. In production, downstream service outages, slow dependencies, network partition events, and high-concurrency spikes can cause cascading failure, thread pool exhaustion, or inconsistent user experiences if not hardened.

We needed to review and harden DevSphere's existing backend resilience capabilities without introducing new business features or altering existing database schemas and core security contracts.

## Decision Drivers

* **Failure Isolation**: A downstream outage in one microservice must not cascade to other components or crash the API Gateway.
* **Bounded Timeouts**: No HTTP or database request should wait indefinitely for a downstream dependency.
* **Targeted Retries**: Retries must be strictly bounded and limited to transient infrastructure failures; non-idempotent business operations must not be retried blindly.
* **Truthful Degradation**: Optional cache failures must fall back to the primary database, while critical service outages must return standardized HTTP 503 error payloads instead of exposing internal stack traces.
* **Bulkhead Concurrency Limits**: Heavy CPU-bound or export operations must be constrained by bulkheads to prevent thread starvation.
* **Observability & Tracing**: All resilience events (fallbacks, circuit breaker trips, retries) must emit low-cardinality Micrometer metrics and preserve OpenTelemetry trace identifiers.

## Considered Options

1. **Option 1**: Rebuild custom resilience filters and client-side retry logic from scratch.
2. **Option 2**: Leverage and harden the existing Resilience4j, Spring Cloud CircuitBreaker, Redis cache fallback, and Outbox architecture already present in DevSphere (Chosen).

## Decision Outcome

Chosen Option: **Option 2**. We audited, hardened, and tested DevSphere's existing resilience capabilities.

### Key Implementation Details:
1. **API Gateway Fallback Standardisation**:
   - `FallbackController` updated to output standardized error payloads with `status: 503`, `error: "SERVICE_UNAVAILABLE"`, `code: "DOWNSTREAM_SERVICE_UNAVAILABLE"`, request `path`, and `traceId`.
   - Integration slice tests (`GatewayResilienceIntegrationTest`) added to verify fallback responses and headers under circuit open/downstream failure.

2. **Bulkhead Protection & Exception Mapping**:
   - Resilience4j `@Bulkhead(name = "userProfileBulkhead")` applied to heavy resume rendering in `ResumeRenderingService`.
   - `GlobalExceptionHandler` updated with handlers for `BulkheadFullException` (HTTP 503 `BULKHEAD_LIMIT_EXCEEDED`), `CallNotPermittedException` (HTTP 503 `DOWNSTREAM_SERVICE_UNAVAILABLE`), and `RequestTimeoutException` (HTTP 504 `DOWNSTREAM_TIMEOUT`).

3. **Redis & Outbox Failure Isolation**:
   - Verified that Redis connection failure during cache operations in `RedisUserProfileCache` falls back gracefully to MySQL, increments `devsphere_resilience_fallback_total{dependency="redis"}`, and returns empty cache optional without failing business operations.
   - Verified that Kafka broker failure in `OutboxPublisher` retains events in `PENDING` status for retry without breaking atomic database transactions or user registration.

4. **Resilience Observability**:
   - Low-cardinality Micrometer metrics maintained (`devsphere_resilience_fallback_total`, `devsphere.outbox.publish.failures.total`).
   - OpenTelemetry tracing IDs preserved across gateway routing, MDC logging, and exception responses.

## Pros and Cons of the Option

### Positive Consequences
* **System Stability**: Bounded timeouts and bulkheads prevent thread pool starvation during downstream degradation.
* **Data Consistency**: Atomic DB + Outbox pattern guarantees message durability even when Kafka is down.
* **Clean Client Contract**: Clients receive predictable HTTP 503 JSON responses with actionable code and traceId.
* **Security & IDOR Integrity**: Fallback handling strictly preserves JWT security, RBAC, and resource ownership checks.

### Negative Consequences / Trade-offs
* **Circuit Breaker Tuning Needed**: Failure rate thresholds (50%) and sliding window size (5 calls) require monitoring under dynamic production load.

## Compliance & Verification

* All microservice test suites (`api-gateway`, `auth-service`, `user-service`) executed and verified green.
* Dedicated failure injection tests added:
  - `GatewayResilienceIntegrationTest` in `api-gateway`
  - `RedisFailureAndResilienceIntegrationTest` in `user-service`
  - `KafkaOutboxResilienceIntegrationTest` in `auth-service`
