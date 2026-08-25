# 21. Kubernetes Ingress, TLS, and External Access Foundation

Date: 2026-08-25

## Status

Accepted

## Context

Lesson 21 established Kubernetes Deployments and ClusterIP Services for all DevSphere microservices. However, ClusterIP services are accessible only from within the Kubernetes cluster network.

To serve external clients safely in production, DevSphere requires a secure perimeter entry point that manages HTTP/HTTPS traffic, terminates TLS encryption, preserves client IP addresses for rate limiting, and routes requests to the API Gateway while keeping all downstream domain services private.

## Decision

We adopt **Kubernetes Ingress** (`networking.k8s.io/v1`) as the external HTTP/HTTPS entry point for DevSphere and create dedicated perimeter manifests ([`infrastructure/kubernetes/gateway/ingress.yaml`](file:///infrastructure/kubernetes/gateway/ingress.yaml)).

Key architectural decisions include:

1. **Gateway Perimeter Exposure**: Route external host traffic (`api.devsphere.example.com`) exclusively to `devsphere-api-gateway` on port `8080`.
2. **Strict Internal Service Isolation**: Maintain `auth-service`, `user-service`, `config-server`, `service-discovery`, Kafka, Redis, and MySQL as private `ClusterIP`-only services without public Ingress routes.
3. **TLS Termination & Secret Reference**: Terminate TLS at the Ingress boundary referencing Secret `devsphere-api-tls`. Provide template [`tls-secret.example.yaml`](file:///infrastructure/kubernetes/config/tls-secret.example.yaml) with `CHANGE_ME` placeholders while ignoring real key files (`tls-secret.yaml`, `*.crt`, `*.key`) in `.gitignore`.
4. **Header & Context Preservation**: Preserve `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host` headers for Redis rate limiting and W3C `traceparent` headers for OpenTelemetry distributed tracing.
5. **Operational Endpoint Protection**: Exclude Actuator endpoints (`/actuator/**`) from public Ingress routing.

## Consequences

### Positive / Benefits
- **Single External Entry Point**: Centralizes external traffic handling through API Gateway.
- **Enhanced Perimeter Security**: Internal microservices remain inaccessible from the public internet.
- **TLS Handshake Offloading**: Offloads SSL/TLS encryption overhead from Java microservice containers to the Ingress Controller.
- **Rate Limit & Tracing Integrity**: Header preservation guarantees client IP rate limiting and OpenTelemetry tracing compatibility.

### Negative / Tradeoffs
- **Ingress Controller Requirement**: Requires an active Ingress Controller (e.g. NGINX Ingress / Traefik) running in the cluster.
- **Certificate Lifecycle**: Requires manual or automated certificate provisioning and secret management.

## Future Work

- **Cert-Manager Integration**: Automate Let's Encrypt TLS certificate issuance and renewal.
- **Cloud Load Balancers**: Integrate AWS ALB Controller or GCP Cloud Load Balancing.
- **Web Application Firewall (WAF)**: Deploy WAF rules for SQLi/XSS inspection.
- **NetworkPolicies**: Enforce pod-to-pod network firewall rules.
