# Kubernetes Deployment Foundation Architecture

## 1. Kubernetes Role in DevSphere
Kubernetes serves as the production container orchestration platform for DevSphere. Following the container delivery foundation established in Lesson 20, Kubernetes consumes immutable container images published to GitHub Container Registry (GHCR) and manages container lifecycle, self-healing, rolling updates, internal cluster DNS networking, and resource allocation.

---

## 2. Namespace Architecture
All DevSphere workloads are strictly isolated inside the `devsphere` namespace:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: devsphere
```

Workloads do not use the `default` namespace. This guarantees multi-tenant isolation, resource boundary management, and scoped access control.

---

## 3. Deployments
Deployments manage the application microservices:
- `devsphere-config-server` (2 replicas)
- `devsphere-service-discovery` (2 replicas)
- `devsphere-auth-service` (2 replicas)
- `devsphere-user-service` (2 replicas)
- `devsphere-api-gateway` (2 replicas)

All application microservices are stateless. Persistent session data and domain entities are managed by external infrastructure (Redis, MySQL, Kafka).

---

## 4. Service Strategy & Internal DNS Networking
Each deployment is paired with a **ClusterIP Service**:
- `config-server` (Port `8888`)
- `service-discovery` (Port `8761`)
- `auth-service` (Port `8081`)
- `user-service` (Port `8082`)
- `api-gateway` (Port `8080`)

### Internal Service DNS:
Microservices communicate internally using fully-qualified Kubernetes DNS names:
- Config Server: `http://config-server.devsphere.svc.cluster.local:8888`
- Eureka Discovery: `http://service-discovery.devsphere.svc.cluster.local:8761/eureka`
- Auth Service: `http://auth-service.devsphere.svc.cluster.local:8081`
- User Service: `http://user-service.devsphere.svc.cluster.local:8082`

`NodePort` and `LoadBalancer` service types are avoided for internal microservices to prevent accidental public exposure.

---

## 5. ConfigMap Architecture
Non-secret application configuration is managed via [`infrastructure/kubernetes/config/configmap.yaml`](file:///infrastructure/kubernetes/config/configmap.yaml):
- Active Spring profiles (`prod`)
- Internal Service DNS endpoints
- Redis and Kafka host/port definitions
- Rate limiting flags

ConfigMaps are injected into containers via `envFrom: [ configMapRef: { name: devsphere-configmap } ]`.

---

## 6. Secrets Architecture
Sensitive credentials (JWT signing keys, database passwords, Redis passwords, Kafka SASL credentials) are decoupled from container images and ConfigMaps:
- Secret templates are documented in [`secret.example.yaml`](file:///infrastructure/kubernetes/config/secret.example.yaml) with `CHANGE_ME` placeholders.
- Real secret files (`secret.yaml`) are ignored by Git (`.gitignore`).
- Secrets are mounted via `secretRef` with `optional: true` fallback support during local manifest dry-run testing.

---

## 7. Health Probes (Liveness, Readiness & Startup)
Spring Boot Actuator health indicators provide container status probes:

| Probe | Endpoint | Initial Delay | Purpose |
| :--- | :--- | :---: | :--- |
| **Startup** | `/actuator/health/liveness` | 15s | Protects slow Spring Boot startups from premature restarts. |
| **Liveness** | `/actuator/health/liveness` | 5s | Verifies container responsiveness. Triggers Pod restart on failure. |
| **Readiness** | `/actuator/health/readiness` | 10s | Controls traffic routing. Removes Pod from Service endpoint on failure. |

> [!NOTE]
> Liveness probes monitor application responsiveness and do **not** fail due to temporary external broker network blips, preventing cascading container restart loops.

---

## 8. Resource Requests & Limits
Every application container defines CPU and memory guardrails:

```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"
  limits:
    cpu: "1000m"
    memory: "768Mi"
```

Java 21 container awareness is enforced via `-XX:MaxRAMPercentage=75.0` in `JAVA_TOOL_OPTIONS`, ensuring JVM heap sizes scale dynamically within cgroup limits.

---

## 9. Security Context & Hardening
Application pods execute under hardened security constraints:
- **Non-root user**: `runAsNonRoot: true`, `runAsUser: 10001`, `runAsGroup: 10001`, `fsGroup: 10001`.
- **Privilege Escalation**: `allowPrivilegeEscalation: false`.
- **Read-Only Root Filesystem**: `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir` mounts for Spring Boot temporary files.
- **Capabilities**: `capabilities: drop: [ALL]`.
- **Service Account**: `automountServiceAccountToken: false` (applications do not require Kubernetes API access).

---

## 10. Zero-Downtime Rolling Updates
Deployments utilize `RollingUpdate` strategy:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```
This guarantees that full operational capacity is preserved throughout software rollouts.

---

## 11. Graceful Shutdown
Deployments specify `terminationGracePeriodSeconds: 30`. Spring Boot graceful shutdown (`server.shutdown: graceful`) completes in-flight HTTP requests before container termination.

---

## 12. External Dependencies
MySQL, Apache Kafka, and Redis are treated as external infrastructure dependencies. They are not managed in application Deployment manifests and communicate via externalized service endpoints.

---

## 13. Image Immutability & Promotion
Deployments reference immutable container images published to GHCR during Lesson 20:
- Image format: `ghcr.io/<owner>/devsphere-<service>:${IMAGE_TAG}`
- Production deployments pin exact cryptographic digests (`ghcr.io/<owner>/devsphere-api-gateway@sha256:<digest>`).

---

## 14. Rollback Strategy
If a newly deployed release exhibits runtime defects, rollbacks are performed without code re-compilation:
```bash
kubectl rollout undo deployment/devsphere-api-gateway -n devsphere
```

---

## 15. Future Roadmap
Lesson 21 establishes the Kubernetes manifest foundation. Future lessons will introduce:
- **Ingress & TLS**: NGINX Ingress Controller / Cert-Manager for public HTTPS entry.
- **Autoscaling (HPA)**: Horizontal Pod Autoscaler based on CPU, memory, and HTTP request metrics.
- **Network Policies**: East-west network segmentation restricting inter-pod traffic.
- **GitOps Orchestration**: ArgoCD / FluxCD automated cluster synchronization.
- **External Secret Management**: HashiCorp Vault / External Secrets Operator.
