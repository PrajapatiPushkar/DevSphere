# 55. Transactional Outbox Pattern for Reliable Event Publication

* **Status**: Accepted
* **Impacted Components**: `user-service`, `auth-service`
* **Date**: 2026-09-01

---

## Context

Following the integration of Apache Kafka in Lesson 56, DevSphere faced the **dual-write problem**: executing local database state changes (`@Transactional`) and publishing messages directly to remote Kafka topics inside the same HTTP thread boundary risks data inconsistency. If the local database commit fails after sending a Kafka record, downstream consumers observe phantom events; conversely, if Kafka network calls fail post-commit, domain events are permanently lost.

We required a production-grade, reliable mechanism to guarantee that domain events are persisted inside the **exact same database transaction** as business operations before being published to Kafka.

---

## Decision

1. **Outbox Schema Contract (`outbox_events`)**:
   - Created `outbox_events` table in `user-service` via Flyway (`V18__create_outbox_events_table.sql`).
   - Defined columns for `event_id` (UUID), `aggregate_type`, `aggregate_id` (Kafka message key), `event_type`, `event_version`, `payload` (JSON), `status` (`PENDING`, `PUBLISHED`, `FAILED`), `retry_count`, `last_error`, `created_at`, and `published_at`.
   - Added unique constraint on `event_id` and composite index on `(status, created_at)`.

2. **Atomic Outbox Persistence (`OutboxService` & `KafkaDomainEventPublisher`)**:
   - Refactored `KafkaDomainEventPublisher` to write `OutboxEvent` entities inside the active business `@Transactional` context via `OutboxService`.
   - Business operations (`ResumeVersionService`, etc.) continue calling `domainEventPublisher.publish(event)` cleanly without direct dependency on Kafka APIs.
   - If the business database transaction rolls back, the corresponding outbox record automatically rolls back.

3. **Asynchronous Scheduled Outbox Publisher (`OutboxPublisher`)**:
   - Implemented `@Scheduled` background worker (`app.outbox.polling-interval=1000`) that queries pending outbox events ordered by creation time.
   - Transmits ProducerRecords to target Kafka topics (`devsphere.domain.events`, `devsphere.user.v1`), attaching MDC `traceId` headers (`X-Trace-Id`) and aggregate message keys.
   - Upon successful ACK, updates record status to `PUBLISHED` with timestamp.
   - Upon transient publication errors, updates `retry_count` and records error messages. Once `retry_count >= maxRetries` (default `5`), updates status to `FAILED`.

4. **Observability & Delivery Semantics**:
   - Registered low-cardinality Micrometer metrics: `devsphere.outbox.events.published.total` and `devsphere.outbox.publish.failures.total`.
   - Guarantees reliable **at-least-once publication** to Kafka topics.

---

## Consequences

* **Positive**:
  - Completely eliminates the dual-write problem across database transactions and message publication.
  - Business domain code remains clean, decoupled, and agnostic of Kafka APIs.
  - Guaranteed trace context preservation (`traceId`) across outbox + Kafka boundary.
  - High resilience during temporary Kafka broker downtime or network degradation.
* **Trade-offs / Future Scope**:
  - Outbox publisher provides at-least-once delivery; consumer-side duplicate handling and deduplication tables are reserved for **Lesson 58**.
