# DevSphere Auth Service

## Purpose
The Auth Service is the dedicated microservice responsible for managing user identity credentials, secure password hashing, user registration, stateless JWT authentication, and transactional outbox event publication for the DevSphere platform.

---

## Current Status
> **Centralized Configuration, Service Discovery & Transactional Outbox Pattern (Lesson 13)**  
> The Auth Service consumes non-secret centralized configuration from Spring Cloud Config Server (`http://localhost:8888`), registers as a Eureka client (`DEVSPHERE-AUTH-SERVICE`), and manages user registration (`POST /api/v1/auth/register`) by persisting credentials (`users`) and domain events (`outbox_events`) atomically. A background worker (`OutboxPublisher`) polls outbox events and dispatches them to Apache Kafka (`devsphere.user.v1`).

---

## Responsibilities
- Centralized configuration import from Spring Cloud Config Server (`spring.config.import=configserver:http://localhost:8888`).
- User account creation and identity registration.
- Service discovery client registration (`DEVSPHERE-AUTH-SERVICE` on port `8081`).
- BCrypt password hashing (`BCryptPasswordEncoder`).
- User authentication and JWT access token issuance (`HS256`).
- Atomic outbox persistence (`outbox_events` table in `devsphere_auth`).
- Scheduled event dispatching (`OutboxPublisher`) to Apache Kafka (`devsphere.user.v1`).
- Bounded retries and failure handling for event publishing.
- Exposing service health metrics via Spring Boot Actuator (`/actuator/health`).

---

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Cloud Config**: Spring Cloud Config Client (`spring-cloud-starter-config`)
- **Security & Tokens**: Spring Security Crypto, JJWT (`io.jsonwebtoken:jjwt-api:0.12.5`)
- **Persistence**: Spring Data JPA, Hibernate, MySQL
- **Database Migrations**: Flyway (`flyway-core`, `flyway-mysql`)
- **Eventing & Outbox**: Spring Kafka, Scheduled Outbox Publisher
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Testing**: JUnit 5, MockMvc, H2 (for isolated test runs)
- **Build Tool**: Maven

---

## Transactional Outbox Architecture

```
POST /api/v1/auth/register
        │
        ├── BEGIN DATABASE TRANSACTION
        │     ├── 1. Save UserCredential (users)
        │     └── 2. Save OutboxEvent (outbox_events - PENDING)
        └── COMMIT TRANSACTION (ATOMIC)
              │
              ▼
    OutboxPublisher (@Scheduled) ──► Kafka (devsphere.user.v1) ──► User Service
```

---

## Environment Variables

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `devsphere_auth` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `JWT_SECRET` | HS256 Secret (Min 32 chars / 256 bits) | *Development fallback* |
| `JWT_EXPIRATION_SECONDS` | Token lifespan in seconds | `3600` (1 hour) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka Brokers | `localhost:9092` |
| `CONFIG_SERVER_URL` | Config Server URL | `http://localhost:8888` |
| `EUREKA_SERVER_URL` | Netflix Eureka Server URL | `http://localhost:8761/eureka/` |

---

## Authentication APIs

### 1. User Registration

`POST /api/v1/auth/register`

#### Example Request
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

#### Response (`HTTP 201 CREATED`)
```json
{
  "id": 1,
  "email": "user@example.com",
  "createdAt": "2026-08-22T16:30:00Z"
}
```

---

### 2. User Login & Token Generation

`POST /api/v1/auth/login`

#### Example Request
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

#### Response (`HTTP 200 OK`)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzU1ODgyOTIwLCJleHAiOjE3NTU4ODY1MjB9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## Running Automated Tests

```bash
cd services/auth-service
mvn test
```
