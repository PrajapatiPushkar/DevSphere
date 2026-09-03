# Kubernetes Health & Self-Healing Architecture — Lesson 66

## 1. Overview & Conceptual Architecture

This document details the health monitoring, self-healing, graceful shutdown, and rolling deployment strategy for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`, MySQL, Redis, Kafka, Zookeeper).

Kubernetes health management separates container lifecycle decisions into three distinct probe responsibilities:

```text
               Container Created / Started
                           │
                           ▼
                  ┌─────────────────┐
                  │  Startup Probe  │
                  └────────┬────────┘
                           │
                Finished Startup? (UP)
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
    ┌─────────────────┐         ┌─────────────────┐
    │ Liveness Probe  │         │ Readiness Probe │
    └────────┬────────┘         └────────┬────────┘
             │                           │
         Healthy?                    Ready?
         ├── YES → OK                ├── YES → Receive Traffic
         └── NO  → Restart           └── NO  → Remove from Service Endpoints
```

---

## 2. Health Endpoint Architecture

DevSphere leverages Spring Boot Actuator's native Kubernetes probe support (`management.health.probes.enabled: true`):

| Endpoint | Managed Group / Probe | Purpose | Exposed Path |
| :--- | :--- | :--- | :--- |
| **Liveness** | `LivenessState` | Verifies container process is alive and not deadlocked. | `/actuator/health/liveness` |
| **Readiness** | `ReadinessState` | Verifies container is initialized and ready to accept HTTP traffic. | `/actuator/health/readiness` |

### Configuration (`config-repo/application.yml` & `config-server`)
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
    probes:
      enabled: true
```

---

## 3. Probe Specifications & Parameters

### Application Workloads (`api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`)

| Probe Type | Path | Port | `initialDelaySeconds` | `periodSeconds` | `timeoutSeconds` | `failureThreshold` | Max Delay Window |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Startup** | `/actuator/health/liveness` | Service Port | `15` | `5` | `1` | `20` | `100s` |
| **Liveness** | `/actuator/health/liveness` | Service Port | `5` | `10` | `3` | `3` | N/A |
| **Readiness** | `/actuator/health/readiness` | Service Port | `10` | `10` | `3` | `3` | N/A |

### Probe Functionality Breakdown

#### 1. Startup Probe
- **Goal**: Gives slow-starting Spring Boot applications (JVM boot, Hibernate schema validation, Kafka consumer group join) sufficient time to start.
- **Protection**: While `startupProbe` is evaluating, `livenessProbe` and `readinessProbe` are temporarily disabled by Kubernetes. This prevents premature container restarts during JVM boot.

#### 2. Liveness Probe
- **Goal**: Determines if the container needs to be restarted by kubelet.
- **Behavior**: Returns `200 OK` when `LivenessState` is `UP`. If liveness fails 3 consecutive times (`30s`), kubelet terminates and restarts the container (`restartPolicy: Always`).
- **Resilience Boundary**: Temporary database/broker connection drops do **NOT** fail liveness (preventing cascading container restart loops).

#### 3. Readiness Probe
- **Goal**: Determines if the pod should receive traffic from Kubernetes `ClusterIP` Services.
- **Behavior**: Returns `200 OK` when `ReadinessState` is `UP`. If readiness fails 3 consecutive times, kubelet removes the pod IP address from the Service endpoint slice.
- **Recovery**: Once readiness returns `200 OK`, the pod IP is immediately re-added to Service endpoints without restarting the container.

---

## 4. Kubernetes Restart & Self-Healing Behavior

```text
                             Failure Event
                                   │
                 ┌─────────────────┴─────────────────┐
                 ▼                                   ▼
          Liveness Failure                   Readiness Failure
                 │                                   │
      Kubelet restarts container           Kubelet removes pod IP
      (restartPolicy: Always)              from ClusterIP Service
                 │                                   │
    Process killed & re-created            No traffic sent to pod;
                                           pod stays running
```

* **Liveness Failure**: Container process killed and restarted automatically.
* **Readiness Failure**: Pod taken out of load balancer rotation without process restart.
* **Startup Failure**: If startup probe fails 20 consecutive checks (`100s`), container is killed and restarted.

---

## 5. Graceful Pod Termination

Rolling deployments and pod termination require zero dropped HTTP requests.

### Graceful Termination Sequence
1. **Deployment Update / Scale-Down**: Kubelet sets pod status to `Terminating`.
2. **Endpoint Removal**: EndpointController asynchronously removes the pod IP from Kubernetes Service endpoints.
3. **`SIGTERM` Signal**: Kubelet sends `SIGTERM` to the container process.
4. **Spring Boot Graceful Shutdown**:
   - Web server (Tomcat/Netty) stops accepting new connections (`server.shutdown: graceful`).
   - Active in-flight requests are allowed up to `20s` (`spring.lifecycle.timeout-per-shutdown-phase: 20s`) to complete.
5. **Clean Exit**: Container process exits cleanly (code `0`). If process exceeds `terminationGracePeriodSeconds` (`30s`), Kubelet sends `SIGKILL`.

---

## 6. Rolling Deployment Strategy

All stateless application microservices (`api-gateway`, `auth-service`, `user-service`) enforce zero-downtime rolling updates:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

### Zero-Downtime Rollout Workflow
```text
Step 1: Start Pod N+1 (Surge 1)
Step 2: Startup Probe succeeds (JVM initialized)
Step 3: Readiness Probe succeeds (Added to ClusterIP Service)
Step 4: Send SIGTERM to Pod 1 (Old Pod)
Step 5: Pod 1 performs Graceful Shutdown (In-flight requests drain in 20s)
Step 6: Pod 1 exits; repeat for remaining pods until rollout completes
```

* **`maxUnavailable: 0`**: Ensures 100% of desired replica capacity remains available throughout rollout.
* **`maxSurge: 1`**: Adds 1 replacement pod at a time.

---

## 7. Validation Commands

Synthesize and validate Kubernetes manifests using `kubectl`:

```bash
kubectl kustomize k8s/
```
