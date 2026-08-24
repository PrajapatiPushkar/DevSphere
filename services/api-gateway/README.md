# DevSphere API Gateway

## Purpose
The API Gateway serves as the single external entry point and perimeter security boundary for all client requests in the DevSphere microservices architecture. It handles request routing, reactive JWT authentication validation, and request header sanitization before forwarding requests to downstream services.

---

## Current Status
> **Production Resilience & Fault Tolerance Boundary (Lesson 16)**  
> The API Gateway enforces bounded downstream timeouts (connect timeout 3s, response timeout 5s, timelimiter 3s), Resilience4j circuit breakers (`authServiceCircuitBreaker`, `userServiceCircuitBreaker`), header sanitization, role extraction, perimeter authorization, and truthful 503 Service Unavailable fallbacks (`FallbackController`) without returning fake business data.

---

## Responsibilities
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Centralized configuration import from Spring Cloud Config Server (`http://localhost:8888`).
- Dynamic discovery-based routing via Netflix Eureka (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).
- Reactive JWT signature (HS256) & expiration validation.
- Role extraction from JWT claims (`roles: ["USER"]` / `["ADMIN"]`).
- Coarse route authorization (e.g. enforcing `ROLE_ADMIN` on `/api/v1/admin/**` routes).
- Bounded downstream timeouts and Spring Cloud Circuit Breaker Resilience4j protection.
- Truthful 503 Service Unavailable fallback handling (`/fallback/auth-service`, `/fallback/user-service`).
- Prometheus metrics exposure via `/actuator/prometheus` including `devsphere_resilience_fallback_total` and Resilience4j circuit breaker metrics.

---

## Actuator & Prometheus Endpoints

- `GET /actuator/health`: Service health indicator.
- `GET /actuator/prometheus`: Micrometer Prometheus metrics scrape target.

---

## Environment Variables

| Variable | Description | Local Default |
| :--- | :--- | :--- |
| `JWT_SECRET` | HS256 Secret Key (Min 32 chars / 256 bits) | *Development fallback* |
| `CONFIG_SERVER_URL` | Spring Cloud Config Server URL | `http://localhost:8888` |
| `EUREKA_SERVER_URL` | Netflix Eureka Server URL | `http://localhost:8761/eureka/` |

---

## Route Security Policy

### 1. Public Routes (No JWT Required)
* `POST /api/v1/auth/register`
* `POST /api/v1/auth/login`
* `GET /actuator/health`
* `GET /actuator/prometheus`

### 2. Protected Routes (JWT Required)
* `GET /api/v1/users/me`

---

## Running Automated Tests

```bash
cd services/api-gateway
mvn test
```
