# DevSphere API Gateway

## Purpose
The API Gateway serves as the single external entry point and perimeter security boundary for all client requests in the DevSphere microservices architecture. It handles request routing, reactive JWT authentication validation, and request header sanitization before forwarding requests to downstream services.

---

## Current Status
> **Service Discovery & Perimeter JWT Validation (Lesson 6 & Lesson 12)**  
> The API Gateway registers as a Eureka client (`DEVSPHERE-API-GATEWAY`) and dynamically routes traffic to downstream microservices using discovery URIs (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`). Perimeter security validates incoming JWT access tokens using a reactive `JwtAuthenticationFilter`.

---

## Responsibilities
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Dynamic discovery-based routing via Netflix Eureka (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).
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
