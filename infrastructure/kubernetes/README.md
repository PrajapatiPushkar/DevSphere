# DevSphere Kubernetes Deployment & Environment Overlay Strategy

This directory contains the production-grade Kubernetes deployment manifests, perimeter entry ingress specifications, security hardening controls, horizontal pod autoscalers, disruption budgets, and Kustomize environment overlays for the **DevSphere** microservices platform.

---

## Directory Structure

```
infrastructure/kubernetes/
├── base/                              # Shared declarative manifests
│   ├── kustomization.yaml             # Base resource aggregation & common labels
│   ├── security/
│   │   └── serviceaccounts.yaml      # ServiceAccounts with automount token disabled
│   ├── autoscaling/
│   │   ├── gateway-hpa.yaml          # HPA v2 for API Gateway (min 2, max 10, CPU 70%)
│   │   ├── auth-hpa.yaml             # HPA v2 for Auth Service (min 2, max 10, CPU 70%)
│   │   └── user-hpa.yaml             # HPA v2 for User Service (min 2, max 10, CPU 70%)
│   ├── availability/
│   │   └── pdb.yaml                  # PodDisruptionBudgets (minAvailable: 1)
│   ├── networking/
│   │   ├── default-deny.yaml         # Namespace default-deny ingress & egress policy
│   │   ├── allow-dns.yaml            # DNS egress resolution (port 53 UDP/TCP)
│   │   ├── allow-ingress-to-gateway.yaml # Ingress access to API Gateway (port 8080)
│   │   ├── allow-gateway-to-services.yaml # Gateway egress to internal services & Redis
│   │   ├── allow-auth-service.yaml   # Auth Service ingress/egress
│   │   ├── allow-user-service.yaml   # User Service ingress/egress
│   │   ├── allow-config-server.yaml  # Config Server ingress from microservices
│   │   ├── allow-service-discovery.yaml # Eureka ingress from microservices
│   │   └── external-egress.yaml      # Egress for Kafka, Redis, and MySQL
│   ├── config/
│   │   ├── configmap.yaml            # Environment property baseline
│   │   ├── secret.example.yaml       # Template for sensitive credentials (CHANGE_ME)
│   │   └── tls-secret.example.yaml   # Template for TLS certificate/key (CHANGE_ME)
│   ├── gateway/
│   │   ├── deployment.yaml           # API Gateway deployment base
│   │   ├── service.yaml              # API Gateway ClusterIP Service
│   │   └── ingress.yaml              # Kubernetes Ingress base router
│   ├── auth/
│   │   ├── deployment.yaml           # Auth Service deployment base
│   │   └── service.yaml              # Auth Service ClusterIP Service
│   ├── user/
│   │   ├── deployment.yaml           # User Service deployment base
│   │   └── service.yaml              # User Service ClusterIP Service
│   ├── config-server/
│   │   ├── deployment.yaml           # Config Server deployment base
│   │   └── service.yaml              # Config Server ClusterIP Service
│   └── service-discovery/
│       ├── deployment.yaml           # Eureka Service Discovery deployment base
│       └── service.yaml              # Eureka ClusterIP Service
│
├── overlays/
│   ├── development/                   # Local development overlay (kind / minikube)
│   │   ├── kustomization.yaml         # Target namespace devsphere-dev, HPA/PDB disabled
│   │   ├── namespace.yaml             # devsphere-dev namespace with PSA restricted
│   │   ├── configmap-patch.yaml       # Dev active profile
│   │   └── ingress-patch.yaml         # Host api.devsphere.local (HTTP)
│   ├── staging/                       # Pre-production staging overlay
│   │   ├── kustomization.yaml         # Target namespace devsphere-staging, SHA tag
│   │   ├── namespace.yaml             # devsphere-staging namespace with PSA restricted
│   │   ├── configmap-patch.yaml       # Staging active profile
│   │   └── ingress-patch.yaml         # Host staging-api.devsphere.example.com (HTTPS)
│   └── production/                    # Production HA overlay
│       ├── kustomization.yaml         # Target namespace devsphere, SHA tag / digest
│       ├── namespace.yaml             # devsphere namespace with PSA restricted
│       ├── configmap-patch.yaml       # Prod active profile
│       └── ingress-patch.yaml         # Host api.devsphere.example.com (HTTPS)
│
└── README.md
```

---

## Environment Synthesis & Kustomize Commands

Validate manifest synthesis for any target environment overlay using `kubectl kustomize`:

```bash
# Synthesize Development Overlay
kubectl kustomize infrastructure/kubernetes/overlays/development

# Synthesize Staging Overlay
kubectl kustomize infrastructure/kubernetes/overlays/staging

# Synthesize Production Overlay
kubectl kustomize infrastructure/kubernetes/overlays/production
```

---

## Local Cluster Deployment (Minikube / Docker Desktop / Kind)

```bash
# Apply development overlay to local cluster
kubectl apply -k infrastructure/kubernetes/overlays/development

# Inspect deployed resources in devsphere-dev namespace
kubectl get pods -n devsphere-dev
kubectl get svc -n devsphere-dev
kubectl get netpol -n devsphere-dev
kubectl get ingress -n devsphere-dev
```

---

## Secret & TLS Protection

> [!CAUTION]
> Real credentials and TLS private keys must never be committed to Git. Real secret files are strictly ignored by `.gitignore`. Use structural templates `secret.example.yaml` and `tls-secret.example.yaml` for reference.
