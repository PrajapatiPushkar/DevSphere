# DevSphere Config Server

Spring Cloud Config Server microservice providing centralized configuration management across all DevSphere services.

## Overview

- **Port**: `8888`
- **Application Name**: `config-server`
- **Main Class**: `com.devsphere.config.DevSphereConfigServerApplication`
- **Annotation**: `@EnableConfigServer`
- **Configuration Repository**: Git-backed (`config-repo/`)

## Architecture

```
                    ┌─────────────────────────┐
                    │    Config Repository    │
                    │      (config-repo)      │
                    └────────────┬────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │     Config Server       │
                    │         :8888           │
                    └────────────┬────────────┘
                                 │
                 ┌───────────────┼────────────────┐
                 ▼               ▼                ▼
          API Gateway      Auth Service      User Service
```

## Running Locally

To start the Config Server:

```bash
cd services/config-server
mvn spring-boot:run
```

## Config Server APIs

Once running, verify served configuration via HTTP GET:

- **API Gateway Config**: `http://localhost:8888/api-gateway/default`
- **Auth Service Config**: `http://localhost:8888/auth-service/default`
- **User Service Config**: `http://localhost:8888/user-service/default`
- **Service Discovery Config**: `http://localhost:8888/service-discovery/default`

## Health Check

- `GET http://localhost:8888/actuator/health`

Returns:
```json
{
  "status": "UP"
}
```

## Secret Policy

No secrets are stored in `config-repo` or exposed via Config Server. Placeholders such as `${JWT_SECRET}` and `${DB_PASSWORD}` are resolved via environment variables at client runtime.

## Testing

Run unit tests:

```bash
mvn test
```
