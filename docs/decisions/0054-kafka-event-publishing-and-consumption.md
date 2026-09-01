# 54. Kafka Event Publishing and Consumption Architecture

* **Status**: Accepted
* **Impacted Components**: `user-service`, `auth-service`
* **Date**: 2026-09-01

---

## Context

Following the establishment of the Event-Driven Architecture Foundation in Lesson 55, DevSphere required a scalable, production-conscious messaging transport for cross-service asynchronous event processing.

We needed to integrate **Apache Kafka** into the architecture while preserving domain layer decoupling so that business services publish events strictly through the `DomainEventPublisher` abstraction without direct dependency on Kafka APIs.

---

## Decision

1. **Decoupled Kafka Event Publisher (`KafkaDomainEventPublisher`)**:
   - Implemented `KafkaDomainEventPublisher` as the primary `DomainEventPublisher` bean in `user-service`.
   - Business services (`ResumeVersionService`, etc.) continue calling `domainEventPublisher.publish(event)` without referencing `KafkaTemplate`.
   - `KafkaDomainEventPublisher` publishes events locally to Spring's `ApplicationEventPublisher` and transmits records to Kafka topics (`devsphere.domain.events`, `devsphere.user.v1`).

2. **Topic Naming & Partitioning Strategy**:
   - `devsphere.domain.events`: Central topic for domain events (such as `ResumeVersionPublishedEvent`, `PublicResumeViewEvent`, `PublicResumeShareRevokedEvent`).
   - `devsphere.user.v1`: Dedicated topic for user registration lifecycle events.
   - Message keys extract entity/profile identifiers (e.g. `resumeProfileId` for resume version events; `userId` for user events) to preserve partition order per domain entity.

3. **Event Serialization Contract**:
   - Transmit domain events using Jackson JSON serialization with type header propagation (`spring.json.use.type.headers=true`).
   - Event contracts maintain explicit metadata: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `traceId`, and domain entity IDs.
   - Payloads strictly exclude secrets, JWTs, passwords, and lazy Hibernate proxies.

4. **Trace Context Propagation**:
   - MDC trace correlation (`traceId`) is attached to Kafka record headers (`X-Trace-Id`, `traceId`).
   - Consumers restore `MDC.put("traceId", traceId)` before executing event handlers and clean up context in `finally` blocks.

5. **Consumer & Error Handling Strategy**:
   - Real Kafka consumers (e.g. `ResumeVersionPublishedEventConsumer`) execute within named consumer groups (`devsphere-resume-activity-group`).
   - `DefaultErrorHandler` configures fixed backoff retries (3 attempts, 1s delay) and routes exhausted or malformed messages to Dead Letter Topics (`devsphere.domain.events.DLT`).

---

## Consequences

* **Positive**:
  - Business domain layer remains completely independent of Kafka dependencies.
  - Multi-service asynchronous message streaming with preserved partition ordering per entity.
  - Unbroken MDC distributed tracing across thread and Kafka network boundaries.
  - Robust dead-letter topic recovery for malformed/unprocessable messages.
* **Trade-offs / Current Limitations**:
  - Event publishing is non-transactional with database writes (at-least-once delivery; database transaction commits and Kafka sends are separate). Transactional Outbox pattern is reserved for Lesson 57.
  - Idempotent deduplication table processing is reserved for Lesson 58.
