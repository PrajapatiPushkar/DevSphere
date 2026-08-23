# Transactional Outbox Pattern Architecture

This document describes the **Transactional Outbox Pattern** implemented in **Lesson 10** for `Auth Service`.

---

## 1. Problem Statement (Previous Direct Publishing Flaw)

In Lesson 8, `Auth Service` attempted direct event publishing to Kafka during user registration:

```
BEGIN TRANSACTION
  Save User to MySQL (devsphere_auth)
COMMIT TRANSACTION
  ↓
Send UserRegisteredEvent to Kafka  ◄── RISK WINDOW: If Kafka call fails or crashes here,
                                        the user exists in MySQL but the event is lost forever!
```

This created an eventual consistency reliability gap:
- Database commit succeeds.
- Kafka publish fails (broker downtime, network split, client crash).
- Downstream `User Service` never receives the registration event and fails to create a profile.

---

## 2. Transactional Outbox Solution Architecture

The **Transactional Outbox Pattern** eliminates this reliability gap by decoupling event creation from Kafka network transport. Event creation and business entity persistence execute within the **SAME atomic database transaction**.

```
Client (POST /api/v1/auth/register)
                  │
                  ▼
          ┌──────────────┐
          │ Auth Service │ (Port 8081)
          └──────┬───────┘
                 │
                 ├── BEGIN DATABASE TRANSACTION
                 │     ├── 1. Save UserCredential (users table)
                 │     └── 2. Save OutboxEvent (outbox_events table - PENDING)
                 └── COMMIT TRANSACTION (ATOMIC)
                       │
                       ▼ (HTTP 201 Created returned immediately)
                 ┌───────────┐
                 │ Auth DB   │ (devsphere_auth)
                 └─────┬─────┘
                       │
                       ▼ (Polling @Scheduled fixedDelay=1000ms, batchSize=50)
             ┌──────────────────┐
             │ Outbox Publisher │
             └─────────┬────────┘
                       │
                       ▼
                 ┌───────────┐
                 │   Kafka   │ Topic: devsphere.user.v1 (Key: userId)
                 └─────┬─────┘
                       │
                       ▼ (Consumer Group: devsphere-user-service)
                ┌──────────────┐
                │ User Service │ (Port 8082 - Idempotent Consumer)
                └──────────────┘
```

---

## 3. Outbox Table Schema & State Machine

The `outbox_events` table is created in `devsphere_auth` via Flyway migration `V2__create_outbox_events.sql`:

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
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
```

### State Machine Transitions

```
  ┌─────────┐
  │ PENDING │ ◄── Created atomically during user registration
  └────┬────┘
       │
       ├─────────────────────────────────────────┐
       │ Successful Kafka Publish                │ Publish Failure (retry_count < maxRetries)
       ▼                                         ▼
 ┌───────────┐                            ┌───────────┐
 │ PUBLISHED │                            │  PENDING  │ (Increments retry_count & stores last_error)
 └───────────┘                            └─────┬─────┘
                                                │
                                                │ Publish Failure (retry_count >= maxRetries)
                                                ▼
                                          ┌───────────┐
                                          │  FAILED   │ (Retained for manual operational recovery)
                                          └───────────┘
```

---

## 4. Key Guarantees & Constraints

1. **Atomic DB Persistence**:
   - `UserCredential` and `OutboxEvent` are written inside the same `@Transactional` method (`AuthService.register`).
   - If user creation succeeds, outbox creation MUST succeed. If either fails, the entire transaction rolls back.
2. **At-Least-Once Kafka Delivery**:
   - Outbox publisher polls `PENDING` records in batches (default `50`).
   - On Kafka publish acknowledgement, status updates to `PUBLISHED`.
   - If app crashes after Kafka send but before updating DB status, the publisher will re-send the message on restart.
3. **Consumer Idempotency**:
   - Because at-least-once publishing can deliver duplicate events, `User Service` maintains application-level idempotency (`findByUserId`) and database constraints (`UNIQUE KEY` on `user_id`).
4. **Credential Isolation**:
   - Outbox `payload` contains JSON for `UserRegisteredEvent` (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `userId`).
   - Outbox payloads NEVER contain passwords, password hashes, JWT secrets, or internal DB credentials.
5. **Kafka Downtime Resilience**:
   - If Kafka is completely offline, user registration returns HTTP 201 Created and persists outbox events with status `PENDING`.
   - Once Kafka recovers, `OutboxPublisher` processes pending events and delivers them to `User Service` without event loss.
