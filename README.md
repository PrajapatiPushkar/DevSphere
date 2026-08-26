# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lessons 1 through 28** are complete:
- **Daily Planner and Task Scheduling Domain (Lesson 28)**: Implemented Daily Planner and Task Scheduling domain inside `services/user-service` as the third core product productivity module. Added `planner_entries` table via Flyway migration (`V5__create_planner_entries.sql`), unique constraint `UNIQUE(user_id, task_id, planned_date)` preventing duplicate task scheduling per date, optional start/end time slots, sort ordering, planned duration estimates, rescheduling, unscheduling (preserving `Task` status and content), atomic reordering (`/days/{date}/reorder`), dynamic daily summary calculations (`totalEntries`, `completedEntries`, `pendingEntries`, `totalPlannedMinutes`, `completionPercentage`), strict JWT-derived identity scoping, mandatory IDOR protection (`findByIdAndUserId`, returning HTTP 404 Not Found), Prometheus metrics (`devsphere_planner_entries_created_total`, `devsphere_planner_entries_deleted_total`, `devsphere_planner_entries_rescheduled_total`, `devsphere_planner_entries_reordered_total`), API Gateway routing (`/api/v1/planner/**`), ADR 0027 ([`docs/decisions/0027-daily-planner-domain.md`](file:///docs/decisions/0027-daily-planner-domain.md)), and daily planner architecture documentation ([`docs/architecture/daily-planner.md`](file:///docs/architecture/daily-planner.md)).
- **Task Management Domain (Lesson 27)**: Implemented Task Management domain inside `services/user-service` as the second core product productivity module. Added `tasks` table via Flyway migration (`V4__create_tasks.sql`), task statuses (`TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`), priorities (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), optional user-scoped goal association (`goalId`), dedicated state transition endpoints (`/start`, `/complete`, `/reopen`, `/cancel`), logical archival (`status = ARCHIVED`, HTTP 204 No Content), dynamic in-memory overdue computation, database pagination, dynamic filtering, default sorting (`dueDate ASC NULLS LAST, createdAt DESC`), strict JWT-derived identity scoping, mandatory IDOR protection (`findByIdAndUserId`, returning HTTP 404 Not Found), Prometheus metrics (`devsphere_tasks_created_total`, `devsphere_tasks_completed_total`, `devsphere_tasks_cancelled_total`, `devsphere_tasks_reopened_total`), API Gateway routing (`/api/v1/tasks/**`), ADR 0026 ([`docs/decisions/0026-task-management-domain.md`](file:///docs/decisions/0026-task-management-domain.md)), and task domain architecture documentation ([`docs/architecture/task-management.md`](file:///docs/architecture/task-management.md)).
- **Developer Profile & Goal Management Domain (Lesson 26)**: Transitioned into the core DevSphere product domain by establishing Developer Profile management (`/api/v1/profile`) and Goal Management (`/api/v1/goals/**`) inside `services/user-service`. Implemented Flyway schema evolution (`V3__create_goals_and_update_profiles.sql`), goal types (`DAILY`, `WEEKLY`, `LONG_TERM`), goal statuses (`ACTIVE`, `COMPLETED`, `ARCHIVED`), in-memory dynamic progress percentage calculation, logical archival (`status = ARCHIVED`, HTTP 204 No Content), strict identity derivation from JWT (`UserPrincipal` / `X-Authenticated-User-Id`), mandatory IDOR user isolation protection (`findByIdAndUserId`, returning HTTP 404 Not Found without leaking resource existence), API Gateway routing (`/api/v1/profile/**`, `/api/v1/goals/**`), Prometheus metrics (`devsphere_profile_updates_total`, `devsphere_goals_created_total`, `devsphere_goals_completed_total`), ADR 0025 ([`docs/decisions/0025-developer-profile-and-goals.md`](file:///docs/decisions/0025-developer-profile-and-goals.md)), and domain architecture documentation ([`docs/architecture/developer-profile-and-goals.md`](file:///docs/architecture/developer-profile-and-goals.md)).
- **Kubernetes Environment Overlays & Production Deployment Strategy (Lesson 25)**: Implemented Kustomize environment overlays ([`infrastructure/kubernetes/overlays/`](file:///infrastructure/kubernetes/overlays)) separating `development` (`devsphere-dev` namespace, host `api.devsphere.local`, HTTP, single-node local cluster compatible with HPA/PDB disabled), `staging` (`devsphere-staging` namespace, host `staging-api.devsphere.example.com`, HTTPS `devsphere-staging-api-tls`, immutable `sha-<commit>` image tags, HPA/PDB enabled), and `production` (`devsphere` namespace, host `api.devsphere.example.com`, HTTPS `devsphere-api-tls`, immutable SHA tags/digests, HPA/PDB enabled, topology spread preserved) from a shared declarative base ([`infrastructure/kubernetes/base/`](file:///infrastructure/kubernetes/base)). Enforced "Build Once, Promote Many" image promotion strategy, documented configuration ownership boundaries, rollback procedures, ADR 0024 ([`docs/decisions/0024-kubernetes-environment-strategy.md`](file:///docs/decisions/0024-kubernetes-environment-strategy.md)), and Kubernetes environment strategy documentation ([`docs/architecture/kubernetes-environment-strategy.md`](file:///docs/architecture/kubernetes-environment-strategy.md)).
- **Kubernetes High Availability, Autoscaling & Workload Reliability (Lesson 24)**: Implemented `autoscaling/v2` Horizontal Pod Autoscalers for `api-gateway`, `auth-service`, and `user-service` (`minReplicas: 2`, `maxReplicas: 10`, CPU utilization `70%`), `policy/v1` PodDisruptionBudgets with `minAvailable: 1` for multi-replica workloads, `topologySpreadConstraints` across `topology.kubernetes.io/zone` and `kubernetes.io/hostname`, standalone replica evaluation for Eureka (`replicas: 1`), fixed startup HA for Config Server (`replicas: 2`), rolling update preservation (`maxUnavailable: 0`, `maxSurge: 1`), health probe guardrails, graceful shutdown (`terminationGracePeriodSeconds: 30`), ADR 0023 ([`docs/decisions/0023-kubernetes-high-availability.md`](file:///docs/decisions/0023-kubernetes-high-availability.md)), and High Availability architecture documentation ([`docs/architecture/kubernetes-high-availability.md`](file:///docs/architecture/kubernetes-high-availability.md)).
- **Kubernetes Security Hardening & Network Isolation (Lesson 23)**: Implemented dedicated workload ServiceAccounts with disabled API token mounting (`automountServiceAccountToken: false`), zero Kubernetes RBAC bindings, Pod Security Admission `restricted` namespace enforcement, hardened pod security contexts (`runAsNonRoot: true`, `10001:10001`, `readOnlyRootFilesystem: true`, dropped capabilities `ALL`, `seccompProfile: RuntimeDefault`), namespace-wide `default-deny-all` NetworkPolicy, DNS egress resolution policy, controlled east-west traffic flow policies between Gateway, Auth, User, Config Server, and Eureka, restricted external infrastructure egress (Kafka, Redis, MySQL), secret externalization, ADR 0022 ([`docs/decisions/0022-kubernetes-security-hardening.md`](file:///docs/decisions/0022-kubernetes-security-hardening.md)), and Kubernetes security architecture documentation ([`docs/architecture/kubernetes-security.md`](file:///docs/architecture/kubernetes-security.md)).
- **Kubernetes Ingress, TLS & External Access Foundation (Lesson 22)**: Implemented Kubernetes Ingress resource for API Gateway perimeter exposure, host-based routing (`api.devsphere.example.com`), TLS termination referencing secret `devsphere-api-tls`, template `tls-secret.example.yaml`, HTTP to HTTPS SSL redirects, forwarded header propagation (`X-Forwarded-For`, `X-Forwarded-Proto`, `traceparent`), strict downstream microservice `ClusterIP` isolation, internal Actuator endpoint protection, and Ingress architecture documentation (`docs/architecture/kubernetes-ingress.md`).
- **Kubernetes Deployment Foundation (Lesson 21)**: Implemented declarative Kubernetes deployment manifests, dedicated `devsphere` namespace, ClusterIP service internal DNS networking (`<service>.devsphere.svc.cluster.local`), decoupled `ConfigMap` configuration, `secret.example.yaml` template, hardened non-root container security contexts (`runAsNonRoot: true`, `readOnlyRootFilesystem: true`, dropped capabilities), Spring Boot Actuator liveness/readiness/startup probes, `RollingUpdate` deployment strategy (`maxUnavailable: 0`, `maxSurge: 1`), CPU/memory requests and limits, Kustomize base structure, and Kubernetes architecture documentation (`docs/architecture/kubernetes-foundation.md`).
- **Container Registry & Continuous Delivery Foundation (Lesson 20)**: Implemented automated GitHub Container Registry (GHCR) Continuous Delivery pipeline ([`.github/workflows/cd.yml`](file:///.github/workflows/cd.yml)), multi-service container image publishing (`ghcr.io/<owner>/devsphere-<service>`), immutable SHA-based image tagging (`sha-<short-sha>` & `${GITHUB_SHA}`), semantic version release tagging (`v1.0.0`), pull-request publish protection, `GITHUB_TOKEN` least-privilege authentication (`packages: write`), cryptographic image digest (`sha256:...`) logging, automated `release-manifest.json` generation, build-once-promote-many architecture, and CD architecture documentation (`docs/architecture/container-registry-cd.md`).
- **Production CI/CD Pipeline & Quality Gates (Lesson 19)**: Implemented automated GitHub Actions workflow (`.github/workflows/ci.yml`), pull request and main branch quality gates, Java 21 environment standardization, multi-service Maven matrix execution (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`), Maven dependency caching, OWASP dependency security vulnerability auditing, repository secret protection checks, multi-stage non-root Docker builds (`devsphere/<service>:${GITHUB_SHA}` validation), Surefire/Failsafe test report artifacts, and CI architecture documentation (`docs/architecture/ci-cd.md`).
- **Distributed Rate Limiting & API Protection (Lesson 18)**: Implemented distributed Redis-backed token-bucket rate limiting at the API Gateway layer.
- **Distributed Tracing Foundation (Lesson 17)**: Integrated Micrometer Tracing with OpenTelemetry bridge and OTLP exporter.
- **Production Resilience & Fault Tolerance (Lesson 16)**: Integrated Spring Cloud Circuit Breaker and Resilience4j bounded timeouts, selective retries, circuit breakers, bulkhead resource isolation.
- **Production Authorization & RBAC (Lesson 15)**: Integrated Role-Based Access Control (`USER`, `ADMIN`).
- **Production Observability Foundation** (`infrastructure/monitoring/prometheus.yml`): Standardized Spring Boot Actuator, Micrometer Prometheus metrics.
- **Config Server** (`services/config-server`, Port `8888`): Dedicated Spring Cloud Config Server.
- **Service Discovery** (`services/service-discovery`, Port `8761`): Standalone Netflix Eureka Service Discovery server.
- **API Gateway** (`services/api-gateway`, Port `8080`): Perimeter Gateway registered with Eureka (`DEVSPHERE-API-GATEWAY`).
- **Auth Service** (`services/auth-service`, Port `8081`): Authentication microservice registered with Eureka (`DEVSPHERE-AUTH-SERVICE`).
- **User Service** (`services/user-service`, Port `8082`): User profile domain microservice registered with Eureka (`DEVSPHERE-USER-SERVICE`).
- **Apache Kafka**: Message broker enabling eventual-consistent asynchronous communication between microservices with DLT routing.
- **Redis**: Distributed store providing high-performance, demand-driven caching and distributed rate limiting.
- **Transactional Outbox Pattern**: Atomic database persistence of business entity and event records in `Auth Service`.

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

## Development Roadmap

- **Lesson 1**: Production Repository Foundation *(Completed)*
- **Lesson 2**: API Gateway Foundation Service *(Completed)*
- **Lesson 3**: Spring Cloud Gateway & Request Routing Foundation *(Completed)*
- **Lesson 4**: Auth Service Foundation & User Registration *(Completed)*
- **Lesson 5**: JWT Authentication & Login *(Completed)*
- **Lesson 6**: API Gateway JWT Validation & Protected Routes *(Completed)*
- **Lesson 7**: User Service & Profile Management *(Completed)*
- **Lesson 8**: Event-Driven User Registration with Apache Kafka *(Completed)*
- **Lesson 9**: Redis Distributed Caching for User Profiles *(Completed)*
- **Lesson 10**: Transactional Outbox Pattern & Reliable Eventing *(Completed)*
- **Lesson 11**: Production-Grade Kafka Consumer Reliability *(Completed)*
- **Lesson 12**: Service Discovery with Eureka *(Completed)*
- **Lesson 13**: Centralized Configuration with Spring Cloud Config *(Completed)*
- **Lesson 14**: Production Observability Foundation *(Completed)*
- **Lesson 15**: Production-Grade Authorization and RBAC *(Completed)*
- **Lesson 16**: Production Resilience and Fault Tolerance *(Completed)*
- **Lesson 17**: Distributed Tracing Foundation with OpenTelemetry *(Completed)*
- **Lesson 18**: Distributed Rate Limiting and API Protection *(Completed)*
- **Lesson 19**: Production CI/CD Pipeline and Quality Gates *(Completed)*
- **Lesson 20**: Container Registry and Continuous Delivery Foundation *(Completed)*
- **Lesson 21**: Kubernetes Deployment Foundation *(Completed)*
- **Lesson 22**: Kubernetes Ingress, TLS and External Access Foundation *(Completed)*
- **Lesson 23**: Kubernetes Security Hardening and Network Isolation *(Completed)*
- **Lesson 24**: Kubernetes High Availability, Autoscaling and Workload Reliability *(Completed)*
- **Lesson 25**: Kubernetes Environment Overlays and Production Deployment Strategy *(Completed)*

---

## License

This project is licensed under the [MIT License](LICENSE).
