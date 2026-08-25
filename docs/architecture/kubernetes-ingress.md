# Kubernetes Ingress, TLS & External Access Architecture

## 1. External Request Flow Architecture
DevSphere enforces strict boundary separation between cluster perimeter routing and application domain routing:

```
Internet / Client Requests
           │
           ▼
[External Load Balancer / Ingress Controller]
           │
           ▼ (HTTPS / Host: api.devsphere.example.com)
  [Kubernetes Ingress (devsphere-ingress)]
           │
           ▼ (HTTP / Port 8080)
   [API Gateway (devsphere-api-gateway ClusterIP)]
           │
           ├─────────────────────────┬─────────────────────────┐
           ▼                         ▼                         ▼
   [Auth Service (:8081)]    [User Service (:8082)]    [Control Plane Services]
    (ClusterIP Only)          (ClusterIP Only)          (ClusterIP Only)
```

---

## 2. Ingress Role vs API Gateway Role

| Responsibilities | Kubernetes Ingress (`ingress.yaml`) | API Gateway (`DEVSPHERE-API-GATEWAY`) |
| :--- | :--- | :--- |
| **Primary Scope** | Cluster Network Perimeter | Application Domain Perimeter |
| **Protocols** | Public HTTPS / TLS Termination | Internal HTTP / Reactive WebFlux |
| **Host/Path Routing**| Route domain (`api.devsphere.example.com`) to Gateway | Route service prefixes (`/api/v1/auth/**`, `/api/v1/users/**`) |
| **Security Controls**| SSL/TLS Handshake, HTTP to HTTPS Redirect | JWT Signature Validation, Role Claims (RBAC), Ownership Checks |
| **Traffic Control** | Connection limits, request body size bounds | Token-bucket rate limiting (`rate_limit:*`), Resilience4j fallbacks |

---

## 3. Internal Service Isolation Policy
To protect application security, only **API Gateway** (`devsphere-api-gateway`, Port `8080`) is exposed as an Ingress backend.

> [!CAUTION]
> Internal services (`auth-service`, `user-service`, `config-server`, `service-discovery`, Kafka, Redis, MySQL) are strictly forbidden from public Ingress routing or `NodePort`/`LoadBalancer` service types.

---

## 4. Host & Path Routing Specification
The Ingress manifest ([`infrastructure/kubernetes/gateway/ingress.yaml`](file:///infrastructure/kubernetes/gateway/ingress.yaml)) routes external domain traffic:
- **Host**: `api.devsphere.example.com`
- **Path**: `/` (`pathType: Prefix`)
- **Backend Service**: `devsphere-api-gateway`
- **Port**: `8080`

The API Gateway retains full ownership over internal microservice routing rules (`POST /api/v1/auth/register`, `GET /api/v1/users/{userId}`).

---

## 5. TLS Termination & Certificate Architecture
TLS is terminated at the Ingress Controller boundary:
- **Client to Ingress**: Encrypted HTTPS (`port 443`).
- **Ingress to API Gateway**: Unencrypted HTTP (`port 8080`) within the private cluster network.

TLS material is referenced via Secret `devsphere-api-tls`:
- Secret template [`tls-secret.example.yaml`](file:///infrastructure/kubernetes/config/tls-secret.example.yaml) defines `tls.crt` and `tls.key` placeholders.
- Real certificate files (`tls-secret.yaml`, `*.crt`, `*.key`) are ignored by `.gitignore` and **never** committed to Git.

---

## 6. HTTPS Enforcement & Redirects
Production Ingress enforces automatic HTTP to HTTPS redirects via annotation:
```yaml
annotations:
  nginx.ingress.kubernetes.io/ssl-redirect: "true"
```
HTTP requests arriving on port 80 receive an HTTP 301/308 redirect to `https://api.devsphere.example.com`.

---

## 7. Forwarded Headers & Client IP Propagation
To ensure Redis-backed rate limiting (Lesson 18) and distributed tracing (Lesson 17) operate correctly, the Ingress Controller preserves HTTP headers:
- `X-Forwarded-For`: Original client IP address (used for public endpoint rate limiting `rate_limit:ip:{ip}`).
- `X-Forwarded-Proto`: Original request protocol (`https`).
- `X-Forwarded-Host`: Original domain host (`api.devsphere.example.com`).
- `traceparent`: W3C OpenTelemetry distributed tracing context.

---

## 8. Actuator Endpoint Protection
Spring Boot Actuator endpoints (`/actuator/**`, `/actuator/env`, `/actuator/configprops`) remain strictly internal. The Ingress manifest routes **only** application APIs, preventing public access to internal health and metric diagnostics.

---

## 9. Local Development Strategy (`api.devsphere.local`)
For local Kubernetes testing (Minikube / Docker Desktop / Kind):
1. Add an entry to the local operating system `hosts` file:
   ```
   127.0.0.1 api.devsphere.local
   ```
2. For local HTTP testing without TLS certificates, disable `ssl-redirect` in local overrides or generate a local self-signed certificate using `mkcert`.

---

## 10. Production TLS & DNS Roadmap
Future lessons will enhance perimeter security:
- **Cert-Manager & Let's Encrypt**: Automated TLS certificate issuance and renewal.
- **Cloud Load Balancer Integration**: AWS ALB / GCP HTTPS Load Balancer integration.
- **Web Application Firewall (WAF)**: Cloud-edge inspection for SQL injection and XSS filtering.
- **Network Policies**: Kubernetes NetworkPolicies restricting east-west inter-pod communication.
