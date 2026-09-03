# 66. Continuous Integration Foundation and Automated Quality Gates

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `.github/workflows/`
* **Date**: 2026-09-03

---

## Context

Following Kubernetes production deployment implementation (Lesson 67), DevSphere required an automated Continuous Integration (CI) foundation. The platform needed standardized Git workflow guidelines, automated Maven build and unit/integration test quality gates, reproducible multi-stage Docker container builds, immutable container image tagging policies, and secure CI artifact management.

---

## Decision

1. **Git Workflow and Branching Strategy**:
   - Enforce a structured feature-branch workflow (`main` protected branch, `feature/*`, `bugfix/*`, `hotfix/*`).
   - Require Pull Requests, automated CI status checks, and Conventional Commits commit message formats (`feat(...)`, `fix(...)`, `docs(...)`).

2. **Automated GitHub Actions CI Pipeline (`.github/workflows/ci.yml`)**:
   - Trigger automated CI runs on `push` and `pull_request` events targeting `main`.
   - Pipeline structure:
     1. `repository-security-check`: Audits repo for tracked build directories (`target/`), unencrypted secret files (`*.env`, `*.key`), and Java 21 enforcement.
     2. `service-build-and-test`: Parallel matrix job compiling and testing all 5 microservices (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`) using JDK 21 Temurin and Maven caching (`mvn clean verify -B`).
     3. `dependency-security-scan`: Scans Maven dependencies for known vulnerabilities via OWASP Dependency-Check.
     4. `docker-build-validation`: Validates multi-stage Docker container builds for all 5 services using Docker Buildx in `push: false` validation mode.

3. **Artifact Management Policy**:
   - Save Surefire/Failsafe test reports (`test-reports-${{ matrix.service }}`) on `always()` condition for build diagnostics.
   - Package compiled microservice JARs (`artifact-${{ matrix.service }}-${{ github.sha }}`) on build success.

4. **Immutable Docker Image Tagging Strategy**:
   - Container images are tagged deterministically using immutable Git commit hashes (`devsphere/<service>:<git-sha>`).
   - Explicitly avoid relying on non-deterministic `:latest` tags in deployment pipelines.

5. **CI/CD Boundary Separation**:
   - Limit scope strictly to Continuous Integration (build validation, testing, image creation).
   - Exclude automated deployment triggers, GitOps synchronization (ArgoCD/Flux), Helm migrations, and production cluster image rollouts.

---

## Consequences

* **Positive**:
  - Establishes automated quality gates preventing broken code or failing tests from entering `main`.
  - Standardizes JDK 21 build and test environments across all microservices.
  - Ensures container images build reproducibly and carry immutable Git commit tags.
  - Maintains strict repository secret security and least-privilege workflow permissions (`contents: read`).
* **Trade-offs / Future Scope**:
  - Live container registry image pushing requires registry credentials and production CD pipeline integration.
  - OWASP dependency scanning adds execution time to CI runs.
