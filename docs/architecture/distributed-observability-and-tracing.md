# Distributed Observability & Tracing Architecture

This document describes the distributed observability architecture, W3C trace context propagation, MDC log correlation, Actuator Liveness and Readiness probes, low-cardinality metric tag policies, and security guardrails in DevSphere (`api-gateway`, `user-service`, `auth-service`).

---

## 1. Executive Summary

Lesson 52 implements an enterprise-grade **Distributed Observability & Tracing Foundation** across DevSphere microservices.

The architecture allows end-to-end diagnosis of any request across service boundaries:

```text
                    ┌───────────────┐
                    │     Client    │
                    └───────┬───────┘
                            │
               W3C Header: traceparent / X-Trace-Id
                            │
                    ┌───────▼───────┐
                    │ API Gateway   │ (TracePropagationGlobalFilter)
                    └───────┬───────┘
                            │
               Propagated Trace Context
                            │
       ┌────────────────────┴────────────────────┐
       │                                         │
┌──────▼────────┐                       ┌────────▼──────┐
│  user-service │                       │  auth-service │
└──────┬────────┘                       └────────┬──────┘
       │                                         │
 ┌─────┴─────┐                             ┌─────┴─────┐
 │ MySQL/    │                             │ MySQL/    │
 │ Redis     │                             │ Kafka     │
 └───────────┘                             └───────────┘
```

---

## 2. Distributed Trace Propagation Flow

### Standard Headers
- `traceparent`: W3C Standard format (`00-{traceId}-{spanId}-{flags}`)
- `tracestate`: Vendor-specific state context
- `X-Trace-Id`: High-visibility trace correlation ID forwarded downstream and echoed in HTTP response headers.

### Gateway Trace Filter (`TracePropagationGlobalFilter`)
1. Inspects incoming request headers for `traceparent` or `X-Trace-Id`.
2. Extracts or generates a unique 128-bit trace ID.
3. Mutates downstream request headers to forward `X-Trace-Id` and `traceparent`.
4. Injects `X-Trace-Id` into the client HTTP response headers.

---

## 3. Structured Logging & MDC Correlation

All microservices write logs formatted with MDC trace correlation variables:

```text
pattern.level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Log Level Guidelines
- `DEBUG`: Internal execution flow & developer diagnostics.
- `INFO`: Business events (user creation, profile update, outbox publication).
- `WARN`: Degraded conditions (Redis connection timeout fallback, bulkhead limit reached).
- `ERROR`: Unhandled exceptions and system failures.

---

## 4. Health & Readiness Probes

Spring Boot Actuator health endpoints are configured with distinct liveness and readiness state separation:

```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
    probes:
      enabled: true
```

- **Liveness Probe (`/actuator/health/liveness`)**: Evaluates process viability. Transient Redis/MySQL outages do **NOT** fail liveness checks, preventing cascading container restarts.
- **Readiness Probe (`/actuator/health/readiness`)**: Evaluates capacity to serve traffic.
- **Security**: `show-details: when_authorized` hides DB connection strings, passwords, or secrets from unauthenticated callers.

---

## 5. Low-Cardinality Metric Tag Policy

To prevent Prometheus memory exhaustion, metric tags MUST remain strictly low-cardinality strings.

### Permitted Tags
- `service`: `user-service`, `auth-service`, `api-gateway`
- `status`: `success`, `failure`, `not_found`, `duplicate`
- `operation`: `compile`, `export`, `login`, `register`
- `format`: `html`, `pdf`, `docx`
- `template`: `professional`, `modern`, `minimal`
- `cache`: `user_profile`, `public_resume`

### Strictly Prohibited High-Cardinality Tags
- `userId`, `resumeId`, `versionId`, `email`, `JWT`, `publicResumeId`, `traceId`, `spanId`, `requestId`.
