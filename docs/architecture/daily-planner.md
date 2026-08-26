# Daily Planner and Task Scheduling Architecture

## 1. Overview & Purpose

The **Daily Planner** capability provides a personal productivity view that allows developers to schedule and organize tasks for specific calendar dates. It acts as an execution layer over the existing Task management domain, enabling developers to structure their daily workflow with start/end time slots, duration estimates, and sort ordering.

---

## 2. Domain Ownership & Bounded Context

Daily Planner belongs to the **productivity bounded context** within `user-service` alongside Developer Profiles, Goals, and Tasks.

```
User Service
├── Developer Profile
├── Goals
├── Tasks
└── Daily Planner
```

*Architectural Principle*: Daily Planner does not duplicate Task data (such as title, description, priority, or completion status). A separate microservice (`planner-service`) is intentionally avoided at this phase to maintain low operational overhead and avoid data duplication.

---

## 3. Data Model (`PlannerEntry`)

Persistence model stored in `planner_entries` table:

| Field | Type | Constraint | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique entry identifier |
| `user_id` | `BIGINT` | `NOT NULL` | Owner user ID extracted from JWT |
| `task_id` | `BIGINT` | `NOT NULL` | Foreign key referencing task in `tasks` |
| `planned_date` | `DATE` | `NOT NULL` | Target user local calendar date |
| `start_time` | `TIME` | `NULLABLE` | Optional local start time |
| `end_time` | `TIME` | `NULLABLE` | Optional local end time |
| `sort_order` | `INT` | `NOT NULL` | Zero or positive order index |
| `planned_minutes` | `INT` | `NULLABLE` | Estimated duration in minutes (1–1440) |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Audit timestamp (UTC) |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Audit timestamp (UTC) |

Unique Constraint: `UNIQUE(user_id, task_id, planned_date)` prevents duplicate scheduling of the same task on the same date for a single user.

---

## 4. Relationships

### Task Relationship
- `Task` is the single source of truth for task title, description, status (`TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`), and priority.
- `PlannerEntry` only maintains scheduling metadata (`planned_date`, `start_time`, `end_time`, `sort_order`, `planned_minutes`).
- `ARCHIVED` or `CANCELLED` tasks cannot be newly scheduled into planner entries.
- If a task is completed, its status updates in `Task`, and the `PlannerEntry` remains intact for historical planning tracking.

### Goal Relationship
- Tasks optionally link to `Goal`. When daily planner items are queried, the optional `goalId` is surfaced through the task projection.

---

## 5. Date & Time Handling

- `planned_date`: Persisted as ISO `DATE` representing the user's intended local calendar day (e.g. `2026-08-26`). No server timezone offset conversion is performed on `planned_date`.
- `start_time` / `end_time`: Persisted as ISO `TIME` representing user local time slots.
- `created_at` / `updated_at`: Stored as UTC `TIMESTAMP` for standard audit logging.

---

## 6. Daily Summary & Dynamic Metrics

The `/api/v1/planner/days/{date}` and `/api/v1/planner/today` APIs return dynamic, unpersisted daily summary calculations:

- `totalEntries`: Total planner entries for the day.
- `completedEntries`: Count of entries whose referenced task has status `COMPLETED`.
- `pendingEntries`: `totalEntries - completedEntries`.
- `totalPlannedMinutes`: Sum of non-null `planned_minutes`.
- `completionPercentage`: Dynamically calculated as `(completedEntries / totalEntries) * 100.0` (0.0 if `totalEntries == 0`).

---

## 7. Ordering, Rescheduling & Unscheduling

- **Ordering**: Entries for a day are fetched with deterministic ordering: `sortOrder ASC`, `startTime ASC NULLS LAST`, `createdAt ASC`.
- **Reordering**: `PATCH /api/v1/planner/days/{date}/reorder` updates `sortOrder` for daily entries in a single atomic transaction. All entries in the request must belong to the current user and target date.
- **Rescheduling**: `PATCH /api/v1/planner/entries/{id}/reschedule` moves an entry to a new date and optional time slot.
- **Unscheduling**: `DELETE /api/v1/planner/entries/{id}` removes the `PlannerEntry` record. The underlying `Task` is preserved without status changes.

---

## 8. Security & IDOR Protection

- User identity (`userId`) is strictly extracted from verified JWT principal (`UserPrincipal`).
- The client cannot supply or manipulate `userId`.
- Attempting to access, modify, reschedule, or reorder another user's planner entry or task returns HTTP `404 Not Found` without information leakage.

---

## 9. Transaction Boundaries & Concurrency

- Entry creation, update, reschedule, and deletion occur within single `@Transactional` boundaries.
- Reordering a day's entries executes in a single transaction that validates all entry IDs prior to persisting updates.
- Duplicate scheduling protection is enforced via the database unique index `(user_id, task_id, planned_date)`.

---

## 10. Observability

Low-cardinality Micrometer metrics:
- `devsphere_planner_entries_created_total`
- `devsphere_planner_entries_deleted_total`
- `devsphere_planner_entries_rescheduled_total`
- `devsphere_planner_entries_reordered_total`

---

## 11. Future Integration

When product scope expands (DSA practice, Project milestones, Job applications), scheduling entries can link directly to corresponding domain entities while sharing the same daily planning paradigm.
