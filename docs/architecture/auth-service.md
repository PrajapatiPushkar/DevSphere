# Auth Service Architecture Document

## Overview

The **Auth Service** is the core identity microservice in the DevSphere platform. It owns authentication credentials, password hashes, and identity registration.

```
[ Frontend Client ]
        │
        ▼
  [ API Gateway ] (Port 8080)
        │
        ▼
  [ Auth Service ] (Port 8081)
        │
        ▼
 [ Auth Database ] (MySQL: devsphere_auth)
```

---

## Domain Ownership & Boundary

1. **Authentication Credentials Only**: The Auth Service strictly owns credential identity (`id`, `email`, `password_hash`, `created_at`, `updated_at`).
2. **Separation from User Profile Domain**: User profile details (name, avatar, bio, career preferences, skills, resume data, etc.) will be owned exclusively by the future **User Service**.
3. **Database Isolation**: Other microservices (API Gateway, User Service, Task Service, etc.) must NEVER query the `devsphere_auth` database directly.

---

## Security Architecture

- **Password Storage**: Passwords are hashed using BCrypt (`BCryptPasswordEncoder`). Raw passwords are never stored or logged.
- **API Response Boundaries**: Data Transfer Objects (`RegisterResponse`) filter out sensitive fields before JSON serialization. `password` and `password_hash` are strictly excluded from all response DTOs.
- **Database Schema Ownership**: Database migrations are managed via Flyway versioned scripts (`V1__create_users_table.sql`). Hibernate auto DDL is set to `validate`.

---

## Future Evolution

- **Lesson 4 (Current)**: User registration (`POST /api/v1/auth/register`), password hashing, MySQL Flyway persistence.
- **Future Lessons**:
  - JWT token issuance (`POST /api/v1/auth/login`).
  - Refresh token rotation & revocation.
  - Gateway perimeter authentication integration.
