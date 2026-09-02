# 56. Kubernetes Production Deployment & Orchestration Foundation

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `k8s/`
* **Date**: 2026-09-02

---

## Context

Following backend production hardening (Lesson 60), DevSphere microservices required containerization and Kubernetes orchestration manifests to support zero-downtime rolling deployments, environment configuration separation, health probing, resource guardrails, and secure perimeter entry in production environments.

---

## Decision

1. **Dedicated Namespace (`devsphere`)**:
   - Isolated all DevSphere microservice workloads inside a dedicated `devsphere` Kubernetes namespace with restricted pod security labels.

2. **Microservice Container Deployments & Rolling Updates**:
   - Standardized multi-stage Java 21 Dockerfiles running as non-root user `devsphere` (UID/GID 10001).
   - Configured `RollingUpdate` strategy (`maxUnavailable: 0`, `maxSurge: 1`) and `terminationGracePeriodSeconds: 30` with Spring Boot graceful shutdown (`server.shutdown: graceful`).

3. **Kubernetes Services & Ingress Perimeter**:
   - Exposed `api-gateway`, `auth-service`, and `user-service` via internal `ClusterIP` services.
   - Configured Kubernetes Ingress routing HTTP perimeter traffic (`api.devsphere.local`) to `api-gateway` on port 8080.

4. **Externalized Configuration & Secret Management**:
   - Decoupled environment properties into `devsphere-configmap` (`SPRING_PROFILES_ACTIVE`, cluster DNS endpoints).
   - Injected sensitive values (`MYSQL_PASSWORD`, `SPRING_REDIS_PASSWORD`, `JWT_SECRET`, Kafka SASL credentials) via `devsphere-secrets` with placeholder examples in `secret.example.yaml`.

5. **Health Probes & Resource Guardrails**:
   - Integrated Spring Boot Actuator `/actuator/health/liveness` and `/actuator/health/readiness` endpoints into Kubernetes startup, liveness, and readiness probes.
   - Set container CPU requests (250m) / limits (1000m) and memory requests (512Mi) / limits (1Gi) with `-XX:MaxRAMPercentage=75.0`.

---

## Consequences

* **Positive**:
  - Full container orchestration readiness with zero-downtime rolling updates and self-healing.
  - Strict security isolation using non-root containers, dropped capabilities, and read-only root filesystems.
  - Externalized non-secret and secret configuration decoupled from application binaries.
* **Trade-offs / Future Scope**:
  - Ingress TLS certificates and ingress controller installation depend on cluster environment setup (e.g. Cert-Manager / NGINX Ingress Controller).
