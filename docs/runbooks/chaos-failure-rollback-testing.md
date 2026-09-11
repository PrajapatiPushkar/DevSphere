# Chaos, Controlled Failure & Automated Rollback Testing Runbook (Lesson 90)

This runbook documents the controlled chaos engineering and deployment rollback verification conducted for the **DevSphere** microservices platform running on a local, resource-constrained Kind cluster.

> [!IMPORTANT]
> **Environment & Scope Boundary**:
> - **Local Kind Cluster**: All procedures, tests, and metrics in this document were executed exclusively on a local Kind cluster (`kind-devsphere`, namespace `devsphere`) on a developer workstation.
> - **Lesson Scope**: This runbook addresses **Lesson 90 only**. Final production verification and acceptance testing (Lesson 91) is explicitly out of scope.
> - **Destruction Prohibition**: No stateful services (MySQL, Kafka, Zookeeper, Redis), persistent volumes (PVCs), or physical host resources were disturbed.

---

## 1. Objective

The primary objective of Lesson 90 is to validate that the DevSphere platform safely handles controlled runtime failures and that native Kubernetes deployment self-healing and rollback mechanisms operate correctly:

```
Normal Healthy State
        │
        ▼
Controlled Failure Injected
        │
        ▼
Kubernetes Detects Failure
        │
        ▼
Recovery / Native Rollback Executed
        │
        ▼
Healthy State Restored
        │
        ▼
Application Functional Verification
```

---

## 2. Environment Specifications

- **Kubernetes Cluster**: Kind (`kindest/node:v1.37.0`)
- **Cluster Name**: `devsphere`
- **Kubernetes Context**: `kind-devsphere`
- **Active Namespace**: `devsphere`
- **Host Constraints**: Windows 11 / WSL2 development environment with bounded RAM (~7.7 GB total physical host RAM, ~3.67 GiB Docker limit).
- **Chaos Frameworks**: Zero external heavy frameworks installed (no Chaos Mesh, no Litmus, no Gremlin). Native Kubernetes primitives (`apps/v1` ReplicaSet controller, `kubectl rollout undo`, rolling update strategy with bounded timeouts) were utilized to maintain cluster stability and resource safety.

---

## 3. Baseline Cluster & Workload Health

Prior to executing failure tests, cluster and workload health was established and audited:

### 3.1 Node & Cluster Resource Baseline
- **Node**: `devsphere-control-plane` (Status: `Ready`, Version: `v1.37.0`)
- **PVC**: `mysql-pv-claim` (`Bound`, 1Gi, StorageClass: `standard`)
- **Ingress**: `devsphere-ingress` (`app.devsphere.local`, `api.devsphere.local`)
- **Ingress Controller**: `ingress-nginx-controller-7848687fb8-qqxzh` (`1/1 Running`)

### 3.2 Workload Pod Baseline
All 12 expected workloads were verified in the `1/1 Running` and `Ready` state:

| Workload Pod | Ready | Status | Restarts | Age |
| :--- | :--- | :--- | :--- | :--- |
| `api-gateway-7c48ddd449-6xhw5` | 1/1 | Running | 1 | 4h4m |
| `auth-service-5549f7fb56-4pcc8` | 1/1 | Running | 3 | 4h2m |
| `user-service-79977fcbc5-vw9bw` | 1/1 | Running | 3 | 4h1m |
| `devsphere-config-server-bcf4ffcb6-ktrnd` | 1/1 | Running | 1 | 4h6m |
| `devsphere-service-discovery-7dc9f8b8d-gww6x` | 1/1 | Running | 2 | 4h5m |
| `devsphere-frontend-766474849f-cfbmr` | 1/1 | Running | 1 | 4h12m |
| `devsphere-mysql-6b6975d74b-hgbgk` | 1/1 | Running | 1 | 4h7m |
| `devsphere-redis-8fbd4bf59-4zh4f` | 1/1 | Running | 1 | 4h14m |
| `devsphere-kafka-65ffdc5b8d-n4gvz` | 1/1 | Running | 2 | 4h9m |
| `devsphere-zookeeper-5cfb4df7f5-sgnsm` | 1/1 | Running | 1 | 4h10m |
| `prometheus-54dcf5985d-6kdxf` | 1/1 | Running | 1 | 3h22m |
| `grafana-558768dd69-cqb97` | 1/1 | Running | 1 | 1m |

### 3.3 Application Endpoint Baseline
- **API Gateway Actuator Health**: `http://api-gateway.devsphere.svc.cluster.local:8080/actuator/health` → `{"status":"UP","groups":["liveness","readiness"]}`
- **Auth Service Actuator Health**: `http://auth-service.devsphere.svc.cluster.local:8081/actuator/health` → `{"status":"UP","groups":["liveness","readiness"]}`
- **User Service Actuator Health**: `http://user-service.devsphere.svc.cluster.local:8082/actuator/health` → `{"status":"UP","groups":["liveness","readiness"]}`
- **Frontend HTTP Response**: `http://devsphere-frontend.devsphere.svc.cluster.local:80` → HTTP 200 (`<title>DevSphere — Microservices Developer Platform</title>`)
- **Grafana Health**: `http://grafana.devsphere.svc.cluster.local:3000/api/health` → `{"commit":"895fbafb7a","database":"ok","version":"10.2.0"}`
- **Prometheus Targets**: 100% of discovered Spring Boot microservice targets active and UP.

---

## 4. Test 1 — Stateless Pod Deletion & Self-Healing

### 4.1 Target & Failure Injection
- **Target Workload**: `deployment/api-gateway` (stateless API perimeter)
- **Target Pod Deleted**: `api-gateway-7c48ddd449-6xhw5`
- **Failure Command**:
  ```powershell
  kubectl delete pod api-gateway-7c48ddd449-6xhw5 -n devsphere
  ```
- **Injection Timestamp**: `12:39:28`

### 4.2 Observed Self-Healing Lifecycle
1. **Detection Time**: < 1 second. The Kubernetes ReplicaSet controller detected that available replicas dropped from 1 to 0.
2. **Replacement Pod Provisioning**: At `12:39:29`, replacement pod `api-gateway-7c48ddd449-cw42t` was scheduled and transitioned to `ContainerCreating` -> `Running`.
3. **Application Boot & Probes**: Netty initialized on port 8080 at `12:40:24`, registered with Eureka service discovery (`DEVSPHERE-API-GATEWAY` status `UP`), and startup probe completed.
4. **Readiness Restoration**: At `12:40:35`, `api-gateway-7c48ddd449-cw42t` reached `1/1 Ready`.
5. **Total Recovery Time**: Approximately **67 seconds** (dominated by Spring Cloud config loading, Eureka registration, and probe polling intervals).

### 4.3 Recovery Evidence
```
NAME                           READY   STATUS    RESTARTS   AGE
api-gateway-7c48ddd449-cw42t   1/1     Running   0          72s
```
- API Gateway health endpoint returned `{"status":"UP","groups":["liveness","readiness"]}`.
- Authenticated/login route forwarded to backend auth-service correctly.

---

## 5. Test 2 — Failed Deployment Revision

### 5.1 Pre-Test Baseline Recording
- **Active Working Image**: `ghcr.io/prajapatipushkar/devsphere-api-gateway:latest`
- **Active Revision**: Revision 2
- **Pre-Rollout History**:
  ```
  deployment.apps/api-gateway 
  REVISION  CHANGE-CAUSE
  1         <none>
  2         <none>
  ```

### 5.2 Controlled Invalid Revision Injection
An intentional, non-destructive failure was triggered by modifying the deployment container image to a non-existent tag:
```powershell
kubectl set image deployment/api-gateway api-gateway=ghcr.io/prajapatipushkar/devsphere-api-gateway:lesson90-invalid -n devsphere
```
- **Injection Timestamp**: `12:43:00`
- **Bounded Rollout Verification Command**:
  ```powershell
  kubectl rollout status deployment/api-gateway -n devsphere --timeout=60s
  ```

### 5.3 Observed Failure Mechanism
1. Kubernetes created a new ReplicaSet (`api-gateway-5f9f7c86fc`) for revision 3.
2. The scheduler assigned replacement surge pod `api-gateway-5f9f7c86fc-z6p5j`.
3. The kubelet attempted image pull:
   ```
   Warning Failed: Failed to pull image "ghcr.io/prajapatipushkar/devsphere-api-gateway:lesson90-invalid": 
   rpc error: code = NotFound desc = failed to pull and unpack image ...: not found
   Warning Failed: Error: ErrImagePull
   Warning Failed: Error: ImagePullBackOff
   ```
4. **Zero-Downtime Guarantee**: Because `deployment.yaml` specifies `maxUnavailable: 0` and `maxSurge: 1`, the existing healthy pod (`api-gateway-7c48ddd449-cw42t`) was **not** terminated.
5. **Rollout Timeout**: At `12:44:16`, `kubectl rollout status` timed out with:
   ```
   Waiting for deployment "api-gateway" rollout to finish: 0 of 1 updated replicas are available...
   error: timed out waiting for the condition
   ```

---

## 6. Rollback Execution & Verification

### 6.1 Rollback Execution
Immediately following the bounded timeout, the native Kubernetes rollback mechanism was invoked:
```powershell
kubectl rollout undo deployment/api-gateway -n devsphere
```
- **Rollback Initiated**: `12:46:07`
- **Rollout Status Command**:
  ```powershell
  kubectl rollout status deployment/api-gateway -n devsphere --timeout=120s
  ```

### 6.2 Rollback Progress & Completion
1. Kubernetes immediately terminated the failing `ImagePullBackOff` pod (`api-gateway-5f9f7c86fc-z6p5j`).
2. Pod `api-gateway-7c48ddd449-nt5jl` was launched under the restored known-good template (revision 4, matching revision 2).
3. The restored pod initialized Spring Boot in 34.49 seconds, registered with Eureka, and passed readiness checks.
4. At `12:46:55`, the rollout completed successfully:
   ```
   deployment "api-gateway" successfully rolled out
   Rollback completed at: 09/11/2026 12:46:55
   ```
- **Total Rollback Time**: **48 seconds**.

### 6.3 Post-Rollback Revision History
```
deployment.apps/api-gateway 
REVISION  CHANGE-CAUSE
1         <none>
3         <none>
4         <none>
```
- **Active Image Reference**: `ghcr.io/prajapatipushkar/devsphere-api-gateway:latest`
- No invalid image reference remains in the cluster configuration.

---

## 7. Post-Rollback Application Functional Smoke Test

To confirm that the restored revision is fully functional without running a full acceptance test:

1. **API Gateway Health Check**:
   ```bash
   GET http://api-gateway.devsphere.svc.cluster.local:8080/actuator/health
   Response: 200 OK -> {"status":"UP","groups":["liveness","readiness"]}
   ```

2. **Cross-Service Routing & Authentication Smoke Test**:
   An intentionally invalid login request was dispatched through the API Gateway to `auth-service`:
   ```bash
   POST http://api-gateway.devsphere.svc.cluster.local:8080/api/v1/auth/login
   Headers: Content-Type: application/json
   Body: {"email":"invalid@devsphere.local","password":"wrongpassword"}
   ```
   - **Response**: `HTTP/1.1 401 Unauthorized`
   - **Validation**:
     - The gateway routed traffic cleanly to `auth-service` via Eureka service discovery.
     - Rate-limiting filters permitted the legitimate request.
     - Circuit breaker remained in closed (healthy) state.
     - The backend application performed credential authentication and rejected the request with expected HTTP 401.

3. **Backend Service Health**:
   - `auth-service` `/actuator/health` → `{"status":"UP"}`
   - `user-service` `/actuator/health` → `{"status":"UP"}`
   - `devsphere-frontend` → HTTP 200 OK

---

## 8. Prometheus & Grafana Observability Observations

- **Prometheus Scrape Targets**:
  - `api-gateway`: Scraped every 10s at `/actuator/prometheus` → `health: "up"` (Response duration: ~55ms).
  - `auth-service`: Scraped every 10s at `/actuator/prometheus` → `health: "up"`.
  - `user-service`: Scraped every 10s at `/actuator/prometheus` → `health: "up"`.
  - `config-server`: Scraped every 10s at `/actuator/prometheus` → `health: "up"`.
  - `service-discovery`: Scraped every 10s at `/actuator/prometheus` → `health: "up"`.
  - Scrape target availability: **5 of 5 targets UP (100%)**.
- **Grafana Datasource**:
  - Queried datasource ID `1` (`Prometheus` at `http://prometheus.devsphere.svc.cluster.local:9090`): Operational and healthy.

---

## 9. Final Cluster Health Audit

Audit executed at conclusion of testing:

```
kubectl get nodes:
NAME                      STATUS   ROLES           AGE     VERSION
devsphere-control-plane   Ready    control-plane   3d20h   v1.37.0

kubectl get pods -n devsphere:
NAME                                          READY   STATUS    RESTARTS       AGE
api-gateway-7c48ddd449-nt5jl                  1/1     Running   0              2m57s
auth-service-5549f7fb56-4pcc8                 1/1     Running   3 (20m ago)    4h20m
devsphere-config-server-bcf4ffcb6-ktrnd       1/1     Running   1 (23m ago)    4h24m
devsphere-frontend-766474849f-cfbmr           1/1     Running   1 (23m ago)    4h30m
devsphere-kafka-65ffdc5b8d-n4gvz              1/1     Running   2 (21m ago)    4h27m
devsphere-mysql-6b6975d74b-hgbgk              1/1     Running   1 (23m ago)    4h25m
devsphere-redis-8fbd4bf59-4zh4f               1/1     Running   1 (23m ago)    4h32m
devsphere-service-discovery-7dc9f8b8d-gww6x   1/1     Running   2 (20m ago)    4h23m
devsphere-zookeeper-5cfb4df7f5-sgnsm          1/1     Running   1 (23m ago)    4h28m
grafana-558768dd69-cqb97                      1/1     Running   2 (6m4s ago)   14m
prometheus-54dcf5985d-6kdxf                   1/1     Running   2 (12m ago)    3h40m
user-service-79977fcbc5-vw9bw                 1/1     Running   3 (20m ago)    4h19m

kubectl get pvc -n devsphere:
NAME             STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS
mysql-pv-claim   Bound    pvc-5c12f154-334c-4f81-82c3-eece2b561862   1Gi        RWO            standard
```

- Zero pods in `ImagePullBackOff`, `ErrImagePull`, or `CrashLoopBackOff`.
- No pending pods or unassigned workloads.
- Node is `Ready`.

---

## 10. Safety Limitations & CI/CD Analysis

### 10.1 Environment Constraints
- **Local Kind Cluster Only**: Tests were designed and scoped for a single-node local Kind cluster.
- **Resource Constraints**: Concurrency of JVM restarts was bounded to prevent host OOM conditions.
- **Stateless-Only Targeting**: Stateful data stores (MySQL, Kafka, Redis, Zookeeper) were excluded from deletion or fault injection.

### 10.2 CI/CD Rollback Capabilities (Lesson 88 Review)
Inspection of `.github/workflows/cd.yml` indicates:
- The Continuous Deployment workflow executes `kubectl apply -k k8s/` followed by `kubectl rollout status deployment/... --timeout=180s`.
- If a rollout fails, the CI/CD job fails, but it **does not** automatically execute `kubectl rollout undo`.
- **Conclusion**: Kubernetes-native rollback (`kubectl rollout undo`) is fully verified and functional in the cluster, but automated pipeline-driven rollback is not currently implemented in GitHub Actions CI/CD.

---

## 11. Reproduction Instructions

To reproduce these controlled tests in a healthy local environment:

### Step 1: Verify Baseline
```powershell
kubectl get pods -n devsphere
kubectl get nodes
```

### Step 2: Pod Deletion Test
```powershell
# Get target pod name
$targetPod = (kubectl get pods -l app.kubernetes.io/name=api-gateway -n devsphere -o jsonpath='{.items[0].metadata.name}')

# Delete target pod
kubectl delete pod $targetPod -n devsphere

# Watch replacement pod become ready
kubectl get pods -l app.kubernetes.io/name=api-gateway -n devsphere -w
```

### Step 3: Rollout Failure & Rollback Test
```powershell
# 1. Inject invalid image tag
kubectl set image deployment/api-gateway api-gateway=ghcr.io/prajapatipushkar/devsphere-api-gateway:lesson90-invalid -n devsphere

# 2. Observe failed rollout status (times out after 60s)
kubectl rollout status deployment/api-gateway -n devsphere --timeout=60s

# 3. Rollback immediately
kubectl rollout undo deployment/api-gateway -n devsphere

# 4. Verify successful rollback
kubectl rollout status deployment/api-gateway -n devsphere --timeout=120s

# 5. Confirm image restored
kubectl get deployment api-gateway -n devsphere -o jsonpath='{.spec.template.spec.containers[0].image}'
```
