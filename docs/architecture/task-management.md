# Architecture Specification — Task Management Domain

## 1. Domain Overview & Bounded Context

The **User Service** (`services/user-service`) owns the **Task Management** product domain alongside Developer Profiles and Goal Management.

```
+-------------------------------------------------------------------------+
|                              User Service                               |
|                         (services/user-service)                         |
+-------------------------------------------------------------------------+
|  +-------------------+   +--------------------+   +------------------+  |
|  | Developer Profile |   |  Goal Management   |   | Task Management  |  |
|  |  (user_profiles)  |   |      (goals)       |   |     (tasks)      |  |
|  +-------------------+   +--------------------+   +------------------+  |
|                                                            |            |
|                                                   optional goal_id      |
|                                                   (User Scoped Check)   |
|                                                            v            |
|                                                   +------------------+  |
|                                                   |  User's Goal A   |  |
|                                                   +------------------+  |
+-------------------------------------------------------------------------+
```

- **Independent Actionable Work**: Tasks represent actionable units of work (e.g. "Solve 5 binary tree problems", "Implement Kafka consumer retry") that function independently of goals.
- **Optional Goal Linking**: Tasks may optionally reference a `goalId`. When assigned, the system verifies that `goalId` belongs to the authenticated user.
- **Identity Scoping**: Identity is strictly derived from JWT (`UserPrincipal` or validated `X-Authenticated-User-Id` header).

---

## 2. Task Data Model & Schema (`V4__create_tasks.sql`)

```sql
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    due_date TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_user_status ON tasks(user_id, status);
CREATE INDEX idx_tasks_user_priority ON tasks(user_id, priority);
CREATE INDEX idx_tasks_user_goal_id ON tasks(user_id, goal_id);
CREATE INDEX idx_tasks_user_due_date ON tasks(user_id, due_date);
```

### Domain Enums
- **`TaskStatus`**: `TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`
- **`TaskPriority`**: `LOW`, `MEDIUM`, `HIGH`, `URGENT`

All enums are stored as strings (`EnumType.STRING`).

---

## 3. Status Transition Matrix

State transitions are enforced deterministically via dedicated endpoints:

| Current Status | Allowed Target Status | Transition Endpoint | Triggers |
|---|---|---|---|
| `TODO` | `IN_PROGRESS` | `PATCH /api/v1/tasks/{id}/start` | Status -> `IN_PROGRESS` |
| `TODO` / `IN_PROGRESS` | `COMPLETED` | `PATCH /api/v1/tasks/{id}/complete` | Status -> `COMPLETED`, `completedAt = Instant.now()`, metric increment |
| `TODO` / `IN_PROGRESS` | `CANCELLED` | `PATCH /api/v1/tasks/{id}/cancel` | Status -> `CANCELLED`, metric increment |
| `COMPLETED` / `CANCELLED` / `ARCHIVED` | `TODO` | `PATCH /api/v1/tasks/{id}/reopen` | Status -> `TODO`, `completedAt = null`, metric increment |
| *Any* | `ARCHIVED` | `DELETE /api/v1/tasks/{id}` | Status -> `ARCHIVED`, `204 No Content` (Logical Archival) |

Direct status alteration via `PUT /api/v1/tasks/{id}` is prohibited (editable fields only: `title`, `description`, `priority`, `dueDate`, `goalId`).

---

## 4. IDOR Protection & Goal Ownership Rules

- **Resource Ownership**: All task operations execute `findByIdAndUserId(taskId, userId)`. Unowned access attempts return HTTP `404 Not Found` without revealing resource existence.
- **Goal Linkage Validation**: Creating or updating a task with `goalId` checks `goalRepository.findByIdAndUserId(goalId, userId)`. Attempting to link another user's goal returns HTTP `404 Not Found`.

---

## 5. Pagination, Sorting & Dynamic Overdue Computation

- **Pagination**: Database-level pagination (`Pageable`). Default page size: 20. Max size: 100.
- **Sorting**: Default ordering places tasks with due dates first (`dueDate ASC`), followed by tasks without due dates, ordered by creation timestamp (`createdAt DESC`).
- **Dynamic Overdue Computation**: `overdue = dueDate != null && dueDate < Instant.now() && status not in (COMPLETED, CANCELLED, ARCHIVED)`. `overdue` is computed dynamically in `TaskResponse` without database field persistence.

---

## 6. Endpoints Summary

| Method | Path | Description | Response |
|---|---|---|---|
| `POST` | `/api/v1/tasks` | Create task (`TODO` initial status) | `201 Created` (`TaskResponse`) |
| `GET` | `/api/v1/tasks` | List user tasks (filters: `status`, `priority`, `goalId`, `page`, `size`) | `200 OK` (`PageResponse<TaskResponse>`) |
| `GET` | `/api/v1/tasks/{id}` | Get task details | `200 OK` / `404 Not Found` |
| `PUT` | `/api/v1/tasks/{id}` | Update task details (title, description, priority, dueDate, goalId) | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/v1/tasks/{id}` | Logically archive task (`status = ARCHIVED`) | `204 No Content` / `404 Not Found` |
| `PATCH` | `/api/v1/tasks/{id}/start` | Transition to `IN_PROGRESS` | `200 OK` / `400 Bad Request` |
| `PATCH` | `/api/v1/tasks/{id}/complete` | Transition to `COMPLETED` | `200 OK` / `400 Bad Request` |
| `PATCH` | `/api/v1/tasks/{id}/reopen` | Transition to `TODO` | `200 OK` |
| `PATCH` | `/api/v1/tasks/{id}/cancel` | Transition to `CANCELLED` | `200 OK` / `400 Bad Request` |
