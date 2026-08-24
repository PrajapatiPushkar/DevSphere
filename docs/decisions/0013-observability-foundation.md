# 13. Production Observability Foundation

- **Status**: Accepted
- **Date**: 2026-08-24
- **Deciders**: DevSphere Core Engineering Team

---

## Context

As DevSphere scales into a distributed microservices platform (`api-gateway`, `auth-service`, `user-service`, `service-discovery`, `config-server`), obtaining operational visibility into application health, HTTP traffic throughput, request latencies, JVM memory utilization, Kafka consumer processing, transactional outbox reliability, duplicate detection, DLT routing, and cache efficiency is required for production engineering.

---

## Decision

We adopt **Spring Boot Actuator**, **Micrometer**, and **Prometheus** to establish the initial production observability foundation for DevSphere.

### Key Implementation Choices:
1. **Actuator & Micrometer Prometheus Integration**:
   - Add `io.micrometer:micrometer-registry-prometheus` across microservices to format metrics for Prometheus scraping (`/actuator/prometheus`).
   - Restrict Actuator web exposure centrally in `config-repo/application.yml`: `health,info,prometheus`.
2. **Custom Low-Cardinality Business Metrics**:
   - Instrument **Auth Service**: `devsphere_auth_registration_total`, `devsphere_auth_login_total`, `devsphere_outbox_events_published_total`, `devsphere_outbox_publish_failures_total`.
   - Instrument **User Service**: `devsphere_kafka_events_processed_total`, `devsphere_kafka_duplicate_events_total`, `devsphere_kafka_events_retry_total`, `devsphere_kafka_events_dlt_total`, `devsphere_user_profile_created_total`, `devsphere_cache_hits_total`, `devsphere_cache_misses_total`.
3. **Strict Cardinality Guardrails**:
   - Prohibit high-cardinality user identifiers (`userId`, `email`, `eventId`, `JWT`) in metric labels to prevent Prometheus memory leaks.
4. **Scrape Infrastructure**:
   - Maintain static Prometheus scrape targets in `infrastructure/monitoring/prometheus.yml`.
5. **Deferred Monitoring Stack Components**:
   - Grafana dashboards, Alertmanager, and OpenTelemetry distributed tracing are intentionally **deferred** to future lessons to avoid over-engineering.

---

## Consequences

### Positive
- **Standardized Operational Metrics**: Provides out-of-the-box JVM, memory, GC, and HTTP server request latency/throughput metrics (`http.server.requests`).
- **Domain Pipeline Visibility**: Exposes real-time counters for user registrations, outbox publishing, Kafka retries, duplicate events, DLT routing, and Redis cache performance.
- **Prometheus Scraping Compatibility**: Exposes standard `/actuator/prometheus` scraping endpoints.

### Negative / Tradeoffs
- **Additional Memory Overhead**: Micrometer meter registries maintain in-memory counters.
- **Cardinality Management**: Developers must enforce strict low-cardinality tagging rules for all future custom metrics.
