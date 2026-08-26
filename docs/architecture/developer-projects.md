# Developer Project Management Architecture

## Overview
The Developer Project Management domain enables software developers using DevSphere to track personal projects across their full development lifecycle.

This domain is built within `services/user-service` and provides project planning, status management, tech stack tracking, URL references (repository, live site, documentation), target date tracking, and logical project archival.

## Domain Model
```
DeveloperProject
├── id: Long (PK)
├── userId: Long (NOT NULL)
├── name: String (NOT NULL, max 255)
├── description: String (TEXT, optional)
├── status: ProjectStatus (STRING, NOT NULL) [PLANNED, IN_PROGRESS, COMPLETED, ON_HOLD, ARCHIVED]
├── projectType: ProjectType (STRING, NOT NULL) [PERSONAL, COLLEGE, PROFESSIONAL, OPEN_SOURCE, FREELANCE, LEARNING, OTHER]
├── repositoryUrl: String (optional, max 512)
├── liveUrl: String (optional, max 512)
├── documentationUrl: String (optional, max 512)
├── techStack: List<String> (JSON string in database TEXT column)
├── startDate: LocalDate (optional)
├── targetEndDate: LocalDate (optional)
├── completedAt: Instant (optional, server managed)
├── createdAt: Instant (NOT NULL, server managed)
└── updatedAt: Instant (NOT NULL, server managed)
```

## Lifecycle & State Transitions
Projects follow a controlled state machine:

```
 PLANNED ───► IN_PROGRESS ───► COMPLETED
    │              │                │
    ├──────────────┼────────────────┤
    ▼              ▼                ▼
 ON_HOLD ────► ARCHIVED ◄───────────┘
    │              ▲
    └──────────────┘
```

### Transition Rules
- `PLANNED` -> `IN_PROGRESS`, `ON_HOLD`, `ARCHIVED`
- `IN_PROGRESS` -> `COMPLETED`, `ON_HOLD`, `ARCHIVED`
- `ON_HOLD` -> `IN_PROGRESS`, `ARCHIVED`
- `COMPLETED` -> `ARCHIVED`
- `ARCHIVED` -> Terminal state. No transitions allowed out of `ARCHIVED`.

## API Endpoints
All endpoints are exposed through API Gateway under `/api/v1/projects/**`.

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `POST` | `/api/v1/projects` | Create a new developer project (initial status: `PLANNED`) | `201 Created` |
| `GET` | `/api/v1/projects` | List projects (supports `status`, `projectType`, `page`, `size`) | `200 OK` |
| `GET` | `/api/v1/projects/{id}` | Get project by ID | `200 OK` |
| `PUT` | `/api/v1/projects/{id}` | Update project metadata and dates | `200 OK` |
| `PATCH` | `/api/v1/projects/{id}/start` | Transition status to `IN_PROGRESS` | `200 OK` |
| `PATCH` | `/api/v1/projects/{id}/complete` | Transition status to `COMPLETED` (`completedAt` set) | `200 OK` |
| `PATCH` | `/api/v1/projects/{id}/hold` | Transition status to `ON_HOLD` | `200 OK` |
| `PATCH` | `/api/v1/projects/{id}/resume` | Transition status from `ON_HOLD` to `IN_PROGRESS` | `200 OK` |
| `DELETE` | `/api/v1/projects/{id}` | Logically archive project (sets status to `ARCHIVED`) | `204 No Content` |

## Ownership & IDOR Protection
- Authenticated user identity (`userId`) is extracted from `SecurityContextHolder` or `X-Authenticated-User-Id` header.
- Every repository call filters by `userId` (e.g. `findByIdAndUserId(projectId, userId)`).
- Attempts to query or mutate projects owned by another user return `404 Not Found`.

## Filtering & Pagination
- Default GET `/api/v1/projects` excludes `ARCHIVED` projects unless explicitly requested (`status=ARCHIVED`).
- Supports combined filters: `status` + `projectType`.
- Max page size capped at 100 at the service layer.
- Sorted by `createdAt DESC`.

## Database Schema & Indexes
Flyway migration: `V7__create_developer_projects.sql`
Indexes:
- `idx_dev_projects_user_id (user_id)`
- `idx_dev_projects_user_status (user_id, status)`
- `idx_dev_projects_user_type (user_id, project_type)`

## Observability
Low-cardinality Micrometer metrics:
- `devsphere_projects_created_total{project_type}`
- `devsphere_projects_completed_total{project_type}`
- `devsphere_projects_archived_total{project_type}`

## Future Expansion
- Optional linking between tasks/goals and projects.
- GitHub repository sync metadata.
- Automated developer portfolio generation.
