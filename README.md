# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lessons 1 through 19** are complete:
- **Production CI/CD Pipeline & Quality Gates (Lesson 19)**: Implemented automated GitHub Actions workflow (`.github/workflows/ci.yml`), pull request and main branch quality gates, Java 21 environment standardization, multi-service Maven matrix execution (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`), Maven dependency caching, OWASP dependency security vulnerability auditing, repository secret protection checks, multi-stage non-root Docker builds (`devsphere/<service>:${GITHUB_SHA}` validation), Surefire/Failsafe test report artifacts, and CI architecture documentation (`docs/architecture/ci-cd.md`).
- **Distributed Rate Limiting & API Protection (Lesson 18)**: Implemented distributed Redis-backed token-bucket rate limiting (`rate_limit:*`) at the API Gateway layer (`DEVSPHERE-API-GATEWAY`). Enforced authenticated identity keys (`rate_limit:user:{userId}`), public client IP keys (`rate_limit:ip:{ip}`), strict login and registration protection (`POST /api/v1/auth/login`, `POST /api/v1/auth/register`), standard HTTP 429 JSON responses with `Retry-After` headers, configurable fail-open/fail-closed Redis error policies (`app.rate-limit.fail-open`), bounded Redis timeouts, low-cardinality Prometheus metrics (`devsphere_rate_limit_requests_total`, `devsphere_rate_limit_rejected_total`), and OpenTelemetry trace span correlation (`rate_limit.result`).
- **Distributed Tracing Foundation (Lesson 17)**: Integrated Micrometer Tracing with OpenTelemetry bridge (`micrometer-tracing-bridge-otel`) and OTLP exporter (`opentelemetry-exporter-otlp`), W3C Trace Context propagation (`traceparent`), HTTP request tracing across Gateway and microservices, asynchronous Kafka trace context propagation via message headers, custom domain business spans (`auth.registration`, `auth.login`, `outbox.publish`, `user.profile.get`, `user.profile.update`, `kafka.user-registered.process`), trace-log correlation in application logs, and configurable sampling probability.
- **Production Resilience & Fault Tolerance (Lesson 16)**: Integrated Spring Cloud Circuit Breaker and Resilience4j bounded timeouts, selective retries, circuit breakers, bulkhead resource isolation, graceful HTTP 503 fallbacks, and failure classification while preserving non-idempotent registration write safety and Kafka consumer retry separation.
- **Production Authorization & RBAC (Lesson 15)**: Integrated Role-Based Access Control (`USER`, `ADMIN`), server-controlled role assignment, JWT role claims (`roles: ["USER"]`), API Gateway perimeter route authorization, microservice-level independent JWT validation, resource ownership checks (`authenticatedUserId == targetUserId OR ROLE_ADMIN`), and standard 401 Unauthorized vs 403 Forbidden HTTP semantics.
- **Production Observability Foundation** (`infrastructure/monitoring/prometheus.yml`): Standardized Spring Boot Actuator, Micrometer Prometheus metrics (`/actuator/prometheus`), JVM, HTTP, and low-cardinality custom business metrics.
- **Config Server** (`services/config-server`, Port `8888`): Dedicated Spring Cloud Config Server backed by an independent local Git repository (`config-repo/`) providing centralized non-secret runtime configuration.
- **Service Discovery** (`services/service-discovery`, Port `8761`): Standalone Netflix Eureka Service Discovery server maintaining an in-memory registry of all active microservice instances.
- **API Gateway** (`services/api-gateway`, Port `8080`): Perimeter Gateway registered with Eureka (`DEVSPHERE-API-GATEWAY`), importing centralized configuration from Config Server, enforcing JWT validation (`HS256`), identity header propagation (`X-Authenticated-User-Id`), coarse route authorization, bounded timeouts, Resilience4j circuit breakers, and distributed rate limiting.
- **Auth Service** (`services/auth-service`, Port `8081`): Authentication microservice registered with Eureka (`DEVSPHERE-AUTH-SERVICE`), owning user credentials (`devsphere_auth`), registration, server-side `USER` role assignment, password hashing, and atomic outbox event persistence (`outbox_events` table).
- **User Service** (`services/user-service`, Port `8082`): User profile domain microservice registered with Eureka (`DEVSPHERE-USER-SERVICE`), enforcing independent Spring Security JWT validation, method-level security (`@PreAuthorize`), resource ownership checks, and graceful Redis cache-to-MySQL fallback.
- **Apache Kafka**: Message broker enabling eventual-consistent asynchronous communication between microservices with DLT routing.
- **Redis**: Distributed store providing high-performance, demand-driven caching for `User Service` profile reads (`user_profile:*`) and distributed rate limiting state (`rate_limit:*`) for `API Gateway`.
- **Transactional Outbox Pattern**: Atomic database persistence of business entity and event records in `Auth Service`.

---

## Architecture Diagram

```
                         ┌─────────────────┐
                         │     Clients     │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │
                         │                 │
                         │ JWT Validation  │
                         │ Rate Limiting   │
                         │ RBAC            │
                         │ Resilience      │
                         └────────┬────────┘
                                  │
                         ┌────────┴────────┐
                         │                 │
                         ▼                 ▼
                    ┌──────────┐      ┌──────────────┐
                    │  Redis   │      │   Eureka     │
                    │          │      │  Discovery   │
                    │ Rate     │      └──────┬───────┘
                    │ Limits   │             │
                    │ + Cache  │             ▼
                    └──────────┘       ┌─────────────┐
                                       │ Microservices│
                                       └─────────────┘
```

---

## CI/CD Pipeline Architecture

```
                  +-----------------------------------+
                  |      GitHub Actions Workflow      |
                  |     (.github/workflows/ci.yml)    |
                  +-----------------+-----------------+
                                    |
          +-------------------------+-------------------------+
          |                         |                         |
+---------v---------+     +---------v---------+     +---------v---------+
| Repository Scan   |     | Maven Service     |     | Dependency        |
| - Secrets         |     | Matrix Build      |     | Security Audit    |
| - Target dirs     |     | - Java 21         |     | - OWASP           |
| - Java 21 POMs    |     | - mvn verify      |     |   Dependency Check|
+-------------------+     +---------+---------+     +-------------------+
                                    |
                          +---------v---------+
                          | Docker Image      |
                          | Validation        |
                          | - Multi-stage     |
                          | - Tagged SHA      |
                          +-------------------+
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

---

## License

This project is licensed under the [MIT License](LICENSE).

