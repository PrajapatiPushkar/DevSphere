# 18. Production CI/CD Pipeline and Quality Gates

Date: 2026-08-25

## Status

Accepted

## Context

DevSphere is a production-grade microservices platform consisting of five independent Spring Boot microservices (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`).

Prior to Lesson 19, verification relied on manual local Maven executions (`mvn clean verify`). As microservices expand, relying on developer discipline for build validation, test execution, dependency vulnerability checking, and Docker image validation introduces operational risk.

We require an automated, repeatable Continuous Integration (CI) foundation and quality gate system that enforces strict validation rules on every pull request and branch commit before code can be merged into `main`.

## Decision

We adopt **GitHub Actions** as the primary Continuous Integration platform for DevSphere and establish an automated quality gate pipeline ([`.github/workflows/ci.yml`](file:///.github/workflows/ci.yml)).

Key architecture decisions include:

1. **Matrix Execution**: Use a matrix strategy across all five microservices (`services/*`) to run `mvn clean verify` in parallel using Java 21 (Temurin).
2. **Strict Test Enforcement**: Prohibit skipping tests (`-DskipTests`) in CI. All unit and integration tests must pass cleanly.
3. **Dependency Caching**: Cache Maven local repository dependencies (`~/.m2/repository`) across runs to optimize execution time.
4. **Repository & Secret Security Checks**: Implement repository scanning steps to detect unencrypted credentials, accidental secret files (`.env`, `*.pem`, `*.key`), and committed build output directories (`target/`).
5. **Dependency Vulnerability Audit**: Integrate OWASP Dependency-Check to detect CVSS high/critical vulnerabilities in transitive dependencies.
6. **Multi-Stage Docker Validation**: Create multi-stage, non-root `Dockerfile` configurations for each microservice and validate Docker buildability in CI using immutable tags (`devsphere/<service>:${GITHUB_SHA}`) without publishing images externally.
7. **Artifact Management**: Preserve Surefire/Failsafe test reports and successful Spring Boot executable JARs as workflow artifacts.

## Consequences

### Positive / Benefits
- **Automated Quality Gates**: Prevents broken builds, failing tests, or uncompilable services from reaching the `main` branch.
- **Parallel Speed & Feedback**: Matrix execution builds services concurrently, giving developer feedback within minutes.
- **Early Defect & Vulnerability Detection**: Identifies dependency vulnerabilities and secret leaks prior to merge.
- **Repeatability**: Standardizes build environments on Java 21 across all development and CI runners.
- **Deployment Readiness**: Validates Docker image construction for all services continuously.

### Negative / Tradeoffs
- **CI Resource Consumption**: Running parallel matrix jobs on every PR consumes GitHub Actions runner minutes.
- **Infrastructure Dependency**: Introduces dependency on GitHub infrastructure for CI availability.
- **Scanner Maintenance**: Vulnerability databases (NVD/OWASP) require maintenance to manage false positives.

## Future Work

- **Continuous Delivery (CD)**: Automate container image publishing to ECR/GHCR upon merging to `main`.
- **GitOps & Orchestration**: Automate Kubernetes deployments via ArgoCD/FluxCD with canary rollouts and health monitoring.
- **Static Analysis Expansion**: Integrate SonarQube/Checkstyle quality platform when codebase complexity warrants.
