# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lessons 1 through 14** are complete:
- **Production Observability Foundation** (`infrastructure/monitoring/prometheus.yml`): Standardized Spring Boot Actuator, Micrometer Prometheus metrics (`/actuator/prometheus`), JVM, HTTP, and low-cardinality custom business metrics.
- **Config Server** (`services/config-server`, Port `8888`): Dedicated Spring Cloud Config Server backed by an independent local Git repository (`config-repo/`) providing centralized non-secret runtime configuration.
- **Service Discovery** (`services/service-discovery`, Port `8761`): Standalone Netflix Eureka Service Discovery server maintaining an in-memory registry of all active microservice instances.
- **API Gateway** (`services/api-gateway`, Port `8080`): Perimeter Gateway registered with Eureka (`DEVSPHERE-API-GATEWAY`), importing centralized configuration from Config Server, enforcing JWT validation (`HS256`), identity header propagation (`X-Authenticated-User-Id`), and dynamic service discovery routing (`lb://`).
- **Auth Service** (`services/auth-service`, Port `8081`): Authentication microservice registered with Eureka (`DEVSPHERE-AUTH-SERVICE`), importing centralized configuration, owning user credentials (`devsphere_auth`), registration, password hashing, and atomic outbox event persistence (`outbox_events` table). Features custom metrics for registrations, logins, and outbox event publishing.
- **User Service** (`services/user-service`, Port `8082`): User profile domain microservice registered with Eureka (`DEVSPHERE-USER-SERVICE`), importing centralized configuration, consuming `UserRegisteredEvent` from Kafka (`devsphere.user.v1`). Features custom metrics for Kafka processing, retries, duplicate events, DLT routing, profile creation, and Redis cache hits/misses.
- **Centralized Configuration**: Spring Cloud Config architecture managing shared and service-specific non-secret configurations in `config-repo/`.
- **Apache Kafka**: Message broker enabling decoupled, eventual-consistent asynchronous communication between microservices with DLT routing.
- **Redis**: Distributed cache store providing high-performance, demand-driven caching for `User Service` profile reads.
- **Transactional Outbox Pattern**: Atomic database persistence of business entity and event records in `Auth Service`.

---

## Architecture Diagram

```
                         ┌──────────────────────┐
                         │      Prometheus      │
                         │        :9090         │
                         └──────────┬───────────┘
                                    │
                             scrape metrics
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
             ▼                      ▼                      ▼
       API Gateway             Auth Service           User Service
       /actuator/*             /actuator/*            /actuator/*
             │                      │                      │
             └────────────── Micrometer ──────────────────┘

Infrastructure:
Config Server :8888 | Eureka Server :8761 | Kafka :9092 | Redis :6379 | MySQL :3306
```

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
- **Lesson 13**: Centralized Configuration with Spring Cloud Config *(Completed)*
- **Lesson 14**: Production Observability Foundation *(Completed)*

---

## License

This project is licensed under the [MIT License](LICENSE).
