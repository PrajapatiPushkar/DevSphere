# ADR 0002: API Gateway Routing & Perimeter Architecture

* **Status**: Accepted
* **Date**: 2026-08-22
* **Context**: Deciding the perimeter architectural pattern for routing external client traffic to downstream DevSphere microservices.

---

## Decision

We will use **Spring Cloud Gateway** (Reactive / Spring WebFlux stack) as the single external entry point for all DevSphere API traffic.

---

## Rationale & Architectural Drivers

1. **Single Entry Point**: Provides a unified API facade for client applications (Web Frontend, Mobile) to communicate with backend microservices.
2. **Centralized Routing**: Decouples client applications from internal microservice hostnames, ports, and instance counts.
3. **Cross-Cutting Perimeter Concerns**: Enables centralized implementation of perimeter security (JWT validation), rate limiting, request correlation, and telemetry without duplicating logic in downstream services.
4. **Independent Domain Evolution**: Downstream microservices can be refactored, split, or scaled independently behind stable Gateway URI routes.
5. **Clean Separation of Concerns**: **The API Gateway does NOT own business logic.** Business rules, data persistence, and domain workflows reside exclusively inside downstream microservices.

---

## Implementation Strategy (Lesson 3)

- **Reactive Foundation**: Spring Cloud Gateway is built on Spring WebFlux and Reactor Netty for non-blocking, high-performance request proxying.
- **Declarative Route Definitions**: Routes are managed via configuration (`application.yml`) using Path predicates and RewritePath filters.
- **Temporary Verification Stub**: A lightweight, embedded HTTP stub running on port `8081` is used temporarily in Lesson 3 to verify gateway-to-downstream HTTP request forwarding before real domain microservices (Auth, Task, User, etc.) are built.
