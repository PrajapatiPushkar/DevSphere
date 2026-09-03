# 62. Kubernetes Configuration Externalization and Secret Protection Strategy

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `k8s/`
* **Date**: 2026-09-02

---

## Context

Following Kubernetes networking implementation (Lesson 63), DevSphere required a formal, production-grade strategy for managing application configuration and sensitive secrets in Kubernetes. The system needed a clear boundary separation between non-sensitive environment properties (ConfigMaps), sensitive credentials (Secrets), application-level properties (Config Server), and future external secret managers (HashiCorp Vault / External Secrets Operator).

---

## Decision

1. **ConfigMap Strategy (`devsphere-configmap`)**:
   - Manages non-sensitive, environment-specific infrastructure parameters (`SPRING_PROFILES_ACTIVE`, cluster DNS endpoints, Redis/Kafka/MySQL host & port mappings, rate limiting toggles).
   - Strictly prohibits storing passwords, JWT secrets, or tokens in ConfigMaps.

2. **Secret Strategy (`devsphere-secrets`)**:
   - Manages sensitive credentials (`MYSQL_PASSWORD`, `SPRING_REDIS_PASSWORD`, `JWT_SECRET`, Kafka SASL credentials).
   - Real credentials are strictly excluded from Git tracking via `.gitignore`. Standard template placeholders (`secrets.example.yaml`) with dummy values (`CHANGE_ME`) are provided for reference and testing.

3. **Deployment Environment Injection**:
   - Containers inject configuration using standard Kubernetes `envFrom` references (`configMapRef` and `secretRef` with `optional: true` fallback support).
   - Injected properties map dynamically to Spring Boot `${PROPERTY_NAME}` properties without altering microservice Java code.

4. **Configuration Hierarchy & Precedence Boundary**:
   - Infrastructure addressing (DNS, DB hosts, ports) is governed by Kubernetes ConfigMaps/Secrets.
   - Microservice application-level properties are served dynamically by Spring Cloud Config Server.
   - Kubernetes environment variables take priority over default `application.yml` defaults while respecting Config Server overrides.

5. **Production Secret Lifecycle Roadmap**:
   - Documented production roadmap utilizing HashiCorp Vault / External Secrets Operator to dynamically sync secrets directly into Kubernetes Secrets without manual YAML creation.

---

## Consequences

* **Positive**:
  - Eliminates plaintext production credentials in Git and container images.
  - Standardizes property injection across all 5 microservices (`config-server`, `service-discovery`, `auth-service`, `user-service`, `api-gateway`).
  - Maintains strict separation between infrastructure environment configuration and application domain properties.
* **Trade-offs / Future Scope**:
  - Live production secrets require external secret management tooling (External Secrets Operator / Vault) for automated rotation.
