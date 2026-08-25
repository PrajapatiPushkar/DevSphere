# Container Registry & Continuous Delivery Architecture

## 1. CI vs CD Responsibilities
In DevSphere's production architecture, Continuous Integration (CI) and Continuous Delivery (CD) have distinct responsibilities:

- **CI (`ci.yml`)**: Responsible for quality verification — code compilation, unit/integration testing, dependency vulnerability auditing, secret scanning, and Dockerfile buildability validation. It **never** pushes images.
- **CD (`cd.yml`)**: Responsible for artifact generation — compiling production JARs, building container images, tagging them immutably, publishing them to GitHub Container Registry (GHCR), capturing cryptographic digests, and producing machine-readable release manifests.

---

## 2. GitHub Container Registry (GHCR)
DevSphere uses **GitHub Container Registry (GHCR)** (`ghcr.io`) as its central, immutable container image artifact registry. 

### Key Registry Features:
- Native integration with GitHub Actions authentication via `secrets.GITHUB_TOKEN`.
- Scoped package permissions (`packages: write`).
- Fine-grained package access control and OCI container image artifact storage.

---

## 3. Image Naming Convention
Image repositories follow a standardized lowercase naming scheme:

```
ghcr.io/<repository-owner>/devsphere-api-gateway
ghcr.io/<repository-owner>/devsphere-auth-service
ghcr.io/<repository-owner>/devsphere-user-service
ghcr.io/<repository-owner>/devsphere-service-discovery
ghcr.io/<repository-owner>/devsphere-config-server
```

The repository owner is dynamically evaluated at runtime (`github.repository_owner`), preventing hardcoded organization or user dependencies.

---

## 4. Image Tagging Strategy
Every published container image carries immutable release tags:

1. **Short Commit SHA**: `sha-<short-sha>` (e.g. `sha-7ab074f`)
2. **Full Commit SHA**: `${GITHUB_SHA}` (e.g. `7ab074f0a...`)
3. **Semantic Version**: `v1.0.0`, `1.0.0`, `1.0` (triggered upon pushing Git tags `v*`).

> [!IMPORTANT]
> The mutable tag `latest` is **not** used as a primary deployment identifier. Deployments target explicit SHA or SemVer tags to guarantee exact code identity.

---

## 5. SHA Identity & Traceability
The commit SHA acts as the primary bridge between source code state and container artifact identity. Any container running in a staging or production cluster can be traced back to its exact Git commit history.

---

## 6. Semantic Versioning Policy
Release versions adhere to Semantic Versioning (`MAJOR.MINOR.PATCH`):
- Pushing a tag `git tag v1.0.0 && git push origin v1.0.0` triggers CD compilation and publishes immutable release images tagged `v1.0.0`, `1.0.0`, `1.0`, and `sha-<short-sha>`.
- Overwriting existing version tags is prohibited by workflow immutability rules.

---

## 7. Cryptographic Image Digest (`sha256:...`)
While tags provide human-readable references, the **Image Digest** (e.g. `sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`) represents immutable cryptographic proof of container content. Production deployment orchestrators consume digests to prevent tag-tampering attacks.

---

## 8. Build-Once, Promote-Many Philosophy
DevSphere enforces a strict **Build-Once, Promote-Many** architecture:
- A single container image is compiled and published during CD execution for a given commit SHA.
- The **exact same** container image digest is promoted sequentially across `development`, `staging`, and `production` environments.
- Images are **never** re-compiled per environment.

---

## 9. Environment Separation
Container images are environment-agnostic. Differences between `staging` and `production` environments are managed strictly through external runtime environment variables or Kubernetes ConfigMaps/Secrets, **never** by creating separate Docker images (`image-staging`, `image-prod`).

---

## 10. Externalized Configuration
Application settings consume external configurations:
- Spring Cloud Config Server URL (`CONFIG_SERVER_URL`)
- Eureka Service Discovery URL (`EUREKA_SERVER_URL`)
- Database Connection Strings & Redis Hosts (`SPRING_REDIS_HOST`)
- Environment-specific flags

No environment endpoints or static hostnames are hardcoded inside container images.

---

## 11. Secrets Isolation Policy
Docker images, image labels, and build logs must **never** contain sensitive credentials:
- No database passwords, JWT secrets, or private keys in `Dockerfile` or `ENV` instructions.
- Passwords and keys are supplied at runtime via secret managers or secure environment injections.

---

## 12. Container Image Security
All service Dockerfiles maintain Lesson 19 container hardening guidelines:
- **Multi-stage compilation**: Discarding build toolchains (Maven/JDK) in favor of lightweight Alpine JRE runtime images.
- **Non-root execution**: Running under dedicated system user (`USER devsphere`).
- **Minimal port exposure**: Exposing only service-specific ports (`8080`, `8081`, `8082`, `8761`, `8888`).

---

## 13. OCI Metadata & Build Provenance
Builds utilize `docker/metadata-action` and Buildx provenance generation (`provenance: true`, `sbom: true`) to attach standard Open Container Initiative (OCI) annotations:
- `org.opencontainers.image.source`
- `org.opencontainers.image.revision`
- `org.opencontainers.image.version`
- `org.opencontainers.image.created`

---

## 14. Machine-Readable Release Manifest (`release-manifest.json`)
The CD pipeline automatically generates an aggregated `release-manifest.json` workflow artifact upon successful image publication:

```json
{
  "commit": "7ab074f0a...",
  "ref": "refs/heads/main",
  "timestamp": "2026-08-25T06:58:00Z",
  "images": [
    {
      "service": "api-gateway",
      "image": "ghcr.io/prajapatipushkar/devsphere-api-gateway",
      "tag": "7ab074f0a...",
      "digest": "sha256:a1b2c3d4..."
    },
    {
      "service": "auth-service",
      "image": "ghcr.io/prajapatipushkar/devsphere-auth-service",
      "tag": "7ab074f0a...",
      "digest": "sha256:e5f6g7h8..."
    }
  ]
}
```

This manifest serves as the formal input artifact for downstream deployment systems.

---

## 15. Conceptual Rollback Strategy
Because every published release tag and digest is immutable, rolling back a deployment requires no code re-compilation. If version `v1.1.0` exhibits runtime defects, deployment orchestrators simply update environment references back to the manifest digest of `v1.0.0`.

---

## 16. Future Deployment Roadmap
Lesson 20 establishes container registry delivery artifacts. Future lessons will implement:
- **Kubernetes Manifests & Helm Charts**: Declarative deployment specs consuming `release-manifest.json` digests.
- **GitOps Orchestration**: ArgoCD / FluxCD automated cluster synchronization.
- **Supply Chain Attestation**: Cosign image signing and Kyverno policy enforcement.
