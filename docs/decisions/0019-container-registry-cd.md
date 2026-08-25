# 19. Container Registry and Continuous Delivery Foundation

Date: 2026-08-25

## Status

Accepted

## Context

Lesson 19 established automated CI quality gates that compiled microservices, ran tests, audited dependencies, and validated Docker buildability. However, Lesson 19 intentionally stopped short of publishing container images to a registry.

To enable automated deployment in cloud and container environments, DevSphere requires a secure, automated Continuous Delivery (CD) mechanism that transforms verified source code into versioned, immutable container images stored in a central artifact registry.

## Decision

We adopt **GitHub Container Registry (GHCR)** (`ghcr.io`) as the authoritative container image registry for DevSphere microservices and establish an automated CD pipeline ([`.github/workflows/cd.yml`](file:///.github/workflows/cd.yml)).

Key architectural decisions include:

1. **Registry Integration**: Publish all microservice container images to `ghcr.io/<owner>/devsphere-<service>`.
2. **Publishing Triggers & PR Protection**: Restrict image publishing strictly to `push` events on `main` and Git release tags (`v*`). Pull requests are explicitly forbidden from publishing images.
3. **Immutable Tagging & Digest Capture**: Tag every container image with commit SHAs (`${GITHUB_SHA}`, `sha-<short-sha>`) and semantic versions (`v1.0.0`). Capture and expose cryptographic SHA256 image digests (`sha256:...`).
4. **Least-Privilege Authentication**: Authenticate using GitHub's native ephemeral `secrets.GITHUB_TOKEN` with write access scoped exclusively to the publish job (`packages: write`).
5. **Release Manifest Artifact**: Generate a machine-readable `release-manifest.json` linking Git commit metadata to published service image URIs, tags, and cryptographic digests.
6. **Build-Once, Promote-Many**: Enforce externalization of runtime environment configurations so that a single built container image can be promoted across environments without re-compilation.

## Consequences

### Positive / Benefits
- **Centralized Deployment Artifacts**: Centralizes release binaries in a secure OCI-compliant registry.
- **Traceability & Auditability**: Enables 1:1 mapping from any running container image back to its source Git commit SHA.
- **Security & Integrity**: Ephemeral token authentication and SHA256 digest logging prevent image tampering and credential leaks.
- **Deployment Readiness**: Provides structured release manifests formatted for future Kubernetes and GitOps rollouts.
- **Zero External Credentials**: Reuses native GitHub infrastructure without requiring external Docker Hub or cloud provider secrets.

### Negative / Tradeoffs
- **Registry Coupling**: Introduces dependency on GitHub Container Registry availability.
- **Package Lifecycle**: Requires establishing registry retention and cleanup policies over time to manage storage usage.

## Future Work

- **Kubernetes CD & GitOps**: Create Kubernetes manifests / Helm charts consuming `release-manifest.json` digests with ArgoCD synchronization.
- **Image Signing & Attestation**: Integrate Cosign keyless signing and SLSA provenance verification.
- **Container Registry Scanning**: Enable automated vulnerability scanning on stored GHCR packages.
