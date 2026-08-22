# DevSphere API Gateway

## Purpose
The API Gateway is designed to serve as the single entry point for all client requests in the DevSphere microservices architecture. It will eventually handle central concerns such as request routing, authentication verification, rate limiting, and request correlation.

---

## Current Status
> **Foundation Service (Lesson 2)**  
> The API Gateway is currently a foundation service. Gateway routing and service discovery will be introduced in later lessons.

---

## Current Responsibility
- Spring Boot application bootstrap and context initialization.
- Health monitoring via Spring Boot Actuator endpoint (`/actuator/health`).

---

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.2.5
- **Modules**: `spring-boot-starter-web`, `spring-boot-starter-actuator`
- **Testing**: `spring-boot-starter-test` (JUnit 5, Spring Boot Test)
- **Build Tool**: Maven

---

## Running Locally

To compile and run the API Gateway locally:

```bash
# Navigate to the service directory
cd services/api-gateway

# Run the application
mvn spring-boot:run
```

The application will start on port `8080`.

---

## Running Automated Tests

To execute unit and application context tests:

```bash
mvn test
```

---

## Health Check

Once the application is running, verify system health using Spring Boot Actuator:

* **Endpoint**: `GET http://localhost:8080/actuator/health`
* **Expected Response**:
  ```json
  {
    "status": "UP"
  }
  ```

---

## Future Responsibility

In future lessons, the API Gateway will be enhanced with Spring Cloud Gateway to provide:
- Dynamic request routing to downstream microservices (Auth, Task, User, Career, etc.).
- JWT token validation and identity extraction.
- Distributed rate limiting (via Redis).
- Observability and request correlation tracing across microservices.
