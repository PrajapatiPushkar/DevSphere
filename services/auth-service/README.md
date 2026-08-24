# DevSphere Auth Service

## Purpose
The Auth Service is the dedicated microservice responsible for managing user identity credentials, secure password hashing, user registration, stateless JWT authentication, and transactional outbox event publication for the DevSphere platform.

---

## Current Status
> **Production Resilience & Fault Tolerance (Lesson 16)**  
> The Auth Service assigns server-controlled roles (`USER`, `ADMIN`), prevents non-idempotent write operation retry storms on registration, enforces BCrypt credential verification without retrying invalid login attempts, integrates Resilience4j configuration via Config Server, exposes Prometheus metrics (`/actuator/prometheus`), registers with Eureka (`DEVSPHERE-AUTH-SERVICE`), and publishes transactional outbox events reliably.

---

## Observability & Custom Metrics
- **Prometheus Metrics Endpoint**: `/actuator/prometheus`
- **Distributed Tracing (`devsphere-auth-service`)**: Micrometer Tracing + OpenTelemetry bridge (`micrometer-tracing-bridge-otel`), OTLP exporter (`http://localhost:4318/v1/traces`), custom business spans (`auth.registration`, `auth.login`, `outbox.publish`), log MDC correlation, and W3C trace header propagation into Kafka record headers.
- **Custom Business Metrics**:
  - `devsphere_auth_registration_total{status="success|failure"}`
  - `devsphere_auth_login_total{status="success|failure"}`
  - `devsphere_auth_authorization_denied_total{reason="unauthenticated|forbidden"}`
  - `devsphere_outbox_events_published_total{event_type="UserRegisteredEvent",status="success|failed"}`
  - `devsphere_outbox_publish_failures_total{event_type="UserRegisteredEvent"}`
  - `devsphere_resilience_fallback_total{service="auth-service",dependency="..."}`

---

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Observability**: Spring Boot Actuator, Micrometer Prometheus, Micrometer Tracing OpenTelemetry Bridge (`micrometer-tracing-bridge-otel`), OpenTelemetry OTLP Exporter (`opentelemetry-exporter-otlp`)
- **Cloud Config**: Spring Cloud Config Client (`spring-cloud-starter-config`)
- **Security & Tokens**: Spring Security Crypto, JJWT (`io.jsonwebtoken:jjwt-api:0.12.5`)
- **Persistence**: Spring Data JPA, Hibernate, MySQL
- **Database Migrations**: Flyway (`flyway-core`, `flyway-mysql`)
- **Eventing & Outbox**: Spring Kafka, Scheduled Outbox Publisher
- **Build Tool**: Maven

---

## Running Automated Tests

```bash
cd services/auth-service
mvn test
```
