# DevSphere API Gateway

## Purpose
The API Gateway serves as the single external entry point and perimeter security boundary for all client requests in the DevSphere microservices architecture. It handles request routing, reactive JWT authentication validation, and request header sanitization before forwarding requests to downstream services.

---

## Current Status
> **Production Perimeter Authorization & Role Extraction (Lesson 15)**  
> The API Gateway validates JWT signatures and expiration, extracts role claims (`roles`), strips untrusted client headers (`X-Role`, `X-User-Role`, `X-Admin`, `X-Authenticated-User-Id`, `X-Authenticated-User-Roles`), enforces coarse route-level authorization (returning 403 Forbidden for unauthorized access to admin paths), forwards trusted user identity and roles downstream, exposes Prometheus metrics (`/actuator/prometheus`), and dynamically routes traffic using Eureka.

---

## Responsibilities
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Centralized configuration import from Spring Cloud Config Server.
- Dynamic discovery-based routing via Netflix Eureka (`lb://DEVSPHERE-AUTH-SERVICE`, `lb://DEVSPHERE-USER-SERVICE`).
- Reactive JWT signature (HS256) & expiration validation.
- Role extraction from JWT claims (`roles: ["USER"]` / `["ADMIN"]`).
- Coarse route authorization (e.g. enforcing `ROLE_ADMIN` on `/api/v1/admin/**` routes).
- Sanitizing client headers and injecting trusted internal identity headers (`X-Authenticated-User-Id`, `X-Authenticated-User-Roles`).
- Prometheus metrics exposure via `/actuator/prometheus` including `devsphere_auth_authorization_denied_total`.
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
