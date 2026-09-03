# 65. Kubernetes Production Deployment Strategy

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `k8s/`
* **Date**: 2026-09-03

---

## Context

Following Kubernetes health & self-healing implementation (Lesson 66), DevSphere required a production-oriented deployment hardening strategy covering zero-downtime rolling updates, availability protection during voluntary cluster disruptions, resource management alignment with HPA/scheduling, and standardized deployment rollback procedures across all stateless microservices.

---

## Decision

1. **Rolling Update & Capacity Preservation**:
   - Stateless microservices (`api-gateway`, `auth-service`, `user-service`) and control services (`config-server`, `service-discovery`) strictly enforce `strategy.type: RollingUpdate` with `maxUnavailable: 0` and `maxSurge: 1`.
   - `maxUnavailable: 0` guarantees zero capacity reduction during deployment rollouts.
   - `maxSurge: 1` creates replacement pods before terminating old instances.

2. **PodDisruptionBudget (PDB) Strategy (`policy/v1`)**:
   - PDB manifests are introduced under `k8s/disruption/pdb.yaml` for stateless microservices (`api-gateway-pdb`, `auth-service-pdb`, `user-service-pdb`) setting `minAvailable: 1`.
   - PDBs protect replica availability during voluntary disruptions (e.g. node draining, cluster maintenance).
   - Single-instance stateful infrastructure (MySQL, Redis, Kafka, Zookeeper) and single-instance control services are excluded from PDBs to avoid deadlocks during cluster drains.

3. **Zero-Downtime Deployment Lifecycle Integration**:
   - New replacement pods undergo `startupProbe` (JVM boot) and `readinessProbe` checks before receiving traffic from `ClusterIP` Services.
   - Old pods receive `SIGTERM` and perform Spring Boot graceful HTTP server shutdown (`server.shutdown: graceful`, 20-second drain window) within `terminationGracePeriodSeconds: 30`.

4. **Resource Management Alignment**:
   - Maintained standardized resource allocations (`cpu: 250m` / `memory: 512Mi` requests; `cpu: 1000m` / `memory: 1Gi` limits) and JVM heap bounds (`-XX:MaxRAMPercentage=75.0`).
   - Resource requests guarantee accurate kube-scheduler node placement and provide the mathematical baseline for HPA metric evaluations.

5. **Deployment Rollback Protocol**:
   - Standardized `kubectl rollout undo` procedures using immutable Kubernetes deployment revisions.
   - Halts automatically if replacement pods fail health probes, leaving active healthy pods untouched.

---

## Consequences

* **Positive**:
  - Eliminates deployment downtime and capacity drops during container image updates.
  - Protects microservice availability against voluntary node drains and cluster upgrades via PDBs.
  - Standardizes resource management across scheduling, HPA autoscaling, and container memory limits.
* **Trade-offs / Future Scope**:
  - Live rollout/rollback execution and node draining require an active multi-node Kubernetes cluster.
  - Advanced progressive delivery (Blue/Green, Canary, Istio service mesh routing) belongs to future deployment topics.
