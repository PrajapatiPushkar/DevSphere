# 22. Kubernetes Security Hardening and Network Isolation

Date: 2026-08-25

## Status

Accepted

## Context

Lesson 21 established Kubernetes Deployments and ClusterIP Services for all DevSphere microservices, and Lesson 22 established the external Ingress perimeter and TLS termination. However, default Kubernetes cluster configurations allow arbitrary pod-to-pod east-west network communication and default identity access to the Kubernetes API server.

To achieve a production-grade defense-in-depth security posture, DevSphere requires workload identity isolation, explicit Kubernetes API token disabling, strict Pod Security Admission controls, seccomp profiling, and NetworkPolicies enforcing default-deny traffic flow.

## Decision

We adopt a **least-privilege Kubernetes security model** for DevSphere, assuming that all network traffic and workload identities are denied unless explicitly permitted.

Key architectural decisions include:

1. **Dedicated ServiceAccounts**: Create distinct ServiceAccounts (`devsphere-api-gateway`, `devsphere-auth-service`, `devsphere-user-service`, `devsphere-config-server`, `devsphere-service-discovery`) for workload identity separation.
2. **Kubernetes API Access Isolation**: Set `automountServiceAccountToken: false` on all ServiceAccounts and Deployments to prevent exposing API server tokens to application containers.
3. **Zero Kubernetes RBAC Permissions**: Create NO RoleBindings or ClusterRoleBindings for application ServiceAccounts. DevSphere applications require zero Kubernetes API permissions.
4. **Pod Security Admission & Context**: Enforce Pod Security Admission `restricted` standards at the namespace level. Enforce container-level security:
   - `runAsNonRoot: true` with dedicated user/group IDs (`10001`)
   - `allowPrivilegeEscalation: false`
   - `readOnlyRootFilesystem: true` with ephemeral memory-backed `/tmp` mounts (`emptyDir`)
   - `capabilities.drop: ["ALL"]`
   - `seccompProfile: { type: RuntimeDefault }`
5. **Default-Deny Network Policies**: Deploy namespace-wide `default-deny-all` NetworkPolicy blocking all ingress and egress by default.
6. **Controlled East-West Allow Policies**: Define explicit `networking.k8s.io/v1` NetworkPolicies:
   - Allow DNS egress (port 53 UDP/TCP) for name resolution.
   - Allow Ingress Controller to access API Gateway on port 8080.
   - Allow API Gateway egress to Auth (8081), User (8082), Config Server (8888), Eureka (8761), and Redis (6379).
   - Allow Auth Service ingress from Gateway (8081) and egress to Config Server (8888), Eureka (8761), Kafka (9092/29092), and MySQL (3306).
   - Allow User Service ingress from Gateway (8082) and egress to Config Server (8888), Eureka (8761), Kafka (9092/29092), Redis (6379), and MySQL (3306).
   - Allow Config Server ingress on port 8888 from Gateway, Auth, User, and Eureka.
   - Allow Eureka ingress on port 8761 from Gateway, Auth, User, and Config Server.
   - Restrict external infrastructure egress (Kafka, Redis, MySQL) to dedicated target ports.

## Consequences

### Positive / Benefits
- **Least Privilege Workload Identity**: Prevents service account reuse and eliminates default identity token mounting.
- **Minimized Attack Surface**: Dropping Linux capabilities, forcing non-root execution, enforcing read-only root filesystems, and applying seccomp profiles prevent privilege escalation and container breakouts.
- **East-West Network Isolation**: Default-deny NetworkPolicies prevent unauthorized pod-to-pod traversal and block arbitrary internet egress.
- **Zero Kubernetes API Exposure**: Pod compromise cannot be leveraged to query or modify Kubernetes cluster resources.
- **Clean Application/Infrastructure Boundary**: No modifications required to JWT auth, Spring Security filters, event schemas, or database logic.

### Negative / Tradeoffs
- **CNI Dependency**: NetworkPolicies require a CNI plugin with policy support (e.g. Calico, Cilium, Weave Net). Standard default CNI drivers (like default flannel/bridge) ignore NetworkPolicies at runtime.
- **Policy Maintenance**: Infrastructure policy updates required whenever new internal microservices or ports are added.
- **Debugging Overhead**: Troubleshooting connectivity failures requires inspecting NetworkPolicy selectors and CNI logs.

## Future Work

- **External Secrets Manager**: Integrate Vault or External Secrets Operator for secret lifecycle management.
- **Service Mesh mTLS**: Evaluate Istio or Linkerd for transparent pod-to-pod mTLS encryption and identity verification.
- **Runtime Security Monitoring**: Integrate Falco or eBPF-based runtime anomaly detection.
