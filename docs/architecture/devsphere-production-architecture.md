# DevSphere Production Architecture

This document details the final production architecture for **DevSphere**, a microservices-based SaaS developer productivity platform.

---

## 1. System Overview

DevSphere is designed as a cloud-native, microservices-architected application deployed on Kubernetes. It consists of a React 18 single-page application (SPA), a Spring Cloud API Gateway, multiple Spring Boot microservices, relational and key-value storage, event-driven messaging, and a Prometheus/Grafana observability stack.

```text
                               🌐 Internet (Ingress Controller)
                                            │
                                            ▼
                                  devsphere-ingress
                             /                         \
                            /                           \
                           ▼                             ▼
                 devsphere-frontend                 api-gateway
                   (Port 80 NGINX)                  (Port 8080)
                                                         │
                                    ┌────────────────────┼────────────────────┐
                                    ▼                    ▼                    ▼
                               auth-service         user-service       config-server /
                                (Port 8082)          (Port 8081)      service-discovery
                                    │                    │
                                    └─────────┬──────────┘
                                              ▼
                                    MySQL / Redis / Kafka
                                              │
                                              ▼
                                      prometheus-service
                                         (Port 9090)
                                              │
                                              ▼
                                       grafana-service
                                         (Port 3000)
```

---

## 2. Component Specifications

### 2.1 Frontend (`devsphere-frontend`)
- **Technology Stack**: React 18, TypeScript, Tailwind CSS, Vite.
- **Production Server**: NGINX 1.25 Alpine running as non-root (`nginx`).
- **SPA Routing**: Handled via NGINX fallback (`try_files $uri $uri/ /index.html;`).
- **Containerization**: Multi-stage Docker build producing a static asset image (< 35 MB).
- **Service Port**: Port 80.

### 2.2 API Gateway (`api-gateway`)
- **Technology Stack**: Java 21, Spring Cloud Gateway, Reactive WebFlux.
- **Responsibilities**:
  - Central entry point for external API traffic (`api.devsphere.local`).
  - JWT Token validation via `JwtAuthenticationFilter`.
  - Header enrichment (`X-Authenticated-User-Id`, `X-Authenticated-User-Email`).
  - Rate limiting using Redis Token Bucket algorithm (`RedisRateLimiter`).
  - CORS header handling and request forwarding.
- **Service Port**: Port 8080.

### 2.3 Auth Service (`auth-service`)
- **Technology Stack**: Java 21, Spring Boot 3.2, Spring Security, BCrypt, Spring Data JPA.
- **Responsibilities**:
  - User registration and password hashing.
  - Authentication and JWT token issuance.
  - Transactional Outbox pattern for publishing `UserRegisteredEvent` via MySQL outbox table.
- **Database**: MySQL 8.0 (`devsphere_auth`).
- **Service Port**: Port 8082.

### 2.4 User Service (`user-service`)
- **Technology Stack**: Java 21, Spring Boot 3.2, Spring Data JPA, Redis, Apache Kafka.
- **Responsibilities**:
  - Managing user profiles, task items, daily planner, DSA progress, and career history.
  - Event consumption: Idempotent processing of `UserRegisteredEvent` from Kafka using a consumed messages log table.
  - Redis Caching: User profile caching with automated TTL and invalidation on profile updates.
- **Database / Cache / Messaging**: MySQL 8.0 (`devsphere_user`), Redis 7.2, Kafka 3.7.
- **Service Port**: Port 8081.

### 2.5 Infrastructure Services
- **Config Server (`config-server`)**: Centralized application configuration on Port 8888.
- **Service Discovery (`service-discovery`)**: Netflix Eureka registry on Port 8761.
- **MySQL 8.0**: Primary transactional storage for Auth and User services.
- **Redis 7.2**: High-speed cache for profiles and rate limiting counters.
- **Apache Kafka 3.7**: Asynchronous event publishing and inter-service communication.

---

## 3. Observability Architecture

- **Prometheus**:
  - Scrapes metrics from `/actuator/prometheus` endpoints across all Spring Boot microservices.
  - Configuration defined in `k8s/observability/prometheus.yaml`.
  - Tracks JVM heap usage, HTTP request latencies (p95, p99), error rates (5xx), and CPU utilization.
- **Grafana**:
  - Deployed on Port 3000 with pre-provisioned Prometheus datasource (`http://prometheus-service:9090`).
  - Dashboard configuration defined in `k8s/observability/grafana.yaml`.

---

## 4. Kubernetes Deployment & Security

- **Namespace**: `devsphere`.
- **Ingress**: `devsphere-ingress` mapping `app.devsphere.local` to `devsphere-frontend:80` and `api.devsphere.local` to `api-gateway:8080`.
- **High Availability**: Deployments run with 2 replicas, configured with PodDisruptionBudgets (`minAvailable: 1`) and HPAs (scaling from 2 to 10 replicas based on CPU/Memory).
- **Security Hardening**:
  - Non-root user execution (`runAsNonRoot: true`, `runAsUser: 10001`).
  - `readOnlyRootFilesystem: true` with ephemeral `/tmp` volume mounts.
  - All default Linux capabilities dropped (`drop: ["ALL"]`).
  - Seccomp profile enforced (`RuntimeDefault`).

---

## 5. CI/CD Integration

- **CI Pipeline (`.github/workflows/ci.yml`)**: Security checks, Maven clean verify across all services, Vitest and type-checking on frontend, dependency vulnerability scans, Docker build validation.
- **CD Pipeline (`.github/workflows/cd.yml`)**: Builds and tags container images with Git SHA (`ghcr.io/prajapatipushkar/devsphere-*`), publishes images to GHCR, updates Kustomize image tags, and executes rolling deployment updates on Kubernetes.
