# 16. Distributed Tracing Foundation with OpenTelemetry

Date: 2026-08-24

## Status

Accepted

## Context

DevSphere is a production-oriented distributed microservices platform. Metrics answer *"How many requests failed?"* and application logs answer *"What happened?"*, but neither can answer *"Where did this particular request spend its time and where did it fail across distributed services?"*.

To diagnose cross-service latency bottlenecks, trace asynchronous event flows (Auth Service → Outbox → Kafka → User Service), and correlate logs across microservices, DevSphere requires a production-grade distributed tracing foundation.

## Decision

We adopt **Micrometer Tracing with OpenTelemetry bridge** and **OTLP export** for vendor-neutral distributed tracing:

1. **Instrumentation & Abstraction**:
   - Integrate `micrometer-tracing-bridge-otel` across all DevSphere microservices (`API Gateway`, `Auth Service`, `User Service`, `Service Discovery`, `Config Server`).
   - Use automatic Spring Boot instrumentation for HTTP server requests, HTTP client requests, WebClient, and Spring Kafka.
   - Add targeted custom business spans (`auth.registration`, `auth.login`, `outbox.publish`, `user.profile.get`, `user.profile.create`, `user.profile.update`, `kafka.user-registered.process`) for domain visibility.

2. **W3C Trace Context Propagation**:
   - Enforce standard W3C Trace Context propagation headers (`traceparent`, `tracestate`) across HTTP request paths.
   - Propagate trace context across asynchronous event flows via Kafka record headers (`Headers`), preserving business event payloads (`UserRegisteredEvent`) and outbox schema integrity.

3. **Vendor-Neutral OTLP Export**:
   - Use `opentelemetry-exporter-otlp` configured via `management.otlp.tracing.endpoint` (`http://localhost:4318/v1/traces`).
   - Avoid lock-in to specific backends like Zipkin, Jaeger, or Tempo; export standardized OpenTelemetry spans.

4. **Log-Trace Correlation**:
   - Configure pattern logging across services to include `%X{traceId:-}` and `%X{spanId:-}` in log entries.

5. **Configurable Trace Sampling**:
   - Provide centralized probability sampling configured via `management.tracing.sampling.probability` (default `1.0` in dev, configurable for production).

## Consequences

### Positive
- **End-to-End Visibility**: Track individual requests across Gateway → Service → Redis/MySQL and event-driven Kafka pipelines.
- **Log Correlation**: Seamlessly connect application logs to distributed trace IDs for immediate failure diagnosis.
- **Vendor Neutrality**: Standardized OTLP output allows swapping trace backends without application code changes.
- **Security & Event Integrity**: Preserves business event schemas and protects sensitive credentials (passwords, JWTs) from span attributes.

### Negative / Trade-offs
- **Runtime Overhead**: Capturing and exporting spans adds minor CPU and memory overhead.
- **Trace Storage Costs**: 100% sampling at high scale requires substantial trace storage capacity.
- **Instrumentation Complexity**: Requires proper header propagation setup across HTTP and messaging boundaries.

## Future Extensions
- Introduce a production trace backend (e.g., Grafana Tempo or Jaeger) and visualization dashboard.
- Implement tail-based sampling to selectively retain error and high-latency traces in high-throughput environments.
