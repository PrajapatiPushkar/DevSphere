# Kubernetes High Availability, Autoscaling & Workload Reliability Architecture

## 1. High Availability Architecture Overview
DevSphere implements a Kubernetes-native high availability model for all stateless application workloads. The architecture guarantees zero-downtime rolling upgrades, automatic horizontal pod autoscaling under load, disruption protection during cluster node maintenance, and zone-aware scheduling distribution.

```
                      [ Public HTTPS Ingress ]
                                 │
                                 ▼
                     [ API Gateway Service ]
                       (ClusterIP :8080)
                                 │
            ┌────────────────────┴────────────────────┐
            ▼ (HPA: 2-10 replicas)                   ▼ (PDB: minAvailable 1)
 [ devsphere-api-gateway #1 ]              [ devsphere-api-gateway #2 ]
 (Zone A / Host 1)                         (Zone B / Host 2)
            │                                         │
            └────────────────────┬────────────────────┘
                                 │
                 ┌───────────────┴───────────────┐
                 ▼                               ▼
      [ Auth Service (2-10) ]         [ User Service (2-10) ]
      (Zone A/B Spreading)            (Zone A/B Spreading)
                 │                               │
                 └───────────────┬───────────────┘
                                 ▼
                    [ External Infrastructure ]
                    (MySQL / Redis / Kafka)
```

---

## 2. Workload Replica & Evaluation Matrix

| Microservice | Initial Replicas | HPA Target (`autoscaling/v2`) | PDB Target (`policy/v1`) | Rationale & Architectural Decision |
| :--- | :--- | :--- | :--- | :--- |
| `devsphere-api-gateway` | `2` | `min: 2, max: 10` (CPU 70%) | `minAvailable: 1` | Primary edge routing entry point; fully stateless; horizontally scalable. |
| `devsphere-auth-service` | `2` | `min: 2, max: 10` (CPU 70%) | `minAvailable: 1` | Handles user registration & authentication; stateless; outbox writes to MySQL. |
| `devsphere-user-service` | `2` | `min: 2, max: 10` (CPU 70%) | `minAvailable: 1` | Handles profile reads/writes & Kafka event consumption; stateless. |
| `devsphere-config-server` | `2` | None (Fixed 2 replicas) | `minAvailable: 1` | Configuration retrieval server; stateless; fixed 2 replicas provide startup HA. |
| `devsphere-service-discovery` | `1` | None (Fixed 1 replica) | None | Eureka standalone server without peer replication; single instance prevents invalid HA. |

---

## 3. Horizontal Pod Autoscaler (HPA v2) Specification
HPAs are defined in [`infrastructure/kubernetes/autoscaling/`](file:///infrastructure/kubernetes/autoscaling/) using standard `autoscaling/v2` resources for `api-gateway`, `auth-service`, and `user-service`:

```yaml
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: devsphere-api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

### CPU-Based Scaling Rationale
- Autoscaling is driven by average CPU utilization calculated against container resource requests (`cpu: 250m`).
- CPU utilization serves as a reliable proxy for application workload volume, request execution throughput, and garbage collection pressure in Java 21 Spring Boot runtimes.

---

## 4. HPA Scaling Behavior & Stabilization Rules
To prevent pod thrashing and rapid oscillation during transient traffic spikes:

- **Scale-Up Policy**:
  - `stabilizationWindowSeconds: 15`
  - Max increase rate: `100%` or `4 pods` every 15 seconds (`selectPolicy: Max`).
  - Ensures rapid capacity expansion when load increases.
- **Scale-Down Policy**:
  - `stabilizationWindowSeconds: 300` (5-minute stabilization delay)
  - Max decrease rate: `10%` per 60 seconds (`selectPolicy: Min`).
  - Guarantees conservative, gradual pod reduction after peak traffic subsides.

---

## 5. Pod Disruption Budgets (PDB v1)
PDBs are defined in [`infrastructure/kubernetes/availability/pdb.yaml`](file:///infrastructure/kubernetes/availability/pdb.yaml) using `policy/v1`:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: devsphere-api-gateway-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: devsphere-api-gateway
```
- `minAvailable: 1` guarantees that voluntary cluster operations (e.g. `kubectl drain`, node OS updates, cloud provider node replacements) will never terminate more than 1 replica simultaneously, leaving at least 1 healthy instance active.
- PDBs are strictly compatible with HPAs: when HPA scales replicas from 2 up to 10, `minAvailable: 1` continues to ensure continuous availability.

---

## 6. Topology-Aware Scheduling (Topology Spread Constraints)
To prevent all pod replicas from landing on the same physical worker node or availability zone, all multi-replica Deployments include `topologySpreadConstraints`:

```yaml
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: ScheduleAnyway
    labelSelector:
      matchLabels:
        app.kubernetes.io/name: devsphere-api-gateway
  - maxSkew: 1
    topologyKey: kubernetes.io/hostname
    whenUnsatisfiable: ScheduleAnyway
    labelSelector:
      matchLabels:
        app.kubernetes.io/name: devsphere-api-gateway
```

### Scheduling Strategy
- `maxSkew: 1`: Pod counts across availability zones or worker hosts differ by at most 1 replica.
- `whenUnsatisfiable: ScheduleAnyway`: Soft constraint ensuring pods remain schedulable in single-zone or single-node development environments (Minikube / Kind / Docker Desktop), while automatically spreading across zones in production multi-node clusters.

---

## 7. Rolling Update Strategy & Probe Hierarchy

### Zero-Downtime Rolling Update Strategy
Deployments maintain zero-downtime rolling updates:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```
- `maxUnavailable: 0` ensures Kubernetes never terminates an existing pod until a new surge pod passes its readiness checks.
- `maxSurge: 1` creates 1 additional temporary pod during image updates.

### Health Probe Guardrails
- **Startup Probe** (`/actuator/health/liveness`): Gives Spring Boot applications up to 100 seconds (`initialDelay: 15s`, `period: 5s`, `failureThreshold: 20`) to initialize JVM, load Config Server properties, and establish database connection pools.
- **Readiness Probe** (`/actuator/health/readiness`): Evaluates every 10 seconds. If an application instance becomes unready, Kubernetes immediately removes it from service endpoints to prevent request routing failures.
- **Liveness Probe** (`/actuator/health/liveness`): Evaluates container JVM health. Bounded timeouts (`timeoutSeconds: 3`) ensure transient downstream database latencies do not trigger unnecessary pod restarts.

---

## 8. Graceful Shutdown & Pod Lifecycle
When Kubernetes terminates a pod replica:
1. Pod status changes to `Terminating`.
2. Endpoint controller removes pod IP from Service backends.
3. Kubelet sends `SIGTERM` to the container process.
4. Spring Boot initiates graceful shutdown (`server.shutdown=graceful`), completing active HTTP requests and draining active connections.
5. Container terminates cleanly within `terminationGracePeriodSeconds: 30`.

---

## 9. Metrics Server Dependency & Runtime Enforcement Notice

> [!WARNING]
> HPA requires the Kubernetes **Metrics Server** (`metrics.k8s.io` API) running in the cluster to retrieve CPU usage. In clusters without Metrics Server installed:
> - HPA resources will be created and validated by the API server.
> - HPA status will show `<unknown>` for CPU utilization.
> - Workloads will maintain their base `minReplicas` count (2 replicas).

---

## 10. Service-Specific Architectural Decisions

### Eureka Scaling Decision
`service-discovery` is assigned `replicas: 1` because standalone Netflix Eureka without peer replication cannot perform registry state synchronization between instances. Multiple standalone Eureka instances would maintain divergent instance registries. Rearchitecting Eureka into a peer-replicating cluster is documented as a future architectural enhancement.

### Config Server Scaling Decision
`config-server` is configured with `replicas: 2` and a PDB (`minAvailable: 1`). Because Config Server is stateless and reads configuration from embedded repository files and environment ConfigMaps, 2 replicas provide high availability for startup configuration fetches.

### Kafka Consumer Group Scaling Considerations
`user-service` consumes user registration events from Apache Kafka. When HPA scales `user-service` from 2 up to 10 replicas:
- Kafka automatically rebalances partitions within the consumer group (`devsphere-user-service`).
- Maximum effective parallel consumers are bounded by the partition count of topic `devsphere.users.user-registered`. If the topic has 3 partitions, only 3 replicas will actively consume events, while additional replicas remain idle standby consumers.

---

## 11. Manifest Validation & Inspection Commands

Validate high availability manifests:

```bash
# Synthesize and inspect Kustomize output
kubectl kustomize infrastructure/kubernetes/

# Inspect deployed HPAs and target metrics
kubectl get hpa -n devsphere

# Inspect deployed PodDisruptionBudgets
kubectl get pdb -n devsphere

# Check real-time pod resource usage (requires Metrics Server)
kubectl top pods -n devsphere
```

---

## 12. Future Reliability & Autoscaling Roadmap
- **KEDA (Kubernetes Event-driven Autoscaling)**: Scale `user-service` consumers dynamically based on Kafka topic consumer lag rather than CPU utilization.
- **Prometheus Adapter for Custom Metrics**: Scale `api-gateway` based on real-time HTTP request throughput (RPS) and latency percentiles.
- **Cluster Autoscaler**: Integrate cloud provider Cluster Autoscaler to add worker nodes dynamically when HPA pod pending events occur.
