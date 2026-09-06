# DevSphere Production Deployment Runbook

This runbook outlines the operational procedures for deploying, verifying, troubleshooting, and rolling back the **DevSphere** production application on Kubernetes.

---

## 1. Prerequisites

Before executing a production deployment, ensure the following CLI tools and credentials are available:

- `docker` (v24.0+)
- `kubectl` (v1.28+)
- `kustomize` (v5.0+)
- `git`
- GitHub personal access token with `write:packages` permission (for GHCR).
- Kubernetes cluster access (`~/.kube/config`).

---

## 2. Pre-Deployment Configuration & Secrets Audit

1. **Verify ConfigMaps & Secrets**:
   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/config/configmap.yaml
   ```
2. **Ensure Production Secrets are Created**:
   - Ensure DB passwords, JWT secret keys, and Kafka credentials are configured in Kubernetes Secrets (`devsphere-secrets`), never committed to source control.

---

## 3. Build & Local Validation

1. **Frontend Validation**:
   ```bash
   cd frontend
   npm ci
   npm run lint
   npm test -- --run
   npm run build
   ```
2. **Backend Services Build**:
   ```bash
   for service in api-gateway auth-service user-service service-discovery config-server; do
     (cd services/$service && mvn clean package -DskipTests)
   done
   ```
3. **Kustomize Manifest Validation**:
   ```bash
   kubectl kustomize k8s/
   ```

---

## 4. Kubernetes Deployment Order

Apply manifests in strict dependency order:

```bash
# 1. Namespace & Config
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/config/configmap.yaml
kubectl apply -f k8s/config/secret.example.yaml

# 2. Infrastructure Dependencies
kubectl apply -f k8s/infrastructure/mysql.yaml
kubectl apply -f k8s/infrastructure/redis.yaml
kubectl apply -f k8s/infrastructure/kafka.yaml

# 3. Discovery & Config Server
kubectl apply -f k8s/services/config-server.yaml
kubectl apply -f k8s/services/service-discovery.yaml

# 4. Core Application Services
kubectl apply -f k8s/services/auth-service.yaml
kubectl apply -f k8s/services/user-service.yaml
kubectl apply -f k8s/services/api-gateway.yaml

# 5. Frontend & Ingress
kubectl apply -f k8s/services/frontend.yaml
kubectl apply -f k8s/ingress/ingress.yaml
kubectl apply -f k8s/networking/loadbalancer-gateway.yaml

# 6. Observability
kubectl apply -f k8s/observability/alerts.yaml
kubectl apply -f k8s/observability/prometheus.yaml
kubectl apply -f k8s/observability/grafana.yaml
```

---

## 5. Rollout Verification

Check rolling update completion across all deployments:

```bash
kubectl rollout status deployment/config-server -n devsphere --timeout=180s
kubectl rollout status deployment/service-discovery -n devsphere --timeout=180s
kubectl rollout status deployment/auth-service -n devsphere --timeout=180s
kubectl rollout status deployment/user-service -n devsphere --timeout=180s
kubectl rollout status deployment/api-gateway -n devsphere --timeout=180s
kubectl rollout status deployment/devsphere-frontend -n devsphere --timeout=180s
```

Verify Pod readiness and service health:

```bash
kubectl get pods -n devsphere
kubectl get svc -n devsphere
kubectl get ingress -n devsphere
```

---

## 6. End-to-End Verification Procedure

1. **Frontend Direct Navigation**: Access `http://app.devsphere.local` in browser. Verify static assets load cleanly.
2. **Authentication Flow**:
   - Register a new account (`/register`).
   - Login (`/login`) and verify JWT token storage in `AuthContext` / `localStorage`.
3. **Task & Profile Journey**:
   - Create a task, edit task title, mark complete, verify priority/status filtering.
   - Open Profile page, update display details, update settings preference.
4. **Logout**: Click Logout, verify token removal, and test that `/dashboard` redirects to `/login`.

---

## 7. Emergency Rollback Procedure

If a critical failure or CrashLoopBackOff is detected post-deployment:

1. **Execute Rollback Command**:
   ```bash
   kubectl rollout undo deployment/api-gateway -n devsphere
   kubectl rollout undo deployment/auth-service -n devsphere
   kubectl rollout undo deployment/user-service -n devsphere
   kubectl rollout undo deployment/devsphere-frontend -n devsphere
   ```
2. **Verify Rollback Status**:
   ```bash
   kubectl rollout status deployment/devsphere-frontend -n devsphere
   ```
3. **Inspect Deployment Revision History**:
   ```bash
   kubectl rollout history deployment/devsphere-frontend -n devsphere
   ```

---

## 8. Troubleshooting Common Production Issues

### `ImagePullBackOff` / `ErrImagePull`
- **Cause**: Image tag not found or GHCR credentials missing.
- **Fix**: Check `kubectl describe pod <pod-name> -n devsphere`. Ensure `imagePullSecrets` or GHCR login is active.

### `CrashLoopBackOff`
- **Cause**: Application startup failure (e.g. database connection timeout or missing env var).
- **Fix**: Inspect logs using `kubectl logs <pod-name> -n devsphere --previous`.

### CORS / API Gateway Connection Errors
- **Cause**: Ingress host mismatch or API Gateway CORS headers missing.
- **Fix**: Verify `VITE_API_GATEWAY_URL` points to Gateway host (`http://api.devsphere.local`).
