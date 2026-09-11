# Local Kind Frontend Deployment Runbook (Lesson 86)

This document records the operational procedure for deploying the **DevSphere** React frontend to a local, resource-constrained Kind cluster during local development.

---

## 1. Local Kind vs. Production Deployment Architecture

> [!IMPORTANT]
> **Architecture Distinction**:
> This runbook applies **strictly to local resource-constrained Kind development environments**.
> - **Production Environment**: Utilizes multi-stage Docker builds executed via GitHub Actions CI/CD runners, high-availability deployments (`replicas: 2` with PodDisruptionBudgets and TopologySpreadConstraints), Ingress controllers with TLS termination, and GHCR container registries.
> - **Local Kind Environment**: Avoids Docker daemon memory starvation on developer machines (~7.7 GB host RAM, ~3.67 GiB Docker ceiling) by compiling assets directly on the host and packaging only static artifacts into a lean NGINX runtime image.

---

## 2. Host-Side Frontend Asset Compilation

To prevent Docker BuildKit out-of-memory crashes (exit code 137) within the constrained Docker Desktop VM, Node.js dependency installation and TypeScript/Vite compilation are performed on the host operating system:

```bash
cd frontend
npm ci
npm run build
npm test
```

- **Output Directory**: `frontend/dist/`
- **Application Configuration**: Vite loads `.env.production` during production build compilation, baking the gateway endpoint (`VITE_API_BASE_URL=http://api.devsphere.local`) into static bundles, preventing accidental binding to `localhost:8080`.

---

## 3. Lightweight NGINX Packaging Approach

Rather than executing Node.js multi-stage build pipelines inside Docker, a local-specific packaging descriptor ([Dockerfile.local](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/frontend/Dockerfile.local)) and ([Dockerfile.local.dockerignore](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/frontend/Dockerfile.local.dockerignore)) copies pre-compiled host assets directly into `nginx:1.25-alpine`:

```dockerfile
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN sed -i 's|^user  nginx;|#user nginx;|g' /etc/nginx/nginx.conf \
    && sed -i 's|/var/run/nginx.pid|/tmp/nginx.pid|g' /etc/nginx/nginx.conf \
    && chown -R 101:101 /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d \
    && chmod -R 777 /var/cache/nginx /var/log/nginx /etc/nginx/conf.d \
    && touch /tmp/nginx.pid \
    && chmod 777 /tmp/nginx.pid
EXPOSE 80
USER 101
CMD ["nginx", "-g", "daemon off;"]
```

This reduces container build execution to ~1.6 seconds, transfers only ~428 kB of layer data, and consumes virtually zero incremental Docker memory.

---

## 4. Frontend Image Tagging & Kind Loading

The local image maintains the canonical repository image identifier:

```bash
docker build -f Dockerfile.local -t ghcr.io/prajapatipushkar/devsphere-frontend:latest ./frontend
kind load docker-image ghcr.io/prajapatipushkar/devsphere-frontend:latest --name devsphere
```

- **Image Pull Policy**: Set to `imagePullPolicy: IfNotPresent` in [frontend.yaml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/k8s/services/frontend.yaml) to ensure the local Kind containerd runtime uses the imported image without making remote GHCR requests.

---

## 5. Kubernetes Resources & Single Replica Rationale

### Single Frontend Replica Justification
In [frontend.yaml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/k8s/services/frontend.yaml), the frontend Deployment is configured with:
```yaml
spec:
  # Local Kind resource constraint; production HA configuration remains a later production concern.
  replicas: 1
```
*Rationale*:
The Kind control-plane node has 3,759 MiB of allocatable memory, with 3,490 MiB (92%) pre-allocated to Lesson 85 backend services and infrastructure. Scheduling a single replica requires 128 MiB (pushing allocation to 3,618 MiB / 96%), maintaining cluster stability. Two replicas would push request allocations to 99.6% and limit overcommit past 240%, triggering pod evictions and health probe timeouts. Multi-replica HA remains preserved for later production environments.

### ClusterIP Service
The frontend exposes port 80 via internal `ClusterIP`:
- **Service Name**: `devsphere-frontend`
- **Port**: `80/TCP` (TargetPort: `80`)
- **Type**: `ClusterIP` (No NodePort or LoadBalancer used)

---

## 6. Verification Results

1. **Pod Readiness & Stability**:
   - `devsphere-frontend` pod scheduled and transitioned to `1/1 Running` with 0 restarts.
   - Endpoint properly registered (`10.244.0.14:80`).
2. **NGINX Root Route**:
   - Internal HTTP request to `http://127.0.0.1:80/` serves the complete React HTML shell with HTTP 200.
3. **Single Page Application (SPA) Fallback**:
   - Direct requests to `/dashboard`, `/tasks`, and `/profile` return `index.html` (HTTP 200) via NGINX `try_files $uri $uri/ /index.html;` directive.
4. **Internal API Gateway Connectivity**:
   - Internal network probe from `devsphere-frontend` to `http://api-gateway:8080/actuator/health` returned `HTTP/1.1 200 OK`, confirming East-West cluster reachability.
5. **Pod Security Standards (PSS Restricted)**:
   - Pod runs non-root (`runAsNonRoot: true`, UID/GID `101`).
   - Seccomp profile enforced as `RuntimeDefault`.
   - Privilege escalation disallowed (`allowPrivilegeEscalation: false`).
   - All Linux capabilities dropped (`drop: [ALL]`).
   - Read/write directories (`/tmp`, `/var/cache/nginx`, `/var/run`) mounted via dedicated `emptyDir` volumes.
