# ADR 0027: Daily Planner and Task Scheduling Domain

- **Status**: Accepted
- **Date**: 2026-08-26
- **Authors**: DevSphere Core Engineering Team

---

## Context

Following the introduction of Developer Profiles, Goals, and Tasks in Lessons 26 and 27, DevSphere requires a Daily Planner capability so users can organize and schedule tasks into daily execution plans with dates, time slots, ordering, and duration estimates.

## Decision

1. **Domain Ownership in User Service**:
   - Daily Planner is incorporated into `services/user-service` as part of the personal productivity domain. No separate `planner-service` microservice is created.

2. **Task Reference & Data Non-Duplication**:
   - `PlannerEntry` references `taskId` without duplicating title, description, priority, or status.
   - `Task` remains the single source of truth for task state and content.

3. **Schema Evolution & Persistence**:
   - Created Flyway migration `V5__create_planner_entries.sql` introducing `planner_entries` table.
   - Enforced database unique constraint `UNIQUE(user_id, task_id, planned_date)` to prevent duplicate task scheduling for the same date.
   - Added indexes on `(user_id, planned_date)`, `(user_id, task_id)`, and `(user_id, planned_date, sort_order)`.

4. **Time Model**:
   - `planned_date` is stored as an unzoned `DATE` (local calendar date).
   - `start_time` and `end_time` are stored as local `TIME` values.
   - `created_at` and `updated_at` timestamps use standard UTC timestamps.

5. **Dynamic Summaries & Non-Persisted Metrics**:
   - Total entries, completed entries, pending entries, total planned minutes, and completion percentages are calculated dynamically at query time rather than stored in the database.

6. **IDOR Protection & Ownership Validation**:
   - All operations validate user ownership (`findByIdAndUserId`). Accessing, updating, or reordering non-owned entries/tasks yields `404 Not Found`.

7. **API Gateway Routing**:
   - `/api/v1/planner/**` routes are configured under `user-service-route` in API Gateway with distributed rate limiting and OpenTelemetry tracing.

## Consequences

- Clean separation between task definition (`Task`) and scheduling metadata (`PlannerEntry`).
- Strict user-level resource isolation and zero duplicate state.
- Flexible daily productivity workflow foundation for future DSA, project, and career execution modules.
