# 63. Kubernetes Scaling Baseline and Horizontal Pod Autoscaler Strategy

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `k8s/`
* **Date**: 2026-09-03

---

## Context

Following configuration externalization and secret management (Lesson 64), DevSphere required a standardized, production-oriented scaling baseline for all microservice application deployments and infrastructure workloads in Kubernetes. The system needed clear boundaries between stateless microservices (horizontally scalable via HPA) and stateful infrastructure services (fixed instances), as well as defined resource requests, limits, and scaling stabilization policies.

---

## Decision

1. **Stateless Service Scaling Baseline**:
   - Microservices handling user traffic (`api-gateway`, `auth-service`, `user-service`) are configured with a baseline count of 2 replicas each to guarantee high availability and horizontal load distribution.
   - Centralized services (`config-server` and `service-discovery`) remain at 1 baseline replica for single-cluster/dev footprints.

2. **Resource Requests and Limits**:
   - All 5 Spring Boot application containers receive explicit resource allocations: CPU request `250m`, CPU limit `1000m`, Memory request `512Mi`, Memory limit `1Gi`.
   - JVM heap memory is bounded via `-XX:MaxRAMPercentage=75.0` to reserve 25% non-heap headroom for JVM Metaspace, threads, and Netty memory buffers.
   - Resource allocations guarantee scheduler placement accuracy and provide baseline targets for HPA metric evaluation.

3. **Horizontal Pod Autoscaler (`autoscaling/v2`)**:
   - Stateless microservices (`api-gateway`, `auth-service`, `user-service`) are bound to HPA resources with `minReplicas: 2` and `maxReplicas: 10`.
   - Dual resource metrics are enforced: CPU target utilization at 70% and Memory target utilization at 80%.

4. **HPA Behavior and Stabilization**:
   - Immediate scale-up policy (`stabilizationWindowSeconds: 0`, expanding up to 100% or 4 pods per 15s) ensures rapid responsiveness to incoming traffic spikes.
   - Conservative scale-down policy (`stabilizationWindowSeconds: 300`, reducing by at most 10% per 60s) prevents pod flapping and premature termination during temporary lull periods.

5. **Stateful Infrastructure Scaling Boundary**:
   - MySQL, Redis, Kafka, and Zookeeper are explicitly excluded from HPA.
   - Stateful infrastructure scaling requires specialized database clustering, storage persistence, and topic partition replication, preserving externalized state separation.

6. **Cluster Requirements**:
   - Metrics Server (`metrics.k8s.io`) is documented as a mandatory cluster dependency for evaluating CPU/memory metrics in active environments.

---

## Consequences

* **Positive**:
  - Establishes a predictable, production-oriented baseline for horizontal microservice scaling.
  - Prevents resource starvation and pod flapping through explicit requests/limits and scale-down stabilization windows.
  - Preserves data safety by locking stateful infrastructure scaling outside standard Deployment HPAs.
* **Trade-offs / Future Scope**:
  - Live autoscaling requires a running Kubernetes cluster with Metrics Server enabled.
  - Database and message broker scaling (e.g. MySQL InnoDB Cluster, Redis Sentinel, Kafka Partitioning) belong to dedicated infrastructure orchestration topics.
