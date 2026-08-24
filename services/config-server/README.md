# DevSphere Config Server

Spring Cloud Config Server microservice providing centralized configuration management across all DevSphere services.

## Overview

- **Port**: `8888`
- **Application Name**: `config-server`
- **Main Class**: `com.devsphere.config.DevSphereConfigServerApplication`
- **Annotation**: `@EnableConfigServer`
- **Configuration Repository**: Git-backed (`config-repo/`)

## Observability & Endpoints

- `GET /actuator/health`: Service health status check.
- `GET /actuator/prometheus`: Micrometer Prometheus metrics scrape target.
- `GET /api-gateway/default`: API Gateway configuration payload.
- `GET /auth-service/default`: Auth Service configuration payload.
- `GET /user-service/default`: User Service configuration payload.

## Testing

```bash
mvn test
```
