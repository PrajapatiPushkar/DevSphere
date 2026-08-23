# 9. Transactional Outbox Pattern for Domain Event Publishing

Date: 2026-08-23

## Status

Accepted

## Context

In Lesson 8, `Auth Service` published `UserRegisteredEvent` to Apache Kafka immediately following user registration. This created a dual-write reliability flaw: if database transaction commits but Kafka publish fails (broker downtime, network split, client crash), the user exists in MySQL but the event is lost forever. We need a guaranteed mechanism to publish domain events atomically with business transactions.

## Decision

We implement the **Transactional Outbox Pattern** in `Auth Service`:

1. **Atomic DB Persistence**: During `POST /api/v1/auth/register`, `UserCredential` and a new `OutboxEvent` record are saved within the same database transaction in `devsphere_auth`.
2. **Outbox Schema**: `outbox_events` table contains `event_id` (UUID), `aggregate_type` ("USER"), `aggregate_id` (userId string), `event_type` ("USER_REGISTERED"), `event_version` (1), `payload` (JSON), `status` (`PENDING`, `PUBLISHED`, `FAILED`), `retry_count`, `last_error`, `created_at`, `published_at`.
3. **Scheduled Publisher**: A background component (`OutboxPublisher`) periodically polls `outbox_events` for `PENDING` records in bounded batches (50) and publishes them to Kafka topic `devsphere.user.v1`.
4. **State Machine & Retries**: Upon Kafka delivery success, status transitions to `PUBLISHED`. On failure, `retry_count` increments up to bounded max retries (5), after which status becomes `FAILED` for operational inspection.
5. **At-Least-Once Delivery**: The system guarantees at-least-once delivery. Downstream `User Service` preserves idempotent event consumption.

## Consequences

### Positive
- Guaranteed event persistence: Zero lost events during Kafka broker outages or network glitches.
- Atomic registration: User creation and event logging commit or roll back together.
- Auditable event log in `Auth Service`.

### Negative / Trade-offs
- Additional table (`outbox_events`) and Flyway migration in `Auth Service`.
- At-least-once delivery requires downstream consumer idempotency.
- Background polling thread adds minor database query activity.
