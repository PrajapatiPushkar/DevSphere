# 53. Event-Driven Architecture Foundation

* **Status**: Accepted
* **Impacted Components**: `user-service`, `auth-service`
* **Date**: 2026-08-31

---

## Context

As DevSphere expands, multiple domain components (user registration, resume compilation, public sharing, analytics logging) require decoupling to maintain modularity, performance, and clean transaction boundaries.

We needed a standardized, production-grade event architecture foundation that establishes:
1. Standardized event metadata (`eventId`, `eventType`, `eventVersion`, `occurredAt`, `traceId`).
2. Business-oriented past-tense event naming conventions.
3. Event payload safety and immutability.
4. Clear transaction boundaries using `@TransactionalEventListener(phase = AFTER_COMMIT)` for post-commit side effects.
5. MDC trace context propagation across thread boundaries.

---

## Decision

1. **Domain Event Contract (`DomainEvent` / `BaseDomainEvent`)**:
   - Standardized domain events to implement `DomainEvent` interface and extend `BaseDomainEvent`.
   - Auto-capture MDC `traceId` context upon event instantiation.

2. **Past-Tense Event Naming & Lightweight Versioning**:
   - Standardized event names in past tense (`UserRegistered`, `PublicResumeViewed`, `ResumeVersionPublished`, `PublicResumeShareRevoked`).
   - Included `eventVersion` (integer, default 1) for schema evolution.

3. **Event Payload Security & Immutability**:
   - Used immutable fields with final modifiers and constructor initialization.
   - Enforced payload minimalization (only business IDs; no passwords, JWTs, hashes, DB credentials, or Hibernate proxies).

4. **Transaction Boundary Handling**:
   - Standardized post-commit event handling using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)` to ensure listeners fire strictly when DB transactions successfully commit.

5. **Publisher Abstraction (`DomainEventPublisher`)**:
   - Provided `DomainEventPublisher` interface and `SpringDomainEventPublisher` implementation wrapping Spring `ApplicationEventPublisher` with MDC trace enrichment.

---

## Consequences

* **Positive**:
  - Clean separation of business operations from secondary side-effects.
  - Listeners only observe committed database state changes.
  - Unbroken distributed tracing across `@Async` thread boundaries.
  - Seamless foundation ready for future Kafka event streaming (Lesson 56) and Transactional Outbox (Lesson 57).
* **Negative / Trade-offs**:
  - Requires maintaining event contract interfaces across microservice boundaries.
