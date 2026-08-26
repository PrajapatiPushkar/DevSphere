# ADR 0029: Developer Project Management Domain in User Service

## Status
Accepted

## Date
2026-08-27

## Context
DevSphere is a production-grade microservices platform for developer life management. The current bounded contexts inside `user-service` include:
- Developer Profile
- Goals
- Tasks
- Daily Planner
- DSA Progress Tracking

Developers require a structured domain to manage their personal software projects (planning, building, maintaining, completing, and archiving).

## Decision
We introduce the **Developer Project Management Domain** directly inside `services/user-service`. We explicitly decide **NOT** to split this into a standalone `project-service` microservice at this stage.

### Key Rationale
1. **Personal Identity & Productivity Data**: Personal projects reflect a developer's individual profile and progress data, matching the bounded context of `user-service`.
2. **Simplified Operational Footprint**: Avoids premature microservice fragmentation, extra network latency, cross-service database access, and additional deployment overhead.
3. **Future Extensibility**: Keeps open clean future integrations with Goals and Tasks without complex distributed transaction requirements.

## Architecture & Principles
- **Entity**: `DeveloperProject` with status lifecycle (`PLANNED`, `IN_PROGRESS`, `COMPLETED`, `ON_HOLD`, `ARCHIVED`) and types (`PERSONAL`, `COLLEGE`, `PROFESSIONAL`, `OPEN_SOURCE`, `FREELANCE`, `LEARNING`, `OTHER`).
- **Strict User Ownership & IDOR Protection**: All queries filter by authenticated `user_id`. Requests for projects owned by other users return `404 Not Found`.
- **Server-Managed Timestamps**: `completedAt` timestamp is set automatically on transition to `COMPLETED` and preserved through logical archival (`ARCHIVED`).
- **Logical Archival**: Logical delete (`DELETE /api/v1/projects/{id}`) transitions status to `ARCHIVED` without physical database deletion.
- **Gateway Routing**: Exposed via API Gateway path predicate `/api/v1/projects/**`.

## Consequences
### Positive
- Unified user productivity bounded context inside `user-service`.
- Strong consistency for project operations.
- Clean database migration path (`V7__create_developer_projects.sql`).
- Zero extra microservice overhead.

### Tradeoffs
- `user-service` continues growing; if team collaboration or project sharing is introduced in future product tiers, project domain decomposition into a separate microservice may be evaluated.
