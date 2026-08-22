# Event-Driven User Registration Architecture

## Overview

DevSphere utilizes **Apache Kafka** for asynchronous, decoupled event-driven communication between microservices. When a user registers in **Auth Service**, the system publishes a `UserRegisteredEvent` to Kafka, which **User Service** asynchronously consumes to create an initial user profile.

```
                  ┌─────────────────┐
                  │   API Gateway   │
                  └────────┬────────┘
                           │ POST /api/v1/auth/register
                           ▼
                  ┌─────────────────┐
                  │  Auth Service   │ (Port 8081)
                  └────────┬────────┘
                           │
             1. Persist User Credential (devsphere_auth)
             2. Commit DB Transaction
             3. Publish UserRegisteredEvent
                           │
                           ▼
               ┌───────────────────────┐
               │      Kafka Broker     │
               │ Topic: devsphere.user.v1 │
               └───────────┬───────────┘
                           │
                           ▼ (Consumer Group: devsphere-user-service)
                  ┌─────────────────┐
                  │  User Service   │ (Port 8082)
                  └────────┬────────┘
                           │
             Create Initial Profile (devsphere_user)
                           │
                           ▼
                  ┌─────────────────┐
                  │  User Database  │
                  └─────────────────┘
```

---

## Key Architectural Principles

### 1. Asynchronous Decoupling vs. Synchronous REST
- **Synchronous HTTP/REST**: Preserved for user-facing, immediate request/response APIs (`POST /api/v1/auth/login`, `GET /api/v1/users/me`, `PUT /api/v1/users/me`).
- **Asynchronous Kafka Events**: Used strictly for domain events that do not block the primary HTTP response path. Auth Service registration returns HTTP 201 immediately to the client without waiting for profile creation in User Service.

### 2. Microservice Autonomy & Loose Coupling
- Auth Service has zero compile-time or runtime knowledge of User Service APIs.
- User Service has zero direct database or HTTP dependencies on Auth Service.
- If User Service is temporarily down during user registration, Kafka retains the `UserRegisteredEvent` in topic `devsphere.user.v1`. When User Service recovers, it resumes consumption from its offset without losing events.

### 3. Partitioning & Key Strategy
- **Kafka Key**: Canonical `userId` (`String.valueOf(userId)`).
- **Partitioning Rationale**: Messages keyed by `userId` are guaranteed to land on the same Kafka partition, ensuring strictly ordered event processing per user.

### 4. Idempotency & Defense in Depth
- Consumers must expect at-least-once delivery semantics in distributed messaging.
- **Application-Level Check**: Before creating a profile, `UserRegisteredEventConsumer` queries `userProfileRepository.findByUserId(userId)`. If present, the duplicate event is logged and skipped.
- **Database Safety Net**: The `user_profiles.user_id` column carries a database `UNIQUE KEY` constraint as the final safety net.

### 5. Eventual Consistency
- System state across services is **eventually consistent**.
- Within milliseconds after registration, Auth Service credentials exist while User Service asynchronously initializes profile state.

---

## Reliability Consideration & Future Outbox Pattern

> [!NOTE]
> **DB-to-Kafka Reliability Limitation**: Currently, Auth Service executes DB persistence followed by Kafka publishing after transaction commit. In rare broker outage scenarios, the DB commit may succeed while event publishing fails.
> **Future Evolution**: Lesson 10 will introduce the **Transactional Outbox Pattern** with Debezium/Kafka Connect to guarantee atomic, zero-loss DB-to-event delivery.
