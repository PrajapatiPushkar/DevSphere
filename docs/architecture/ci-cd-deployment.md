# Continuous Deployment Architecture — Lesson 69

## 1. Overview & CD Pipeline Strategy

This document details the production-oriented Continuous Deployment (CD) pipeline architecture for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`).

Lesson 69 establishes the automated deployment flow connecting successful Continuous Integration (CI) builds to GitHub Container Registry (GHCR) image publishing, dynamic Kustomize image reference overrides using immutable Git commit SHAs, secure Kubernetes authentication via GitHub Secrets, and automated rolling deployment status verification.

### End-to-End CI/CD Pipeline Architecture

```text
                  Git Push (main branch)
                            │
                            ▼
              ┌───────────────────────────┐
              │ CI Pipeline (ci.yml)       │
              │  - Secret Checks          │
              │  - Maven Build & Package  │
              │  - Unit & Integration Test│
              │  - Dependency CVE Scan    │
              └─────────────┬─────────────┘
                            │
                   CI SUCCESS Gate
                            │
                            ▼
              ┌───────────────────────────┐
              │ CD Pipeline (cd.yml)      │
              │  1. Publish Docker Images │
              │     (ghcr.io/<owner>/...) │
              │  2. Release Manifest      │
              │  3. Deploy to K8s Job     │
              └─────────────┬─────────────┘
                            │
                            ▼
           ┌─────────────────────────────────┐
           │ Kustomize Image Overrides       │
           │ edit set image <sha-tag>        │
           └────────────────┬────────────────┘
                            │
                            ▼
           ┌─────────────────────────────────┐
           │ Kubernetes Cluster Deployment   │
           │  - secrets.KUBE_CONFIG auth     │
           │  - kubectl apply -k k8s/        │
           │  - RollingUpdate (0 unavail)    │
           │  - kubectl rollout status       │
           └─────────────────────────────────┘
```

---

## 2. Container Registry & Immutable Image Tagging

Container images are compiled and pushed to **GitHub Container Registry (GHCR)**:

```text
ghcr.io/<repository-owner>/devsphere-<service>:<git-sha>
```

### Image Matrix Blueprint
* `ghcr.io/<owner>/devsphere-api-gateway:<git-sha>`
* `ghcr.io/<owner>/devsphere-auth-service:<git-sha>`
* `ghcr.io/<owner>/devsphere-user-service:<git-sha>`
* `ghcr.io/<owner>/devsphere-config-server:<git-sha>`
* `ghcr.io/<owner>/devsphere-service-discovery:<git-sha>`

### Why Immutable `<git-sha>` Tags Are Essential
1. **Traceability**: Every running container instance maps 1-to-1 to an exact Git commit SHA in the repository history.
2. **Deterministic Rollouts**: Prevents node cache inconsistency issues inherent with mutable `:latest` tags.
3. **Auditability & Rollback**: Enables instant, exact-version deployment rollbacks via `kubectl rollout undo`.

---

## 3. GHCR & Kubernetes Authentication Security

### Registry Authentication
* Authenticates to GHCR using Docker Login Action (`docker/login-action@v3`).
* Credentials use GitHub's native `GITHUB_TOKEN` scoped with minimal required permissions (`permissions: contents: read`, `packages: write`). No hardcoded personal access tokens or passwords are permitted.

### Kubernetes Authentication
* Authenticates to the target Kubernetes cluster using the `KUBE_CONFIG` repository secret.
* At runtime, the workflow decodes `${{ secrets.KUBE_CONFIG }}` into a temporary, permissions-restricted `~/.kube/config` file (`chmod 600`).
* **Fallback Safety**: If `KUBE_CONFIG` is unconfigured (e.g. in local/test repository forks), the pipeline validates Kustomize manifest synthesis (`kubectl kustomize k8s/`) and cleanly logs that live cluster deployment is awaiting secret setup without causing false CI failures.

---

## 4. Kustomize Image Reference Overrides

Rather than mutating raw Deployment YAML manifests in Git, the CD pipeline applies dynamic Kustomize image overrides at build time (`kustomize edit set image`):

```bash
OWNER=$(echo "${{ github.repository_owner }}" | tr '[:upper:]' '[:lower:]')
COMMIT_SHA="${{ github.sha }}"

kustomize edit set image ghcr.io/prajapatipushkar/devsphere-api-gateway=ghcr.io/${OWNER}/devsphere-api-gateway:${COMMIT_SHA}
kustomize edit set image ghcr.io/prajapatipushkar/devsphere-auth-service=ghcr.io/${OWNER}/devsphere-auth-service:${COMMIT_SHA}
kustomize edit set image ghcr.io/prajapatipushkar/devsphere-user-service=ghcr.io/${OWNER}/devsphere-user-service:${COMMIT_SHA}
kustomize edit set image ghcr.io/prajapatipushkar/devsphere-config-server=ghcr.io/${OWNER}/devsphere-config-server:${COMMIT_SHA}
kustomize edit set image ghcr.io/prajapatipushkar/devsphere-service-discovery=ghcr.io/${OWNER}/devsphere-service-discovery:${COMMIT_SHA}
```

---

## 5. Deployment Strategy & Verification

The CD pipeline leverages the existing production deployment foundation established in Lessons 65–67:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

### Rollout Lifecycle Sequence
1. **Apply Manifests**: Executes `kubectl apply -k k8s/`.
2. **Surge Pod Creation**: Kubelet spawns replacement pod with the new `<git-sha>` image tag.
3. **Health Probe Gate**: Pod must pass `startupProbe` (JVM initialization) and `readinessProbe` (readiness state `UP`).
4. **Service Registration**: Pod IP is added to the ClusterIP Service endpoints to receive active HTTP traffic.
5. **Graceful Old Pod Shutdown**: Old pod receives `SIGTERM`, triggers Spring Boot `server.shutdown: graceful` (draining in-flight requests during a 20s window), and exits cleanly within `terminationGracePeriodSeconds: 30`.
6. **Rollout Status Verification**:
   ```bash
   kubectl rollout status deployment/config-server -n devsphere --timeout=180s
   kubectl rollout status deployment/service-discovery -n devsphere --timeout=180s
   kubectl rollout status deployment/auth-service -n devsphere --timeout=180s
   kubectl rollout status deployment/user-service -n devsphere --timeout=180s
   kubectl rollout status deployment/api-gateway -n devsphere --timeout=180s
   ```
   If any service fails to achieve healthy status within 180 seconds, the deployment step exits with code `1`, failing the CD workflow run.

---

## 6. Rollback Strategy & Operational Commands

In the event of a failed rollout or runtime error, operators can immediately issue Kubernetes rollback commands:

```bash
# Check current rollout status
kubectl rollout status deployment/api-gateway -n devsphere

# Revert deployment to previous revision
kubectl rollout undo deployment/api-gateway -n devsphere

# Revert to a specific historical revision
kubectl rollout undo deployment/api-gateway --to-revision=2 -n devsphere
```

---

## 7. Validation Commands

Synthesize manifests with Kustomize overrides statically using `kubectl`:

```bash
kubectl kustomize k8s/
```
