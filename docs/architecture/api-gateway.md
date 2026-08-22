# API Gateway Architecture Document

## Overview

The API Gateway acts as the single external gateway and reverse proxy for DevSphere client applications (Web Frontend, mobile apps, or external integrations).

```
[ Frontend Client ]
        │
        ▼
  [ API Gateway ] (Port 8080)
        │
 ┌──────┼──────────────┬──────────────┐
 ▼      ▼              ▼              ▼
[Auth] [User]        [Task]        [Career]  ... (Microservices)
```

---

## Lesson 3 — Gateway Routing

Spring Cloud Gateway (reactive implementation built on Spring WebFlux and Reactor Netty) has been integrated into `services/api-gateway`.

### Routing Architecture

```
[ Client Request ] (GET http://localhost:8080/api/demo/hello)
        │
        ▼
[ Spring Cloud Gateway ] (Port 8080)
  - Predicate: Path=/api/demo/**
  - Filter: RewritePath=/api/demo/(?<segment>.*), /internal/demo/${segment}
        │
        ▼
[ Downstream Service ] (GET http://localhost:8081/internal/demo/hello)
  └─► Lesson 3 Verification Only: Temporary Demo Stub Server (Port 8081)
```

### Key Highlights
- **Configuration-Driven Routing**: Routes are defined declaratively in `application.yml`.
- **Reactive Engine**: Utilizes non-blocking I/O for scalable request proxying.
- **Temporary Verification Stub**: A lightweight local stub running on port `8081` validates gateway-to-downstream HTTP forwarding.
- **Service Isolation**: Real business services (Auth, User, Task, Career, etc.) will replace the temporary demo route in subsequent lessons.

---

## Current Status (Lesson 3 Implementation)

### Implemented Functionality
- **Reactive Bootstrap**: Spring Boot 3.2.5 + Spring Cloud Gateway (`2023.0.1`) running on Java 21.
- **Gateway Routing Engine**: Active routing from `/api/demo/**` to `http://localhost:8081`.
- **Health Endpoint**: `/actuator/health` preserved and operational on Spring WebFlux / Netty stack.
- **Automated Tests**: Integration test verifying Gateway routing and Actuator health endpoints.

### Intentionally NOT Implemented Yet
- ❌ **Production Auth Routing**: Direct gateway route mapping to `auth-service` (Future routing flow: `Client -> API Gateway -> Auth Service`).
- ❌ **Authentication Filter**: JWT perimeter validation filter.
- ❌ **Rate Limiting Filter**: Redis-backed rate limiting.
- ❌ **Service Discovery**: Eureka / Consul dynamic service routing.
- ❌ **Load Balancing**: Client-side load balancing via Spring Cloud LoadBalancer.
