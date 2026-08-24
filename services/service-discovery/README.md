# DevSphere Service Discovery

## Overview

The **Service Discovery** microservice provides dynamic service registration and lookup capabilities for the DevSphere microservices ecosystem using **Spring Cloud Netflix Eureka Server**. It operates on port `8761`.

```
                         ┌─────────────────────────────────┐
                         │   DEVSPHERE-SERVICE-DISCOVERY   │ (Port 8761)
                         └────────────────┬────────────────┘
                                          │
        ┌────────────────────────────────┼────────────────────────────────┐
        │ Register & Heartbeat           │ Register & Heartbeat           │ Register & Heartbeat
        ▼                                ▼                                ▼
┌───────────────┐                ┌───────────────┐                ┌───────────────┐
│  API Gateway  │                │ Auth Service  │                │ User Service  │
│  (Port 8080)  │                │  (Port 8081)  │                │  (Port 8082)  │
└───────────────┘                └───────────────┘                └───────────────┘
```

---

## Domain Responsibilities

- **Dynamic Service Registry**: Maintains an in-memory registry of active microservice instances.
- **Self-Preservation & Heartbeats**: Accepts periodic heartbeats from registered clients (`API Gateway`, `Auth Service`, `User Service`).
- **Eureka Dashboard**: Exposes an operational monitoring dashboard at `http://localhost:8761`.
- **Standalone Discovery Server**: Configured with `register-with-eureka: false` and `fetch-registry: false` so it does not register itself as a client.
- **Centralized Configuration Import (Lesson 13)**: Can consume non-secret centralized properties from Spring Cloud Config Server (`http://localhost:8888`).

---

## Service Registry Overview

| Logical Service Name | Port | Description |
|---|---|---|
| `DEVSPHERE-SERVICE-DISCOVERY` | `8761` | Netflix Eureka Service Discovery Server |
| `DEVSPHERE-API-GATEWAY` | `8080` | Perimeter Gateway & Discovery Route Resolver |
| `DEVSPHERE-AUTH-SERVICE` | `8081` | Identity & Authentication Service |
| `DEVSPHERE-USER-SERVICE` | `8082` | User Profile Domain Service |

---

## Running Locally

```powershell
# Run service-discovery locally
mvn spring-boot:run
```

Once running, access the dashboard at:
- `http://localhost:8761`

---

## Running Tests

```powershell
mvn test
```
