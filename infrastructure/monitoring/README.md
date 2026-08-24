# DevSphere Monitoring & Observability Infrastructure

Prometheus metrics scraping configuration for DevSphere microservices.

## Overview

This directory provides Prometheus configuration (`prometheus.yml`) to scrape application metrics, JVM metrics, HTTP latency/throughput, and custom business metrics exposed via Spring Boot Actuator endpoints (`/actuator/prometheus`).

## Scrape Targets

| Service | Target Port | Metrics Path | Description |
| :--- | :---: | :--- | :--- |
| **API Gateway** | `8080` | `/actuator/prometheus` | HTTP request routing, latency, status codes |
| **Auth Service** | `8081` | `/actuator/prometheus` | Registration, login, outbox event publishing |
| **User Service** | `8082` | `/actuator/prometheus` | Kafka event consumption, idempotency, retries, DLT, profile creation, Redis cache |
| **Service Discovery** | `8761` | `/actuator/prometheus` | Eureka server JVM & memory metrics |
| **Config Server** | `8888` | `/actuator/prometheus` | Config server JVM & memory metrics |

## Local Prometheus Startup

To run a local Prometheus instance scraping DevSphere microservices:

```bash
prometheus --config.file=infrastructure/monitoring/prometheus.yml
```

Once running, access the Prometheus UI at:
- `http://localhost:9090`

## Key DevSphere Business Metrics

- `devsphere_auth_registration_total{status="success|failure"}`
- `devsphere_auth_login_total{status="success|failure"}`
- `devsphere_outbox_events_published_total{event_type="UserRegisteredEvent",status="success|failed"}`
- `devsphere_outbox_publish_failures_total{event_type="UserRegisteredEvent"}`
- `devsphere_kafka_events_processed_total{event_type="UserRegisteredEvent",status="success|duplicate|failure"}`
- `devsphere_kafka_duplicate_events_total{event_type="UserRegisteredEvent"}`
- `devsphere_kafka_events_retry_total{event_type="UserRegisteredEvent"}`
- `devsphere_kafka_events_dlt_total{event_type="UserRegisteredEvent"}`
- `devsphere_user_profile_created_total{source="kafka|http"}`
- `devsphere_cache_hits_total{cache="user_profile"}`
- `devsphere_cache_misses_total{cache="user_profile"}`
- `devsphere_rate_limit_requests_total{result="allowed|rejected|error"}`
- `devsphere_rate_limit_rejected_total{route="auth-register|auth-login|auth-service|user-service"}`
- `devsphere_resilience_fallback_total{service="auth-service|user-service|gateway",dependency="http|redis"}`
- `resilience4j_circuitbreaker_state{name="authServiceCircuitBreaker|userServiceCircuitBreaker",state="closed|open|half_open"}`
- `resilience4j_circuitbreaker_calls_total{name=...,kind="successful|failed|ignored|not_permitted"}`

## Distributed Tracing & OTLP Export

DevSphere uses **Micrometer Tracing** with **OpenTelemetry bridge** and **OTLP export** for vendor-neutral distributed tracing across all services.

### Tracing Architecture

- **Metrics**: Exposed via `/actuator/prometheus` -> Scraped by Prometheus (`:9090`).
- **Traces**: Exported via OpenTelemetry OTLP -> Exporter endpoint (`http://localhost:4318/v1/traces`).
- **Logs**: Formatted with MDC trace correlation (`[traceId-spanId]`).

### Key Trace Configuration

- `management.tracing.sampling.probability`: Default `1.0` (100% sampling for dev).
- `management.otlp.tracing.endpoint`: `http://localhost:4318/v1/traces`.
- Propagation: Standard W3C Trace Context (`traceparent` HTTP header & Kafka record headers).

### Low-Cardinality Security Rule

> [!IMPORTANT]
> Prometheus metric labels and trace span tags must never include dynamic user identifiers, credentials, or sensitive data (`userId`, `email`, `password`, `JWT`). Tags are restricted to static, bounded strings (`service.operation`, `event.type`).

