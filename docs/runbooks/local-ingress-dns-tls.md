# Local Ingress, DNS & HTTPS/TLS Runbook (Lesson 87)

This runbook documents the operational procedure, routing architecture, local DNS resolution, and HTTPS/TLS termination for the **DevSphere** microservices and frontend application running on a local, resource-constrained Kind cluster.

> [!IMPORTANT]
> **Production vs. Local Development Distinction**:
> **This is local Kind HTTPS/DNS configuration, not public production DNS or publicly trusted production TLS.**
> In a future production environment, public internet DNS records (`A` / `CNAME`), automated ACME Let's Encrypt certificates issued via cert-manager, multi-replica high-availability Ingress controllers, and cloud load balancers will be utilized.

---

## 1. Local Ingress Architecture

The ingress perimeter terminates external client traffic on the host and routes requests directly to internal Kubernetes `ClusterIP` services based on hostname rules:

```
[ Local Browser / Host Client ]
         │
         │ HTTPS (Port 443) / HTTP (Port 80)
         ▼
[ Ingress Controller (ingress-nginx) ]
   Namespace: ingress-nginx (Single Replica)
   Local Endpoint: 127.0.0.1:80 & 127.0.0.1:443
         │
         ├── Host: app.devsphere.local ──► devsphere-frontend:80 (ClusterIP)
         │                                       │
         │                                       └── React SPA (index.html, static assets)
         │
         └── Host: api.devsphere.local ──► api-gateway:8080 (ClusterIP)
                                                 │
                                                 ├── /actuator/health
                                                 ├── /api/v1/auth/** ──► devsphere-auth-service:8081
                                                 └── /api/v1/users/** ──► devsphere-user-service:8082
```

Internal infrastructure components (MySQL, Redis, Kafka, Zookeeper, Eureka, Config Server) are **never** exposed via Ingress or NodePort.

---

## 2. Ingress Controller Specification

To operate within host RAM limitations (~7.7 GB total physical RAM, ~3.67 GiB Docker limit), the Ingress Controller is deployed as a single lightweight replica:

- **Controller**: NGINX Ingress Controller ([ingress-nginx-controller.yaml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/k8s/ingress/ingress-nginx-controller.yaml))
- **Image**: `registry.k8s.io/ingress-nginx/controller:v1.12.0`
- **Namespace**: `ingress-nginx`
- **Replicas**: 1
- **IngressClass**: `nginx` (`spec.controller: k8s.io/ingress-nginx`)
- **Resource Requests / Limits**:
  - `requests.cpu: 50m`, `requests.memory: 90Mi`
  - `limits.cpu: 500m`, `limits.memory: 256Mi`
- **Security Context**:
  - `runAsUser: 101`, `runAsGroup: 82`, `runAsNonRoot: true`
  - `allowPrivilegeEscalation: false`
  - `capabilities.drop: ["ALL"]`, `capabilities.add: ["NET_BIND_SERVICE"]`
  - `seccompProfile: { type: RuntimeDefault }`
  - No `hostNetwork`, no `hostPID`, no `hostIPC`, no privileged containers.
- **Excluded Upstream Overheads**: Admission webhook certificate generator jobs and validating webhooks are omitted to avoid CPU spikes and pod scheduling deadlocks under constrained memory.

---

## 3. Kubernetes Ingress Routing

The DevSphere Ingress manifest ([ingress.yaml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/k8s/ingress/ingress.yaml)) routes traffic in namespace `devsphere`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: devsphere-ingress
  namespace: devsphere
  labels:
    app.kubernetes.io/name: devsphere-ingress
    app.kubernetes.io/part-of: devsphere
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - app.devsphere.local
        - api.devsphere.local
      secretName: devsphere-tls
  rules:
    - host: app.devsphere.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: devsphere-frontend
                port:
                  number: 80
    - host: api.devsphere.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 8080
```

---

## 4. Local DNS & Host Resolution Setup

Because `devsphere.local` is a private development domain without public authoritative DNS records, local hostname resolution maps traffic to the Ingress controller endpoint:

### Host Mapping Requirement
- `app.devsphere.local` → `127.0.0.1`
- `api.devsphere.local` → `127.0.0.1`

### Windows Hosts File Configuration
Modifying `C:\Windows\System32\drivers\etc\hosts` requires Windows Administrator privileges. Run the following command in an **Elevated (Administrator) PowerShell**:

```powershell
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "`n127.0.0.1 app.devsphere.local`n127.0.0.1 api.devsphere.local"
```

To verify the entries were appended:
```powershell
Get-Content C:\Windows\System32\drivers\etc\hosts | Select-String "devsphere.local"
```

> [!NOTE]
> Standard Windows DNS lookup utilities like `nslookup` query upstream DNS servers directly (bypassing the Windows `hosts` file) and will report `Non-existent domain`. Applications using the Windows socket resolver (`ping`, Chrome, Edge, curl) consult the `hosts` file and resolve `127.0.0.1`.

---

## 5. Local TLS Certificate Strategy

A local SAN (Subject Alternative Name) X.509 certificate was generated covering both development hostnames:

- **Common Name (CN)**: `devsphere-local`
- **Subject Alternative Names (SAN)**:
  - `DNS:app.devsphere.local`
  - `DNS:api.devsphere.local`
  - `DNS:localhost`
  - `IP:127.0.0.1`
- **Validity**: 365 days
- **Secret Name**: `devsphere-tls` in namespace `devsphere`

### Secret Provisioning
The secret is created dynamically without committing private key material to Git:
```powershell
kubectl create secret tls devsphere-tls \
  --cert="tls.crt" \
  --key="tls.key" \
  -n devsphere
```
A template structure is provided in [tls-secret.example.yaml](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/k8s/config/tls-secret.example.yaml). Private keys (`*.key`, `*.pem`, `*.crt`, `tls.yaml`, `tls-secret.yaml`) are excluded by `.gitignore`.

> [!WARNING]
> Because this is a self-signed certificate and not signed by a public Certificate Authority, web browsers will present an initial security warning (`NET::ERR_CERT_AUTHORITY_INVALID`). You can proceed past the warning or import the certificate into the Windows Trusted Root Certification Authorities store for local development.

---

## 6. Kind Cluster Network Exposure

The Kind cluster container (`devsphere-control-plane`) publishes only port 6443 to the host (`127.0.0.1:50100 -> 6443`). To expose the Ingress Controller without recreating the cluster, traffic is forwarded safely via `kubectl port-forward`:

```powershell
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 80:80 443:443 --address 127.0.0.1
```

This binds local host ports 80 and 443 strictly to the Ingress Controller service.

---

## 7. Frontend Mixed-Content Alignment

To prevent browsers from blocking insecure HTTP API requests when accessing `https://app.devsphere.local`:

- [frontend/.env.production](file:///c:/Users/kppus/OneDrive/Documents/Desktop/DevSphere/frontend/.env.production) sets `VITE_API_BASE_URL=https://api.devsphere.local`.
- Assets are compiled via `npm run build` on the host.
- The runtime image is packaged via `Dockerfile.local` and loaded directly into Kind.

---

## 8. Verification Commands & Results

### 1. HTTP to HTTPS Redirection
```bash
curl -I --resolve app.devsphere.local:80:127.0.0.1 http://app.devsphere.local/
curl -I --resolve api.devsphere.local:80:127.0.0.1 http://api.devsphere.local/
```
*Result*: `HTTP/1.1 308 Permanent Redirect` with `Location: https://app.devsphere.local` and `https://api.devsphere.local`.

### 2. Frontend HTTPS Root & SPA Routing
```bash
# Root HTML shell
curl -k -I --resolve app.devsphere.local:443:127.0.0.1 https://app.devsphere.local/

# SPA Route fallbacks
curl -k -I --resolve app.devsphere.local:443:127.0.0.1 https://app.devsphere.local/dashboard
curl -k -I --resolve app.devsphere.local:443:127.0.0.1 https://app.devsphere.local/tasks
curl -k -I --resolve app.devsphere.local:443:127.0.0.1 https://app.devsphere.local/profile
```
*Result*: `HTTP/1.1 200 OK` (`Content-Type: text/html`, `Strict-Transport-Security: max-age=31536000; includeSubDomains`).

### 3. API Gateway HTTPS Health & Route Checks
```bash
# Actuator health endpoint
curl -k -i --resolve api.devsphere.local:443:127.0.0.1 https://api.devsphere.local/actuator/health
```
*Result*: `HTTP/1.1 200 OK` with JSON payload `{"status":"UP","groups":["liveness","readiness"]}` and trace header `X-Trace-Id`.

```bash
# API Route (testing downstream microservice routing)
curl -k -i --resolve api.devsphere.local:443:127.0.0.1 https://api.devsphere.local/api/v1/users
```
*Result*: HTTP response from `DEVSPHERE-USER-SERVICE` routed via API Gateway with gateway trace ID.

---

## 9. Resource Health & Stability

Following Ingress Controller deployment and frontend updates:

- **Docker Memory Usage**: ~3.12 GiB used / 3.67 GiB ceiling (~85%)
- **Node Conditions**: `MemoryPressure=False`, `DiskPressure=False`, `PIDPressure=False`, `Ready=True`
- **Workload Status**: All 11 pods (10 backend/frontend + 1 ingress controller) remain `1/1 Running` with 0 flapping or unhandled restarts.

---

## 10. Local-Only Limitations Summary

1. **Self-Signed TLS**: Browser trust warning is expected unless explicitly added to Windows Root CA.
2. **Local Port Forwarding**: Background `kubectl port-forward` provides ingress connectivity to preserve the existing Kind cluster.
3. **Hosts File Resolution**: Windows Administrator elevation is required to add `devsphere.local` entries to `C:\Windows\System32\drivers\etc\hosts`.
4. **No Public DNS or Cloud LoadBalancer**: Not intended for public internet exposure.
