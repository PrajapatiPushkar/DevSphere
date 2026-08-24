# DevSphere Configuration Repository

Centralized configuration repository for DevSphere microservices platform. Managed via Spring Cloud Config Server.

## Architecture

This repository holds externalized non-secret configuration for all microservices in the DevSphere system:

```
config-repo/
├── application.yml        # Shared configuration (Eureka zone, Kafka bootstrap servers, Actuator exposure)
├── api-gateway.yml        # API Gateway routes, server port, application name
├── auth-service.yml       # Auth Service DB URL pattern, JPA, Outbox polling, Kafka producer, JWT expiration
├── user-service.yml       # User Service DB URL pattern, JPA, Kafka consumer retries, Redis settings, Cache TTL
├── service-discovery.yml # Eureka Service Discovery server settings
└── README.md
```

## Secret Policy

> [!CAUTION]
> **NO SECRETS PERMITTED IN THIS REPOSITORY**
> 
> Never place the following in tracked configuration files:
> - Database passwords (`DB_PASSWORD`)
> - JWT signing keys (`JWT_SECRET`)
> - Redis passwords (`REDIS_PASSWORD`)
> - API keys or OAuth client secrets
> - Private keys or TLS certificates
> 
> Environment variable placeholders (e.g. `${DB_PASSWORD}`, `${JWT_SECRET}`) are used so actual secrets are injected at runtime from secure environment variables or secret managers.

## Version Control & Local Git Setup

This directory is structured to be maintained as an independent Git repository.

To initialize local Git repository:

```bash
cd config-repo
git init
git add .
git commit -m "chore(config): initialize centralized configuration repository"
```

## Spring Cloud Config API

Spring Cloud Config Server serves configuration via HTTP GET endpoints:

- `GET http://localhost:8888/api-gateway/default`
- `GET http://localhost:8888/auth-service/default`
- `GET http://localhost:8888/user-service/default`
- `GET http://localhost:8888/service-discovery/default`
