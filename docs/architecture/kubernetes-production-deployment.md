# Kubernetes Production Deployment Architecture — Lesson 67

## 1. Overview & Production Deployment Strategy

This document details the production-grade Kubernetes deployment architecture for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`, MySQL, Redis, Kafka, Zookeeper).

Lesson 67 strengthens production deployment resiliency across four core pillars:
1. **Rolling Updates**: Zero-capacity-reduction Deployment strategies (`maxUnavailable: 0`, `maxSurge: 1`).
2. **Zero-Downtime Deployment Lifecycle**: Seamless pod replacement using startup probes, readiness probes, and graceful HTTP server shutdown.
3. **Pod Disruption Budgets (PDB)**: Guaranteeing replica availability during voluntary disruptions (cluster maintenance, node draining).
4. **Resource Management & Scheduling**: Standardized CPU/Memory requests and limits aligned with HPA metrics and scheduler placement.

---

## 2. Production Deployment Lifecycle Diagram

```text
                  Developer / CI Automation
                             │
                             ▼
              Container Image Tag Update (:v2.0.0)
                             │
                             ▼
            kubectl apply / Deployment Triggered
                             │
                             ▼
                 ┌──────────────────────┐
                 │  RollingUpdate Spec  │
                 │  maxUnavailable: 0   │
                 │  maxSurge: 1         │
                 └───────────┬──────────┘
                             │
                             ▼
                   Create New Pod (Surge)
                             │
                             ▼
                   ┌──────────────────┐
                   │  Startup Probe   │
                   └─────────┬────────┘
                             │
                  JVM Initialized (UP)
                             │
                             ▼
                   ┌──────────────────┐
                   │ Readiness Probe  │
                   └─────────┬────────┘
                             │
                   App Ready for Traffic
                             │
                             ▼
             Add New Pod to ClusterIP Endpoint
                             │
                             ▼
           Send SIGTERM to Old Pod (Termination)
                             │
                             ▼
            Spring Boot Graceful Shutdown (20s)
            (Drain in-flight HTTP requests)
                             │
                             ▼
             Old Pod Exits Cleanly & Removed
```

---

## 3. Rolling Updates & Zero-Downtime Deployment

### Rolling Update Parameters (`k8s/services/*.yaml`)
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

### Key Parameters Explained
* **`maxUnavailable: 0`**: Ensures that 100% of the desired pod replica count remains running and healthy throughout the rollout. Application capacity is never artificially reduced during updates.
* **`maxSurge: 1`**: Instructs Kubernetes to spawn exactly 1 new replacement pod at a time before attempting to terminate any existing pods.

### Traffic Protection Mechanism
1. **Startup Protection**: The new replacement pod must pass its `startupProbe` (`/actuator/health/liveness`, providing up to 100s for JVM boot).
2. **Readiness Gate**: The new pod must pass its `readinessProbe` (`/actuator/health/readiness`). Only after returning `200 OK` is the pod IP registered in the Service endpoint slice.
3. **Graceful Drain**: The old pod receives `SIGTERM`. Spring Boot's graceful shutdown (`server.shutdown: graceful`, `timeout-per-shutdown-phase: 20s`) stops accepting new HTTP connections while draining active in-flight requests before exiting cleanly within the `terminationGracePeriodSeconds: 30` window.

---

## 4. Pod Disruption Budget (PDB) Strategy

To prevent voluntary cluster maintenance operations from compromising service availability, PodDisruptionBudgets (`policy/v1`) are defined under `k8s/disruption/pdb.yaml`:

| Target Service | PDB Resource Name | `minAvailable` | Selector Label | Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `api-gateway-pdb` | `1` | `app.kubernetes.io/name: api-gateway` | Guarantees at least 1 gateway pod remains active during node drains. |
| **Auth Service** | `auth-service-pdb` | `1` | `app.kubernetes.io/name: auth-service` | Prevents authentication downtime during cluster maintenance. |
| **User Service** | `user-service-pdb` | `1` | `app.kubernetes.io/name: user-service` | Protects core user domain availability during host upgrades. |

### Manifest Blueprint (`k8s/disruption/pdb.yaml`)
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: api-gateway-pdb
  namespace: devsphere
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: api-gateway
```

### Voluntary vs. Involuntary Disruption Boundary

```text
                             Disruption Types
                                    │
               ┌────────────────────┴────────────────────┐
               ▼                                         ▼
      Voluntary Disruptions                    Involuntary Disruptions
   (Protected by PDBs)                      (NOT Protected by PDBs)
               │                                         │
   • Node draining (kubectl drain)           • Hardware failure
   • Cluster upgrades                        • Node kernel panic / power loss
   • Node auto-scaling scale-down            • Out-Of-Memory (OOM) kills
   • Voluntary pod evictions                 • Application process crashes
```

> [!IMPORTANT]
> **PDB Boundary Note**: PodDisruptionBudgets govern *voluntary evictions* initiated via the Kubernetes Eviction API. They cannot prevent pod termination caused by hardware failures, node kernel panics, or OOM kills.

---

## 5. Resource Management & HPA Interaction

Every production deployment specifies explicit CPU and Memory resource requests and limits:

```yaml
resources:
  requests:
    cpu: "250m"
    memory: "512Mi"
  limits:
    cpu: "1000m"
    memory: "1Gi"
```

### Scheduler & HPA Relationship
1. **Resource Requests**: Represent guaranteed allocations. Kube-scheduler uses CPU and Memory requests to select nodes with sufficient allocatable capacity.
2. **HPA Calculations**: Horizontal Pod Autoscalers calculate resource utilization against requests:
   $$\text{Utilization \%} = \frac{\text{Current Pod Resource Usage}}{\text{Requested Resource Value}} \times 100$$
3. **Resource Limits**: Upper boundary enforced by cgroups. Reaching CPU limits results in CPU throttling (slowing thread execution). Exceeding Memory limits triggers Linux kernel OOM killer (`SIGKILL`).
4. **JVM Memory Alignment**: All Spring Boot containers set `-XX:MaxRAMPercentage=75.0`, ensuring JVM heap occupies at most 75% of the `1Gi` container memory limit, leaving ~256MB non-heap headroom for Metaspace, thread stacks, and native Netty buffers.

---

## 6. Deployment Rollback Strategy

In the event of a faulty deployment rollout (e.g. failing readiness probe or crashing application version), Kubernetes maintains an immutable deployment revision history for instant rollback:

### Rollback Commands Reference

```bash
# 1. Check status of an ongoing rollout
kubectl rollout status deployment/api-gateway -n devsphere

# 2. View revision history
kubectl rollout history deployment/api-gateway -n devsphere

# 3. Undo deployment and revert to previous revision
kubectl rollout undo deployment/api-gateway -n devsphere

# 4. Undo deployment and revert to a specific historical revision
kubectl rollout undo deployment/api-gateway --to-revision=2 -n devsphere
```

### Rollback Safety Mechanics
Because `maxUnavailable: 0` is enforced, if a new deployment revision fails its `startupProbe` or `readinessProbe`, the rollout halts automatically. The old healthy pods remain running, and issuing `kubectl rollout undo` cleanly cancels the pending surge pods without causing service downtime.

---

## 7. Stateful vs. Stateless Workload Boundaries

| Workload Type | Services | Scaling Strategy | PDB Strategy | Rolling Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **Stateless Microservices** | `api-gateway`, `auth-service`, `user-service` | Horizontal (HPA `2`–`10`) | `minAvailable: 1` | `RollingUpdate` (`maxUnavailable: 0`) |
| **Control Plane / Registry** | `config-server`, `service-discovery` | Fixed `1` replica | Excluded from PDB | `RollingUpdate` (`maxUnavailable: 0`) |
| **Stateful Infrastructure** | MySQL, Redis, Kafka, Zookeeper | Fixed `1` replica (PVC) | Excluded from PDB | Recreate / Fixed Single Instance |

---

## 8. Validation Commands

Synthesize all production manifests including PDBs using `kubectl`:

```bash
kubectl kustomize k8s/
```
