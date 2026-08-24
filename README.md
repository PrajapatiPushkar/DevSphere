# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lessons 1 through 17** are complete:
- **Distributed Tracing Foundation (Lesson 17)**: Integrated Micrometer Tracing with OpenTelemetry bridge (`micrometer-tracing-bridge-otel`) and OTLP exporter (`opentelemetry-exporter-otlp`), W3C Trace Context propagation (`traceparent`), HTTP request tracing across Gateway and microservices, asynchronous Kafka trace context propagation via message headers, custom domain business spans (`auth.registration`, `auth.login`, `outbox.publish`, `user.profile.get`, `user.profile.update`, `kafka.user-registered.process`), trace-log correlation in application logs, and configurable sampling probability.
- **Production Resilience & Fault Tolerance (Lesson 16)**: Integrated Spring Cloud Circuit Breaker and Resilience4j bounded timeouts, selective retries, circuit breakers, bulkhead resource isolation, graceful HTTP 503 fallbacks, and failure classification while preserving non-idempotent registration write safety and Kafka consumer retry separation.
- **Production Authorization & RBAC (Lesson 15)**: Integrated Role-Based Access Control (`USER`, `ADMIN`), server-controlled role assignment, JWT role claims (`roles: ["USER"]`), API Gateway perimeter route authorization, microservice-level independent JWT validation, resource ownership checks (`authenticatedUserId == targetUserId OR ROLE_ADMIN`), and standard 401 Unauthorized vs 403 Forbidden HTTP semantics.
- **Production Observability Foundation** (`infrastructure/monitoring/prometheus.yml`): Standardized Spring Boot Actuator, Micrometer Prometheus metrics (`/actuator/prometheus`), JVM, HTTP, and low-cardinality custom business metrics.
- **Config Server** (`services/config-server`, Port `8888`): Dedicated Spring Cloud Config Server backed by an independent local Git repository (`config-repo/`) providing centralized non-secret runtime configuration.
- **Service Discovery** (`services/service-discovery`, Port `8761`): Standalone Netflix Eureka Service Discovery server maintaining an in-memory registry of all active microservice instances.
- **API Gateway** (`services/api-gateway`, Port `8080`): Perimeter Gateway registered with Eureka (`DEVSPHERE-API-GATEWAY`), importing centralized configuration from Config Server, enforcing JWT validation (`HS256`), identity header propagation (`X-Authenticated-User-Id`), coarse route authorization, bounded timeouts, and Resilience4j circuit breakers.
- **Auth Service** (`services/auth-service`, Port `8081`): Authentication microservice registered with Eureka (`DEVSPHERE-AUTH-SERVICE`), owning user credentials (`devsphere_auth`), registration, server-side `USER` role assignment, password hashing, and atomic outbox event persistence (`outbox_events` table).
- **User Service** (`services/user-service`, Port `8082`): User profile domain microservice registered with Eureka (`DEVSPHERE-USER-SERVICE`), enforcing independent Spring Security JWT validation, method-level security (`@PreAuthorize`), resource ownership checks, and graceful Redis cache-to-MySQL fallback.
- **Apache Kafka**: Message broker enabling eventual-consistent asynchronous communication between microservices with DLT routing.
- **Redis**: Distributed cache store providing high-performance, demand-driven caching for `User Service` profile reads.
- **Transactional Outbox Pattern**: Atomic database persistence of business entity and event records in `Auth Service`.

---

## Architecture Diagram

```
                         ┌─────────────────────┐
                         │    OTLP Collector   │
                         │      :4317/:4318    │
                         └──────────┬──────────┘
                                    │
                                  traces
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
 API Gateway                  Auth Service                User Service
        │                           │                           │
        │                           │                           ├── Redis
        │                           │                           └── MySQL
        │                           │
        └────────────── W3C Trace Context ─────────────────────┘
                                    │
                                    ▼
                                  Kafka
                                    │
                                    ▼
                              User Service

Metrics:  Services ──► Prometheus (/actuator/prometheus)
Logs:     Services ──► Application Logs [traceId-spanId MDC Correlation]
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

---

## License

This project is licensed under the [MIT License](LICENSE).

