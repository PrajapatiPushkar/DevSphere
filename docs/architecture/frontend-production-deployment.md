# Frontend Production Deployment & CI/CD Architecture

This document details the production build pipeline, multi-stage Docker containerization, GHCR artifact delivery, GitHub Actions CI/CD workflows, and Kubernetes deployment topology for the DevSphere React frontend application.

---

## 1. Production Architecture Overview

```text
Developer (Git Push)
   │
   ▼
GitHub Actions CI/CD Workflows (.github/workflows/ci.yml & cd.yml)
   │
   ├── 1. Frontend Security & Secret Audit
   ├── 2. Node 20 Setup & Dependency Install (npm ci)
   ├── 3. Type Checking (tsc --noEmit) & Vitest Unit Tests (npm test)
   ├── 4. Static Production Asset Compilation (vite build -> dist/)
   ├── 5. Multi-Stage Docker Image Build (NGINX SPA Fallback)
   │      │
   │      ▼
   └── 6. Publish to GitHub Container Registry (ghcr.io/${OWNER}/devsphere-frontend:${SHA})
          │
          ▼
Kubernetes Cluster (Namespace: devsphere)
   │
   ├── Ingress (devsphere-ingress)
   │     ├── app.devsphere.local ──► Service: devsphere-frontend (Port 80)
   │     └── api.devsphere.local ──► Service: api-gateway (Port 8080)
   │
   └── Frontend Deployment (devsphere-frontend)
         ├── Replicas: 2
         ├── Container: NGINX 1.25 Alpine (Port 80)
         └── Probes: HTTP Readiness & Liveness on /
```

---

## 2. Multi-Stage Containerization (`Dockerfile`)

- **Stage 1 (Builder)**: Uses `node:20-alpine` to execute `npm ci` and `npm run build`, generating compiled static bundles in `dist/`.
- **Stage 2 (Runtime)**: Uses `nginx:1.25-alpine`. Copies `dist/` static files into `/usr/share/nginx/html` and applies custom [nginx.conf](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/frontend/nginx.conf).

### NGINX SPA Routing Fallback
React Router requires single-page application route fallback to prevent 404 HTTP errors upon direct browser navigation or page refreshes.
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

---

## 3. GitHub Actions Integration

### CI Workflow (`.github/workflows/ci.yml`)
- `frontend-build-and-test`: Installs dependencies, runs TypeScript type checks, unit test suites, and production bundle compilation.
- `docker-build-validation`: Validates frontend Dockerfile image compilation without publishing.

### CD Workflow (`.github/workflows/cd.yml`)
- `publish-frontend-container-image`: Authenticates to `ghcr.io`, tags image with git SHA (`ghcr.io/${OWNER}/devsphere-frontend:${github.sha}`), and pushes artifact to GHCR.
- `deploy-to-kubernetes`: Dynamically sets image overrides using `kustomize edit set image`, synthesizes manifests (`kubectl kustomize k8s/`), applies resources (`kubectl apply -k k8s/`), and verifies deployment rollout status (`kubectl rollout status deployment/devsphere-frontend -n devsphere`).

---

## 4. Kubernetes Topology & Routing

- **Deployment**: `k8s/services/frontend.yaml` defines 2 replicas with resource limits (`cpu: 500m`, `memory: 256Mi`) and HTTP health probes.
- **Service**: `ClusterIP` exposing port 80 internally within namespace `devsphere`.
- **Ingress**: `k8s/ingress/ingress.yaml` routes `app.devsphere.local` to `devsphere-frontend:80` and `api.devsphere.local` to `api-gateway:8080`.
