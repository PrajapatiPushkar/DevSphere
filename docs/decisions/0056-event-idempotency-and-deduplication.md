# 56. Consumer Event Idempotency and Deduplication Architecture

* **Status**: Accepted
* **Impacted Components**: `user-service`
* **Date**: 2026-09-01

---

## Context

With Apache Kafka and the Transactional Outbox Pattern active across DevSphere, domain events are delivered with **at-least-once** semantics. Network rebalances, producer retry attempts, or consumer restarts after database commits can cause duplicate event redelivery.

We required a production-grade consumer deduplication mechanism ensuring that processing the same `eventId` more than once does not execute duplicate business side-effects.

---

## Decision

1. **Composite Uniqueness Model (`(event_id, consumer_group)`)**:
   - Created Flyway migration `V19__add_consumer_group_to_processed_events.sql` in `user-service`.
   - Updated `processed_events` schema to include `consumer_group` with composite unique constraint `uk_processed_events_event_consumer UNIQUE (event_id, consumer_group)`.
   - Ensures distinct consumer groups (e.g. `devsphere-resume-activity-group` vs `devsphere-user-service`) process the same domain event independently without cross-group interference.

2. **Idempotency Service (`EventIdempotencyService` & `EventProcessingResult`)**:
   - Implemented `EventIdempotencyService` providing `executeIdempotent(eventId, eventType, consumerGroup, businessAction)`.
   - Wraps business side-effect execution and `ProcessedEvent` marker creation within the **exact same database transaction**.
   - If business logic throws an exception, transaction rolls back, leaving no `ProcessedEvent` marker and permitting Kafka retries.
   - Catches `DataIntegrityViolationException` to safely handle concurrent duplicate processing across multi-instance deployments.

3. **Consumer Integration**:
   - Updated `ResumeVersionPublishedEventConsumer` and `UserRegisteredEventConsumer` to process domain events via `EventIdempotencyService`.

4. **Observability**:
   - Registered Micrometer metric `devsphere.events.idempotency.total` with tags `event_type` and `result` (`processed`, `duplicate`, `failed`).

---

## Consequences

* **Positive**:
  - Guarantees exact-once business side-effect execution per consumer group across at-least-once Kafka delivery.
  - Supports multi-instance concurrent delivery protection via DB composite unique constraint.
  - Preserves trace correlation (`traceId`) and logging security.
* **Trade-offs / Future Scope**:
  - Requires maintaining `processed_events` table storage. High-volume cleanup policies can be added in future maintenance tasks.
