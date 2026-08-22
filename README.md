# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lessons 1 through 9** are complete:
- **API Gateway** (`services/api-gateway`, Port `8080`): Perimeter Gateway enforcing JWT validation (`HS256`) and identity header propagation (`X-Authenticated-User-Id`).
- **Auth Service** (`services/auth-service`, Port `8081`): Authentication microservice owning user credentials (`devsphere_auth`), registration, password hashing, and publishing `UserRegisteredEvent` domain events to Apache Kafka.
- **User Service** (`services/user-service`, Port `8082`): User profile domain microservice (`devsphere_user`), consuming `UserRegisteredEvent` from Kafka (`devsphere.user.v1`) to initialize user profiles asynchronously and idempotently. Features Redis distributed caching (`user-profile:{userId}`) using the cache-aside pattern for high-performance profile reads with MySQL as the source of truth.
- **Apache Kafka**: Message broker enabling decoupled, eventual-consistent asynchronous communication between microservices.
- **Redis**: Distributed cache store providing high-performance, demand-driven caching for `User Service` profile reads.

---

## Architecture Diagram

```
                 ┌──────────────┐
                 │ API Gateway  │ (Port 8080 - Perimeter JWT Validation)
                 └──────┬───────┘
                        │
         ┌──────────────┴──────────────┐
         │ /api/v1/auth/**             │ /api/v1/users/**
         ▼                             ▼
  ┌──────────────┐             ┌──────────────┐   Cache-Aside   ┌──────────────┐
  │ Auth Service │             │ User Service ├────────────────►│    Redis     │
  └──────┬───────┘             └──────┬───────┘                 └──────────────┘
         │                            │
   UserRegisteredEvent                │
         │                            ▼
         ▼                      MySQL Database
   ┌───────────┐               (devsphere_user)
   │   Kafka   │
   └─────┬─────┘
         │ (Consumer Group: devsphere-user-service)
         └────────────────────────────┘
```

---

## Vision

DevSphere is envisioned as a production-grade multi-user SaaS platform built for developers. The goal is to provide a single, unified workspace for personal productivity, skill growth, and career trajectory management, backed by a resilient distributed system architecture.

---

## Technology Stack

### Backend
- **Language & Core Framework**: Java 21, Spring Boot 3.2.5
- **Gateway & Routing**: Spring Cloud Gateway 4.1.2
- **Security & Identity**: Spring Security, JJWT (JWT generation & validation)
- **Persistence & Migration**: Spring Data JPA, Hibernate, Flyway, MySQL
- **Event-Driven Messaging**: Spring Kafka, Apache Kafka
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
- **Lesson 10**: Transactional Outbox Pattern & Reliable Eventing *(Upcoming)*

---

## License

This project is licensed under the [MIT License](LICENSE).
