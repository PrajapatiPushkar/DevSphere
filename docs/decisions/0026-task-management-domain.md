# ADR 0026: Task Management Domain Architecture

- **Status**: Accepted
- **Date**: 2026-08-26
- **Authors**: DevSphere Core Engineering Team

---

## Context

Following the introduction of Developer Profiles and Goals in Lesson 26, DevSphere requires an execution layer for software developers to manage actionable units of work (tasks). Tasks must function independently or optionally link to goals.

## Decision

1. **Domain Ownership in User Service**:
   - `services/user-service` is designated as the domain owner for Tasks alongside Profiles and Goals. No dedicated task microservice is created at this phase.

2. **Schema Evolution & Persistence**:
   - Created Flyway migration `V4__create_tasks.sql` adding `tasks` table with indexes for `user_id`, `status`, `priority`, `goal_id`, and `due_date`.
   - Enums (`TaskStatus`: `TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`; `TaskPriority`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`) are persisted as strings.

3. **Deterministic State Transitions**:
   - Status changes are restricted to dedicated endpoints (`/start`, `/complete`, `/reopen`, `/cancel`). Direct status updates via `PUT /api/v1/tasks/{id}` are prohibited to prevent illegal state bypasses.

4. **Logical Archival**:
   - `DELETE /api/v1/tasks/{id}` executes logical archival (`status = ARCHIVED`, returns `204 No Content`). Physical deletion of historical records is avoided.

5. **IDOR Protection & Goal Verification**:
   - All database queries enforce user scoping (`findByIdAndUserId`). Access attempts on non-owned tasks or linking non-owned goals return HTTP `404 Not Found` without information disclosure.

6. **Dynamic Overdue & Pagination**:
   - `overdue` status is computed dynamically in `TaskResponse` without database persistence.
   - Dynamic user-scoped filtering (`status`, `priority`, `goalId`) and pagination (max page size 100) are performed at database level.

7. **API Gateway Routing**:
   - `/api/v1/tasks/**` routes are configured in API Gateway under `user-service-route` preserving rate limiting and tracing.

## Consequences

- Direct, transactionally consistent integration between developer tasks and goals.
- Strict resource isolation preventing IDOR vulnerabilities.
- Zero extra operational overhead from avoiding premature microservice split.
