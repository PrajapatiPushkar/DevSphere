# DevSphere Production Observability Architecture

This document describes the observability architecture introduced in **Lesson 14** of the DevSphere platform using **Spring Boot Actuator**, **Micrometer**, and **Prometheus**.

---

## 1. Overview & Rationale

As DevSphere scales across multiple microservices (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`), understanding system health, request throughput, latencies, failure rates, JVM heap usage, and event-driven pipeline execution becomes critical.

### Observability Pillars in DevSphere
DevSphere focuses on three operational signals:
1. **Metrics**: Quantitative numerical time-series measuring system health, request counts, latencies, and business events (scraped via Prometheus).
2. **Logs**: Contextual application logs containing structured identifiers (`service`, `eventType`).
3. **Health/Status**: Standalone endpoints (`/actuator/health`) providing dependency status indicators.

> **Note**: Distributed tracing (OpenTelemetry / Jaeger / Zipkin) is intentionally **deferred** to future lessons.

---

## 2. Target Observability Architecture

```
                         ┌──────────────────────┐
                         │      Prometheus      │
                         │        :9090         │
                         └──────────┬───────────┘
                                    │
                             scrape metrics
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
             ▼                      ▼                      ▼
       API Gateway             Auth Service           User Service
       /actuator/*             /actuator/*            /actuator/*
             │                      │                      │
             └────────────── Micrometer ──────────────────┘
```

---

## 3. Actuator & Micrometer Standardization

### Actuator Endpoint Exposure
Actuator endpoints are standardized and centralized via `config-repo/application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

Sensitive endpoints (`env`, `beans`, `configprops`, `heapdump`) are strictly restricted from public web exposure.

---

## 4. Strict Low-Cardinality Metric Labeling Policy

> [!CAUTION]
> **CRITICAL CARDINALITY RULE**
> 
> High-cardinality label values (e.g. `userId`, `email`, `eventId`, `JWT`, `offset`, raw exception messages) create unbounded metric series in Prometheus, causing memory exhaustion and storage failures.
> 
> **Permitted Labels**: Bounded, low-cardinality enum values only (`status`, `event_type`, `source`, `cache`).

### Good vs. Bad Metric Labeling Examples

| Good (Low-Cardinality) | Bad (High-Cardinality - FORBIDDEN) |
| :--- | :--- |
| `devsphere_auth_registration_total{status="success"}` | `devsphere_auth_registration_total{email="user@example.com"}` |
| `devsphere_kafka_events_processed_total{status="duplicate"}` | `devsphere_kafka_events_processed_total{event_id="evt-12345"}` |
| `devsphere_user_profile_created_total{source="kafka"}` | `devsphere_user_profile_created_total{user_id="101"}` |

---

## 5. DevSphere Custom Business Metrics

| Service | Metric Name | Tags | Description |
| :--- | :--- | :--- | :--- |
| **Auth Service** | `devsphere_auth_registration_total` | `status` (`success`, `failure`) | Counter of user registration attempts |
| **Auth Service** | `devsphere_auth_login_total` | `status` (`success`, `failure`) | Counter of user login attempts |
| **Auth Service** | `devsphere_outbox_events_published_total` | `event_type`, `status` (`success`, `failed`) | Outbox event publishing results |
| **Auth Service** | `devsphere_outbox_publish_failures_total` | `event_type` | Outbox event publishing failure count |
| **User Service** | `devsphere_kafka_events_processed_total` | `event_type`, `status` (`success`, `duplicate`, `failure`) | Kafka consumer event processing status |
| **User Service** | `devsphere_kafka_duplicate_events_total` | `event_type` | Duplicate Kafka events detected & safely skipped |
| **User Service** | `devsphere_kafka_events_retry_total` | `event_type` | Kafka consumer retry attempts |
| **User Service** | `devsphere_kafka_events_dlt_total` | `event_type` | Events routed to Dead Letter Topic (`.DLT`) |
| **User Service** | `devsphere_user_profile_created_total` | `source` (`kafka`, `http`) | Profile initialization source counter |
| **User Service** | `devsphere_cache_hits_total` | `cache` (`user_profile`) | Redis cache-aside hits |
| **User Service** | `devsphere_cache_misses_total` | `cache` (`user_profile`) | Redis cache-aside misses |

---

## 6. Standard System & Application Metrics

Micrometer automatically collects standard JVM and HTTP metrics:
- `http.server.requests`: Request throughput, status code distribution (`2xx`, `4xx`, `5xx`), and response latency timers.
- `jvm.memory.used` / `jvm.memory.max`: JVM heap and non-heap memory usage.
- `jvm.gc.pause`: Garbage collection pause durations.
- `jvm.threads.live`: Live thread counts.
- `process.uptime`: Service process uptime seconds.

---

## 7. Prometheus Infrastructure Scraping

Prometheus configuration is defined in `infrastructure/monitoring/prometheus.yml`.

### Scrape Configuration
```yaml
scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']

  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']

  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8082']
```

---

## 8. Limitations & Deferred Capabilities

- **Grafana Dashboards**: Visualization UI is deferred to future lessons.
- **Alertmanager**: Prometheus alerting rules and notification channels are deferred.
- **Distributed Tracing**: OpenTelemetry trace context propagation is deferred.
