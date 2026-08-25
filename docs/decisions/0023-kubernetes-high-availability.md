# 23. Kubernetes High Availability, Autoscaling, and Workload Reliability

Date: 2026-08-25

## Status

Accepted

## Context

Lesson 21 established Kubernetes Deployments and ClusterIP Services, Lesson 22 established perimeter ingress and TLS, and Lesson 23 hardened workload security and network policies. However, static pod deployments are vulnerable to sudden traffic spikes, voluntary node maintenance disruptions, and zone-level scheduling imbalances.

To achieve production-grade availability and workload reliability, DevSphere requires horizontal pod autoscaling, disruption budgets, topology-aware scheduling constraints, zero-downtime rolling update strategies, and health probe guardrails.

## Decision

We adopt **Kubernetes-native high availability, autoscaling, and disruption protection mechanisms** for DevSphere workloads while maintaining complete application statelessness.

Key architectural decisions include:

1. **Horizontally Scalable Replicas**: Configure a minimum of 2 replicas for stateless workloads (`api-gateway`, `auth-service`, `user-service`, `config-server`).
2. **Eureka Standalone Replica Decision**: Set `replicas: 1` for `devsphere-service-discovery` (Eureka) because the current standalone configuration does not feature peer replication. Exclude Eureka from HPA and PDB to avoid false high availability claims.
3. **Horizontal Pod Autoscaling (HPA v2)**: Implement `autoscaling/v2` HPAs for `api-gateway`, `auth-service`, and `user-service`:
   - `minReplicas: 2`, `maxReplicas: 10`
   - Target metric: CPU average utilization `70%`
   - Controlled scale-up policies (`15s` stabilization window) and conservative scale-down policies (`300s` stabilization window) to prevent oscillation.
4. **Pod Disruption Budgets (PDB v1)**: Implement `policy/v1` PDBs (`minAvailable: 1`) for multi-replica workloads (`api-gateway`, `auth-service`, `user-service`, `config-server`) to ensure at least 1 pod remains active during voluntary cluster disruptions (e.g., node drain/maintenance).
5. **Topology Spread Constraints**: Add `topologySpreadConstraints` across `topology.kubernetes.io/zone` and `kubernetes.io/hostname` with `maxSkew: 1` and `whenUnsatisfiable: ScheduleAnyway` to ensure balanced pod distribution across failure domains without breaking single-node local development clusters.
6. **Zero-Downtime Rolling Update Strategy**: Retain `strategy: { type: RollingUpdate, rollingUpdate: { maxUnavailable: 0, maxSurge: 1 } }` combined with Spring Boot Actuator readiness probes (`/actuator/health/readiness`), liveness probes (`/actuator/health/liveness`), startup probes, and graceful shutdown (`terminationGracePeriodSeconds: 30`).

## Consequences

### Positive / Benefits
- **Elastic Scale & Traffic Resilience**: Automatically scales application pods up during load spikes and down during low traffic.
- **Disruption Safety**: Guarantees availability during node drains and cluster updates via PDBs.
- **Fault Domain Spreading**: Prevents co-location of all pod replicas on a single failed node or availability zone.
- **Zero-Downtime Rollouts**: `maxUnavailable: 0` ensures existing traffic is served continuously during image upgrades.
- **Accurate Architectural Boundaries**: Evaluates microservices independently, avoiding artificial HA claims for single-node Eureka.

### Negative / Tradeoffs
- **Metrics Server Dependency**: HPA requires `metrics.k8s.io` (Kubernetes Metrics Server) running in the cluster.
- **Resource Footprint**: Multi-replica baseline increases minimum memory/CPU reservation requirements.
- **Kafka Consumer Group Dynamics**: Horizontally scaling `user-service` consumers requires Kafka topic partition count planning to avoid idle consumers.

## Future Work

- **Cluster Autoscaler & KEDA**: Integrate Kubernetes Cluster Autoscaler for node auto-provisioning and KEDA for event-driven autoscaling based on Kafka consumer lag.
- **Custom Application Metrics**: Scale on HTTP request rates or latency via Prometheus Adapter.
- **Multi-Node Eureka Peer Clustering**: Rearchitect Eureka for peer-to-peer registry replication across availability zones.
