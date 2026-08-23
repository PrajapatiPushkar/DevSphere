# Kafka Consumer Reliability Architecture

This document details the production-grade reliability mechanisms implemented in the DevSphere `User Service` for consuming events from Apache Kafka.

---

## 1. At-Least-Once Delivery & Idempotency

Kafka consumers operate under **at-least-once** delivery semantics. Network retries, consumer restarts, or uncommitted consumer offsets after database transactions can cause duplicate event deliveries. 

To prevent duplicate business side effects (e.g. creating duplicate user profiles), event processing is strictly **idempotent**.

```
                           +----------------------+
                           |  Kafka Topic          |
                           |  devsphere.user.v1   |
                           +----------+-----------+
                                      |
                                      v
                       +--------------+---------------+
                       | User Service Consumer         |
                       +--------------+---------------+
                                      |
                           +----------v-----------+
                           |  Processed Event     |
                           |  Idempotency Check   |
                           +----------+-----------+
                                      |
                   +------------------+------------------+
                   |                                     |
             Already Processed                     First Delivery
                   |                                     |
                   v                                     v
         +---------+---------+                 +---------+---------+
         | Skip Business     |                 | Create User       |
         | Execution         |                 | Profile           |
         +---------+---------+                 +---------+---------+
                   |                                     |
                   |                           +---------+---------+
                   |                           | Record            |
                   |                           | processed_events  |
                   |                           +---------+---------+
                   |                                     |
                   +------------------+------------------+
                                      |
                                      v
                           +----------+-----------+
                           | Commit DB Transaction|
                           +----------+-----------+
                                      |
                                      v
                           +----------+-----------+
                           | Acknowledge Kafka    |
                           | Message Offset       |
                           +----------------------+
```

---

## 2. Processed Events Storage & Atomic Transaction

Idempotency tracking relies on a dedicated database table owned exclusively by `User Service`:

```sql
CREATE TABLE processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_processed_events_event_id UNIQUE (event_id)
);
```

### Atomic Database Transaction Rules

- Profile creation (`user_profiles`) and processed event tracking (`processed_events`) are performed within the **same JPA `@Transactional` boundary**.
- If profile creation succeeds but recording `processed_events` fails (or vice versa), the entire database transaction is **rolled back**.
- The database `UNIQUE (event_id)` constraint provides non-bypassable protection against concurrent event delivery races.

---

## 3. Kafka Offset Acknowledgment

Kafka offset acknowledgment occurs **only after** the database transaction successfully commits.

If processing fails or database transaction rolls back, the offset is not acknowledged, enabling retry and backoff handling.

---

## 4. Controlled Retries & Backoff Strategy

Consumer retries are managed by Spring Kafka's `DefaultErrorHandler` with configurable parameters:

- `app.kafka.consumer.max-attempts`: Maximum total delivery attempts (Default: `3`).
- `app.kafka.consumer.retry-backoff-ms`: Interval between retries in milliseconds (Default: `1000ms`).

### Retry Flow

```
Attempt 1 (Failure) ---> Wait 1000ms ---> Attempt 2 (Failure) ---> Wait 1000ms ---> Attempt 3 (Failure) ---> Route to DLT
```

---

## 5. Dead Letter Topic (DLT) Routing

When all retry attempts are exhausted or when permanent payload errors occur (e.g. malformed JSON deserialization failures, invalid event versions), the message is automatically routed to the Dead Letter Topic:

- **Primary Topic**: `devsphere.user.v1`
- **Dead Letter Topic (DLT)**: `devsphere.user.v1.DLT`

### Preserved Kafka Metadata & Headers

Spring Kafka's `DeadLetterPublishingRecoverer` preserves original record details in Kafka headers:

| Header Key | Description |
| --- | --- |
| `kafka_dlt-original-topic` | Original topic name (`devsphere.user.v1`) |
| `kafka_dlt-original-partition` | Original partition ID |
| `kafka_dlt-original-offset` | Original offset number |
| `kafka_dlt-exception-fqcn` | Fully qualified exception class name |
| `kafka_dlt-exception-message` | Exception message |
| `kafka_dlt-exception-stacktrace` | Full error stacktrace |

---

## 6. Poison Messages & Error Classification

- **Duplicate Events**: Handled safely as already processed; acknowledged immediately without triggering retries or DLT routing.
- **Transient Failures** (DB connection drops, temporary network issues): Retried up to `max-attempts`.
- **Permanent / Poison Messages** (Malformed JSON, missing required fields, unsupported event versions): Retried according to policy and routed to DLT without crashing or blocking consumer partition execution.

---

## 7. Operational Recovery & Future Tooling

DLT topics serve as operational holding areas for unprocessable events. DLT events are **not auto-consumed** in an infinite loop. Future operational CLI or administrative tooling can inspect, modify, and replay messages from `devsphere.user.v1.DLT` back into `devsphere.user.v1`.
