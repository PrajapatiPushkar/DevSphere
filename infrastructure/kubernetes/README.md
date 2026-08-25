# DevSphere Kubernetes Deployment & Security Hardening Foundation

This directory contains the production-grade Kubernetes deployment manifests, perimeter entry ingress specifications, and security hardening controls for the **DevSphere** microservices platform.

---

## Directory Structure

```
infrastructure/kubernetes/
├── namespace.yaml                # DevSphere isolated namespace with Pod Security Admission restricted
├── kustomization.yaml            # Kustomize base aggregation file
├── security/
│   └── serviceaccounts.yaml      # Dedicated ServiceAccounts with automount token disabled
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

## Architecture & Security Hardening

1. **Dedicated Namespace & Pod Security Admission**: All workloads deploy into `devsphere` namespace enforcing `pod-security.kubernetes.io/enforce: restricted`.
2. **Workload Identity Separation**: Dedicated ServiceAccounts ([`security/serviceaccounts.yaml`](file:///infrastructure/kubernetes/security/serviceaccounts.yaml)) for every microservice.
3. **Zero Kubernetes API Exposure**: `automountServiceAccountToken: false` set on all ServiceAccounts/Deployments and zero Kubernetes RBAC permissions assigned.
4. **Perimeter Ingress Routing**: Public HTTPS traffic (`api.devsphere.example.com`) routes through [`gateway/ingress.yaml`](file:///infrastructure/kubernetes/gateway/ingress.yaml) to `devsphere-api-gateway` (Port `8080`).
5. **Strict Internal Service Isolation**: Downstream microservices (`auth-service`, `user-service`, `config-server`, `service-discovery`, Kafka, Redis, MySQL) remain strictly private `ClusterIP`-only services.
6. **NetworkPolicy East-West Firewalling**:
   - `default-deny-all`: Denies all ingress and egress traffic by default.
   - `allow-dns-egress`: Allows DNS name resolution (port 53 UDP/TCP).
   - Ingress controller restricted to API Gateway port 8080.
   - Gateway allowed to call Auth (8081), User (8082), Config Server (8888), Eureka (8761), and Redis (6379).
   - Auth & User allowed egress to Kafka (9092/29092), MySQL (3306), Redis (6379), Config Server (8888), and Eureka (8761).
   - Direct pod-to-pod cross-talk between `auth-service` and `user-service` is denied.
7. **Hardened Security Contexts**:
   - `runAsNonRoot: true` (UID/GID `10001`)
   - `allowPrivilegeEscalation: false`
   - `readOnlyRootFilesystem: true` with ephemeral `/tmp` `emptyDir` mounts
   - `capabilities: drop: [ALL]`
   - `seccompProfile: { type: RuntimeDefault }`
8. **Spring Boot Health Probes & Resilience**:
   - Rolling update strategy: `RollingUpdate` (`maxUnavailable: 0`, `maxSurge: 1`)
   - Graceful termination: `terminationGracePeriodSeconds: 30`
   - Resource requests: `cpu: 250m`, `memory: 256Mi`; Limits: `cpu: 1000m`, `memory: 768Mi`

---

## Secret & TLS Handling

> [!CAUTION]
> Never commit real secret files (`secret.yaml`, `tls-secret.yaml`, `*.key`, `*.crt`) to Git. Real secret files are strictly ignored by `.gitignore`.

To configure local or staging secrets:
1. Copy `config/secret.example.yaml` to `config/secret.yaml` and `config/tls-secret.example.yaml` to `config/tls-secret.yaml`.
2. Replace `CHANGE_ME` placeholders with real Base64 encoded secrets.

---

## Dry-Run Manifest Validation

Validate manifests via Kustomize or `kubectl`:

```bash
# Validate Kustomize synthesis
kustomize build infrastructure/kubernetes/

# Validate dry-run client application
kubectl apply --dry-run=client -k infrastructure/kubernetes/

# Verify ServiceAccount permission isolation (expected: no)
kubectl auth can-i get pods --as=system:serviceaccount:devsphere:devsphere-auth-service -n devsphere
```

---

## Local Cluster Testing (Minikube / Docker Desktop / Kind)

To apply workloads to a local development cluster:

```bash
# Apply resources via Kustomize
kubectl apply -k infrastructure/kubernetes/

# Check status of deployed pods, services, network policies, and ingress
kubectl get pods -n devsphere
kubectl get svc -n devsphere
kubectl get netpol -n devsphere
kubectl get ingress -n devsphere
```
