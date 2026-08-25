# DevSphere Kubernetes Deployment, Security Hardening & High Availability Foundation

This directory contains the production-grade Kubernetes deployment manifests, perimeter entry ingress specifications, security hardening controls, horizontal pod autoscalers, and disruption budgets for the **DevSphere** microservices platform.

---

## Directory Structure

```
infrastructure/kubernetes/
├── namespace.yaml                # DevSphere isolated namespace with Pod Security Admission restricted
├── kustomization.yaml            # Kustomize base aggregation file
├── security/
│   └── serviceaccounts.yaml      # Dedicated ServiceAccounts with automount token disabled
├── autoscaling/
│   ├── gateway-hpa.yaml          # HPA v2 for API Gateway (min 2, max 10, CPU 70%)
│   ├── auth-hpa.yaml             # HPA v2 for Auth Service (min 2, max 10, CPU 70%)
│   └── user-hpa.yaml             # HPA v2 for User Service (min 2, max 10, CPU 70%)
├── availability/
│   └── pdb.yaml                  # PodDisruptionBudgets (minAvailable: 1) for Gateway, Auth, User, Config
├── networking/
│   ├── default-deny.yaml         # Namespace default-deny ingress & egress policy
│   ├── allow-dns.yaml            # DNS egress resolution (port 53 UDP/TCP)
│   ├── allow-ingress-to-gateway.yaml # Ingress controller access to API Gateway (port 8080)
│   ├── allow-gateway-to-services.yaml # API Gateway egress to Auth, User, Config, Eureka, Redis
│   ├── allow-auth-service.yaml   # Auth Service ingress from Gateway & egress to Infra/DB
│   ├── allow-user-service.yaml   # User Service ingress from Gateway & egress to Infra/DB/Cache
│   ├── allow-config-server.yaml  # Config Server ingress from application microservices
│   ├── allow-service-discovery.yaml # Eureka discovery ingress from application microservices
│   └── external-egress.yaml      # Documented egress for Kafka, Redis, and MySQL
├── config/
│   ├── configmap.yaml            # Non-secret environment properties
│   ├── secret.example.yaml       # Template for sensitive credentials (CHANGE_ME)
│   └── tls-secret.example.yaml   # Template for TLS certificate/key (CHANGE_ME)
├── gateway/
│   ├── deployment.yaml           # API Gateway deployment (Port 8080, Replicas 2, TopologySpread)
│   ├── service.yaml              # API Gateway ClusterIP Service
│   └── ingress.yaml              # Kubernetes Ingress perimeter router
├── auth/
│   ├── deployment.yaml           # Auth Service deployment (Port 8081, Replicas 2, TopologySpread)
│   └── service.yaml              # Auth Service ClusterIP Service
├── user/
│   ├── deployment.yaml           # User Service deployment (Port 8082, Replicas 2, TopologySpread)
│   └── service.yaml              # User Service ClusterIP Service
├── config-server/
│   ├── deployment.yaml           # Config Server deployment (Port 8888, Replicas 2, TopologySpread)
│   └── service.yaml              # Config Server ClusterIP Service
└── service-discovery/
    ├── deployment.yaml           # Eureka Service Discovery deployment (Port 8761, Replicas 1)
    └── service.yaml              # Eureka ClusterIP Service
```

---

## Architecture, Security & High Availability Highlights

1. **Horizontal Scaling Strategy**:
   - `devsphere-api-gateway`: Replicas `2`, HPA `min: 2, max: 10`
   - `devsphere-auth-service`: Replicas `2`, HPA `min: 2, max: 10`
   - `devsphere-user-service`: Replicas `2`, HPA `min: 2, max: 10`
   - `devsphere-config-server`: Replicas `2` (fixed, startup HA)
   - `devsphere-service-discovery`: Replicas `1` (standalone Eureka mode without peer replication)
2. **Horizontal Pod Autoscaling (HPA v2)**: CPU utilization target `70%` with `15s` scaleUp stabilization and `300s` scaleDown stabilization window.
3. **Pod Disruption Budgets (PDB v1)**: `minAvailable: 1` configured in [`availability/pdb.yaml`](file:///infrastructure/kubernetes/availability/pdb.yaml) for multi-replica workloads.
4. **Topology Spread Constraints**: Soft `maxSkew: 1` constraints on `topology.kubernetes.io/zone` and `kubernetes.io/hostname` with `whenUnsatisfiable: ScheduleAnyway`.
5. **Zero-Downtime Rolling Updates**: `strategy: RollingUpdate` (`maxUnavailable: 0`, `maxSurge: 1`).
6. **Workload Security & Identity**: Dedicated ServiceAccounts, disabled token automounting, zero RBAC, namespace `pod-security.kubernetes.io/enforce: restricted`, `runAsNonRoot: true`, `readOnlyRootFilesystem: true`, dropped capabilities (`ALL`), and `seccompProfile: RuntimeDefault`.
7. **Perimeter Ingress & Network Isolation**: NGINX Ingress entry point with TLS termination (`devsphere-api-tls`) and `default-deny-all` east-west NetworkPolicies.

---

## Secret & TLS Handling

> [!CAUTION]
> Never commit real secret files (`secret.yaml`, `tls-secret.yaml`, `*.key`, `*.crt`) to Git. Real secret files are strictly ignored by `.gitignore`.

---

## Dry-Run Manifest Validation

Validate manifests via Kustomize or `kubectl`:

```bash
# Validate Kustomize synthesis
kubectl kustomize infrastructure/kubernetes/

# Inspect HPA definitions
kubectl get hpa -n devsphere

# Inspect PodDisruptionBudgets
kubectl get pdb -n devsphere
```

---

## Local Cluster Testing (Minikube / Docker Desktop / Kind)

```bash
# Apply resources via Kustomize
kubectl apply -k infrastructure/kubernetes/

# Check status of deployed pods, services, HPAs, PDBs, network policies, and ingress
kubectl get pods -n devsphere
kubectl get svc -n devsphere
kubectl get hpa -n devsphere
kubectl get pdb -n devsphere
kubectl get netpol -n devsphere
kubectl get ingress -n devsphere
```
