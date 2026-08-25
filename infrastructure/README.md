# DevSphere Infrastructure

This directory contains the infrastructure, container runtime configurations, monitoring, and orchestration specifications for the **DevSphere** platform.

---

## Infrastructure Directories

| Component | Path | Description |
| :--- | :--- | :--- |
| **Docker Compose** | [`docker/`](file:///infrastructure/docker) | Docker Compose configurations for local development infrastructure dependencies (Kafka, Zookeeper, Redis). |
| **Kubernetes** | [`kubernetes/`](file:///infrastructure/kubernetes) | Production-ready Kubernetes deployment manifests, ClusterIP services, ConfigMaps, Secret templates, and Kustomize base structure. |
| **Monitoring** | [`monitoring/`](file:///infrastructure/monitoring) | Prometheus scrape configurations (`prometheus.yml`) for microservice observability. |

---

## Deployment & Perimeter Architecture

```
                       Internet / Public HTTPS
                                │
                                ▼
              [Kubernetes Ingress (devsphere-ingress)]
                                │
                                ▼
                       devsphere-api-gateway
                         (ClusterIP :8080)
                                │
   ┌────────────────────┬───────┴────────────┬────────────────────┐
   │                    │                    │                    │
   ▼                    ▼                    ▼                    ▼
Auth Service        User Service       Config Server      Service Discovery
(ClusterIP :8081)    (ClusterIP :8082)   (ClusterIP :8888)  (ClusterIP :8761)
   │                    │
   └────────────────────┼────────────────────┐
                        │                    │
                        ▼                    ▼
             External Database        Async Message Broker
              - MySQL Cluster           - Apache Kafka
              - Redis Cache
```

