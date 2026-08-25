# DevSphere Infrastructure

This directory contains the infrastructure, container runtime configurations, monitoring, network security, autoscaling, and orchestration specifications for the **DevSphere** platform.

---

## Infrastructure Directories

| Component | Path | Description |
| :--- | :--- | :--- |
| **Docker Compose** | [`docker/`](file:///infrastructure/docker) | Docker Compose configurations for local development infrastructure dependencies (Kafka, Zookeeper, Redis). |
| **Kubernetes** | [`kubernetes/`](file:///infrastructure/kubernetes) | Production-ready Kubernetes deployment manifests, ClusterIP services, ConfigMaps, Secret templates, ServiceAccounts, NetworkPolicies, HPAs, PDBs, and Kustomize base structure. |
| **Monitoring** | [`monitoring/`](file:///infrastructure/monitoring) | Prometheus scrape configurations (`prometheus.yml`) for microservice observability. |

---

## Deployment, Perimeter, Security & High Availability Architecture

```
                       Internet / Public HTTPS
                                │
                                ▼
              [Kubernetes Ingress (devsphere-ingress)]
                                │
                                ▼ (NetworkPolicy port 8080)
                       devsphere-api-gateway
                   (HPA: 2-10 | PDB min: 1)
                                │
   ┌────────────────────┬───────┴────────────┬────────────────────┐
   │                    │                    │                    │
   ▼                    ▼                    ▼                    ▼
Auth Service        User Service       Config Server      Service Discovery
(HPA: 2-10 | PDB: 1) (HPA: 2-10 | PDB: 1) (Fixed 2 | PDB: 1) (Fixed 1 | Standalone)
(ClusterIP :8081)    (ClusterIP :8082)   (ClusterIP :8888)  (ClusterIP :8761)
   │                    │
   └────────────────────┼────────────────────┐
                        │                    │
                        ▼                    ▼
             External Database        Async Message Broker
              - MySQL Cluster           - Apache Kafka
              - Redis Cache
```

### Security & Availability Highlights
- **Horizontal Scaling & HPA v2**: CPU utilization target `70%` (`minReplicas: 2`, `maxReplicas: 10`) for Gateway, Auth, and User services.
- **Pod Disruption Protection**: `policy/v1` PDBs (`minAvailable: 1`) protecting multi-replica workloads from voluntary disruptions.
- **Topology-Aware Scheduling**: `topologySpreadConstraints` across zones and hosts with `maxSkew: 1`.
- **Least Privilege Identity**: Dedicated ServiceAccounts with `automountServiceAccountToken: false` and zero Kubernetes RBAC permissions.
- **Container Hardening**: Non-root execution (`10001:10001`), read-only root filesystems, dropped capabilities (`ALL`), privilege escalation disabled, and `seccompProfile: RuntimeDefault`.
- **Pod Security Admission**: Namespace level enforcement of `pod-security.kubernetes.io/enforce: restricted`.
- **Network Isolation**: Namespace `default-deny-all` policy paired with explicit `networking.k8s.io/v1` allow rules for DNS, Gateway, microservices, and infrastructure egress.
