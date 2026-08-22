# DevSphere

DevSphere is a developer career and productivity platform designed to help developers manage their tasks, goals, learning, coding practice, projects and career journey from one place.

---

## Project Status

🚧 **Under Active Development**

DevSphere is progressing through its incremental milestone lessons. **Lesson 1 (Repository Foundation)**, **Lesson 2 (API Gateway Foundation)**, and **Lesson 3 (Spring Cloud Gateway & Request Routing Foundation)** are complete. Spring Cloud Gateway routing is functional using a temporary verification stub route. Real business microservices will be implemented in upcoming lessons.

---

## Vision

DevSphere is envisioned as a production-grade multi-user SaaS platform built for developers. The goal is to provide a single, unified workspace for personal productivity, skill growth, and career trajectory management, backed by a resilient distributed system architecture.

---

## Planned Features

- **Authentication & User Isolation**: Secure multi-user access with identity-isolated data access.
- **Task Management**: Production-style task creation, prioritization, and tracking.
- **Daily Planning**: Day planning and productivity scheduling.
- **Goal Tracking**: Long-term career and skill milestone tracking.
- **DSA / Coding Progress Tracking**: Practice problem logging, topic breakdown, and revision cycles.
- **Learning Management**: Course, book, and article study tracking.
- **Personal Project Management**: Ideas, roadmap milestones, and repository linkages.
- **Internship & Job Application Tracking**: Pipeline management for applications, interview stages, and offers.
- **Professional Profile & Resume Management**: Structured resume data management and exportable profile details.
- **Notification Engine**: System and user notification alerts.
- **Productivity & Career Analytics**: Visual metrics on coding progress, task completion, and application success.

---

## Planned Architecture

DevSphere will transition into a modern microservices architecture consisting of service-owned databases, explicit API contracts, and event-driven communication where appropriate:

- **API Gateway**: Single entry point for routing, authentication verification, and rate limiting.
- **Auth Service**: Identity management, authentication, and token issuance.
- **User Service**: User profiles and user settings management.
- **Task Service**: Task lifecycle, categories, and planning state.
- **Learning Service**: Educational resources, courses, and DSA practice tracking.
- **Career Service**: Job applications, internship tracking, and resume data.
- **Project Service**: Personal project roadmaps and tracking.
- **Notification Service**: Asynchronous notifications dispatched via messaging queues.
- **Analytics Service**: Event-driven productivity and progress metrics calculation.

---

## Technology Stack

> **Note**: The technologies listed below represent the *planned* stack for DevSphere. They are not yet installed or configured in this initial lesson.

### Backend (Planned)
- **Language & Core Framework**: Java 21, Spring Boot
- **Distributed System Infrastructure**: Spring Cloud (Service Discovery, Gateway, Configuration Management)
- **Security & Identity**: Spring Security, JWT (JSON Web Tokens)
- **Persistence & ORM**: Spring Data JPA, Hibernate, MySQL
- **Build Tool**: Maven
- **DTO Mapping & Validation**: MapStruct, Bean Validation (JSR 380)
- **API Documentation**: OpenAPI / Swagger
- **Testing**: JUnit 5, Mockito

### Distributed Systems & Infrastructure (Planned)
- **API Gateway**: Spring Cloud Gateway
- **Message Broker**: Apache Kafka (for asynchronous event-driven workflows)
- **Caching & In-Memory Storage**: Redis (when genuinely required for caching or rate-limiting)
- **Containerization**: Docker, Docker Compose
- **Observability**: Prometheus, Grafana, Distributed Tracing (Micrometer Tracing / Zipkin)
- **CI/CD**: GitHub Actions

### Frontend (Planned)
- **Framework & Language**: React, TypeScript
- **Build Tooling**: Vite
- **Routing**: React Router
- **HTTP Client**: Axios
- **Styling**: Tailwind CSS

---

## Repository Structure

```
DevSphere/
│
├── services/               # Microservices (Auth, Task, Learning, Career, etc.)
│   └── .gitkeep
│
├── frontend/               # React + TypeScript single-page application
│   └── .gitkeep
│
├── infrastructure/         # Infrastructure configurations
│   ├── docker/             # Docker Compose & container files
│   ├── monitoring/         # Observability configs (Prometheus, Grafana)
│   └── deployment/         # CI/CD & deployment configurations
│
├── docs/                   # Documentation
│   ├── architecture/       # Architectural diagrams & design docs
│   ├── api/                # OpenAPI specs & API contracts
│   ├── database/           # Schema design & ER diagrams
│   └── decisions/          # Architecture Decision Records (ADRs)
│
├── .github/                # GitHub configurations & Workflows
│   └── workflows/
│
├── .gitignore              # Multi-stack gitignore
├── README.md               # Project overview
└── LICENSE                 # Open-source license (MIT)
```

---

## Development Philosophy

1. **Incremental Evolution**: Built step-by-step with clear lesson boundaries.
2. **Production-Grade Quality**: Strict coding standards, explicit domain boundaries, and zero throwaway patterns.
3. **No Fake Functionality**: Features are implemented with real backend logic and persistence—never mock placeholders.
4. **Architectural Justification**: Microservices, Kafka, Redis, and infrastructure are introduced only when real domain requirements dictate them.
5. **Verified Milestones**: Every lesson leaves the repository in a fully working, tested, and validated state before moving forward.

---

## Development Roadmap

- **Lesson 1**: Production Repository Foundation *(Completed)*
- **Lesson 2**: API Gateway Foundation Service *(Completed)*
- **Lesson 3**: Spring Cloud Gateway & Request Routing Foundation *(Current)*
- **Lesson 4**: Identity & Authentication Service Implementation *(Upcoming)*
- **Lesson 5**: Core Domain Services & Data Persistence *(Upcoming)*
- **Lesson 6**: Event-Driven Integration & Messaging *(Upcoming)*
- **Lesson 7**: Frontend Core SPA Setup & Gateway Integration *(Upcoming)*
- **Lesson 8**: Observability, Monitoring & Production Deployment *(Upcoming)*

---

## Future Deployment

The target deployment strategy will leverage containerized microservices running behind an API Gateway, with continuous integration and delivery (CI/CD) pipelines validating code builds, automated tests, and Docker images before deployment to a cloud environment.

---

## License

This project is licensed under the [MIT License](LICENSE).
