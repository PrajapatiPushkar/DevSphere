# DevSphere Infrastructure

This directory contains the infrastructure, container runtime configurations, monitoring, network security, autoscaling, environment overlays, and orchestration specifications for the **DevSphere** platform.

---

## Infrastructure Directories

| Component | Path | Description |
| :--- | :--- | :--- |
| **Docker Compose** | [`docker/`](file:///infrastructure/docker) | Docker Compose configurations for local development infrastructure dependencies (Kafka, Zookeeper, Redis). |
| **Kubernetes** | [`kubernetes/`](file:///infrastructure/kubernetes) | Production-ready Kubernetes deployment manifests, base resources, security controls, NetworkPolicies, HPAs, PDBs, and Kustomize environment overlays (`development`, `staging`, `production`). |
| **Monitoring** | [`monitoring/`](file:///infrastructure/monitoring) | Prometheus scrape configurations (`prometheus.yml`) for microservice observability. |

---

## Environment Promotion & Kubernetes Architecture

```
                    GitHub Commit
                          │
                          ▼
              GitHub Actions CI Pipeline
                          │
                          ▼
            Immutable Container Image (sha-<commit>)
                          │
                          ▼
              GitHub Container Registry (GHCR)
                          │
       ┌──────────────────┼──────────────────┐
       ▼                  ▼                  ▼
  Development          Staging           Production
 (devsphere-dev)  (devsphere-staging)    (devsphere)
       │                  │                  │
       ▼                  ▼                  ▼
 Kustomize Dev     Kustomize Staging  Kustomize Prod
   Overlay            Overlay            Overlay
       │                  │                  │
       └──────────────────┼──────────────────┘
                          ▼
                  Kubernetes Cluster
```

---

## Kubernetes Layering & High Availability Highlights

- **Kustomize Environment Overlays**: Shared base manifests with `development` (single-node local cluster, HPA/PDB disabled), `staging` (pre-prod release validation, HPA/PDB enabled, immutable SHA tags), and `production` (full HA, HPA 2–10, PDB `minAvailable: 1`, topology spreading, immutable SHA tags/digests).
- **Horizontal Scaling & HPA v2**: CPU utilization target `70%` (`minReplicas: 2`, `maxReplicas: 10`) for Gateway, Auth, and User services in staging and production.
- **Pod Disruption Protection**: `policy/v1` PDBs (`minAvailable: 1`) protecting multi-replica workloads from voluntary disruptions.
- **Topology-Aware Scheduling**: `topologySpreadConstraints` across zones and hosts with `maxSkew: 1`.
- **Least Privilege Identity**: Dedicated ServiceAccounts with `automountServiceAccountToken: false` and zero Kubernetes RBAC permissions.
- **Container Hardening**: Non-root execution (`10001:10001`), read-only root filesystems, dropped capabilities (`ALL`), privilege escalation disabled, and `seccompProfile: RuntimeDefault`.
- **Pod Security Admission**: Namespace level enforcement of `pod-security.kubernetes.io/enforce: restricted`.
- **Network Isolation**: Namespace `default-deny-all` policy paired with explicit `networking.k8s.io/v1` allow rules for DNS, Gateway, microservices, and infrastructure egress.
