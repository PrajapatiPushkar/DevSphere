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
│   └── secret.example.yaml       # Template for sensitive credentials (CHANGE_ME)
├── gateway/
│   ├── deployment.yaml           # API Gateway deployment (Port 8080)
│   └── service.yaml              # API Gateway ClusterIP Service
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
2. **ClusterIP Internal Networking**: Internal services communicate via Kubernetes cluster DNS (`<service>.devsphere.svc.cluster.local`). Microservice endpoints are protected from public exposure.
3. **Hardened Security Contexts**:
   - `runAsNonRoot: true` (UID/GID `10001`)
   - `allowPrivilegeEscalation: false`
   - `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir` mounts
   - Linux capabilities dropped (`capabilities: drop: [ALL]`)
   - `automountServiceAccountToken: false`
4. **Spring Boot Health Probes**:
   - Liveness Probe: `GET /actuator/health/liveness`
   - Readiness Probe: `GET /actuator/health/readiness`
   - Startup Probe: `GET /actuator/health/liveness`
5. **Zero-Downtime Rolling Updates**:
   - Strategy: `RollingUpdate` (`maxUnavailable: 0`, `maxSurge: 1`)
   - Graceful termination: `terminationGracePeriodSeconds: 30`
6. **Resource Guardrails**:
   - Requests: `cpu: 250m`, `memory: 256Mi`
   - Limits: `cpu: 1000m`, `memory: 768Mi`
   - JVM container memory tuning: `-XX:MaxRAMPercentage=75.0`

---

## Secret Handling & Setup

> [!CAUTION]
> Never commit real secret files (`secret.yaml`) to Git. Real secret files are strictly ignored by `.gitignore`.

To configure local or staging secrets:
1. Copy `config/secret.example.yaml` to `config/secret.yaml`:
   ```bash
   cp config/secret.example.yaml config/secret.yaml
   ```
2. Replace all `CHANGE_ME` placeholder strings with real secret values.
3. Include `- config/secret.yaml` under `resources:` in `kustomization.yaml`.

---

## Dry-Run Manifest Validation

If `kubectl` is installed locally, validate the manifests without applying them to a live cluster:

```bash
# Validate individual manifests
kubectl apply --dry-run=client -f namespace.yaml
kubectl apply --dry-run=client -f config/configmap.yaml
kubectl apply --dry-run=client -k .
```

---

## Local Cluster Testing (Minikube / Docker Desktop / Kind)

To apply workloads to a local development cluster:

```bash
# Create namespace and apply resources via Kustomize
kubectl apply -k infrastructure/kubernetes/

# Check status of deployed pods and services
kubectl get pods -n devsphere
kubectl get svc -n devsphere
```
