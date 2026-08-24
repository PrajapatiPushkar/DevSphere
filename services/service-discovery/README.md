# DevSphere Service Discovery

## Overview

The **Service Discovery** microservice provides dynamic service registration and lookup capabilities for the DevSphere microservices ecosystem using **Spring Cloud Netflix Eureka Server**. It operates on port `8761` and exposes Prometheus application metrics at `/actuator/prometheus`.

---

## Observability & Endpoints

- `GET /actuator/health`: Service health status check.
- `GET /actuator/prometheus`: Micrometer Prometheus metrics scrape target.
- `GET /`: Eureka operational monitoring dashboard.

---

## Running Tests

```powershell
mvn test
```
