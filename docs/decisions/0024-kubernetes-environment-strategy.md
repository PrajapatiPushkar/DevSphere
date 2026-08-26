# ADR 0024: Kubernetes Environment Overlays & Production Deployment Strategy

- **Status**: Accepted
- **Date**: 2026-08-26
- **Context**: Lessons 21–24 established a production-grade Kubernetes deployment architecture (including Ingress, TLS, Security Hardening, NetworkPolicies, HPAs, and PodDisruptionBudgets), but all resources represented a single environment. As the platform prepares for continuous delivery, DevSphere requires isolated configurations for Development, Staging, and Production without code duplication or manifest drift.

---

## Decision

We adopt **Kustomize Base and Environment Overlays** (`development`, `staging`, `production`) for managing Kubernetes deployment manifests in DevSphere.

1. **Shared Base (`infrastructure/kubernetes/base/`)**:
   - Contains all common microservice Deployments, ClusterIP Services, NGINX Ingress base, NetworkPolicies, ServiceAccounts, HPAs, and PodDisruptionBudgets.
   - Manifests are generic and namespace-agnostic.

2. **Environment Overlays (`infrastructure/kubernetes/overlays/`)**:
   - **`development`**: Targets namespace `devsphere-dev`. Disables HPAs and PDBs for single-node cluster compatibility. Sets replica counts to 1 and configures ingress host `api.devsphere.local`.
   - **`staging`**: Targets namespace `devsphere-staging`. Enables HPAs, PDBs, and multi-replica execution. Configures ingress host `staging-api.devsphere.example.com` with TLS and updates container images using immutable commit SHA tags.
   - **`production`**: Targets production namespace `devsphere`. Enables full HA (HPA min 2, max 10, PDB `minAvailable: 1`, topology spreading). Configures ingress host `api.devsphere.example.com` with TLS and mandates immutable commit SHA tags/digests.

3. **Build Once, Promote Many**:
   - Container images are built and published once to GitHub Container Registry (GHCR) with immutable tags (`sha-<commit>`). The identical image tested in staging is promoted to production.

---

## Consequences & Benefits

- **Zero Manifest Duplication**: Common pod specs, security contexts, probes, and network policies are maintained in a single base directory.
- **Environment Isolation**: Workloads run in dedicated namespaces (`devsphere-dev`, `devsphere-staging`, `devsphere`), preventing accidental cross-environment resource access.
- **Local Developer Ergonomics**: The development overlay can run on local Kubernetes distributions (kind, minikube, Docker Desktop) without requiring Metrics Server or multi-node scheduling constraints.
- **Immutable Artifact Promotion**: Production deployments never use mutable `:latest` or `-SNAPSHOT` tags, enabling reliable, reproducible releases and instant rollbacks.
- **Hardened Perimeter & Micro-segmentation**: All overlays preserve Pod Security Admission `restricted` enforcement, non-root user execution, read-only root filesystems, dropped capabilities, and default-deny NetworkPolicies.

---

## Tradeoffs & Considerations

- **Kustomize Learning Curve**: Requires developers to understand Kustomize patch syntax and transformation transformers (`images`, `replicas`, `configMapGenerator`).
- **Configuration Boundary Maintenance**: Developers must clearly distinguish between application logic properties (managed via Spring Cloud Config) and infrastructure orchestration properties (managed via Kustomize).

---

## Future Outlook

- **GitOps Continuous Deployment**: Integration with GitOps operators (e.g. ArgoCD or Flux) to monitor overlay updates.
- **Automated Promotion Pipeline**: GitHub Actions workflows for staging deployment and gated production promotion.
- **External Secret Management**: Future integration of secret injection operators to source database and JWT credentials dynamically.
