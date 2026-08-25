# Kubernetes Security Hardening & Network Isolation Architecture

## 1. Kubernetes Least-Privilege Security Model
DevSphere enforces a strict **least-privilege security model** across the Kubernetes cluster, adhering to the core security principle:
> *"Everything is denied unless explicitly required."*

Network and identity architecture:

```
                  [ Internet ]
                       │
                       ▼
            [ Ingress Controller ]
                       │
                       ▼ (NetworkPolicy: port 8080)
             [ devsphere-api-gateway ]
            (SA: devsphere-api-gateway)
                       │
        ┌──────────────┴──────────────┐
        ▼ (Port 8081)                 ▼ (Port 8082)
[ devsphere-auth-service ]     [ devsphere-user-service ]
(SA: devsphere-auth)           (SA: devsphere-user)
        │                              │
        ├──────────────┬───────────────┼──────────────┬──────────────┐
        ▼              ▼               ▼              ▼              ▼
 [Config Server]   [Eureka]         [Kafka]        [Redis]        [MySQL]
    (:8888)        (:8761)       (:9092/29092)     (:6379)        (:3306)
```

---

## 2. Pod Security & Container Hardening
All application deployments ([`auth`](file:///infrastructure/kubernetes/auth/deployment.yaml), [`user`](file:///infrastructure/kubernetes/user/deployment.yaml), [`gateway`](file:///infrastructure/kubernetes/gateway/deployment.yaml), [`config-server`](file:///infrastructure/kubernetes/config-server/deployment.yaml), [`service-discovery`](file:///infrastructure/kubernetes/service-discovery/deployment.yaml)) enforce hardened container security contexts:

- **Non-Root Execution**: `runAsNonRoot: true` with explicitly assigned non-root UID/GID (`10001:10001`).
- **Privilege Escalation Prevention**: `allowPrivilegeEscalation: false`.
- **Read-Only Root Filesystem**: `readOnlyRootFilesystem: true`. Writable temporary storage is restricted to an in-memory `emptyDir` mounted at `/tmp`.
- **Capability Elimination**: `capabilities.drop: ["ALL"]`. No Linux capabilities are granted to application containers.
- **Seccomp Profiling**: `seccompProfile: { type: RuntimeDefault }` applied at pod security context level.
- **Prohibited Controls**: Never use `privileged: true`, `hostNetwork: true`, `hostPID: true`, `hostIPC: true`, or `hostPath` mounts.

---

## 3. Workload Identity Separation (ServiceAccounts)
Every microservice uses a dedicated Kubernetes `ServiceAccount` ([`security/serviceaccounts.yaml`](file:///infrastructure/kubernetes/security/serviceaccounts.yaml)):

- `devsphere-api-gateway`
- `devsphere-auth-service`
- `devsphere-user-service`
- `devsphere-config-server`
- `devsphere-service-discovery`

The `default` ServiceAccount is strictly prohibited. Dedicated ServiceAccounts establish distinct workload identities for auditing, future fine-grained role mapping, and isolation.

---

## 4. Kubernetes API Isolation & RBAC
Application microservices in DevSphere do not require access to the Kubernetes API server.

- **Token Automounting Disabled**: `automountServiceAccountToken: false` is configured on all ServiceAccounts and Deployments, preventing API credentials from being mounted into pods (`/var/run/secrets/kubernetes.io/serviceaccount`).
- **Zero RBAC Bindings**: Zero `Role`, `ClusterRole`, `RoleBinding`, or `ClusterRoleBinding` manifests are created for application workloads.

---

## 5. Pod Security Admission (PSA)
The `devsphere` namespace ([`namespace.yaml`](file:///infrastructure/kubernetes/namespace.yaml)) is labeled for standard Pod Security Admission enforcement:

```yaml
metadata:
  name: devsphere
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: latest
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```
Deprecated `PodSecurityPolicy` resources are omitted.

---

## 6. Network Policy Framework & Default Deny
Network isolation is implemented using standard `networking.k8s.io/v1` NetworkPolicies located in [`infrastructure/kubernetes/networking/`](file:///infrastructure/kubernetes/networking/):

### Default-Deny-All Policy
[`default-deny.yaml`](file:///infrastructure/kubernetes/networking/default-deny.yaml) enforces a baseline deny-all policy for both ingress and egress across all pods in the `devsphere` namespace (`podSelector: {}`).

---

## 7. DNS Egress Resolution
[`allow-dns.yaml`](file:///infrastructure/kubernetes/networking/allow-dns.yaml) explicitly permits egress on UDP/TCP port 53 across all pods, allowing internal cluster DNS resolution to `kube-dns` / `CoreDNS`.

---

## 8. Ingress Controller Perimeter Access
[`allow-ingress-to-gateway.yaml`](file:///infrastructure/kubernetes/networking/allow-ingress-to-gateway.yaml) allows ingress traffic on port 8080 **only** to `devsphere-api-gateway` pods from the Ingress Controller namespace (`ingress-nginx`).

---

## 9. API Gateway Communication Rules
[`allow-gateway-to-services.yaml`](file:///infrastructure/kubernetes/networking/allow-gateway-to-services.yaml) permits egress from `devsphere-api-gateway` to:
- Auth Service (TCP 8081)
- User Service (TCP 8082)
- Config Server (TCP 8888)
- Service Discovery / Eureka (TCP 8761)
- Redis (TCP 6379)

API Gateway is strictly denied direct access to MySQL or Kafka.

---

## 10. Internal Service Isolation Rules
East-west pod communication is strictly constrained:

- **Auth Service** ([`allow-auth-service.yaml`](file:///infrastructure/kubernetes/networking/allow-auth-service.yaml)):
  - Ingress: Allowed ONLY from `devsphere-api-gateway` on port 8081.
  - Egress: Allowed to Config Server (8888), Eureka (8761), Kafka (9092/29092), and MySQL (3306).
- **User Service** ([`allow-user-service.yaml`](file:///infrastructure/kubernetes/networking/allow-user-service.yaml)):
  - Ingress: Allowed ONLY from `devsphere-api-gateway` on port 8082.
  - Egress: Allowed to Config Server (8888), Eureka (8761), Kafka (9092/29092), Redis (6379), and MySQL (3306).
- **Config Server** ([`allow-config-server.yaml`](file:///infrastructure/kubernetes/networking/allow-config-server.yaml)):
  - Ingress: Allowed on port 8888 from Gateway, Auth, User, and Eureka.
- **Service Discovery** ([`allow-service-discovery.yaml`](file:///infrastructure/kubernetes/networking/allow-service-discovery.yaml)):
  - Ingress: Allowed on port 8761 from Gateway, Auth, User, and Config Server.

Direct pod-to-pod connections between `auth-service` and `user-service` are denied.

---

## 11. External Infrastructure Network Policies
Egress to external infrastructure services (Kafka, Redis, MySQL) is explicitly defined in [`external-egress.yaml`](file:///infrastructure/kubernetes/networking/external-egress.yaml):
- **Kafka**: TCP ports 9092, 29092 (Auth, User)
- **Redis**: TCP port 6379 (Gateway, User)
- **MySQL**: TCP port 3306 (Auth, User)

Unrestricted internet egress (`0.0.0.0/0`) is strictly denied.

---

## 12. Secret Protection & Handling
Secret templates ([`secret.example.yaml`](file:///infrastructure/kubernetes/config/secret.example.yaml), [`tls-secret.example.yaml`](file:///infrastructure/kubernetes/config/tls-secret.example.yaml)) specify required key structures. Real secret files (`secret.yaml`, `tls-secret.yaml`, `*.key`, `*.crt`) are excluded via `.gitignore`. Production secrets must be provisioned out-of-band:
```bash
kubectl create secret generic devsphere-secrets \
  --from-literal=JWT_SECRET="<production-256-bit-key>" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="<production-db-pass>" \
  -n devsphere
```

---

## 13. Perimeter TLS & Ingress Protection
Perimeter TLS termination occurs at the Ingress boundary (`devsphere-api-tls`). HTTPS redirect is enforced, client IPs (`X-Forwarded-For`) and OpenTelemetry trace contexts (`traceparent`) are forwarded, and Actuator diagnostic endpoints (`/actuator/**`) remain strictly internal.

---

## 14. Application Security Boundary Separation
Kubernetes security controls (NetworkPolicies, ServiceAccounts, Pod Security Admission) operate independently from application security controls (JWT signature validation, Spring Security filters, RBAC role claims, domain ownership checks). Neither layer overrides or replaces the other.

---

## 15. Observability Compatibility
NetworkPolicies explicitly permit Actuator health probe checks from Kubernetes kubelet, Micrometer metric scraping, and OTLP trace propagation over permitted service ports.

---

## 16. CNI Requirement & Enforceability Notice
> [!WARNING]
> Kubernetes NetworkPolicies require a CNI plugin with policy support (such as Calico, Cilium, or Weave Net). In basic clusters without CNI policy engines (e.g. default Minikube/Docker Desktop bridge drivers), NetworkPolicy resources will be accepted by the API server but will **not** filter network traffic at runtime.

---

## 17. Runtime & Dry-Run Validation Commands
Validate Kubernetes security manifests statically or against a running cluster:

```bash
# Manifest synthesis & validation via Kustomize
kustomize build infrastructure/kubernetes/

# Dry-run client application check
kubectl apply --dry-run=client -k infrastructure/kubernetes/

# Validate ServiceAccount permissions (should return 'no')
kubectl auth can-i get pods --as=system:serviceaccount:devsphere:devsphere-auth-service -n devsphere

# Connectivity verification using temporary curl pod
kubectl run network-test --rm -it --image=curlimages/curl -n devsphere -- sh
```

---

## 18. Future Security Hardening Roadmap
- **External Secrets Operator / Vault Integration**: Dynamically inject secrets into pods from HashiCorp Vault.
- **Service Mesh mTLS**: Deploy Linkerd or Istio for automatic pod-to-pod mTLS encryption and identity attestation.
- **Runtime Security Guardrails**: Deploy Falco for eBPF runtime threat detection and syscall monitoring.
- **Container Image Signing**: Implement Cosign/Sigstore image signatures in GitHub Actions CD pipeline.
