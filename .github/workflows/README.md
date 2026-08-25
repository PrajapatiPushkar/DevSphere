# DevSphere CI/CD Workflows

This directory contains the GitHub Actions workflows, quality gates, and Continuous Delivery pipelines for the **DevSphere** microservices platform.

---

## Workflow Overview

| Workflow | File | Purpose | Triggers | Artifacts Generated |
| :--- | :--- | :--- | :--- | :--- |
| **Production CI Pipeline** | [`ci.yml`](file:///.github/workflows/ci.yml) | Validates code compilation, unit/integration tests, dependency security, secret protection, and Docker buildability. | `push` to `main`, `pull_request` to `main` | Test Reports (`surefire-reports`, `failsafe-reports`), JAR Binaries |
| **Container CD Pipeline** | [`cd.yml`](file:///.github/workflows/cd.yml) | Compiles, builds, and publishes immutable container images to GitHub Container Registry (GHCR) with digest capture. | `push` to `main`, `push` of tags `v*` | Service Image Metadata, `release-manifest.json` |

---

## CI vs CD Architecture

```
                                    +-----------------------------------+
                                    |           GitHub Trigger          |
                                    +-----------------+-----------------+
                                                      |
                                                      v
                   +----------------------------------+----------------------------------+
                   |                                                                     |
         [Pull Request Trigger]                                                [Push to main / Tag v*]
                   |                                                                     |
                   v                                                                     v
    +------------------------------+                                      +------------------------------+
    |   ci.yml (Quality Gates)     |                                      |   cd.yml (Delivery Pipeline) |
    | - Repository & Secret Scan   |                                      | - Authenticate GHCR          |
    | - Java 21 POM Verification   |                                      | - Compile Service JAR        |
    | - Maven Matrix Verify        |                                      | - Build Multi-Stage Image    |
    | - OWASP Dependency Audit     |                                      | - Tag Immutable SHA / SemVer |
    | - Docker Build Validation    |                                      | - Push to ghcr.io/<owner>/   |
    |   (push: false)              |                                      | - Capture Image Digest       |
    +------------------------------+                                      | - Generate Release Manifest  |
                                                                          +------------------------------+
```

---

## GHCR Registry Naming & Tagging Policy

Container images are published to **GitHub Container Registry (GHCR)** at `ghcr.io`:

```
ghcr.io/<owner>/devsphere-api-gateway
ghcr.io/<owner>/devsphere-auth-service
ghcr.io/<owner>/devsphere-user-service
ghcr.io/<owner>/devsphere-service-discovery
ghcr.io/<owner>/devsphere-config-server
```

### Tagging Conventions:
- **Git Commit SHA (Primary)**: `${GITHUB_SHA}` and `sha-<short-sha>` (e.g. `ghcr.io/prajapatipushkar/devsphere-auth-service:sha-7ab074f`).
- **Semantic Version**: `v1.0.0`, `1.0.0`, `1.0` when a Git release tag matching `v*` is pushed.
- **Mutable `latest` Tag**: Excluded as a primary release tag to guarantee deployment immutability.

---

## Security & Publishing Policy

1. **Pull Request Safety**: Pull requests **never** publish images to GHCR.
2. **Least Privilege**: The publish workflow uses minimal required scopes:
   ```yaml
   permissions:
     contents: read
     packages: write
   ```
3. **Authentication**: Authenticates securely using ephemeral `GITHUB_TOKEN` (`secrets.GITHUB_TOKEN`).
4. **Machine-Readable Release Manifest**: Generates an aggregated `release-manifest.json` linking Git commit SHA, git ref, timestamp, image URIs, tags, and cryptographic SHA256 digests (`sha256:...`).
