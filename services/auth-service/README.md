# DevSphere Auth Service

## Purpose
The Auth Service is the dedicated microservice responsible for managing user identity credentials, secure password hashing, and user registration for the DevSphere platform.

---

## Current Status
> **User Registration Foundation (Lesson 4)**  
> The Auth Service provides user registration (`POST /api/v1/auth/register`), BCrypt password hashing, Flyway database migrations, and isolated database persistence.  
> **Note**: JWT authentication, login endpoints, refresh tokens, and email verification are NOT implemented yet.

---

## Responsibilities
- User account creation and identity registration.
- BCrypt password hashing (never stores or logs raw passwords).
- Enforcing email uniqueness at both application and database layers.
- Exposing service health metrics via Spring Boot Actuator (`/actuator/health`).

---

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Persistence**: Spring Data JPA, Hibernate, MySQL
- **Database Migrations**: Flyway (`flyway-core`, `flyway-mysql`)
- **Security & Hashing**: Spring Security Crypto (`BCryptPasswordEncoder`)
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Testing**: JUnit 5, MockMvc, H2 (for isolated test runs)
- **Build Tool**: Maven

---

## Database Configuration & Schema
The Auth Service owns its MySQL database (`devsphere_auth`). Schema creation is managed exclusively via Flyway migrations (`db/migration/V1__create_users_table.sql`). Hibernate auto DDL is set to `validate`.

### Table: `users`
* `id` (`BIGINT AUTO_INCREMENT PRIMARY KEY`)
* `email` (`VARCHAR(255) NOT NULL UNIQUE`)
* `password_hash` (`VARCHAR(255) NOT NULL`)
* `created_at` (`TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`)
* `updated_at` (`TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`)

---

## Environment Variables

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `devsphere_auth` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(empty)* |

---

## Running Locally

### Prerequisites
* MySQL 8.x running on `localhost:3306` with database `devsphere_auth` created.

```bash
# Navigate to the auth-service directory
cd services/auth-service

# Run application
mvn spring-boot:run
```

The application will start on port `8081`.

---

## Running Automated Tests

```bash
mvn test
```

Automated tests run against an in-memory H2 database using the `test` Spring profile.

---

## API Endpoints

### 1. User Registration

`POST /api/v1/auth/register`

#### Example Request
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

#### Example Successful Response (`201 CREATED`)
```json
{
  "id": 1,
  "email": "user@example.com",
  "createdAt": "2026-08-22T16:30:00Z"
}
```

#### Error Responses

* **Duplicate Email (`409 CONFLICT`)**:
  ```json
  {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "An account with this email already exists"
  }
  ```

* **Validation Error (`400 BAD REQUEST`)**:
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "errors": {
      "email": "must be a valid email address",
      "password": "Password must be between 8 and 100 characters"
    }
  }
  ```

---

## Security Notes
- Passwords are salted and hashed using BCrypt (`BCryptPasswordEncoder`).
- Passwords and password hashes are never logged.
- Passwords and password hashes are never returned in API responses.
- Plaintext passwords exist only transiently in memory during registration processing.

---

## Current Limitations & Future Responsibilities
- **Current Limitations**: No login endpoint, no JWT generation, no token verification, no email verification, no password reset.
- **Future Responsibilities**: Issuing JWT access/refresh tokens, login verification (`/api/v1/auth/login`), password change/reset workflows.
