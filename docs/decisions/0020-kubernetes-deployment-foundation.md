# 20. Kubernetes Deployment Foundation

Date: 2026-08-25

## Status

Accepted

## Context

Following Lesson 20, DevSphere produces versioned, immutable container images in GitHub Container Registry (GHCR). However, operating multi-service microservices in production requires container orchestration — managing container lifecycles, internal service discovery, zero-downtime rolling updates, self-healing restarts, non-root security contexts, and resource bounds.

We require a declarative, production-grade Kubernetes deployment manifest foundation for DevSphere application services (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`) while keeping infrastructure dependencies (MySQL, Redis, Kafka) decoupled.

## Decision

We adopt **Kubernetes** as the production orchestration platform for DevSphere microservices and establish a declarative manifest foundation ([`infrastructure/kubernetes/`](file:///infrastructure/kubernetes)).

Key architectural decisions include:

1. **Dedicated Namespace**: Isolate all DevSphere workloads inside the `devsphere` namespace.
2. **ClusterIP Internal Services**: Deploy internal ClusterIP services using Kubernetes DNS (`<service>.devsphere.svc.cluster.local`) for service-to-service networking.
3. **Decoupled Configuration & Secret Templates**: Manage non-secret environment properties via `ConfigMap` and provide template `secret.example.yaml` files with `CHANGE_ME` placeholders. Real secret files (`secret.yaml`) are ignored by Git.
4. **Hardened Security Context**: Enforce `runAsNonRoot: true` (UID/GID `10001`), `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir` mounts, dropped capabilities (`capabilities: drop: [ALL]`), and `automountServiceAccountToken: false`.
5. **Spring Boot Health Probes**: Configure Actuator liveness (`/actuator/health/liveness`), readiness (`/actuator/health/readiness`), and startup probes.
6. **Zero-Downtime Rolling Updates**: Enforce `RollingUpdate` deployment strategy (`maxUnavailable: 0`, `maxSurge: 1`) and graceful termination (`terminationGracePeriodSeconds: 30`).
7. **Resource Bounds**: Define explicit CPU and memory requests (`cpu: 250m`, `memory: 256Mi`) and limits (`cpu: 1000m`, `memory: 768Mi`).
8. **Kustomize Base Architecture**: Provide `kustomization.yaml` for environment aggregation and image substitution.

## Consequences

### Positive / Benefits
- **Automated Container Orchestration**: Enables automated self-healing restarts, health checking, and zero-downtime rolling updates.
- **Enhanced Container Security**: Non-root, read-only root filesystem, and dropped capabilities reduce runtime attack surface.
- **Predictable Resource Allocation**: Resource requests and limits prevent noisy-neighbor memory exhaustion.
- **Service Isolation & DNS**: Internal ClusterIP services prevent accidental public exposure of microservice endpoints.
- **GitOps Readiness**: Declarative manifests provide the foundation for future ArgoCD/FluxCD automated cluster synchronization.

### Negative / Tradeoffs
- **Manifest Maintenance**: Requires maintaining Kubernetes YAML manifests alongside application code.
- **Operational Complexity**: Kubernetes introduces cluster management and networking overhead.

## Future Work

- **Ingress & TLS**: Implement NGINX Ingress Controller with Cert-Manager for HTTPS perimeter traffic.
- **Autoscaling (HPA)**: Configure Horizontal Pod Autoscalers based on CPU, memory, and HTTP metrics.
- **GitOps & Operators**: Implement ArgoCD for automated cluster state synchronization.
- **Network Policies**: Enforce east-west pod network segmentation policies.
