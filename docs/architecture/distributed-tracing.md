# Distributed Tracing Foundation with OpenTelemetry

DevSphere utilizes OpenTelemetry-compatible distributed tracing powered by Micrometer Tracing and OTLP (OpenTelemetry Protocol) export.

## 1. Why Distributed Tracing?

In a production microservices platform like DevSphere, requests traverse multiple network hops, databases, caches, and asynchronous event streams. Diagnosing performance degradation or intermittent errors requires understanding the full end-to-end execution path of a single request.

## 2. Metrics vs Logs vs Traces

| Pillar | Primary Question Answered | DevSphere Tooling | Example Output |
| :--- | :--- | :--- | :--- |
| **Metrics** | *"How many requests failed?"* | Micrometer + Prometheus | `http_server_requests_seconds_count` |
| **Logs** | *"What happened during execution?"* | SLF4J / Logback | `[traceId=... spanId=...] User profile updated` |
| **Traces** | *"Where did this request spend time and fail?"* | OpenTelemetry + OTLP | Waterfall trace diagram across 4 services |

## 3. OpenTelemetry & Micrometer Tracing

DevSphere integrates **Micrometer Tracing** with the **OpenTelemetry bridge** (`micrometer-tracing-bridge-otel`).
- **Micrometer Tracing**: Provides a vendor-neutral Java tracing API.
- **OpenTelemetry Bridge**: Implements the Micrometer Tracing interface using the OpenTelemetry Java SDK.
- **OTLP Exporter**: Exports completed spans via standard OTLP over HTTP (`http://localhost:4318/v1/traces`).

## 4. W3C Trace Context Propagation

DevSphere adheres strictly to standard W3C Trace Context propagation headers:
- `traceparent`: `00-{traceId}-{spanId}-{traceFlags}` (e.g. `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`)
- `tracestate`: Vendor-specific state key-value pairs.

Arbitrary custom trace headers (like `X-Trace-Id`) are avoided in favor of W3C compliance.

## 5. Trace ID & Span ID

- **Trace ID**: A globally unique 128-bit identifier that identifies an entire distributed transaction from client entrance through microservices, databases, and Kafka consumers.
- **Span ID**: A 64-bit identifier that represents a single unit of work within a trace (e.g., a specific HTTP request handling in Gateway, a Redis cache lookup, or a Kafka consumer execution).

## 6. HTTP & Gateway Trace Propagation

```
Client
  │ (HTTP Request)
  ▼
API Gateway (:8080)
  │ [Creates/Extracts Trace Context: traceId=abc spanId=s1]
  ▼ (HTTP + W3C traceparent header)
Auth/User Service (:8081 / :8082)
  │ [Child Span: traceId=abc spanId=s2]
  └── Redis / MySQL
```

1. **API Gateway**: Intercepts incoming HTTP requests, initializes or extracts W3C trace context, creates a Gateway span, and injects `traceparent` into downstream HTTP headers (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).
2. **Downstream Microservices**: Spring Boot MVC web filters extract `traceparent`, create server spans, and bind trace context to thread MDC.

## 7. Kafka Asynchronous Trace Context Propagation

DevSphere connects asynchronous event processing to the originating HTTP request trace without modifying business event payloads:

```
Auth Service (HTTP Register)
  │ [Trace Context: traceId=abc spanId=s1]
  ▼
Transactional Outbox
  │ [Outbox Publisher Span: traceId=abc spanId=s2]
  ▼ (Kafka Record Headers: traceparent)
Kafka Broker (devsphere.user.v1)
  │ (Kafka Record Headers: traceparent)
  ▼
User Service (UserRegisteredEventConsumer)
  │ [Consumer Span: traceId=abc spanId=s3]
  ▼
MySQL (user_profiles + processed_events)
```

- **Kafka Headers**: W3C trace context is injected into Kafka record headers by `KafkaTemplate` (`setObservationEnabled(true)`).
- **Clean Event Payload**: `UserRegisteredEvent` payload remains clean (`eventId`, `eventVersion`, `eventType`, `userId`, `occurredAt`).
- **Idempotency & Reliability**: Kafka retry, backoff, DLT, and `processed_events` idempotency operate seamlessly with active trace context.

## 8. Custom Business Spans

DevSphere records stable custom spans for key domain operations:
- `auth.registration`: User credential registration.
- `auth.login`: User authentication & JWT generation.
- `outbox.publish`: Outbox event dispatching to Kafka.
- `user.profile.get`: User profile fetch / lazy creation.
- `user.profile.create`: User profile creation.
- `user.profile.update`: Profile data updates.
- `kafka.user-registered.process`: Asynchronous Kafka consumer execution.

## 9. Sensitive Data Protection

To enforce strict security and privacy:
- **Never Tagged**: Passwords, password hashes, JWT tokens, Authorization headers, email addresses, phone numbers, and full user profiles.
- **Allowed Attributes**: Stable operational attributes such as `service.operation` and `event.type`.

## 10. Log Correlation

Microservice log formats are configured with MDC keys:
```
2026-08-24T10:07:32.495+05:30 INFO 9684 --- [auth-service] [main] [4411ccd277d3e08e45a29c8c6d0a98a1-9cff8db0535e98ed] c.devsphere.auth.outbox.OutboxService : Outbox event created
```
This enables immediate correlation between log lines and distributed traces.

## 11. Trace Sampling & Configuration

- **Sampling Property**: `management.tracing.sampling.probability` (default `1.0` for local dev).
- **OTLP Endpoint**: `management.otlp.tracing.endpoint` (`http://localhost:4318/v1/traces`).
- **Centralized Management**: Managed centrally via Spring Cloud Config Server (`config-repo/application.yml`).

## 12. Resilience Integration

Tracing operates alongside Resilience4j circuit breakers, timeouts, and fallbacks:
- If a downstream service times out or circuit opens, API Gateway records the error status in the trace span before invoking the fallback controller.
- Redis cache fallbacks to MySQL record cache miss / fallback status without breaking the surrounding trace.
