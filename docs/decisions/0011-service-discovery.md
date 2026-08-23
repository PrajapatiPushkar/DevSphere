# 11. Service Discovery with Netflix Eureka

Date: 2026-08-23

## Status

Accepted

## Context

Prior to Lesson 12, `API Gateway` routed incoming client traffic using hardcoded static HTTP endpoints (`http://localhost:8081` for Auth Service and `http://localhost:8082` for User Service). 

Hardcoded IP addresses and ports prevent dynamic auto-scaling, load balancing across multi-instance deployments, and zero-downtime service relocation. We require dynamic service registration and lookup.

## Decision

We adopt **Spring Cloud Netflix Eureka** for microservice service discovery:

1. **Eureka Discovery Server (`services/service-discovery`)**:
   - Created a standalone Eureka Server module operating on port `8761`.
   - Application Name: `DEVSPHERE-SERVICE-DISCOVERY`.
   - Disabled self-registration (`register-with-eureka: false`, `fetch-registry: false`).

2. **Eureka Discovery Clients**:
   - Registered `API Gateway` (`DEVSPHERE-API-GATEWAY`), `Auth Service` (`DEVSPHERE-AUTH-SERVICE`), and `User Service` (`DEVSPHERE-USER-SERVICE`) as Eureka clients.
   - Microservices register automatically upon startup and send periodic heartbeats to Eureka Server.

3. **Dynamic Gateway Routing (`lb://`)**:
   - Updated `API Gateway` routes to use `lb://<SERVICE-NAME>` URIs (`lb://DEVSPHERE-AUTH-SERVICE` and `lb://DEVSPHERE-USER-SERVICE`).
   - Removed all hardcoded downstream service URLs from API Gateway.

4. **Security & Perimeter Integrity**:
   - Preserved perimeter JWT validation in API Gateway prior to discovery-based route dispatching.
   - Event-driven Kafka messaging and Redis caching remain completely decoupled from Eureka.

## Consequences

### Positive
- **Dynamic Routing**: Eliminates hardcoded downstream service IPs/ports from API Gateway.
- **Horizontal Scaling Foundation**: Enables multi-instance load balancing (`lb://`) across registered service instances.
- **Operational Visibility**: Provides interactive registry status via Eureka Dashboard (`http://localhost:8761`).

### Negative / Trade-offs
- Additional infrastructure service (`service-discovery`).
- Adds minor heartbeat network traffic between clients and Eureka Server.
