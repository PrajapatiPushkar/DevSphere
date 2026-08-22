# DevSphere API Gateway

## Purpose
The API Gateway is designed to serve as the single entry point for all client requests in the DevSphere microservices architecture. It handles central concerns such as request routing, authentication verification, rate limiting, and request correlation.

---

## Current Status
> **Spring Cloud Gateway Routing (Lesson 3)**  
> The API Gateway is configured with Spring Cloud Gateway (reactive implementation). Gateway routing is operational using a temporary downstream verification route. Real microservice routes will be introduced as business services are built.

---

## Current Responsibility
- Reactive Spring Cloud Gateway bootstrap on port `8080`.
- Configuration-driven request routing (`/api/demo/**` → `http://localhost:8081`).
- Health monitoring via Spring Boot Actuator endpoint (`/actuator/health`).

---

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Spring Cloud**: 2023.0.1 (`spring-cloud-starter-gateway`)
- **Web Engine**: Spring WebFlux / Reactor Netty
- **Actuator**: `spring-boot-starter-actuator`
- **Testing**: `spring-boot-starter-test` (JUnit 5, WebTestClient)
- **Build Tool**: Maven

---

## Gateway Routing

Spring Cloud Gateway routes external client requests to downstream services based on path predicates.

### Temporary Verification Route
To verify routing functionality without creating premature business microservices, a temporary verification route is configured:

* **Client Request**: `GET http://localhost:8080/api/demo/hello`
* **Gateway Action**: Rewrites `/api/demo/hello` to `/internal/demo/hello` and forwards to the temporary stub running on port `8081`.
* **Expected Response**:
  ```json
  {
    "service": "temporary-demo-service",
    "message": "Request successfully routed through DevSphere API Gateway"
  }
  ```

> **Note**: This route exists only to verify gateway-to-downstream routing and will later be replaced by real microservice routes.

---

## Running Locally

To compile and run the API Gateway locally:

```bash
# Navigate to the service directory
cd services/api-gateway

# Run the application
mvn spring-boot:run
```

The application will start on port `8080` (and the temporary verification stub will start on port `8081`).

---

## Running Automated Tests

To execute unit and application context tests:

```bash
mvn clean test
```

---

## Health Check

Verify system health using Spring Boot Actuator:

* **Endpoint**: `GET http://localhost:8080/actuator/health`
* **Expected Response**:
  ```json
  {
    "status": "UP"
  }
  ```

---

## Future Responsibility

In future lessons, the API Gateway will be expanded to provide:
- Dynamic request routing to real downstream microservices (Auth, Task, User, Career, etc.).
- Perimeter security via JWT token validation filters.
- Distributed rate limiting via Redis.
- Observability and request correlation tracing across microservices.
