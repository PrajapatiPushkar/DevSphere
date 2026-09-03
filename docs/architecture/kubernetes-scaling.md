# Kubernetes Scaling Architecture — Lesson 65

## 1. Overview & Strategy

This document details the production-oriented Kubernetes scaling strategy for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`, MySQL, Redis, Kafka, Zookeeper).

The objective is to establish a resilient, highly available scaling baseline while maintaining strict architectural boundaries between stateless application microservices and stateful infrastructure services.

### Conceptual Scaling Diagram

```text
                 Incoming Traffic
                       │
                       ▼
                 Ingress / Gateway
                       │
                       ▼
              API Gateway Pods
              (Replicas + HPA)
                /          \
               /            \
              ▼              ▼
       Auth Service      User Service
     (Replicas + HPA)  (Replicas + HPA)
              │              │
              └──────┬───────┘
                     ▼
              Shared Infrastructure
           MySQL / Redis / Kafka / Eureka
               (Single-Instance State)
```

---

## 2. Replica Strategy

DevSphere establishes clear replica baselines based on workload statelessness and availability requirements:

| Service | Type | Baseline Replicas | Auto-Scaling (HPA) | Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | Stateless | `2` | Enabled (`2`–`10`) | Entry point for incoming HTTP requests. High concurrency demand; requires horizontal redundancy. |
| **Auth Service** | Stateless | `2` | Enabled (`2`–`10`) | Handles user login, authentication, and JWT validation. Critical for availability. |
| **User Service** | Stateless | `2` | Enabled (`2`–`10`) | Handles user profile, resume operations, and domain API requests. Main business workload. |
| **Config Server** | Stateless/Shared | `1` | Disabled | Centralized configuration provider. Single instance baseline for local/dev environment footprint. |
| **Service Discovery** | Infra/Registry | `1` | Disabled | Eureka registry instance. Stateful in-memory peer discovery; preserving single instance avoids complex multi-peer Eureka clustering overhead. |
| **MySQL** | Stateful Infra | `1` | Disabled | Relational database. State must be preserved on PersistentVolumeClaim; horizontal pod scaling prohibited. |
| **Redis** | Stateful Infra | `1` | Disabled | In-memory cache & rate limiter storage. State requires specialized Redis Sentinel/Cluster for multi-node. |
| **Kafka & Zookeeper** | Stateful Infra | `1` | Disabled | Event streaming broker. Requires topic partition management and cluster orchestration. |

---

## 3. Resource Requests and Limits

Every Kubernetes container workload in DevSphere specifies explicit CPU and memory resource requests and limits.

### Application Workload Baseline Table

| Service / Container | CPU Request | CPU Limit | Memory Request | Memory Limit | JVM Heap Setting |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **api-gateway** | `250m` | `1000m` | `512Mi` | `1Gi` | `-XX:MaxRAMPercentage=75.0` |
| **auth-service** | `250m` | `1000m` | `512Mi` | `1Gi` | `-XX:MaxRAMPercentage=75.0` |
| **user-service** | `250m` | `1000m` | `512Mi` | `1Gi` | `-XX:MaxRAMPercentage=75.0` |
| **config-server** | `250m` | `1000m` | `512Mi` | `1Gi` | `-XX:MaxRAMPercentage=75.0` |
| **service-discovery** | `250m` | `1000m` | `512Mi` | `1Gi` | `-XX:MaxRAMPercentage=75.0` |
| **devsphere-mysql** | `250m` | `1000m` | `512Mi` | `1Gi` | N/A |
| **devsphere-redis** | `100m` | `500m` | `128Mi` | `512Mi` | N/A |
| **devsphere-zookeeper**| `100m` | `500m` | `256Mi` | `512Mi` | N/A |
| **devsphere-kafka** | `250m` | `1000m` | `512Mi` | `1Gi` | N/A |

### Rationale & Rules
1. **CPU Requests**: Guaranteed CPU allocation used by Kubernetes scheduler and HPA CPU utilization calculations (`CPU % = Actual CPU / Requested CPU`).
2. **Memory Requests**: Guaranteed memory allocation ensuring scheduler places pods on nodes with adequate allocatable memory.
3. **CPU Limits**: Upper ceiling preventing single container resource starvation without triggering out-of-memory kills (CPU throttling occurs when limit is reached).
4. **Memory Limits**: Upper memory boundary protecting node stability. Standard Spring Boot deployments set `-XX:MaxRAMPercentage=75.0` to leave ~25% heap headroom for Metaspace, thread stacks, and native Netty buffers.

---

## 4. Horizontal Pod Autoscaler (HPA)

Autoscaling is implemented using the Kubernetes `autoscaling/v2` API for stateless application workloads (`api-gateway`, `auth-service`, `user-service`).

### HPA Configuration Blueprint (`k8s/autoscaling/hpa.yaml`)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: devsphere
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

## 5. HPA Scaling Policy & Behavior

To prevent rapid pod creation/destruction cycle (flapping) during transient traffic bursts, explicit `behavior` rules are applied to each HPA resource:

```yaml
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      selectPolicy: Max
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 4
          periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      selectPolicy: Min
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
```

### Key Scaling Parameters
- **Scale-Up**: Triggers immediately (`stabilizationWindowSeconds: 0`) when average CPU exceeds 70% or average Memory exceeds 80%. Allows doubling pod count or adding up to 4 pods every 15 seconds.
- **Scale-Down**: Enforces a `300` second (5-minute) stabilization window. Gradual scale-down reduces active pods by a maximum of 10% per minute, giving existing traffic time to settle without premature pod termination.

---

## 6. Stateful Infrastructure Boundaries

Stateful infrastructure resources (**MySQL**, **Redis**, **Kafka**, **Zookeeper**) are explicitly **excluded** from Horizontal Pod Autoscaling.

### Why Stateful Infrastructure is Not Autoscaled with HPA
1. **MySQL**: Adding database pods blindly via Deployment replicas creates un-synchronized MySQL instances without data replication or shared write-ahead logs.
2. **Redis**: In-memory data structures, cache keys, and rate-limiting counters require Redis Cluster or Sentinel topology for distributed operation.
3. **Kafka & Zookeeper**: Kafka topic partitions require stateful node IDs, disk persistence alignment, and partition rebalancing via Kafka controllers.
4. **Data Integrity Boundary**: Application microservices store state externally in these infrastructure backends, enabling stateless app pods to scale horizontally without risk of data loss or inconsistency.

---

## 7. Metrics Server & Cluster Requirements

HPA relies on live resource metrics collected by the Kubernetes **Metrics Server** (`metrics.k8s.io` API).

### Operational Formula
$$\text{Target Replica Count} = \left\lceil \text{Current Replicas} \times \left( \frac{\text{Current Metric Value}}{\text{Target Metric Value}} \right) \right\rceil$$

### Cluster Prerequisite
In local or bare-metal Kubernetes environments (e.g. Minikube, Kind, k3s), Metrics Server must be enabled:
```bash
minikube addons enable metrics-server
```
Without Metrics Server installed, `kubectl get hpa` will report `<unknown>` for CPU/Memory utilization, and replica counts remain at `minReplicas`.

---

## 8. High Availability & Traffic Routing

Multiple replicas receive load-balanced traffic through Kubernetes internal `ClusterIP` Services using pod label selectors (`app.kubernetes.io/name`).

### High Availability Mechanics
- **RollingUpdate Strategy**: `maxUnavailable: 0` and `maxSurge: 1` ensure zero downtime during rollouts.
- **Topology Spread Constraints**: `topologySpreadConstraints` schedule replicas across different availability zones and nodes (`topologyKey: topology.kubernetes.io/zone` and `kubernetes.io/hostname`) to prevent single-host failures.
- **Health Probes**: `startupProbe`, `livenessProbe`, and `readinessProbe` ensure unready pods do not receive traffic until fully booted.

---

## 9. Baseline vs. Production Tuning

The resource requests/limits and HPA targets defined in DevSphere represent a **production-oriented baseline**. In actual production deployments, these values must be continuously tuned based on:
1. Observed P95/P99 response latencies.
2. Production load testing results (e.g., k6, Locust, JMeter).
3. Heap usage trends and Garbage Collection pause metrics.
