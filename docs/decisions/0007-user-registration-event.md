# ADR 0007: Event-Driven User Profile Initialization via Apache Kafka

* **Status**: Accepted
* **Date**: 2026-08-23
* **Context**: Decoupling Auth Service credential registration from User Service profile initialization without introducing synchronous REST dependencies.

---

## Decision

Introduce Apache Kafka event-driven publishing and consumption for user registration:
1. **Auth Service** publishes `UserRegisteredEvent` to Kafka topic `devsphere.user.v1` upon successful registration persistence.
2. **User Service** consumes `UserRegisteredEvent` in consumer group `devsphere-user-service` to asynchronously initialize minimal user profile records (`devsphere_user`).

---

## Event Schema & Contract

- **Topic**: `devsphere.user.v1`
- **Key**: `userId` (for partition ordering)
- **Payload**:
  - `eventId`: UUID
  - `eventType`: `"USER_REGISTERED"`
  - `eventVersion`: `1`
  - `occurredAt`: Instant timestamp
  - `userId`: Canonical user ID
- **Security Rule**: Event payload MUST NEVER contain passwords, password hashes, secrets, JWTs, or sensitive credentials.

---

## Benefits & Rationale

- **Decoupled Architecture**: Auth Service has no direct compile-time or runtime dependencies on User Service.
- **Asynchronous Execution**: HTTP registration response returns immediately (HTTP 201) without waiting for downstream profile creation.
- **Microservice Resilience**: If User Service undergoes maintenance or temporary downtime, Kafka buffers events safely without losing registration records or failing Auth Service APIs.
- **Independent Scalability**: Event processing load can scale independently from synchronous authentication traffic.

---

## Tradeoffs & Mitigation

- **Eventual Consistency**: Profile creation happens milliseconds after registration response.
  - *Mitigation*: Fallback lazy profile creation on `GET /api/v1/users/me` remains available if profile is accessed before event consumption completes.
- **Duplicate Event Delivery**: Messaging networks guarantee at-least-once delivery.
  - *Mitigation*: `UserRegisteredEventConsumer` enforces application-level idempotency checks combined with database `UNIQUE` constraints on `user_id`.
- **Reliability Gap**: Small window between DB commit and Kafka send.
  - *Mitigation*: Transactional Outbox Pattern will be implemented in a future lesson for 100% atomic outbox event dispatch.
