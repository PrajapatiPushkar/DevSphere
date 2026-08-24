# Centralized Configuration Management Architecture

This document describes the centralized configuration architecture introduced in **Lesson 13** of the DevSphere platform using **Spring Cloud Config Server**.

---

## 1. Overview & Rationale

Prior to Lesson 13, non-secret runtime configurations were embedded directly within each microservice's `src/main/resources/application.yml`. As the system scales with additional services, this approach creates several challenges:
- **Duplication**: Shared settings (e.g., Eureka server URLs, Kafka bootstrap brokers, Actuator settings) are duplicated across multiple repositories/directories.
- **Tightly Coupled Configuration**: Modifying runtime options requires recompiling or redeploying microservice artifacts.
- **Lack of Version History**: Configuration changes are tied to application code commits, making config-only auditing difficult.

### Objectives
- Centralize all non-secret runtime configuration into a dedicated Git repository (`config-repo/`).
- Serve configuration dynamically via a standalone **Spring Cloud Config Server** (`services/config-server`) on port `8888`.
- Preserve clear separation of concerns:
  - **Spring Cloud Config**: Configuration server (`:8888`)
  - **Netflix Eureka**: Service discovery server (`:8761`)
- Maintain strict secret security and isolated unit testing.

---

## 2. Target Architecture

```
                         ┌──────────────────────┐
                         │    Config Git Repo   │
                         │   (config-repo)      │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Config Server     │
                         │        :8888         │
                         └──────────┬───────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
        API Gateway            Auth Service          User Service
              │                     │                     │
              └─────────────────────┼─────────────────────┘
                                    │
                                    ▼
                              Eureka :8761
                                    │
                         Service Discovery
```

---

## 3. Git-Backed Configuration Repository (`config-repo/`)

The configuration source is externalized into `config-repo/` at the root of the project workspace.

### File Hierarchy
```
config-repo/
├── application.yml        # System-wide defaults (Eureka zone, Kafka brokers, Actuator)
├── api-gateway.yml        # API Gateway routes, port 8080, application name
├── auth-service.yml       # Auth Service DB URL, JPA, Outbox polling, Kafka producer, JWT expiration
├── user-service.yml       # User Service DB URL, JPA, Kafka consumer, Redis host/port, Cache TTL
└── service-discovery.yml # Eureka Service Discovery server settings
```

### Git Repository Versioning
The `config-repo/` directory is maintained as a distinct local Git repository:
- **Version Control**: Every configuration change is committed with a clear audit history.
- **Rollback Capability**: Reverting configuration changes requires rolling back a Git commit in `config-repo`.
- **Reviewability**: Configuration changes undergo PR/commit reviews independently of code changes.

---

## 4. Configuration Classification & Secret Policy

> [!CAUTION]
> **CRITICAL SECRET MANAGEMENT RULE**
> 
> Git-backed configuration **does NOT** store plain-text secrets. 
> 
> The following sensitive credentials are **strictly prohibited** in `config-repo`:
> - JWT signing secrets (`JWT_SECRET`)
> - Database passwords (`DB_PASSWORD`)
> - Kafka SASL credentials (`KAFKA_PASSWORD`)
> - Redis passwords (`REDIS_PASSWORD`)
> - Private keys & TLS certificates

### Handled Configuration Types

| Category | Location | Examples |
| :--- | :--- | :--- |
| **Shared Non-Secrets** | `config-repo/application.yml` | Eureka URL, Kafka brokers, Actuator exposure |
| **Service Non-Secrets** | `config-repo/<service-name>.yml` | Gateway routes, server ports, cache TTLs, retry backoffs |
| **Secret Credentials** | Local environment / Secret Manager | `JWT_SECRET`, `DB_PASSWORD`, `REDIS_PASSWORD` |

---

## 5. Configuration Precedence

Spring Boot evaluates configuration in a defined order of precedence (highest to lowest):

1. **Command-line arguments** (`--server.port=9000`)
2. **OS Environment Variables** (`DB_PASSWORD=secret`)
3. **Config Server application-specific properties** (`config-repo/auth-service.yml`)
4. **Config Server profile-specific properties** (`config-repo/application-dev.yml`)
5. **Config Server shared properties** (`config-repo/application.yml`)
6. **Application local resources** (`src/main/resources/application.yml`)

This precedence guarantees that environment variables and runtime CLI flags can safely override centralized values during deployment without modifying `config-repo`.

---

## 6. Microservice Startup Sequence

To ensure seamless system boot:

1. **Start Config Server** (`services/config-server` on port `8888`)
   - Reads `config-repo/` configuration files.
2. **Start Eureka Server** (`services/service-discovery` on port `8761`)
   - Loads its configuration from Config Server.
3. **Start Core Microservices**:
   - `Auth Service` (`services/auth-service` on port `8081`)
   - `User Service` (`services/user-service` on port `8082`)
   - `API Gateway` (`services/api-gateway` on port `8080`)
   - Microservices pull non-secret parameters from Config Server and register with Eureka.

---

## 7. Config Server Failure & Test Isolation

### Production Failure Behavior
If Config Server is unavailable at startup for a production microservice:
- Service startup **fails fast** with `ConfigClientFailFastException`.
- Prevents microservices from running with missing or stale configuration.

### Test Isolation
To prevent unit and integration tests from requiring a running Config Server:
- Test configurations (`src/test/resources/application-test.yml`) configure `spring.config.import: ""` or `spring.cloud.config.enabled: false`.
- Isolated test profiles allow `mvn test` to execute fast and independently offline.

---

## 8. Limitations & Future Roadmap

- **Configuration Refresh**: In Lesson 13, configuration is read at startup. Dynamic refresh (`/actuator/refresh` or Spring Cloud Bus) is intentionally deferred to future lessons.
- **Secrets Management**: Plain text secrets are parameterized via environment variables. Integration with Vault or Kubernetes Secrets will be introduced in future lessons.
