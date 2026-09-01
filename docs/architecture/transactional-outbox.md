# Transactional Outbox Pattern Architecture

This document describes the Transactional Outbox Pattern implementation in DevSphere (`user-service`, `auth-service`), addressing the dual-write problem between database state transitions and Kafka message publication.

---

## 1. Dual-Write Problem Context

In event-driven microservice architectures, updating a database and publishing an event to a message broker (Kafka) in a single business operation introduces a fundamental consistency challenge known as the **dual-write problem**:

```text
               Business Operation (e.g. Publish Resume Version)
                                      │
                   ┌──────────────────┴──────────────────┐
                   │                                     │
                   ▼                                     ▼
        Database Transaction                      Kafka Publish
     (Local SQL ACID Commit)                  (Remote Network I/O)
```

### Failure Scenarios Solved by Transactional Outbox:
- **Scenario A (DB Commit Fails, Kafka Succeeds)**: Remote Kafka receives an event for data that was rolled back locally, producing phantom events in downstream services.
- **Scenario B (DB Commit Succeeds, Kafka Fails)**: Local state is saved, but network timeout / Kafka unavailability drops the event permanently, leading to inconsistent secondary state.
- **Scenario C (Application Crash Post-DB Commit)**: Application crashes after committing SQL changes but before Kafka network call executes.

---

## 2. Target Outbox Architecture

The Transactional Outbox Pattern guarantees that event creation and database updates execute inside the **exact same database transaction**:

```text
Business Service (e.g. ResumeVersionService)
       │
       ▼ @Transactional
┌───────────────────────────────────────────────────────────┐
│ Database Transaction                                      │
│                                                           │
│ 1. UPDATE business tables (e.g. resume_versions)          │
│ 2. INSERT into outbox_events table (status = PENDING)     │
└─────────────────────────────┬─────────────────────────────┘
                              │
                        COMMIT (Atomic)
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│ OutboxPublisher (@Scheduled Poller)                       │
│                                                           │
│ 1. SELECT * FROM outbox_events WHERE status = 'PENDING'   │
│ 2. Transmit ProducerRecord to Kafka (with trace headers)  │
│ 3. On ACK success: UPDATE status = 'PUBLISHED'            │
│ 4. On Error: increment retry_count, status = 'FAILED'     │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
                 Apache Kafka Topic Stream
```

---

## 3. Database Schema Contract (`outbox_events`)

```sql
CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_version INT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_user_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_user_outbox_status_created ON outbox_events(status, created_at);
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT` | Primary key identity |
| `event_id` | `VARCHAR(36)` | Unique domain event UUID |
| `aggregate_type` | `VARCHAR(50)` | Business aggregate name (`"RESUME_PROFILE"`, `"USER"`) |
| `aggregate_id` | `VARCHAR(50)` | Business aggregate identifier (`resumeProfileId`, `userId`) used as Kafka key |
| `event_type` | `VARCHAR(50)` | Past-tense domain event type (`"ResumeVersionPublished"`) |
| `event_version` | `INT` | Event schema version (default `1`) |
| `payload` | `TEXT` | Serialized JSON event payload (with `traceId` context) |
| `status` | `VARCHAR(20)` | `PENDING`, `PUBLISHED`, or `FAILED` |
| `retry_count` | `INT` | Incremented upon publication attempt failure |
| `last_error` | `TEXT` | Diagnostic error message from failed publication attempts |

---

## 4. Lifecycle & Delivery Semantics

1. **Atomic Outbox Persistence**:
   - `DomainEventPublisher` writes the `OutboxEvent` entity into `outbox_events` inside the active `@Transactional` context.
   - If the business operation rolls back, the outbox record is rolled back automatically.

2. **Asynchronous Outbox Polling**:
   - `OutboxPublisher` runs periodically (`app.outbox.polling-interval=1000`).
   - Fetches pending events ordered by `created_at ASC` using page batches (`app.outbox.batch-size=50`).

3. **At-Least-Once Delivery & Idempotency Boundary**:
   - Outbox publisher guarantees **at-least-once publication** to Kafka.
   - If an application crashes after Kafka receives the record but before outbox status updates to `PUBLISHED`, the record will be re-sent upon restart.
   - **Consumer-side deduplication** and idempotent handling belong to **Lesson 58**.

4. **Retry & Error Recovery**:
   - Transient failures (Kafka broker disconnection, network timeouts) update `retry_count` and log diagnostics while preserving the event in `PENDING` status.
   - Once `retry_count >= maxRetries` (`app.outbox.max-retries=5`), the record transitions to `FAILED` and increments failure metrics.

---

## 5. Metrics & Observability

- `devsphere.outbox.events.published.total` (Tags: `event_type`, `status`)
- `devsphere.outbox.publish.failures.total` (Tags: `event_type`)
