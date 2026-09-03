# Kubernetes Configuration & Secrets Architecture — Lesson 64

## 1. Overview & Strategy

This document details the configuration externalization and secret management strategy for the **DevSphere** microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`).

DevSphere enforces a strict three-tier configuration hierarchy:

```text
┌─────────────────────────────────────────────────────────┐
│                    Kubernetes ConfigMap                 │
│  (Non-sensitive environment properties & Cluster DNS)   │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                    Kubernetes Secrets                   │
│   (Sensitive DB passwords, JWT secrets, SASL keys)      │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                Spring Cloud Config Server               │
│     (Application domain properties & feature flags)     │
└─────────────────────────────────────────────────────────┘
```

---

## 2. ConfigMap Architecture (`devsphere-configmap`)

ConfigMaps contain exclusively non-sensitive environment configuration.

### Baseline Properties
- `SPRING_PROFILES_ACTIVE`: `prod`
- `CONFIG_SERVER_URL`: `http://devsphere-config-server.devsphere.svc.cluster.local:8888`
- `EUREKA_SERVER_URL`: `http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: `http://devsphere-service-discovery.devsphere.svc.cluster.local:8761/eureka/`
- `SPRING_REDIS_HOST`: `devsphere-redis`
- `SPRING_REDIS_PORT`: `6379`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: `devsphere-kafka:9092`
- `MYSQL_HOST`: `devsphere-mysql`
- `MYSQL_PORT`: `3306`
- `APP_RATE_LIMIT_ENABLED`: `true`
- `APP_RATE_LIMIT_FAIL_OPEN`: `true`

---

## 3. Secret Protection Strategy (`devsphere-secrets`)

Sensitive values are managed via Kubernetes `Secret` resources (`Opaque` type).

### Sensitive Parameters
- `MYSQL_PASSWORD`: Database authentication password.
- `SPRING_REDIS_PASSWORD`: Redis authentication password.
- `JWT_SECRET`: Base64-encoded 256-bit signing key for JWT token validation.
- `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG`: Kafka SASL authentication configuration.

### Git Security Rules
1. Real `secret.yaml` files containing actual passwords are strictly ignored by `.gitignore`.
2. Standard structural templates (`secrets.example.yaml` and `secret.example.yaml`) provide placeholders (`CHANGE_ME`) for reference.
3. No plain-text passwords or secret keys are committed to Git repositories, image layers, or logs.

---

## 4. Environment Injection in Deployments

Deployments inject configuration properties using standard Kubernetes container specifications:

```yaml
envFrom:
  - configMapRef:
      name: devsphere-configmap
  - secretRef:
      name: devsphere-secrets
      optional: true
```

### Fallback & Safety
`optional: true` on secret references guarantees that dry-run manifest synthesis and local testing succeed without requiring pre-existing cluster secrets.

---

## 5. Configuration Precedence Boundary

Spring Boot resolves configuration in the following order (highest to lowest priority):

1. **Environment Variables**: Injected by Kubernetes Deployment `env`/`envFrom`.
2. **Config Server Properties**: Fetched dynamically from `config-server`.
3. **Application Properties**: Bundled `application.yml` defaults.

Kubernetes environment variables override local defaults while allowing Config Server to manage application-level domain settings.

---

## 6. Secret Management Lifecycle

### Local Development
- Uses `.env.example` templates and local Docker Compose / local Kubernetes Secrets with development-safe credentials.

### Staging & Kubernetes Testing
- Uses environment-specific Kubernetes Secrets populated via secure CI/CD pipelines.

### Production Roadmap
- Production clusters integrate external secret providers (HashiCorp Vault / External Secrets Operator) to synchronize secrets directly from secret stores into Kubernetes Secrets automatically.

---

## 7. Verification & Manifest Synthesis

Synthesize all configuration resources statically using `kubectl`:

```bash
kubectl kustomize k8s/
```
