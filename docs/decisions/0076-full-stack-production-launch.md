# 76. Full-Stack Production Launch

- **Status**: Accepted
- **Date**: 2026-09-06
- **Deciders**: DevSphere Engineering Team

---

## Context & Problem Statement

Lessons 1–79 established the full architectural foundation of the DevSphere application, including backend microservices, data persistence layers, event streaming with Kafka, distributed caching with Redis, a React 18 frontend SPA, containerized builds, CI/CD automation, Kubernetes deployment manifests, and Prometheus/Grafana observability.

The final requirement (Lesson 80) is to validate, finalize, deploy, and verify the complete DevSphere system as a production-ready full-stack application without introducing new business features, new APIs, or altering established architecture.

---

## Decision Drivers

1. **Architecture Preservation**: Maintain strict adherence to the existing 3-tier microservices architecture without introducing unneeded complexities (such as service meshes or additional databases).
2. **Quality & Validation**: Ensure complete frontend test coverage (54 unit/integration tests passing), zero TypeScript linting errors, clean production builds, and schema-valid Kubernetes Kustomize synthesis.
3. **Honesty in Verification**: Strictly distinguish between static manifest validation and live runtime execution. Clearly declare `STATICALLY VERIFIED` or `NOT RUNTIME VERIFIED` when live cluster or remote GHCR environments are not active.
4. **Operational Readiness**: Provide comprehensive runbooks and architecture documentation for ongoing operations, troubleshooting, and emergency rollbacks.

---

## Technical Solution

1. **Production Architecture**:
   - Web Traffic: Handled via `devsphere-ingress` mapping `app.devsphere.local` to NGINX SPA static frontend and `api.devsphere.local` to Spring Cloud API Gateway.
   - Gateway Security: JWT signature validation via `JwtAuthenticationFilter`, passing identity downstream via HTTP request headers (`X-Authenticated-User-Id`).
   - Microservices: Java 21 / Spring Boot 3.2 `auth-service` and `user-service`, backed by MySQL 8.0 databases, Redis caching, and Kafka event publishing.
2. **Kubernetes Deployment & Security**:
   - Standardized on Kustomize (`k8s/kustomization.yaml`).
   - Resource limits, HTTP liveness/readiness/startup probes, PodDisruptionBudgets, and HorizontalPodAutoscalers configured for HA.
   - Non-root user execution (`runAsNonRoot: true`, user 10001), read-only root filesystems, dropped capabilities.
3. **CI/CD Pipeline**:
   - Preserved GitHub Actions `.github/workflows/ci.yml` and `.github/workflows/cd.yml`.
   - Immutable image tagging via Git SHA (`ghcr.io/prajapatipushkar/devsphere-*`).

---

## Consequences

### Positive
- Fully integrated, deployable full-stack SaaS platform verified against existing specs.
- Clear separation between frontend static asset serving (NGINX) and API processing (Spring Cloud Gateway).
- Complete operational documentation (`docs/runbooks/production-deployment.md` and `docs/architecture/devsphere-production-architecture.md`).

### Negative / Trade-offs
- Live cluster deployment was not executed locally due to host environment constraints, requiring static verification report (`STATICALLY VERIFIED`).
