# DevSphere Kubernetes Deployment Foundation

This directory contains the production-grade Kubernetes deployment manifests for the **DevSphere** microservices platform.

---

## Directory Structure

```
infrastructure/kubernetes/
├── namespace.yaml                # DevSphere isolated namespace definition
├── kustomization.yaml            # Kustomize base aggregation file
├── config/
│   ├── configmap.yaml            # Non-secret environment properties
│   ├── secret.example.yaml       # Template for sensitive credentials (CHANGE_ME)
│   └── tls-secret.example.yaml   # Template for TLS certificate/key (CHANGE_ME)
├── gateway/
│   ├── deployment.yaml           # API Gateway deployment (Port 8080)
│   ├── service.yaml              # API Gateway ClusterIP Service
│   └── ingress.yaml              # Kubernetes Ingress perimeter router
├── auth/
│   ├── deployment.yaml           # Auth Service deployment (Port 8081)
│   └── service.yaml              # Auth Service ClusterIP Service
├── user/
│   ├── deployment.yaml           # User Service deployment (Port 8082)
│   └── service.yaml              # User Service ClusterIP Service
├── config-server/
│   ├── deployment.yaml           # Config Server deployment (Port 8888)
│   └── service.yaml              # Config Server ClusterIP Service
└── service-discovery/
    ├── deployment.yaml           # Eureka Service Discovery deployment (Port 8761)
    └── service.yaml              # Eureka ClusterIP Service
```

---

## Architecture Highlights

1. **Dedicated Namespace**: All workloads deploy strictly into the `devsphere` namespace.
2. **Perimeter Ingress Routing**: Public HTTPS traffic (`api.devsphere.example.com`) routes through [`gateway/ingress.yaml`](file:///infrastructure/kubernetes/gateway/ingress.yaml) to `devsphere-api-gateway` (Port `8080`).
3. **Internal Microservice Isolation**: Downstream microservices (`auth-service`, `user-service`, `config-server`, `service-discovery`, Kafka, Redis, MySQL) remain strictly private `ClusterIP`-only services.
4. **TLS Termination & Headers**: Ingress terminates TLS referencing `devsphere-api-tls` and preserves `X-Forwarded-For` and `traceparent` headers for rate limiting and tracing.
5. **Hardened Security Contexts**:
   - `runAsNonRoot: true` (UID/GID `10001`)
   - `allowPrivilegeEscalation: false`
   - `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir` mounts
   - Linux capabilities dropped (`capabilities: drop: [ALL]`)
   - `automountServiceAccountToken: false`
6. **Spring Boot Health Probes**:
   - Liveness Probe: `GET /actuator/health/liveness`
   - Readiness Probe: `GET /actuator/health/readiness`
   - Startup Probe: `GET /actuator/health/liveness`
7. **Zero-Downtime Rolling Updates**:
   - Strategy: `RollingUpdate` (`maxUnavailable: 0`, `maxSurge: 1`)
   - Graceful termination: `terminationGracePeriodSeconds: 30`
8. **Resource Guardrails**:
   - Requests: `cpu: 250m`, `memory: 256Mi`
   - Limits: `cpu: 1000m`, `memory: 768Mi`
   - JVM container memory tuning: `-XX:MaxRAMPercentage=75.0`

---

## Secret & TLS Handling

> [!CAUTION]
> Never commit real secret files (`secret.yaml`, `tls-secret.yaml`, `*.key`, `*.crt`) to Git. Real secret files are strictly ignored by `.gitignore`.

To configure local or staging TLS certificates:
1. Copy `config/tls-secret.example.yaml` to `config/tls-secret.yaml`:
   ```bash
   cp config/tls-secret.example.yaml config/tls-secret.yaml
   ```
2. Replace `CHANGE_ME` placeholders with Base64/PEM certificate and private key strings.
3. For local testing with `api.devsphere.local`, add `127.0.0.1 api.devsphere.local` to your local operating system `hosts` file.

---

## Dry-Run Manifest Validation

If `kubectl` is installed locally, validate the manifests without applying them to a live cluster:

```bash
# Validate individual manifests
kubectl apply --dry-run=client -f namespace.yaml
kubectl apply --dry-run=client -f config/configmap.yaml
kubectl apply --dry-run=client -f gateway/ingress.yaml
kubectl apply --dry-run=client -k .
```

---

## Local Cluster Testing (Minikube / Docker Desktop / Kind)

To apply workloads to a local development cluster:

```bash
# Apply resources via Kustomize
kubectl apply -k infrastructure/kubernetes/

# Check status of deployed pods, services, and ingress
kubectl get pods -n devsphere
kubectl get svc -n devsphere
kubectl get ingress -n devsphere
```

