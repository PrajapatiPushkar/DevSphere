# DevSphere API Gateway

## Purpose
The API Gateway serves as the single external entry point and perimeter security boundary for all client requests in the DevSphere microservices architecture. It handles request routing, reactive JWT authentication validation, and request header sanitization before forwarding requests to downstream services.

---

## Current Status
> **Perimeter JWT Validation & Protected Routing (Lesson 6)**  
> The API Gateway validates incoming JWT access tokens at the perimeter using a reactive `JwtAuthenticationFilter`. Public routes (`/api/v1/auth/*`, `/actuator/health`) pass through freely, while protected routes (`/api/demo/protected`) require a valid `Bearer` JWT token.

---

## Responsibilities
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Configuration-driven request routing (`/api/v1/auth/**` → Auth Service on `8081`, `/api/demo/**` → Temporary Stub on `8081`).
- Reactive JWT signature (HS256) & expiration validation.
- Sanitizing and injecting trusted internal identity header (`X-Authenticated-User-Id`).
- Returning standardized `401 Unauthorized` JSON responses.
- Exposing service health metrics via Spring Boot Actuator (`/actuator/health`).

---

## Environment Variables

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `JWT_SECRET` | HS256 Secret Key (Min 32 chars / 256 bits) | *Development fallback* |

---

## Route Security Policy

### 1. Public Routes (No JWT Required)
* `POST /api/v1/auth/register`
* `POST /api/v1/auth/login`
* `GET /actuator/health`
* `GET /api/demo/hello` (Lesson 3 Routing Verification)

### 2. Protected Routes (JWT Required)
* `GET /api/demo/protected` (Requires `Authorization: Bearer <valid-jwt-token>`)

---

## Error Responses

* **Missing / Malformed Token (`401 UNAUTHORIZED`)**:
  ```json
  {
    "code": "UNAUTHORIZED",
    "message": "Authentication is required"
  }
  ```

* **Invalid / Expired Token (`401 UNAUTHORIZED`)**:
  ```json
  {
    "code": "INVALID_TOKEN",
    "message": "The access token is invalid or expired"
  }
  ```

---

## Running Automated Tests

```bash
cd services/api-gateway
mvn test
```
