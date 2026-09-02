# Kubernetes Deployment Architecture — Lesson 62

## 1. Overview & Kubernetes Role in DevSphere

Kubernetes serves as the production container orchestration platform for the entire **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`).

The production architecture exposes the system to clients via a Kubernetes Ingress Controller, which routes perimeter traffic directly into the API Gateway. Internal microservices register with Eureka Service Discovery, retrieve centralized configuration from Config Server, and communicate over internal Kubernetes ClusterIP DNS endpoints.

```text
                         Client / Internet
                                 │
                                 ▼
                         Kubernetes Ingress (api.devsphere.local)
                                 │
                                 ▼
                            API Gateway (8080)
                              /       \
                             /         \
                            ▼           ▼
                     Auth Service    User Service
                     (Port 8081)     (Port 8082)
                           |              |
                           +------+-------+
                                  │
                           Service Discovery (Eureka: 8761)
                           /      |       \
                          /       |        \
                       Redis    Kafka    Config Server (8888)
                      (6379)   (9092)
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

## 3. Deployment Matrix & Microservice Lifecycles

Application microservices are deployed as Kubernetes `Deployment` resources:

- `config-server` (1 replica, port 8888)
- `service-discovery` (1 replica, port 8761)
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

### Graceful Termination
Containers specify `terminationGracePeriodSeconds: 30`. When `SIGTERM` is emitted, Spring Boot graceful shutdown (`server.shutdown: graceful`) drains in-flight HTTP requests and completes active Transactional Outbox batches before process exit.

---

## 4. Service Discovery & Internal DNS Networking

Internal service-to-service communication relies on Kubernetes DNS and Eureka Service Discovery:

- Config Server: `http://devsphere-config-server.devsphere.svc.cluster.local:8888`
- Service Discovery (Eureka): `http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka/`
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

## 6. Local Development Infrastructure Pods

For local development and testing in Kubernetes (e.g. Minikube / Kind / Docker Desktop), manifest foundations are provided under `k8s/infrastructure/`:

- **MySQL (`k8s/infrastructure/mysql.yaml`)**: PersistentVolumeClaim (`mysql-pv-claim`, 1Gi), Deployment (`mysql:8.0`), ClusterIP Service (`3306`).
- **Redis (`k8s/infrastructure/redis.yaml`)**: Deployment (`redis:7-alpine`), ClusterIP Service (`6379`).
- **Kafka & Zookeeper (`k8s/infrastructure/kafka.yaml`)**: Zookeeper Deployment/Service (`2181`) and Kafka Broker Deployment/Service (`9092`) with `PLAINTEXT://devsphere-kafka.devsphere.svc.cluster.local:9092`.

---

## 7. Autoscaling Foundation (HPA)

HorizontalPodAutoscalers (`autoscaling/v2`) defined in `k8s/autoscaling/hpa.yaml`:
- Targets: `api-gateway`, `auth-service`, `user-service`.
- Replicas: min `2`, max `10`.
- Metric: CPU average utilization threshold of `70%`.

---

## 8. Health Probes & Actuator Integration

Containers utilize Spring Boot Actuator endpoints for health monitoring:

| Probe Type | Actuator Endpoint | Purpose |
| :--- | :--- | :--- |
| **Startup Probe** | `/actuator/health/liveness` | Protects initial Spring Boot JVM context startup from premature restarts. |
| **Liveness Probe** | `/actuator/health/liveness` | Monitors container process responsiveness. Restarts unresponsive containers. |
| **Readiness Probe** | `/actuator/health/readiness` | Controls traffic routing. Removes unhealthy pods from Service endpoint rotators. |

---

## 9. Security & Hardening

- **Non-root user execution**: UID/GID `10001` (`USER devsphere`).
- **Read-only root filesystem**: `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir`.
- **Capability dropping**: `capabilities.drop: [ALL]` and `allowPrivilegeEscalation: false`.
- **Token mounting**: `automountServiceAccountToken: false`.

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

## 11. Verification & Synthesis

Synthesize and validate all manifests statically using `kubectl`:

```bash
kubectl kustomize k8s/
```
