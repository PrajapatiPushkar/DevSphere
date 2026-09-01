# Event Idempotency and Deduplication Architecture

This document describes the consumer-side **Event Idempotency and Deduplication** architecture in DevSphere (`user-service`), guaranteeing that domain events are processed exactly once per consumer group despite at-least-once Kafka delivery.

---

## 1. Dual Delivery & Duplicate Event Context

Kafka event streaming combined with the Transactional Outbox Pattern provides **at-least-once delivery** semantics. Consequently, duplicate domain events can occur under common operational scenarios:

```text
                               Duplicate Sources
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        │                              │                              │
        ▼                              ▼                              ▼
 Kafka Producer Retry        Consumer Network Rebalance       Consumer Crash Post-Process
(Outbox re-send after ACK)    (Partition re-assignment)       (Kafka offset commit delay)
```

Without consumer-side deduplication, receiving duplicate events causes duplicated business side-effects (e.g. creating duplicate activity records, re-triggering notifications, or double-invoking handlers).

---

## 2. Target Idempotency Architecture

DevSphere enforces consumer idempotency using atomic business database transactions coupled with composite database unique constraints:

```text
Kafka Topic (e.g. devsphere.domain.events)
       │
       ▼
Kafka Consumer (e.g. ResumeVersionPublishedEventConsumer)
       │
       ▼ @Transactional
┌─────────────────────────────────────────────────────────────────────────┐
│ EventIdempotencyService.executeIdempotent(...)                          │
│                                                                         │
│ 1. Check if (event_id, consumer_group) exists in processed_events       │
│    ├── TRUE  ──> Skip business action & return EventProcessingResult    │
│    └── FALSE ──> Execute business side-effect                           │
│ 2. INSERT into processed_events (event_id, event_type, consumer_group)  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                               COMMIT (Atomic)
                                     │
                                     ▼
                        Kafka Offset Commit (ACK)
```

---

## 3. Database Schema Contract (`processed_events`)

```sql
ALTER TABLE processed_events
    ADD COLUMN consumer_group VARCHAR(100) NOT NULL DEFAULT 'default';

ALTER TABLE processed_events
    DROP CONSTRAINT uk_processed_events_event_id;

ALTER TABLE processed_events
    ADD CONSTRAINT uk_processed_events_event_consumer UNIQUE (event_id, consumer_group);
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT` | Primary key identity |
| `event_id` | `VARCHAR(255)` | Unique domain event UUID |
| `event_type` | `VARCHAR(100)` | Domain event type (`"ResumeVersionPublished"`, `"UserRegistered"`) |
| `consumer_group` | `VARCHAR(100)` | Consumer group identifier (`"devsphere-resume-activity-group"`, `"devsphere-user-service"`) |
| `processed_at` | `TIMESTAMP` | UTC timestamp when processing succeeded |

---

## 4. Key Architectural Guarantees

1. **Composite Consumer Group Isolation**:
   - Deduplication is scoped to `(event_id, consumer_group)`.
   - Distinct consumer groups (e.g., analytics consumer group vs activity logging consumer group) process the same event ID independently without interfering with each other's idempotency markers.

2. **Atomic Business & Marker Transaction**:
   - The business side-effect and the `ProcessedEvent` marker insert occur within the exact same database transaction.
   - If business processing throws an exception, the transaction rolls back, leaving no marker in `processed_events` so Kafka retries can proceed safely.

3. **Concurrent Duplicate Delivery Protection**:
   - Database unique constraint `uk_processed_events_event_consumer` acts as the final guard when concurrent consumers attempt processing simultaneously.
   - Caught `DataIntegrityViolationException` safely marks the duplicate execution and logs warnings without throwing retriable errors.

---

## 5. Metrics & Logging

- **Metric**: `devsphere.events.idempotency.total`
- **Tags**: `event_type`, `result` (`processed`, `duplicate`, `failed`)
- **Logging**: MDC trace context (`traceId`) is retained throughout idempotency verification and business execution. Secrets, passwords, and tokens are strictly excluded.
