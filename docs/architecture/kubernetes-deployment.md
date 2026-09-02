# Kubernetes Deployment Architecture — Lesson 61

## 1. Overview & Kubernetes Role in DevSphere

Kubernetes serves as the production container orchestration platform for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`).

The production architecture exposes the system to clients via a Kubernetes Ingress Controller, which routes perimeter traffic directly into the API Gateway. Internal microservices communicate securely over internal Kubernetes ClusterIP DNS endpoints.

```text
                    Internet / Client
                           |
                           v
                    Kubernetes Ingress (api.devsphere.local)
                           |
                           v
                    API Gateway (Port 8080)
                           |
             +-------------+-------------+
             |                           |
             v                           v
        auth-service (8081)        user-service (8082)
             |                           |
             +-------------+-------------+
                           |
                  MySQL / Redis / Kafka
```

---

## 2. Namespace Strategy

All DevSphere application workloads are strictly deployed into a dedicated namespace:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: devsphere
  labels:
    app.kubernetes.io/name: devsphere
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: latest
```

Using a dedicated `devsphere` namespace ensures multi-tenant isolation, precise RBAC authorization boundaries, and prevention of naming collisions with system workloads in `default` or `kube-system`.

---

## 3. Deployment Configuration & Microservice Lifecycle

Application microservices are deployed as Kubernetes `Deployment` resources:

- `api-gateway` (2 replicas, port 8080)
- `auth-service` (2 replicas, port 8081)
- `user-service` (2 replicas, port 8082)

### Rolling Update Strategy
Every deployment specifies zero-downtime rolling updates to ensure uninterrupted client traffic during deployments:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

`maxUnavailable: 0` guarantees that existing operational pods are never terminated before new replacement pods pass readiness health checks.

### Graceful Termination
Containers specify `terminationGracePeriodSeconds: 30`. When `SIGTERM` is emitted, Spring Boot graceful shutdown (`server.shutdown: graceful`) drains in-flight HTTP requests and completes active Transactional Outbox batches before process exit.

---

## 4. Kubernetes Services & Internal DNS Discovery

Internal service-to-service communication relies on Kubernetes DNS rather than hardcoded IPs or host discovery servers:

- API Gateway: `http://api-gateway.devsphere.svc.cluster.local:8080`
- Auth Service: `http://auth-service.devsphere.svc.cluster.local:8081`
- User Service: `http://user-service.devsphere.svc.cluster.local:8082`
- MySQL: `devsphere-mysql.devsphere.svc.cluster.local:3306`
- Redis: `devsphere-redis.devsphere.svc.cluster.local:6379`
- Kafka: `PLAINTEXT://devsphere-kafka.devsphere.svc.cluster.local:9092`

All internal microservices use `ClusterIP` services to prevent unauthorized external exposure.

---

## 5. Configuration & Secret Management

### ConfigMap (`devsphere-configmap`)
Non-sensitive runtime properties are managed centrally in `k8s/config/configmap.yaml` and mounted via `envFrom`:

```yaml
data:
  SPRING_PROFILES_ACTIVE: "prod"
  CONFIG_SERVER_URL: "http://devsphere-config-server.devsphere.svc.cluster.local:8888"
  EUREKA_SERVER_URL: "http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka"
  SPRING_REDIS_HOST: "devsphere-redis"
  SPRING_REDIS_PORT: "6379"
  SPRING_KAFKA_BOOTSTRAP_SERVERS: "devsphere-kafka:9092"
  MYSQL_HOST: "devsphere-mysql"
  MYSQL_PORT: "3306"
  APP_RATE_LIMIT_ENABLED: "true"
  APP_RATE_LIMIT_FAIL_OPEN: "true"
```

### Secrets (`devsphere-secrets`)
Sensitive parameters (`MYSQL_PASSWORD`, `SPRING_REDIS_PASSWORD`, `JWT_SECRET`, `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG`) are injected using Kubernetes `Secret` resources (`k8s/config/secrets.example.yaml`). Plain text secrets are strictly excluded from Git tracking via `.gitignore`. Template examples are provided in `k8s/config/secrets.example.yaml`.

---

## 6. Infrastructure Foundation (Local / Dev Kubernetes)

For local development and testing in Kubernetes (e.g. Minikube / Kind / Docker Desktop), manifest foundations are provided under `k8s/infrastructure/`:

- **MySQL (`k8s/infrastructure/mysql.yaml`)**:
  - PersistentVolumeClaim (`mysql-pv-claim`, 1Gi)
  - Deployment (1 replica, `mysql:8.0`, TCP liveness/readiness probes on port 3306)
  - ClusterIP Service (`devsphere-mysql:3306`)
  - *Production Note*: Production MySQL must be hosted on external managed infrastructure (AWS RDS, GCP Cloud SQL) rather than an in-cluster single pod.
- **Redis (`k8s/infrastructure/redis.yaml`)**:
  - Deployment (1 replica, `redis:7-alpine`, TCP probes on 6379)
  - ClusterIP Service (`devsphere-redis:6379`)
  - *Production Note*: Production Redis should utilize managed Redis or ElastiCache with multi-AZ replication.
- **Kafka (`k8s/infrastructure/kafka.yaml`)**:
  - Zookeeper Deployment + Service (`devsphere-zookeeper:2181`)
  - Kafka Broker Deployment + Service (`devsphere-kafka:9092`) with `KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://devsphere-kafka.devsphere.svc.cluster.local:9092`
  - Preserves existing topic names, Transactional Outbox pattern, and consumer groups (`devsphere-user-service`, `devsphere-resume-activity-group`).

---

## 7. Autoscaling Foundation (HPA)

HorizontalPodAutoscalers (`autoscaling/v2`) are defined under `k8s/autoscaling/hpa.yaml`:

```yaml
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
```

- Targets: `api-gateway`, `auth-service`, `user-service`.
- Metric: CPU average utilization threshold (70%).
- *Dependency Note*: HPA requires `metrics-server` to be deployed in the Kubernetes cluster to collect real-time container CPU metrics.

---

## 8. Health Probes & Actuator Integration

Containers utilize Spring Boot Actuator endpoints for health monitoring:

| Probe Type | Actuator Endpoint | Purpose |
| :--- | :--- | :--- |
| **Startup Probe** | `/actuator/health/liveness` | Protects initial Spring Boot JVM context startup from premature restarts (initial delay 15s, 20 retries). |
| **Liveness Probe** | `/actuator/health/liveness` | Monitors container process responsiveness. Restarts unresponsive containers. |
| **Readiness Probe** | `/actuator/health/readiness` | Controls traffic routing. Removes unhealthy pods from Service endpoint rotators. |

---

## 9. Resource Allocation & Security Hardening

### Resource Requests & Limits
```yaml
resources:
  requests:
    cpu: "250m"
    memory: "512Mi"
  limits:
    cpu: "1000m"
    memory: "1Gi"
```
Container JVM heap limits are dynamically aligned via `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"`.

### Security Context
Pods run with non-root container isolation:
- `runAsNonRoot: true`
- `runAsUser: 10001`
- `runAsGroup: 10001`
- `allowPrivilegeEscalation: false`
- `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir`
- `capabilities.drop: [ALL]`
- `automountServiceAccountToken: false`

---

## 10. Ingress Perimeter Entry

External traffic enters via Kubernetes `Ingress` (`k8s/ingress/ingress.yaml`) pointing to `api-gateway`:

```yaml
spec:
  rules:
    - host: api.devsphere.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 8080
```

---

## 11. Verification & Dry-Run Operations

Synthesize and validate all manifests statically using `kubectl`:

```bash
kubectl kustomize k8s/
```
