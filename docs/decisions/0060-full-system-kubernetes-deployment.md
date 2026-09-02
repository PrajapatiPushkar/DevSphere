# 60. Full Microservices System Kubernetes Deployment

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `k8s/`
* **Date**: 2026-09-02

---

## Context

Following the initial Kubernetes foundation (Lesson 61), DevSphere required a full-system declarative Kubernetes deployment encompassing all 5 core Spring Boot microservices (`config-server`, `service-discovery` / Eureka, `auth-service`, `user-service`, `api-gateway`) along with infrastructure dependencies (MySQL with PVC, Redis, Apache Kafka & Zookeeper).

---

## Decision

1. **Complete Microservice Deployment Matrix**:
   - Deployed `config-server` (Port 8888), `service-discovery` (Port 8761), `auth-service` (Port 8081), `user-service` (Port 8082), and `api-gateway` (Port 8080) into the `devsphere` namespace.

2. **Spring Cloud Config Server & Service Discovery Integration**:
   - Exposed `config-server` and `service-discovery` via ClusterIP services.
   - Configured microservice client applications to resolve Config Server (`http://devsphere-config-server.devsphere.svc.cluster.local:8888`) and Eureka Discovery (`http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka/`) via internal Kubernetes DNS.

3. **In-Cluster Infrastructure Deployments (Local/Dev Foundation)**:
   - Configured MySQL Deployment with 1Gi PersistentVolumeClaim (`mysql-pv-claim`) on port 3306.
   - Configured Redis Deployment and ClusterIP Service on port 6379.
   - Configured Apache Kafka & Zookeeper Deployments with `KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://devsphere-kafka.devsphere.svc.cluster.local:9092`.

4. **Security & Resilience Policies**:
   - Maintained non-root container user execution (`10001`), read-only root filesystems, dropped Linux capabilities, and `/tmp` `emptyDir` mounts.
   - Configured Spring Boot Actuator liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) health probes across all 5 microservices.

---

## Consequences

* **Positive**:
  - The complete DevSphere architecture (`api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`) is fully deployable via `kubectl kustomize k8s/`.
  - Service discovery, centralized configuration, event publishing, and caching resolve using internal Kubernetes DNS names without hard-coded `localhost` references.
* **Trade-offs / Future Scope**:
  - Production deployments should use managed external database (Cloud SQL / RDS) and Kafka clusters.
