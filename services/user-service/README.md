# DevSphere User Service

## Overview

The **User Service** is the core application domain microservice responsible for managing authenticated user profile information in DevSphere. It operates on port `8082`, exposes Prometheus metrics (`/actuator/prometheus`), consumes centralized configuration from Spring Cloud Config Server (`http://localhost:8888`), and maintains strict database ownership over `devsphere_user` with Redis distributed caching and production-grade Kafka consumer reliability.

```
Client ──► API Gateway (:8080) ──[JWT Validation]──► User Service (:8082) ──┬──► Redis Cache (:6379)
                                                                              └──► MySQL (devsphere_user)

Kafka Topic (devsphere.user.v1) ──► Consumer ──[Idempotency & Retries]──► MySQL / DLT
```

---

## Observability & Custom Metrics

- **Prometheus Metrics Endpoint**: `/actuator/prometheus`
- **Custom Business Metrics**:
  - `devsphere_kafka_events_processed_total{event_type="UserRegisteredEvent",status="success|duplicate|failure"}`
  - `devsphere_kafka_duplicate_events_total{event_type="UserRegisteredEvent"}`
  - `devsphere_kafka_events_retry_total{event_type="UserRegisteredEvent"}`
  - `devsphere_kafka_events_dlt_total{event_type="UserRegisteredEvent"}`
  - `devsphere_user_profile_created_total{source="kafka|http"}`
  - `devsphere_cache_hits_total{cache="user_profile"}`
  - `devsphere_cache_misses_total{cache="user_profile"}`

---

## Running Tests

```powershell
# Run unit and integration tests
mvn test
```
