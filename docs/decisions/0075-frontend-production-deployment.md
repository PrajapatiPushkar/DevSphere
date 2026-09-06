# Architectural Decision Record — 0075 Frontend Production Build & CI/CD Pipeline

## Status
Accepted

## Date
2026-09-06

## Context
Following the completion of Lessons 71–78, DevSphere required production-grade containerization, GHCR delivery, GitHub Actions CI/CD pipeline integration, and Kubernetes deployment resources for the React 18 SPA frontend application.

## Key Decisions

1. **Multi-Stage Dockerfile with NGINX SPA Support**:
   - Implemented a 2-stage build (`node:20-alpine` -> `nginx:1.25-alpine`).
   - Configured NGINX for SPA route fallbacks (`try_files $uri $uri/ /index.html`) to support client-side React Router routing.
   - Enforced security headers (`X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`) and Gzip compression.

2. **Extension of Lesson 68/69 CI/CD Pipeline**:
   - Extended `.github/workflows/ci.yml` with `frontend-build-and-test` and frontend Docker validation.
   - Extended `.github/workflows/cd.yml` with `publish-frontend-container-image`, Kustomize image override, and Kubernetes rollout verification for `deployment/devsphere-frontend`.
   - Used immutable git SHA image tags for GHCR publishing (`ghcr.io/${OWNER}/devsphere-frontend:${COMMIT_SHA}`).

3. **Kubernetes Resources & Ingress Integration**:
   - Added `k8s/services/frontend.yaml` specifying Deployment (2 replicas, topology spread constraints, resource limits, HTTP liveness/readiness probes) and ClusterIP Service (port 80).
   - Extended `k8s/ingress/ingress.yaml` to route `app.devsphere.local` to `devsphere-frontend:80` alongside API Gateway.

4. **Zero Infrastructure Redesign**:
   - Preserved all existing microservice build jobs, Maven pipelines, and Kubernetes manifests without rewriting or breaking existing workflows.

## Consequences
The DevSphere frontend is fully containerized, published to GHCR, integrated into CI/CD quality gates, and declared in Kubernetes Kustomize manifests alongside the microservices backend.
