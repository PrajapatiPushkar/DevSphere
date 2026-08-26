# DSA Progress Tracking Architecture

## 1. Overview & Purpose

The **DSA Progress Tracking** domain provides a specialized learning and problem-solving execution record for software engineers within DevSphere. It allows developers to log Data Structures and Algorithms problems, classify them by coding platform, difficulty level, and topic, record attempt counts and time spent, transition problem states (`TODO`, `IN_PROGRESS`, `SOLVED`, `REVISIT`, `ARCHIVED`), and view dynamic daily progress and aggregate statistics.

---

## 2. Bounded Context & Domain Ownership

DSA Progress Tracking is embedded inside `services/user-service` as part of the developer productivity and career growth bounded context.

```
User Service
├── Developer Profile
├── Goals
├── Tasks
├── Daily Planner
└── DSA Progress Tracking
```

*Architectural Principle*: DSA tracking represents personal developer-growth data owned by the authenticated user (`userId`). No standalone `dsa-service` microservice or public global problem catalog is created at this phase.

---

## 3. Data Model (`DsaProblem`)

Persistence model stored in `dsa_problems` table:

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique problem ID |
| `user_id` | `BIGINT` | `NOT NULL` | Owner user ID extracted from JWT |
| `task_id` | `BIGINT` | `NULLABLE` | Optional foreign reference to `tasks` |
| `goal_id` | `BIGINT` | `NULLABLE` | Optional foreign reference to `goals` |
| `title` | `VARCHAR(255)` | `NOT NULL` | Problem title |
| `description` | `TEXT` | `NULLABLE` | Problem description or statement snippet |
| `platform` | `VARCHAR(50)` | `NOT NULL` | Platform enum (`LEETCODE`, `CODEFORCES`, etc.) |
| `problem_url` | `VARCHAR(512)` | `NULLABLE` | External problem URL |
| `difficulty` | `VARCHAR(30)` | `NOT NULL` | Difficulty enum (`EASY`, `MEDIUM`, `HARD`) |
| `topic` | `VARCHAR(50)` | `NOT NULL` | Topic enum (`ARRAY`, `DYNAMIC_PROGRAMMING`, etc.) |
| `status` | `VARCHAR(30)` | `NOT NULL` | Status enum (`TODO`, `IN_PROGRESS`, `SOLVED`, `REVISIT`, `ARCHIVED`) |
| `solved_at` | `TIMESTAMP` | `NULLABLE` | Timestamp when problem was solved |
| `time_spent_minutes` | `INT` | `NULLABLE` | Time spent solving problem in minutes |
| `attempt_count` | `INT` | `NOT NULL DEFAULT 0` | Incrementing attempt counter |
| `notes` | `TEXT` | `NULLABLE` | User solution notes, approach, complexity |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Audit timestamp (UTC) |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Audit timestamp (UTC) |

---

## 4. State Lifecycle & Transitions

```
[TODO] ───► [IN_PROGRESS] ───► [SOLVED] ───► [REVISIT]
  │               │              │               │
  └───────────────┴──────────────┴───────────────┴──► [ARCHIVED]
```

State rules:
- **`TODO -> IN_PROGRESS`**: Endpoint `PATCH /api/v1/dsa/problems/{id}/start`.
- **`IN_PROGRESS / TODO / REVISIT -> SOLVED`**: Endpoint `PATCH /api/v1/dsa/problems/{id}/solve`. Sets `solvedAt = Instant.now()`.
- **`SOLVED -> REVISIT`**: Endpoint `PATCH /api/v1/dsa/problems/{id}/revisit`. Preserves existing `solvedAt` for historical progress tracking.
- **`REVISIT -> SOLVED`**: Endpoint `PATCH /api/v1/dsa/problems/{id}/solve`. Updates `solvedAt = Instant.now()`.
- **`Logical Archival`**: Endpoint `DELETE /api/v1/dsa/problems/{id}` updates `status = ARCHIVED` (returns HTTP `204 No Content`). Direct queries for archived records return `404 Not Found`.

---

## 5. Attempt Tracking & Time Spent

- `attemptCount`: Initialized to `0`. Incremented via `POST /api/v1/dsa/problems/{id}/attempt`. Cannot be manipulated directly through general update endpoints.
- `timeSpentMinutes`: Maintained as non-negative integer tracking cumulative solving duration in minutes.

---

## 6. Daily Progress & Aggregate Statistics

Dynamic query-time metrics (non-persisted counters):
- **Daily Progress (`GET /api/v1/dsa/progress/daily?date={date}`)**: Calculates `problemsSolved` on target date, `totalAttempts`, and `timeSpentMinutes`.
- **Statistics (`GET /api/v1/dsa/statistics`)**: Aggregates `totalProblems`, `solvedProblems`, `inProgressProblems`, `revisitProblems`, difficulty breakdown (`easySolved`, `mediumSolved`, `hardSolved`), `totalTimeSpentMinutes`, and `totalAttempts`.

---

## 7. Security & IDOR Isolation

- Identity (`userId`) is strictly derived from verified JWT `UserPrincipal`.
- Attempts to query, update, archive, start, solve, revisit, or increment attempts for non-owned DSA problems, tasks, or goals yield HTTP `404 Not Found` without information leakage.

---

## 8. Database Indexing & Performance

Flyway migration `V6__create_dsa_problems.sql` adds single and composite indexes:
- `idx_dsa_user_id` on `(user_id)`
- `idx_dsa_user_status` on `(user_id, status)`
- `idx_dsa_user_difficulty` on `(user_id, difficulty)`
- `idx_dsa_user_topic` on `(user_id, topic)`
- `idx_dsa_user_platform` on `(user_id, platform)`
- `idx_dsa_user_solved_at` on `(user_id, solved_at)`

---

## 9. Observability

Low-cardinality Micrometer metrics:
- `devsphere_dsa_problems_created_total{difficulty="...", platform="..."}`
- `devsphere_dsa_problems_solved_total{difficulty="...", platform="..."}`
- `devsphere_dsa_problems_revisited_total{difficulty="...", platform="..."}`
- `devsphere_dsa_attempts_total{difficulty="...", platform="..."}`

---

## 10. Future Integration

Future phases may expand DSA progress tracking with automated platform sync (LeetCode API webhooks), streak calculations, topic mastery analytics, and spaced-repetition revision schedules.
