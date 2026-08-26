# DevSphere — Kubernetes Environment Overlays & Production Deployment Strategy

This document describes the environment layering architecture, deployment strategy, and release promotion workflow for the **DevSphere** production-oriented developer life management platform.

---

## 1. Executive Summary & Purpose

As microservice architectures progress from initial containerization to production delivery, managing Kubernetes deployment configurations across multiple runtime environments becomes critical.

To eliminate configuration drift and avoid maintaining duplicated manifest copies, DevSphere adopts **Kustomize** overlay layering. A single, declarative **Base** manifest set defines common application resources, while environment-specific **Overlays** (`development`, `staging`, `production`) tailor runtime attributes such as replica counts, autoscaling behavior, ingress domains, TLS termination, and image tags.

---

## 2. Architecture: Base vs. Overlays

```
infrastructure/kubernetes/
│
├── base/                              # Shared declarative manifests
│   ├── kustomization.yaml             # Base resource aggregation & common labels
│   ├── auth/                          # Auth Service Deployment & ClusterIP Service
│   ├── autoscaling/                   # Base HPA v2 definitions
│   ├── availability/                  # Base PodDisruptionBudget definitions
│   ├── config/                        # Shared ConfigMap & secret templates
│   ├── config-server/                 # Config Server Deployment & ClusterIP Service
│   ├── gateway/                       # API Gateway Deployment, Service & Ingress base
│   ├── networking/                    # 9 default-deny & zero-trust NetworkPolicies
│   ├── security/                      # Dedicated ServiceAccounts (token automount disabled)
│   ├── service-discovery/             # Eureka Service Discovery Deployment & Service
│   └── user/                          # User Service Deployment & ClusterIP Service
│
├── overlays/                          # Environment-specific overlays
│   ├── development/                   # Local cluster (kind / minikube / Docker Desktop)
│   │   ├── kustomization.yaml         # Namespace devsphere-dev, HPA/PDB disabled, replicas 1
│   │   ├── namespace.yaml             # devsphere-dev with PSA restricted enforcement
│   │   ├── configmap-patch.yaml       # Dev active profile & internal DNS resolution
│   │   └── ingress-patch.yaml         # Local host api.devsphere.local (HTTP)
│   │
│   ├── staging/                       # Pre-production release validation
│   │   ├── kustomization.yaml         # Namespace devsphere-staging, HPA/PDB enabled, immutable SHA tags
│   │   ├── namespace.yaml             # devsphere-staging with PSA restricted enforcement
│   │   ├── configmap-patch.yaml       # Staging active profile
│   │   └── ingress-patch.yaml         # Host staging-api.devsphere.example.com with TLS
│   │
│   └── production/                    # Production HA workload execution
│       ├── kustomization.yaml         # Namespace devsphere, HPA/PDB enabled, immutable SHA tags
│       ├── namespace.yaml             # devsphere with PSA restricted enforcement
│       ├── configmap-patch.yaml       # Prod active profile
│       └── ingress-patch.yaml         # Host api.devsphere.example.com with TLS
│
└── README.md                          # Usage & Kustomize validation sitemap
```

---

## 3. Environment Overlays Detailed Specification

| Environment Feature | Development (`development`) | Staging (`staging`) | Production (`production`) |
| :--- | :--- | :--- | :--- |
| **Kubernetes Namespace** | `devsphere-dev` | `devsphere-staging` | `devsphere` |
| **Ingress Domain Host** | `api.devsphere.local` | `staging-api.devsphere.example.com` | `api.devsphere.example.com` |
| **TLS Architecture** | HTTP (or local self-signed) | HTTPS (`devsphere-staging-api-tls`) | HTTPS (`devsphere-api-tls`) |
| **API Gateway Replicas** | `1` | `2` | `2+` (HPA target 70% CPU) |
| **Auth Service Replicas** | `1` | `2` | `2+` (HPA target 70% CPU) |
| **User Service Replicas** | `1` | `2` | `2+` (HPA target 70% CPU) |
| **Config Server Replicas**| `1` | `2` | `2` (Stateless fixed HA) |
| **Service Discovery (Eureka)** | `1` | `1` | `1` (Standalone instance) |
| **HPA Autoscaling** | Disabled (`$patch: delete`) | Enabled (`min: 2, max: 10`) | Enabled (`min: 2, max: 10`) |
| **PodDisruptionBudget (PDB)** | Disabled (`$patch: delete`) | Enabled (`minAvailable: 1`) | Enabled (`minAvailable: 1`) |
| **Topology Spreading** | Single-node compatible | Topology spread aware | Multi-zone/node spreading |
| **Container Image Tag** | Environment tag / local | Immutable `sha-<commit>` | Immutable `sha-<commit>` / Digest |

---

## 4. Immutable Artifact Strategy & Promotion Model

DevSphere strictly adheres to the **"Build Once, Promote Many"** paradigm. Application source code is compiled once into an immutable container image tagged with a deterministic commit hash (`sha-<commit>`) or image digest. The exact same image artifact tested in staging is promoted to production without rebuilding.

```
       Source Code Commit
               │
               ▼
   GitHub Actions CI Pipeline
               │
               ▼
Build & Test Artifact (Single Jar/Image)
               │
               ▼
Publish to GHCR (Tagged: sha-<commit>)
               │
      ┌────────┴────────┐
      ▼                 ▼
Deploy to Staging  Deploy to Production
(Kustomize Overlay) (Kustomize Overlay)
```

> [!IMPORTANT]
> - Container images tagged with `:latest` or `-SNAPSHOT` are **strictly forbidden** in production overlays.
> - Production deployment MUST reference an immutable image tag (e.g. `sha-a1b2c3d`) or image digest (`@sha256:...`).

---

## 5. Configuration Ownership & Boundary Matrix

To maintain clean separation between application runtime logic and infrastructure platform orchestrations, DevSphere establishes clear boundaries for configuration settings:

| Configuration Category | Responsible Component | Examples |
| :--- | :--- | :--- |
| **Application Logic & Feature Flags** | Spring Cloud Config / App Properties | Database connection pool limits, JWT expiration rules, Resilience4j circuit breaker thresholds, Kafka consumer topic names |
| **Infrastructure & Orchestration** | Kubernetes / Kustomize | Replica counts, CPU/Memory resource limits, Ingress hostname, TLS secret references, HPA scale target percentages, PodDisruptionBudgets |
| **Environment Identity** | Kubernetes ConfigMap (`devsphere-configmap`) | `SPRING_PROFILES_ACTIVE`, cluster-local service endpoint URLs |
| **Sensitive Credentials** | External Secret Injection / Kubernetes Secret | Database passwords, JWT signing keys, TLS private keys |

---

## 6. Secret & Security Hardening Governance

All environment overlays maintain the strict security baseline established in Lesson 23:
1. **Pod Security Admission**: Namespace labels enforce `pod-security.kubernetes.io/enforce: restricted`.
2. **Container Runtime Hardening**: `runAsNonRoot: true`, `runAsUser: 10001`, `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, capabilities dropped (`ALL`), `seccompProfile: RuntimeDefault`.
3. **Identity Security**: Dedicated ServiceAccounts per microservice with `automountServiceAccountToken: false` and zero RBAC permissions.
4. **Network Perimeter & Isolation**: Default-deny-all NetworkPolicy with explicit ingress/egress permissions. Internal microservices (`auth-service`, `user-service`, `config-server`, `service-discovery`) remain strictly private on `ClusterIP`.
5. **Secret Protection**: No real database credentials, JWT keys, or TLS private keys are committed to Git repository. `secret.example.yaml` and `tls-secret.example.yaml` serve as structural references only.

---

## 7. Deployment & Rollback Strategy

### Deployment Workflow
1. Developer commits code to `main`.
2. CI builds the application Jars and runs automated tests across all microservices.
3. CI builds immutable container images and pushes them to GitHub Container Registry (GHCR) with `sha-<commit>`.
4. CI updates the image tag in `infrastructure/kubernetes/overlays/staging/kustomization.yaml` and applies the manifest via `kubectl apply -k`.
5. After validation in staging, human approval approves promotion to production.
6. CI updates the image tag in `infrastructure/kubernetes/overlays/production/kustomization.yaml` and applies the manifest via `kubectl apply -k`.

### Rollback Workflow
If an anomaly occurs during a production release:
1. Rollback is executed by reverting the image tag in `overlays/production/kustomization.yaml` to the previous immutable SHA tag (e.g. `sha-previous`).
2. Alternatively, run:
   ```bash
   kubectl rollout undo deployment/devsphere-api-gateway -n devsphere
   kubectl rollout undo deployment/devsphere-auth-service -n devsphere
   kubectl rollout undo deployment/devsphere-user-service -n devsphere
   ```
3. Never use `:latest` to attempt a rollout or rollback.

---

## 8. Synthesis Verification & Validation Commands

Synthesize and validate each Kustomize overlay manifest set locally:

```bash
# Validate Development Overlay
kubectl kustomize infrastructure/kubernetes/overlays/development

# Validate Staging Overlay
kubectl kustomize infrastructure/kubernetes/overlays/staging

# Validate Production Overlay
kubectl kustomize infrastructure/kubernetes/overlays/production
```
