# DevSphere Auth Service

## Purpose
The Auth Service is the dedicated microservice responsible for managing user identity credentials, secure password hashing, user registration, stateless JWT authentication, and transactional outbox event publication for the DevSphere platform.

---

## Current Status
> **Observability Foundation, Centralized Config, Service Discovery & Outbox Pattern (Lesson 14)**  
> The Auth Service exposes Prometheus application and business metrics (`/actuator/prometheus`), consumes non-secret centralized configuration from Spring Cloud Config Server (`http://localhost:8888`), registers as a Eureka client (`DEVSPHERE-AUTH-SERVICE`), and manages user registration (`POST /api/v1/auth/register`) by persisting credentials (`users`) and domain events (`outbox_events`) atomically.

---

## Observability & Custom Metrics
- **Prometheus Metrics Endpoint**: `/actuator/prometheus`
- **Custom Business Metrics**:
  - `devsphere_auth_registration_total{status="success|failure"}`
  - `devsphere_auth_login_total{status="success|failure"}`
  - `devsphere_outbox_events_published_total{event_type="UserRegisteredEvent",status="success|failed"}`
  - `devsphere_outbox_publish_failures_total{event_type="UserRegisteredEvent"}`

---

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Observability**: Spring Boot Actuator, Micrometer Prometheus (`micrometer-registry-prometheus`)
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
