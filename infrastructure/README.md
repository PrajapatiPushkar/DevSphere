# DevSphere Infrastructure

This directory contains the infrastructure, container runtime configurations, monitoring, network security, and orchestration specifications for the **DevSphere** platform.

---

## Infrastructure Directories

| Component | Path | Description |
| :--- | :--- | :--- |
| **Docker Compose** | [`docker/`](file:///infrastructure/docker) | Docker Compose configurations for local development infrastructure dependencies (Kafka, Zookeeper, Redis). |
| **Kubernetes** | [`kubernetes/`](file:///infrastructure/kubernetes) | Production-ready Kubernetes deployment manifests, ClusterIP services, ConfigMaps, Secret templates, ServiceAccounts, NetworkPolicies, and Kustomize base structure. |
| **Monitoring** | [`monitoring/`](file:///infrastructure/monitoring) | Prometheus scrape configurations (`prometheus.yml`) for microservice observability. |

---

## Deployment, Perimeter & Network Security Architecture

```
                       Internet / Public HTTPS
                                │
                                ▼
              [Kubernetes Ingress (devsphere-ingress)]
                                │
                                ▼ (NetworkPolicy port 8080)
                       devsphere-api-gateway
                   (SA: devsphere-api-gateway)
                                │
   ┌────────────────────┬───────┴────────────┬────────────────────┐
   │                    │                    │                    │
   ▼                    ▼                    ▼                    ▼
Auth Service        User Service       Config Server      Service Discovery
(SA: auth-service)  (SA: user-service)  (SA: config-srvr)  (SA: discovery)
(ClusterIP :8081)    (ClusterIP :8082)   (ClusterIP :8888)  (ClusterIP :8761)
   │                    │
   └────────────────────┼────────────────────┐
                        │                    │
                        ▼                    ▼
             External Database        Async Message Broker
              - MySQL Cluster           - Apache Kafka
              - Redis Cache
```

### Security Highlights
- **Least Privilege Identity**: Dedicated ServiceAccounts with `automountServiceAccountToken: false` and zero Kubernetes RBAC permissions.
- **Container Hardening**: Non-root execution (`10001:10001`), read-only root filesystems, dropped capabilities (`ALL`), privilege escalation disabled, and `seccompProfile: RuntimeDefault`.
- **Pod Security Admission**: Namespace level enforcement of `pod-security.kubernetes.io/enforce: restricted`.
- **Network Isolation**: Namespace `default-deny-all` policy paired with explicit `networking.k8s.io/v1` allow rules for DNS, Gateway, microservices, and infrastructure egress.
