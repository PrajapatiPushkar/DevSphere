# CI/CD Foundation Architecture — Lesson 68

## 1. Overview & CI/CD Strategy

This document details the Continuous Integration (CI) foundation for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`).

The objective is to establish an automated, security-hardened, multi-stage build and test pipeline that validates repository integrity, compiles Java 21 microservices, executes unit/integration test suites, builds container images, and manages build artifacts cleanly without deploying to production or mutating git history.

### CI Pipeline Stage Diagram

```text
                  Developer Git Push / PR Trigger
                                │
                                ▼
         ┌──────────────────────────────────────────────┐
         │ 1. Repository & Secret Security Check        │
         │    (Secret scan, tracked target/ check, JDK) │
         └──────────────────────┬───────────────────────┘
                                │
                                ▼
         ┌──────────────────────────────────────────────┐
         │ 2. Parallel Service Build & Test Matrix      │
         │    (api-gateway, auth, user, eureka, config) │
         │    mvn clean verify -B (JDK 21 Temurin)      │
         └──────────────────────┬───────────────────────┘
                                │
                                ├──► Upload Test Reports (Surefire/Failsafe)
                                ├──► Upload Package Artifacts (.jar)
                                │
                                ▼
         ┌──────────────────────────────────────────────┐
         │ 3. Parallel Docker Build Validation Matrix   │
         │    (Multi-stage Dockerfile build, push:false)│
         │    Tag: devsphere/<service>:<git-sha>        │
         └──────────────────────────────────────────────┘
```

---

## 2. Git Workflow

DevSphere enforces a structured, trunk-based feature-branch Git workflow:

```text
main ───────────────────────────────●───────────────────► (Production Ready)
       \                           /
        └── feature/user-cache ───● (Pull Request + CI Checks Pass)
```

### Branch Naming Conventions
* `main`: Protected production-ready baseline branch. Direct pushes prohibited; modifications require PR approval.
* `feature/<feature-name>`: Topic branches for new features (e.g. `feature/user-profile-cache`).
* `bugfix/<issue-name>`: Fixes for non-critical bugs (e.g. `bugfix/jwt-expiration-handling`).
* `hotfix/<critical-issue>`: Emergency production patches (e.g. `hotfix/cors-gateway-bypass`).

### Pull Request & Merge Policy
1. **Pull Request Mandate**: All changes must enter `main` via Pull Requests.
2. **Automated Quality Gates**: CI pipeline (`.github/workflows/ci.yml`) must run and pass 100% of security checks, Maven builds, unit/integration tests, and Docker image builds before merge.
3. **Merge Strategy**: Squash-and-merge or Rebase-and-merge preferred to maintain clean linear Git commit logs.
4. **Commit Message Convention**: Follow Conventional Commits format:
   - `feat(<scope>): description`
   - `fix(<scope>): description`
   - `docs(<scope>): description`
   - `refactor(<scope>): description`

---

## 3. GitHub Actions CI Pipeline (`.github/workflows/ci.yml`)

The CI workflow is triggered automatically on `push` and `pull_request` events targeting the `main` branch.

### 1. Stage 1: `repository-security-check`
- **Objective**: Protects repository hygiene before starting resource-heavy builds.
- **Checks**:
  - Verifies no tracked `target/` directories or build outputs exist in Git (`git ls-files`).
  - Scans for accidental committed sensitive key files (`*.env`, `*.pem`, `*.key`, `*id_rsa*`).
  - Audits `config-repo` for unencrypted hardcoded production credentials.
  - Verifies Java version property (`<java.version>21</java.version>`) across all microservice `pom.xml` files.

### 2. Stage 2: `service-build-and-test` (Matrix Job)
- **Objective**: Compiles, packages, and tests each microservice in parallel.
- **Matrix Target**: `[api-gateway, auth-service, user-service, service-discovery, config-server]`
- **JDK Configuration**: Java 21 Temurin (`actions/setup-java@v4`) with automated Maven dependency caching (`cache: 'maven'`).
- **Execution Command**:
  ```bash
  cd services/${{ matrix.service }}
  mvn clean verify -B
  ```
- **Fail-Fast Policy**: Compilation errors or test failures immediately mark the CI job as `FAILED`, blocking PR merge.

### 3. Stage 3: `dependency-security-scan`
- **Objective**: Audits microservice dependencies for known CVE vulnerabilities using OWASP Dependency-Check plugin.

### 4. Stage 4: `docker-build-validation` (Matrix Job)
- **Objective**: Validates Docker image construction for all 5 microservices without pushing to remote registries.
- **Dependency**: Requires successful completion of `service-build-and-test`.
- **Docker Builder**: Uses Docker Buildx (`docker/setup-buildx-action@v3` and `docker/build-push-action@v5`).
- **Push Policy**: `push: false` (validation-only mode).

---

## 4. Artifact Management & Lifecycle

CI artifacts are systematically generated, tagged, and archived:

```text
Source Code
    │
    ▼ (mvn clean verify)
JAR Package Artifacts (services/<service>/target/*.jar)
    │
    ▼ (actions/upload-artifact@v4)
GitHub Actions Build Artifacts (retention: 7 days)
    │
    ▼ (docker build)
Container Images (devsphere/<service>:<git-sha>)
```

### Artifact Catalog
1. **Surefire & Failsafe Test Reports**: Uploaded on `always()` execution condition (`test-reports-${{ matrix.service }}`, retention: 14 days) to assist debugging failed builds.
2. **Packaged JAR Artifacts**: Uploaded on build `success()` (`artifact-${{ matrix.service }}-${{ github.sha }}`, retention: 7 days).

---

## 5. Docker Tagging Strategy

DevSphere enforces deterministic, immutable image tagging:

```text
devsphere/<service>:<git-sha>
```
Example:
- `devsphere/api-gateway:f30cd05`
- `devsphere/auth-service:f30cd05`
- `devsphere/user-service:f30cd05`

### Tagging Best Practices
* **Immutable Commit Hashes (`<git-sha>`)**: Guarantees traceability between running container images and exact Git source commits. Prevents accidental pod overwrites during deployments.
* **`:latest` Tag Avoidance**: `:latest` tags are non-deterministic and can lead to inconsistent pod versions across cluster nodes. In CI/CD pipelines, explicit commit SHAs or release tags must always be used.

---

## 6. Security & Credential Isolation

* **Least-Privilege Permissions**: Workflow enforces `permissions: contents: read`.
* **Zero Committed Secrets**: Secrets (`MYSQL_PASSWORD`, `JWT_SECRET`, Redis/Kafka credentials) are injected exclusively via Kubernetes ConfigMaps/Secrets or CI environment secrets—never hardcoded in `pom.xml`, Dockerfiles, or workflow YAML files.
* **Registry Isolation**: Docker builds operate in `push: false` validation mode, avoiding unauthorized external image publishing.

---

## 7. CI/CD Boundary & Future Scope

```text
┌─────────────────────────────────────────────────────────────┐
│                    Lesson 68 CI Boundary                    │
│  [Code Check] ──► [Maven Build] ──► [Test] ──► [Docker]    │
└──────────────────────────────┬──────────────────────────────┘
                               │ (Artifacts & Images Ready)
┌──────────────────────────────▼──────────────────────────────┐
│                    Future CD Boundary                       │
│  [Container Registry Push] ──► [GitOps / K8s Deployment]    │
└─────────────────────────────────────────────────────────────┘
```

Lesson 68 establishes the **Continuous Integration (CI)** foundation. It explicitly excludes:
- Automatic container registry pushes
- Continuous Deployment (CD) triggers
- GitOps controllers (ArgoCD / Flux)
- Automated Kubernetes manifest mutation
