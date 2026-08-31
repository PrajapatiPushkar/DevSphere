# Event-Driven Architecture Foundation

This document describes the architectural standards, domain event contract, versioning conventions, payload security guidelines, transaction boundaries (`@TransactionalEventListener`), asynchronous processing, trace correlation, and Event Catalog for DevSphere.

---

## 1. Executive Summary

Lesson 55 establishes a standardized **Domain and Application Event Architecture Foundation** across DevSphere microservices (`user-service`, `auth-service`).

The event architecture decouples core business domain state transitions from secondary side-effects (such as cache invalidation, asynchronous analytics, notification delivery, and transactional outbox persistence).

```text
               Business Operation / Transaction
                              │
               ┌──────────────▼──────────────┐
               │   SpringDomainEventPub.     │
               └──────────────┬──────────────┘
                              │ Publishes DomainEvent (with MDC traceId)
               ┌──────────────▼──────────────┐
               │ ApplicationEventPublisher   │
               └──────────────┬──────────────┘
                              │
       ┌──────────────────────┴──────────────────────┐
       │                                             │
┌──────▼─────────────────────┐             ┌─────────▼──────────────────┐
│ @Async Event Listener      │             │ @TransactionalEventListener│
│ (Non-blocking analytics)   │             │ (AFTER_COMMIT side effects)│
└────────────────────────────┘             └────────────────────────────┘
```

---

## 2. Core Domain Event Contract (`DomainEvent`)

All domain events in DevSphere implement the `DomainEvent` contract (or extend `BaseDomainEvent`):

| Property | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `eventId` | `String` | Unique UUID identifying the specific event instance | `"f47ac10b-58cc-4372-a567-0e02b2c3d479"` |
| `eventType` | `String` | Business-oriented past-tense event type | `"PublicResumeViewed"`, `"UserRegistered"` |
| `eventVersion` | `Integer` | Schema version number (starts at 1) | `1` |
| `occurredAt` | `Instant` | UTC timestamp when event was produced | `"2026-08-31T06:00:00Z"` |
| `traceId` | `String` | MDC trace context for distributed tracing correlation | `"4e1bc11f9f577563edf9b63e7f361e06"` |

---

## 3. Naming & Versioning Conventions

1. **Past-Tense Business Names**:
   - Events describe something that **already happened** (e.g. `UserRegistered`, `PublicResumeViewed`, `ResumeVersionPublished`, `PublicResumeShareRevoked`).
   - Command names (`PublishResume`, `UpdateProfile`) and generic names (`DataChanged`, `SomethingUpdated`) are prohibited.

2. **Schema Versioning**:
   - Every event payload includes `eventVersion` (default `1`).
   - Major structural changes increment `eventVersion`, allowing backward-compatible consumer parsing.

---

## 4. Payload Security & Immutability

1. **Immutability**:
   - Event classes use `final` payload fields and constructor initialization to prevent state mutation between publication and listener processing.

2. **Payload Safety**:
   - Events contain **only business identifiers** (`userId`, `resumeProfileId`, `publicId`, `versionId`).
   - Events MUST NEVER contain:
     - Passwords or BCrypt hashes
     - JWT tokens or refresh tokens
     - Database credentials or secret keys
     - Lazy-loaded JPA entity graphs or proxies

---

## 5. Transaction Boundaries & Listener Patterns

1. **Post-Commit Listeners (`@TransactionalEventListener`)**:
   - Side effects dependent on committed database state MUST use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)`.
   - Ensures listeners do NOT execute for transactions that subsequently roll back.

2. **Asynchronous Non-Blocking Execution (`@Async`)**:
   - Non-critical secondary operations (analytics logging, background metric calculation) execute asynchronously via `@Async @EventListener`.

3. **MDC Trace Correlation & Failure Isolation**:
   - Listeners restore MDC `traceId` from `event.getTraceId()` during execution.
   - Failures in secondary event listeners are isolated (caught, logged, and incrementing error metrics) without crashing primary business transactions.

---

## 6. DevSphere Event Catalog

| Event Name | Version | Producer Service | Trigger Condition | Payload Summary | Consumers / Handlers |
| :--- | :---: | :--- | :--- | :--- | :--- |
| `UserRegistered` | 1 | `auth-service` | New user completes registration | `userId` | `UserRegisteredEventConsumer` (`user-service`) |
| `PublicResumeViewed` | 1 | `user-service` | Public resume accessed via `/api/v1/public/resumes/{publicId}` | `publicId`, `resumeProfileId`, `clientIp`, `referrer`, `userAgent` | `PublicResumeAnalyticsService` (`@Async`) |
| `ResumeVersionPublished` | 1 | `user-service` | User publishes a resume version snapshot | `resumeProfileId`, `resumeVersionId`, `versionNumber`, `userId` | `ResumeActivityEventListener` (`AFTER_COMMIT`) |
| `PublicResumeShareRevoked` | 1 | `user-service` | User revokes public sharing access for a resume | `resumeProfileId`, `publicId`, `userId` | `ResumeActivityEventListener` (`AFTER_COMMIT`) |

---

## 7. Future Kafka & Outbox Integration Boundary

Lesson 55 establishes the in-memory Spring application event and domain event foundation. In upcoming lessons:
- **Lesson 56**: Kafka Event Streaming integration.
- **Lesson 57**: Transactional Outbox Pattern persistence (`outbox_events`).
- **Lesson 58**: Duplicate Event Idempotency deduplication.
