# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lessons 1 through 12** are complete:
- **Service Discovery** (`services/service-discovery`, Port `8761`): Standalone Netflix Eureka Service Discovery server maintaining an in-memory registry of all active microservice instances.
- **API Gateway** (`services/api-gateway`, Port `8080`): Perimeter Gateway registered with Eureka (`DEVSPHERE-API-GATEWAY`), enforcing JWT validation (`HS256`), identity header propagation (`X-Authenticated-User-Id`), and dynamic service discovery routing (`lb://`).
- **Auth Service** (`services/auth-service`, Port `8081`): Authentication microservice registered with Eureka (`DEVSPHERE-AUTH-SERVICE`), owning user credentials (`devsphere_auth`), registration, password hashing, and atomic outbox event persistence (`outbox_events` table). Features scheduled `OutboxPublisher` for reliable Kafka event dispatching.
- **User Service** (`services/user-service`, Port `8082`): User profile domain microservice registered with Eureka (`DEVSPHERE-USER-SERVICE`), consuming `UserRegisteredEvent` from Kafka (`devsphere.user.v1`). Hardened with database-backed idempotency (`processed_events` table), atomic JPA transactions, controlled retries, fixed backoff, and Dead Letter Topic (`devsphere.user.v1.DLT`) poison message isolation. Features Redis distributed caching (`user-profile:{userId}`) using cache-aside with MySQL source of truth.
- **Apache Kafka**: Message broker enabling decoupled, eventual-consistent asynchronous communication between microservices with DLT routing.
- **Redis**: Distributed cache store providing high-performance, demand-driven caching for `User Service` profile reads.
- **Transactional Outbox Pattern**: Atomic database persistence of business entity and event records in `Auth Service`, eliminating dual-write failure windows during Kafka broker downtime.
- **Consumer Reliability & DLT**: Database-backed event idempotency, Spring Kafka `DefaultErrorHandler` retries, exponential/fixed backoff, and Dead Letter Topic routing.

---

## Architecture Diagram

```
                 ┌──────────────┐
                 │ API Gateway  │ (Port 8080 - Perimeter JWT Validation)
                 └──────┬───────┘
                        │
                  Service Discovery (Eureka Query)
                        │
                        ▼
         ┌──────────────────────────────┐
         │ DEVSPHERE-SERVICE-DISCOVERY  │ (Port 8761 - Netflix Eureka Server)
         └──────────────┬───────────────┘
                        │
         ┌──────────────┴──────────────┐
         │ lb://DEVSPHERE-AUTH-SERVICE │ lb://DEVSPHERE-USER-SERVICE
         ▼                             ▼
  ┌──────────────┐             ┌──────────────┐   Cache-Aside   ┌──────────────┐
  │ Auth Service │             │ User Service ├────────────────►│    Redis     │
  └──────┬───────┘             └──────┬───────┘                 └──────────────┘
         │                            │
   Atomic Transaction          Idempotent Consumer
   (users + outbox)            (processed_events)
         │                            │
         ▼                            ▼
  ┌──────────────┐             MySQL Database
  │ Outbox Table │              (devsphere_user)
  └──────┬───────┘
         │
         ▼ (OutboxPublisher @Scheduled)
   ┌───────────┐                     ┌───────────────────────────┐
   │   Kafka   ├────────────────────►│ devsphere.user.v1.DLT     │ (Poison Message Holding Area)
   └─────┬─────┘                     └───────────────────────────┘
         │ (Topic: devsphere.user.v1, Group: devsphere-user-service - Retries & Backoff)
         └────────────────────────────┘
```

---

## Vision

DevSphere is envisioned as a production-grade multi-user SaaS platform built for developers. The goal is to provide a single, unified workspace for personal productivity, skill growth, and career trajectory management, backed by a resilient distributed system architecture.

---

## Technology Stack

### Backend
- **Language & Core Framework**: Java 21, Spring Boot 3.2.5
- **Service Discovery**: Spring Cloud Netflix Eureka Server & Client (2023.0.1)
- **Gateway & Routing**: Spring Cloud Gateway 4.1.2 (Dynamic `lb://` routing)
- **Security & Identity**: Spring Security, JJWT (JWT generation & validation)
- **Persistence & Migration**: Spring Data JPA, Hibernate, Flyway, MySQL
- **Event-Driven Messaging & Outbox**: Spring Kafka, Apache Kafka, Transactional Outbox Pattern, Dead Letter Topic (DLT)
- **Distributed Caching**: Spring Data Redis, Redis 7.2
- **Build Tool**: Maven

---

## Development Roadmap

- **Lesson 1**: Production Repository Foundation *(Completed)*
- **Lesson 2**: API Gateway Foundation Service *(Completed)*
- **Lesson 3**: Spring Cloud Gateway & Request Routing Foundation *(Completed)*
- **Lesson 4**: Auth Service Foundation & User Registration *(Completed)*
- **Lesson 5**: JWT Authentication & Login *(Completed)*
- **Lesson 6**: API Gateway JWT Validation & Protected Routes *(Completed)*
- **Lesson 7**: User Service & Profile Management *(Completed)*
- **Lesson 8**: Event-Driven User Registration with Apache Kafka *(Completed)*
- **Lesson 9**: Redis Distributed Caching for User Profiles *(Completed)*
- **Lesson 10**: Transactional Outbox Pattern & Reliable Eventing *(Completed)*
- **Lesson 11**: Production-Grade Kafka Consumer Reliability *(Completed)*
- **Lesson 12**: Service Discovery with Eureka *(Completed)*



---

## License

This project is licensed under the [MIT License](LICENSE).
