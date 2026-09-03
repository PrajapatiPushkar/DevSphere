# 67. Continuous Deployment Pipeline and Automated Kubernetes Rollout

* **Status**: Accepted
* **Impacted Components**: `api-gateway`, `auth-service`, `user-service`, `config-server`, `service-discovery`, `.github/workflows/`, `k8s/`
* **Date**: 2026-09-03

---

## Context

Following CI foundation implementation (Lesson 68), DevSphere required a production-oriented Continuous Deployment (CD) pipeline. The platform needed an automated workflow connecting successful CI builds to GitHub Container Registry (GHCR) container image publishing, dynamic Kustomize image reference updates using immutable Git commit SHAs, secure Kubernetes cluster authentication, and automated rolling deployment status verification.

---

## Decision

1. **Pipeline Job Dependencies and Gatekeepers**:
   - Deployment runs strictly after successful CI completion (`needs: [publish-container-images, generate-release-manifest]`).
   - Prevents unverified or failing code from being published or deployed.

2. **Container Registry & Immutable Tagging**:
   - Publish multi-stage microservice container images to GitHub Container Registry (`ghcr.io/<owner>/devsphere-<service>:<git-sha>`).
   - Authenticate to GHCR using native `GITHUB_TOKEN` credentials with scoped `packages: write` permissions.
   - Enforce immutable Git commit SHA image tagging (`<git-sha>`) for complete deployment traceability.

3. **Dynamic Kustomize Image Overrides**:
   - Use `kustomize edit set image` in the deployment workflow to set image references to `ghcr.io/<owner>/devsphere-<service>:<git-sha>` dynamically at deployment time without mutating committed source YAML files.

4. **Kubernetes Cluster Authentication and Deployment Verification**:
   - Authenticate to the Kubernetes cluster using `${{ secrets.KUBE_CONFIG }}` injected dynamically into `~/.kube/config`.
   - Apply manifests via `kubectl apply -k k8s/`.
   - Verify deployment rollout success for all 5 services using `kubectl rollout status deployment/<service> -n devsphere --timeout=180s`.
   - Safely handle missing cluster secrets by performing static Kustomize validation (`kubectl kustomize k8s/`) and logging a clean notice.

5. **Reuse Production Deployment Strategy**:
   - Preserve existing `RollingUpdate` strategy (`maxUnavailable: 0`, `maxSurge: 1`), health probes (`startupProbe`, `livenessProbe`, `readinessProbe`), PDBs (`minAvailable: 1`), HPAs, and Spring Boot graceful shutdown (`server.shutdown: graceful`).

---

## Consequences

* **Positive**:
  - Automates end-to-end delivery from Git push to running Kubernetes cluster.
  - Ensures 1-to-1 traceability between Git commit SHAs, GHCR container image tags, and Kubernetes deployments.
  - Maintains zero-downtime rolling updates and explicit rollout health verification.
  - Protects credentials via GitHub Actions Secrets (`KUBE_CONFIG`) and scoped `GITHUB_TOKEN` permissions.
* **Trade-offs / Future Scope**:
  - Live deployment execution and cluster rollout status checks require a running Kubernetes cluster and configured `KUBE_CONFIG` secret.
  - Advanced deployment patterns (ArgoCD GitOps, Helm chart packaging, Canary/Blue-Green traffic routing) belong to future operations topics.
