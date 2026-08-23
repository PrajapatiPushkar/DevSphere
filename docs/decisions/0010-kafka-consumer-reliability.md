# 10. Production-Grade Kafka Consumer Reliability

Date: 2026-08-23

## Status

Accepted

## Context

In Lesson 8 & Lesson 10, event publishing was hardened via the Transactional Outbox pattern. However, Kafka consumption in `User Service` lacked production-grade resilience against duplicate event deliveries, transient database/network outages, and poison messages (invalid JSON, unsupported event versions).

Kafka guarantees **at-least-once** delivery semantics. Downstream consumers must handle re-deliveries safely without corrupting business state or blocking event processing partitions indefinitely.

## Decision

We implement production-grade Kafka consumer reliability in `User Service`:

1. **Database-Backed Idempotency**:
   - Created a dedicated `processed_events` table in `devsphere_user` database with a `UNIQUE(event_id)` constraint.
   - `eventId` from `UserRegisteredEvent` is used as the unique identity key.
   - Duplicate events are detected, safely acknowledged, and skipped without triggering retries or DLT routing.

2. **Atomic Database Transaction Boundary**:
   - `user_profiles` creation and `processed_events` persistence occur inside the same JPA `@Transactional` boundary.
   - Both operations commit together or roll back completely upon failure.

3. **Controlled Retries & Backoff**:
   - Configured `DefaultErrorHandler` with configurable retry count (`app.kafka.consumer.max-attempts`, default `3`) and backoff (`app.kafka.consumer.retry-backoff-ms`, default `1000ms`).

4. **Dead Letter Topic (DLT)**:
   - Configured `DeadLetterPublishingRecoverer` to route failed/poison records to `devsphere.user.v1.DLT` after retry exhaustion.
   - Preserves original topic, partition, offset, payload, and exception headers.
   - Configured `ErrorHandlingDeserializer` to handle JSON deserialization failures cleanly via DLT.
   - DLT is treated as an operational holding area and is not auto-consumed in an infinite retry loop.

## Consequences

### Positive
- **Duplicate Protection**: Zero duplicate profile creations across network retries or application crashes.
- **Poison Message Isolation**: Unprocessable messages route to DLT without blocking valid events.
- **Operational Auditability**: Full metadata and exception details captured in DLT headers.
- **Resilient Processing**: Graceful recovery from temporary database connectivity hiccups.

### Negative / Trade-offs
- Additional database table (`processed_events`) and Flyway migration (`V2__create_processed_events.sql`).
- Requires operational monitoring and eventual replay tooling for messages in DLT.
