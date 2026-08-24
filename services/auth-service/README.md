# DevSphere Auth Service

## Purpose
The Auth Service is the dedicated microservice responsible for managing user identity credentials, secure password hashing, user registration, stateless JWT authentication, and transactional outbox event publication for the DevSphere platform.

---

## Current Status
> **Production Authorization & Role-Based Access Control (Lesson 15)**  
> The Auth Service assigns server-controlled roles (`USER`, `ADMIN`), prevents public self-registration as `ADMIN`, incorporates role authorization claims (`roles: ["USER"]`) into signed JWT tokens, safely seeds an initial local development admin user (`admin@devsphere.local`), exposes Prometheus application metrics (`/actuator/prometheus`), registers as a Eureka client (`DEVSPHERE-AUTH-SERVICE`), and publishes outbox events atomically.

---

## Authorization & Role Model
- **USER Role**: Default role assigned server-side to all publicly registered users.
- **ADMIN Role**: Assigned to administrative accounts via controlled initializer (`AdminUserInitializer` for local dev/testing).
- **JWT Claims**: Tokens include `sub` (userId), `email`, and `roles` (`["USER"]` or `["ADMIN"]`). Tokens NEVER contain passwords, password hashes, or secrets.

---

## Observability & Custom Metrics
- **Prometheus Metrics Endpoint**: `/actuator/prometheus`
- **Custom Business Metrics**:
  - `devsphere_auth_registration_total{status="success|failure"}`
  - `devsphere_auth_login_total{status="success|failure"}`
  - `devsphere_auth_authorization_denied_total{reason="unauthenticated|forbidden"}`
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
