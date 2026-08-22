# DevSphere — High-Level Architecture Overview

> **Status**: Planned Architecture (Lesson 1 — Foundation Stage)  
> *None of the components described below are implemented yet. This document establishes the target architectural blueprint for DevSphere.*

---

## Target Architectural Flow

DevSphere is designed as a distributed, multi-service SaaS platform. The high-level data and request flow is structured as follows:

```
[ User Browser / Client ]
           │
           ▼
  [ Frontend SPA ] (React + TypeScript)
           │
           ▼
   [ API Gateway ] (Routing, Rate Limiting, Central Security)
           │
 ┌─────────┼───────────────────┬───────────────────┐
 ▼         ▼                   ▼                   ▼
[Auth Service] [User Service] [Task Service] [Career Service] ... (Microservices)
 │         │                   │                   │
 ▼         ▼                   ▼                   ▼
[(DB)]   [(DB)]              [(DB)]              [(DB)]      (Service-Owned DBs)
 └─────────┴───────────────────┴───────────────────┘
           │
           ▼ (Asynchronous Events)
    [ Apache Kafka ]
           │
           ▼
 [ Notification & Analytics Services ]
           │
           ▼
[ Observability & Monitoring ] (Prometheus, Grafana, Distributed Tracing)
           │
           ▼
[ CI/CD Pipeline & Production Deployment ] (GitHub Actions, Docker Containers)
```

---

## Component Responsibilities

1. **Frontend**: Single-page application built with React and TypeScript, communicating with backend microservices strictly via the API Gateway.
2. **API Gateway**: Entry point for all external client traffic. Handles routing, authentication verification, CORS, and request rate-limiting.
3. **Microservices**: Independent domain services (Auth, User, Task, Learning, Career, Project, Notification, Analytics). Each service encapsulates its business logic and data model.
4. **Service-Owned Databases**: Each microservice manages its own dedicated database instance. Direct cross-database queries or access between services are strictly prohibited.
5. **Event-Driven Communication**: Apache Kafka broker handles asynchronous, event-driven workflows (e.g., dispatching notifications or calculating analytics) without coupling synchronous microservice requests.
6. **Observability**: Prometheus metrics collection, Grafana dashboards, and distributed tracing across microservices to ensure production-grade reliability.
7. **CI/CD & Production Deployment**: Automated build, test, and containerization pipelines deploying containerized microservices to cloud environments.
