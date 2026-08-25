# CI/CD Pipeline & Quality Gates Architecture

## 1. CI Purpose
The DevSphere Continuous Integration (CI) pipeline automates quality verification for every code submission to the repository. In a production-grade multi-service architecture, manual build testing is error-prone and unscalable. The CI system ensures that every pull request or branch merge satisfies mandatory quality, compilation, security, and build integrity gates before code is eligible for production deployment.

---

## 2. GitHub Actions Platform
DevSphere uses **GitHub Actions** as its native CI orchestration platform. GitHub Actions provides declarative workflow configurations ([`.github/workflows/ci.yml`](file:///.github/workflows/ci.yml)), native secret protection, containerized runner environments (`ubuntu-latest`), parallel matrix execution, and seamless integration with pull requests.

Workflows execute under least-privilege access permissions:
```yaml
permissions:
  contents: read
```

---

## 3. Maven Matrix Strategy
DevSphere is structured as a multi-repository/multi-service system where services reside in separate directories under `services/` without a single root parent POM.

To ensure fast feedback, service isolation, and parallel execution, the build job utilizes a matrix strategy:

```yaml
strategy:
  fail-fast: false
  matrix:
    service:
      - api-gateway
      - auth-service
      - user-service
      - service-discovery
      - config-server
```

### Benefits of Matrix Strategy:
- **Parallel Execution**: All five microservices build concurrently in isolated virtual environments.
- **Defect Isolation**: Build or test failures in one service are pinpointed instantly without obscuring other services.
- **Scalability**: Adding new microservices requires adding a single matrix item.

---

## 4. Java 21 Runtime Environment
All microservices in DevSphere target **Java 21 LTS**. The CI workflow uses `actions/setup-java@v4` with the `temurin` OpenJDK distribution:

```yaml
- name: Set up JDK 21 (Temurin)
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: 'maven'
```

Silently downgrading to Java 17 or upgrading to unratified versions is prohibited across all `pom.xml` files.

---

## 5. Test Strategy & Lifecycle
CI execution enforces the complete Maven verification lifecycle:
```bash
mvn clean verify -B
```

### Key Policy:
- **No Test Skipping**: Flags such as `-DskipTests` or `-Dmaven.test.skip=true` are strictly forbidden in CI.
- **Unit & Integration Tests**: Both Surefire unit tests and Failsafe/Spring Boot integration tests (with embedded Kafka, H2, and mock Redis fallbacks) are executed.

---

## 6. Dependency Caching
To minimize build duration and reduce external HTTP calls to Maven Central, dependency caching is enabled at the setup-java step.
- **Cached Path**: `~/.m2/repository`
- **Excluded Path**: `target/` directories remain ephemeral and disposable.

---

## 7. Security Scanning
The CI pipeline incorporates automated dependency vulnerability auditing using **OWASP Dependency-Check**:
- Scans external libraries against the National Vulnerability Database (NVD).
- Configured to flag high/critical severity vulnerabilities (CVSS $\ge 8.0$).
- Ensures early detection of vulnerable transitive dependencies.

---

## 8. Docker Build Validation
Each microservice includes a multi-stage, production-oriented `Dockerfile`:
- **Stage 1 (Builder)**: `maven:3.9.6-eclipse-temurin-21-alpine` compiles the application JAR.
- **Stage 2 (Runtime)**: `eclipse-temurin:21-jre-alpine` provides a minimal runtime footprint.
- **Container Security**: Executes under a dedicated non-root user (`devsphere`), exposing only designated service ports (`8080`, `8081`, `8082`, `8761`, `8888`).
- **Tagging**: Images are tagged immutably using `${GITHUB_SHA}` (`devsphere/<service>:${GITHUB_SHA}`).
- **No Push**: Docker images are validated locally within CI runners and are **not** pushed to external registries in Lesson 19.

---

## 9. Artifact Handling
The pipeline generates and preserves two classes of artifacts:
1. **Test Reports**: Surefire/Failsafe XML/HTML test reports uploaded on completion or failure (`retention-days: 14`).
2. **Application Binaries**: Runnable Spring Boot executable JARs (`target/*.jar`) uploaded on build success (`retention-days: 7`).

> [!CAUTION]
> Artifacts must never expose `.env` files, JWT private keys, or raw credentials.

---

## 10. Pull Request Quality Gates
A pull request targeting `main` cannot be merged unless all quality gates pass:
1. Repository secret protection & `target/` file check: **PASSED**
2. POM Java 21 compliance: **PASSED**
3. Matrix compilation and unit/integration tests for all 5 services: **PASSED**
4. Dependency vulnerability scan: **PASSED**
5. Docker build validation for all 5 services: **PASSED**

---

## 11. Recommended Branch Protection Rules
While GitHub Action workflows cannot modify repository settings directly, the following branch protection rules are recommended for the `main` branch:

- **Require a pull request before merging** (minimum 1 review).
- **Require status checks to pass before merging**:
  - `Repository & Secret Protection Check`
  - `Build & Test (api-gateway)`
  - `Build & Test (auth-service)`
  - `Build & Test (user-service)`
  - `Build & Test (service-discovery)`
  - `Build & Test (config-server)`
  - `Dependency Vulnerability Audit`
  - `Docker Build Validation (api-gateway)`
  - `Docker Build Validation (auth-service)`
  - `Docker Build Validation (user-service)`
  - `Docker Build Validation (service-discovery)`
  - `Docker Build Validation (config-server)`
- **Require branches to be up to date before merging**.
- **Block force pushes** and deletion of `main`.

---

## 12. Secrets & Credentials Policy
- **No Hardcoded Secrets**: DB passwords, JWT signing keys, Redis passwords, or Kafka credentials must never be committed to Git or embedded in Dockerfiles.
- **Environment Injections**: Configuration properties consume environment variables with safe dev defaults for tests.
- **Config Repository**: `config-repo/` files consume placeholder variables (`${JWT_SECRET:...}`) rather than raw production secrets.

---

## 13. Failure Handling & Debugging
When a CI job fails:
1. Inspect the matrix runner log in GitHub Actions UI.
2. Download the uploaded `test-reports-<service>` artifact to inspect exact test failure stack traces.
3. Run the local verification command to reproduce failure:
   ```powershell
   cd services/<failing-service>
   mvn clean verify
   ```

---

## 14. Future Continuous Delivery (CD) Strategy
Future lessons will extend this CI foundation into automated Continuous Delivery (CD):
- **Container Registry Push**: Pushing signed Docker images to Amazon ECR / GitHub Container Registry (GHCR).
- **GitOps Deployment**: Triggering automated deployments to Kubernetes clusters via ArgoCD / FluxCD.
- **Environment Promotion**: Automated promotion across `staging`, `pre-prod`, and `production` environments with smoke tests and canary rollouts.
