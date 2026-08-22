# API Gateway Architecture Document

## Overview

The API Gateway acts as the single external gateway and reverse proxy for DevSphere client applications (Web Frontend, mobile apps, or external integrations).

```
[ Frontend Client ]
        │
        ▼
  [ API Gateway ]
        │
 ┌──────┼──────────────┬──────────────┐
 ▼      ▼              ▼              ▼
[Auth] [User]        [Task]        [Career]  ... (Microservices)
```

---

## Current Status (Lesson 2 Implementation)

The API Gateway is currently initialized as a **Spring Boot Foundation Service**.

### Implemented Functionality
- **Service Bootstrap**: Spring Boot 3.x application initialization on Java 21.
- **Port Binding**: Runs on port `8080`.
- **Health Endpoint**: Exposes `/actuator/health` via Spring Boot Actuator.
- **Automated Testing**: Base application context loading verification.

### Intentionally NOT Implemented Yet
- ❌ **Gateway Routing**: No Spring Cloud Gateway route definitions or proxying.
- ❌ **Authentication Integration**: No JWT validation or security filters.
- ❌ **Rate Limiting**: No Redis or token-bucket rate limiters.
- ❌ **Service Discovery**: No Eureka / Consul registration or dynamic routing.
- ❌ **Load Balancing**: No client-side or server-side load balancing.

---

## Planned Future Responsibilities

As DevSphere evolves, the API Gateway will assume the following cross-cutting responsibilities:

1. **Request Routing**: Intelligently forwarding client requests to specific downstream microservices (`/api/v1/auth/*` → Auth Service, `/api/v1/tasks/*` → Task Service).
2. **Centralized Security**: Validating incoming JWT access tokens at the perimeter before forwarding requests to internal services.
3. **Rate Limiting & Throttling**: Protecting backend microservices against overuse or denial-of-service attacks.
4. **Request Correlation & Tracing**: Injecting correlation IDs (`X-Correlation-ID`) into request headers for distributed tracing across microservices.
5. **Observability Integration**: Exporting gateway-level HTTP latency, throughput, and error metrics to Prometheus.
