# 58. Backend Production Hardening and Deployment Readiness

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`
* **Date**: 2026-09-01

---

## Context

As DevSphere reaches the final phase of backend development (Lesson 60), the existing microservices architecture (`user-service`, `auth-service`, `api-gateway`, `config-server`, `service-discovery`) required a comprehensive production-hardening and deployment-readiness audit. All system components needed to meet strict production standards for security, configuration externalization, container isolation, graceful shutdown, health probing, error sanitization, resilience, and observability.

---

## Decision

1. **Secret & Environment Configuration Externalization**:
   - Verified that no plain-text passwords, JWT secrets, or database credentials are committed in configuration files.
   - All connection URIs, credentials, pool sizes, Redis endpoints, Kafka bootstrap servers, and JWT signing keys are dynamically configurable via standard environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`, etc.).

2. **Docker Container Hardening**:
   - Standardized multi-stage Alpine Dockerfiles across all microservices using non-root execution (`USER devsphere`).
   - Configured production JVM container flags: `-XX:+UseG1GC`, `-XX:MaxRAMPercentage=75.0`, and `-Djava.security.egd=file:/dev/./urandom` to ensure efficient memory utilization, garbage collection, and fast seed generation.

3. **Graceful Shutdown & Lifecycle Management**:
   - Enabled Spring Boot graceful shutdown (`server.shutdown: graceful`) with a 30-second phase timeout (`spring.lifecycle.timeout-per-shutdown-phase: 30s`).
   - Guaranteed that in-flight HTTP requests and active Transactional Outbox publishing batches complete cleanly upon `SIGTERM` before process termination.

4. **Container Health Probes**:
   - Enabled Spring Boot Actuator liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) probes across microservices (`management.endpoint.health.probes.enabled: true`).

5. **Security & Error Sanitization**:
   - Verified that internal stack traces, database details, or exception class names are never exposed in API error responses.
   - Preserved `X-Trace-Id` trace correlation and low-cardinality Micrometer Prometheus metric tags.

---

## Consequences

* **Positive**:
  - Microservices can be safely deployed into production container environments.
  - Zero disruption to in-flight requests during rolling updates or autoscaling shutdowns.
  - Secrets and credentials are managed strictly outside application code and images.
  - Full observability and tracing remain intact without security or performance risks.
* **Trade-offs / Future Scope**:
  - Container memory allocations must adhere to configured RAM percentages (`MaxRAMPercentage=75.0`).
