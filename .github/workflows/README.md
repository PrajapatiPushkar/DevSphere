# DevSphere CI/CD Workflows

This directory contains the GitHub Actions workflows and quality gates for the **DevSphere** microservices platform.

---

## Workflow Overview

| Workflow | File | Description | Triggers |
| :--- | :--- | :--- | :--- |
| **DevSphere Production CI/CD Pipeline** | [`ci.yml`](file:///.github/workflows/ci.yml) | Validates code compilation, unit/integration tests, dependency security, repository secret protection, and multi-stage Docker image builds. | `push` to `main`, `pull_request` to `main` |

---

## Pipeline Architecture & Stages

```
                  +-----------------------------------+
                  |           GitHub Trigger          |
                  | (push: main / pull_request: main) |
                  +-----------------+-----------------+
                                    |
                  +-----------------v-----------------+
                  |   Repository & Secret Protection  |
                  |  - Target directory check         |
                  |  - Secret key scan (.env, .pem)   |
                  |  - Config-repo inspection         |
                  |  - Java 21 POM verification       |
                  +--------+-----------------+--------+
                           |                 |
         +-----------------+                 +-----------------+
         |                                                     |
+--------v-------------------------+         +-----------------v------------------+
| Service Matrix Build & Test      |         | Dependency Vulnerability Audit     |
| - api-gateway                    |         | - OWASP Dependency-Check           |
| - auth-service                   |         | - CVSS threshold verification      |
| - user-service                   |         +------------------------------------+
| - service-discovery              |
| - config-server                  |
| - mvn clean verify               |
| - Test Report Artifact Upload    |
| - Jar Build Artifact Upload      |
+--------+-------------------------+
         |
+--------v-------------------------+
| Docker Build Validation          |
| - Multi-stage image build        |
| - Tag: devsphere/<service>:${SHA}|
| - push: false (validation only)  |
+----------------------------------+
```

---

## Matrix Strategy

To achieve isolated, parallel execution and clear defect isolation across microservices, the build job executes over a service matrix:

- `services/api-gateway`
- `services/auth-service`
- `services/user-service`
- `services/service-discovery`
- `services/config-server`

Each matrix runner executes `mvn clean verify` independently using **Java 21 (Temurin)** and GitHub Actions Maven dependency caching (`~/.m2/repository`).

---

## Quality Gates & Verification Policies

1. **Compilation & Packaging**: Every microservice must compile without warnings/errors and produce a bootable Spring Boot JAR.
2. **Automated Testing**: 100% of unit and integration tests must pass. Skipping tests (`-DskipTests`) is strictly forbidden in CI.
3. **Secret Protection**: Scans workspace for unencrypted secrets, `.env` files, private `.pem`/`.key` files, or committed `target/` directories.
4. **Dependency Security**: Scans dependencies with OWASP Dependency-Check to flag CVSS high/critical vulnerabilities.
5. **Docker Image Buildability**: Validates multi-stage Dockerfile compilation for each microservice using non-root user execution (`devsphere`). Docker images are **not** pushed to external registries in Lesson 19.
6. **Artifact Retention**: Test reports (`surefire-reports`, `failsafe-reports`) are retained for 14 days; successful build JARs are retained for 7 days.
