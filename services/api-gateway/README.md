# DevSphere API Gateway

## Purpose
The API Gateway serves as the single external entry point and perimeter security boundary for all client requests in the DevSphere microservices architecture. It handles request routing, reactive JWT authentication validation, and request header sanitization before forwarding requests to downstream services.

---

## Current Status
> **Observability, Centralized Config, Service Discovery & Perimeter JWT Validation (Lesson 14)**  
> The API Gateway exposes operational metrics via `/actuator/prometheus` (`io.micrometer:micrometer-registry-prometheus`), consumes centralized configuration from Spring Cloud Config Server (`http://localhost:8888`), registers as a Eureka client (`DEVSPHERE-API-GATEWAY`), and dynamically routes traffic to downstream microservices using discovery URIs (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).

---

## Responsibilities
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Centralized configuration import from Spring Cloud Config Server.
- Dynamic discovery-based routing via Netflix Eureka (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).
- Reactive JWT signature (HS256) & expiration validation.
- Sanitizing and injecting trusted internal identity header (`X-Authenticated-User-Id`).
- Prometheus metrics exposure via `/actuator/prometheus`.
- Exposing service health metrics via Spring Boot Actuator (`/actuator/health`).

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
