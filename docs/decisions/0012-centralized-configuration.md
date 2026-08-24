# 12. Centralized Configuration with Spring Cloud Config

- **Status**: Accepted
- **Date**: 2026-08-24
- **Deciders**: DevSphere Core Engineering Team

---

## Context

In previous lessons, non-secret application properties (such as Gateway routes, Eureka zone URLs, Kafka serialization settings, cache TTLs, and database URL formats) were maintained in individual `application.yml` files inside each microservice repository.

As DevSphere grows across multiple microservices (`api-gateway`, `auth-service`, `user-service`, `service-discovery`), managing distributed `application.yml` files creates configuration duplication, drift across environments, and requires code rebuilds for runtime configuration adjustments.

---

## Decision

We adopt **Spring Cloud Config Server** for centralized non-secret configuration management backed by a local Git repository (`config-repo/`).

### Key Implementation Choices:
1. **Dedicated Config Server (`services/config-server`)**:
   - Runs on port `8888` using Spring Boot 3.2.5 and Spring Cloud 2023.0.1.
   - Annotated with `@EnableConfigServer`.
2. **Git-Backed Configuration (`config-repo/`)**:
   - Stores shared (`application.yml`) and service-specific (`api-gateway.yml`, `auth-service.yml`, `user-service.yml`, `service-discovery.yml`) files.
   - Managed as an independent local Git repository for version control, audit trails, and rollback capability.
3. **Config Data Import**:
   - Config clients (`api-gateway`, `auth-service`, `user-service`, `service-discovery`) import centralized configuration using Spring Boot's modern Config Data API: `spring.config.import=configserver:http://localhost:8888`.
4. **Strict Secret Policy**:
   - Sensitive credentials (JWT secrets, database passwords, Redis passwords) remain outside Git-backed configuration and are supplied via environment variables (`${JWT_SECRET}`, `${DB_PASSWORD}`).
5. **Clear Separation of Concerns**:
   - Config Server handles **Configuration** (`http://localhost:8888`).
   - Eureka handles **Service Discovery** (`http://localhost:8761`).
   - Config Server is accessed directly by HTTP URL during bootstrap rather than via Eureka discovery to eliminate circular startup dependencies.
6. **Isolated Test Execution**:
   - `application-test.yml` across all services isolates unit/integration tests from live Config Server instances.

---

## Consequences

### Positive
- **Centralized Management**: Shared defaults (Eureka, Kafka, Actuator) are maintained in one location.
- **Auditability & Version History**: Configuration changes are tracked via Git commits in `config-repo`.
- **Environment Flexibility**: Runtime precedence allows environment variables and CLI parameters to override centralized defaults when necessary.
- **Service Decoupling**: Application code and deployment artifacts are decoupled from non-secret runtime tuning parameters.

### Negative / Tradeoffs
- **Infrastructure Dependency**: Microservices require Config Server to be available during startup.
- **Startup Ordering**: Config Server must start before other platform microservices in production.
- **Separate Secret Management**: Git-backed configuration cannot handle secrets directly; integration with Vault or Secret Managers is required for production secrets.
