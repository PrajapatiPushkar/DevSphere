# 64. Kubernetes Health and Self-Healing Strategy

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `config-repo`, `k8s/`
* **Date**: 2026-09-03

---

## Context

Following Kubernetes scaling implementation (Lesson 65), DevSphere required a standardized, production-grade strategy for container health probes (`startupProbe`, `livenessProbe`, `readinessProbe`), self-healing restart behavior, graceful HTTP server shutdown, and zero-downtime rolling deployments across all microservices.

---

## Decision

1. **Spring Boot Actuator Probe Integration**:
   - Microservices leverage Spring Boot Actuator's native Kubernetes probe endpoints (`/actuator/health/liveness` and `/actuator/health/readiness`).
   - Enabled via `management.health.probes.enabled: true`, `livenessstate.enabled: true`, and `readinessstate.enabled: true` in shared `config-repo/application.yml` and `config-server`.

2. **Probe Lifecycle Separation**:
   - **Startup Probe**: Configured with `initialDelaySeconds: 15`, `periodSeconds: 5`, `failureThreshold: 20` (providing up to 100s window for JVM boot, Hibernate initialization, and Kafka consumer group join). Temporarily disables liveness/readiness probes during startup.
   - **Liveness Probe**: Configured with `initialDelaySeconds: 5`, `periodSeconds: 10`, `failureThreshold: 3`. Failure triggers automatic container restart (`restartPolicy: Always`).
   - **Readiness Probe**: Configured with `initialDelaySeconds: 10`, `periodSeconds: 10`, `failureThreshold: 3`. Failure removes pod IP from Kubernetes Service endpoints without restarting the container.

3. **Graceful Application Shutdown**:
   - Web server graceful shutdown (`server.shutdown: graceful`) and a 20-second phase timeout (`spring.lifecycle.timeout-per-shutdown-phase: 20s`) are configured in shared application defaults.
   - Aligns with Kubernetes Deployment `terminationGracePeriodSeconds: 30` to allow active in-flight requests to complete during rolling deployments or container termination.

4. **Zero-Downtime Rolling Update Strategy**:
   - Stateless microservices (`api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`) utilize `strategy.type: RollingUpdate` with `maxUnavailable: 0` and `maxSurge: 1`.
   - Replacement pods must pass startup and readiness probes before old pods receive `SIGTERM` signals.

---

## Consequences

* **Positive**:
  - Prevents premature container restart loops during slow JVM boot.
  - Removes unhealthy or unready pods from Service load balancer rotation without unnecessary container restarts.
  - Guarantees zero dropped HTTP requests during rolling updates via graceful shutdown and `maxUnavailable: 0`.
* **Trade-offs / Future Scope**:
  - Live kubelet probe evaluation and pod restart observation require a running Kubernetes cluster.
  - Stateful database and message broker health management belong to dedicated infrastructure operator patterns.
