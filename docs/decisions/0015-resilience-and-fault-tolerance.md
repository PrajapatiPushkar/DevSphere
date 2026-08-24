# ADR 0015: Production Resilience and Fault Tolerance

## Status
Accepted

## Context
DevSphere is a distributed microservices platform composed of an API Gateway, Auth Service, User Service, Config Server, Eureka Service Discovery, Kafka, Redis, and MySQL. In a distributed environment, remote network calls, database connections, and cache dependencies can experience transient delays, network partitions, or unexpected outages. Without failure isolation, a failure in one service can consume system resources (hanging threads, memory exhaustion) and cause cascading outages across the entire platform.

## Decision
We adopt **Spring Cloud Circuit Breaker** with **Resilience4j** to enforce application-level resilience and fault tolerance across DevSphere:

1. **Perimeter Circuit Breaking & Bounded Timeouts**:
   - Integrate `spring-cloud-starter-circuitbreaker-reactor-resilience4j` in API Gateway.
   - Configure bounded connection timeouts (`3s`), response timeouts (`5s`), and TimeLimiter (`3s`).
   - Define circuit breakers (`authServiceCircuitBreaker`, `userServiceCircuitBreaker`) with sliding window size of 5, 50% failure rate threshold, and 5000ms wait duration in OPEN state.

2. **Truthful Downstream Fallbacks**:
   - Implement Gateway `FallbackController` returning standard HTTP 503 Service Unavailable JSON payloads when downstream services fail or open circuit breakers.
   - Prohibit fake business fallbacks (e.g. returning dummy user profiles or dummy tokens).

3. **Selective Retries & Non-Idempotent Write Protection**:
   - Limit retries to transient network errors.
   - Exclude 400 Bad Request, 401 Unauthorized, and 403 Forbidden exceptions from retries and circuit breaker failure counts.
   - Exclude non-idempotent registration operations (`POST /api/v1/auth/register`) from automatic retries to avoid duplicate side-effects.

4. **Independent Redis Cache Fallback**:
   - When Redis is unavailable in User Service, operations log a warning and fall back to the MySQL database (source of truth).
   - If MySQL is also unavailable, return 503 Service Unavailable without fake fallback data.

5. **Separation of Concerns**:
   - Keep Kafka consumer retry/DLT mechanisms independent of HTTP Resilience4j resilience.
   - Centralize resilience configuration in Spring Cloud Config Server (`config-repo/`).

## Consequences

### Positive
- Prevents cascading failures and thread starvation across services.
- Provides predictable, bounded latency and fast failure during downstream outages.
- Preserves data consistency by avoiding automatic retries on non-idempotent writes.
- Maintains truthful error responses (503 Service Unavailable) without masking outages with fake data.
- Exposes resilience state via Micrometer Prometheus metrics.

### Tradeoffs / Negative
- Requires careful tuning of sliding window sizes and wait durations for local dev vs production.
- Adds Resilience4j filter configuration overhead in API Gateway and services.
