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

## Deployment Architecture

```
                    GitHub Container Registry (GHCR)
                                │
                                ▼
                       Immutable Images
                                │
                                ▼
                      Kubernetes Namespace
                          (devsphere)
                                │
   ┌────────────────────┬───────┴────────────┬────────────────────┐
   │                    │                    │                    │
   ▼                    ▼                    ▼                    ▼
API Gateway         Auth Service        User Service       Control Plane
 (:8080)              (:8081)              (:8082)        (Config / Eureka)
   │                    │                    │
   └────────────────────┼────────────────────┘
                        │
                        ▼
           External Infrastructure Layer
            - MySQL Database Cluster
            - Apache Kafka Message Broker
            - Redis Distributed Cache
```
