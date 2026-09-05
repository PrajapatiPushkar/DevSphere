# Frontend Main Dashboard Architecture

## 1. Overview
The DevSphere Main Dashboard (`/dashboard`) provides developers with a modern SaaS workspace overview. It integrates directly with existing microservice backend endpoints (`user-service` REST APIs at `/api/v1/tasks` and `/api/v1/users/me`), calculating real-time task statistics, task completion overview, upcoming due tasks, recent activity streams, quick action shortcuts, and user profile summaries.

## 2. Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                            React Frontend                              │
│                                                                        │
│  [ AuthContext ] ──> user data                                         │
│                                                                        │
│  [ DashboardPage ]                                                     │
│        │                                                               │
│        ├── [ taskService ] ──(HTTP GET /api/v1/tasks)──┐               │
│        │                                               │               │
│        ├── Calculate DashboardStats                    │               │
│        │    ├── Total, Pending, Completed, Overdue     │               │
│        │    └── Completion Percentage (0-100%)          │               │
│        │                                               │               │
│        ├── [ WelcomeSection ]                          │               │
│        ├── [ StatsGrid ]                               │               │
│        ├── [ QuickActions ]                            │               │
│        ├── [ CompletionOverview ]                      │               │
│        ├── [ UpcomingTasks ]                           │               │
│        ├── [ RecentActivity ]                          │               │
│        └── [ UserSummary ]                             │               │
└────────────────────────────────────────────────────────┴───────────────┘
                                                         │
                                               [ API Gateway ]
                                                         │
                                               [ user-service ]
```

## 3. Component Hierarchy
- `DashboardPage`: Top-level page container managing state (`tasks`, `isLoading`, `isError`, `isModalOpen`), data fetching, and statistics memoization.
- `WelcomeSection`: Personalized developer greeting using authenticated `useAuth()` state.
- `StatsGrid`: 4 key metric cards (Total, Pending, Completed, Overdue tasks) styled with design system tokens and HSL accent colors.
- `StatCard`: Reusable metric summary card.
- `CompletionOverview`: Goal execution progress bar showing completion ratio and percentage (handles 0 total tasks safely without `NaN%`).
- `RecentActivity`: Derived timeline of task creations and completions with relative time formatting, displaying an empty state when no activity is available.
- `UpcomingTasks`: Prioritized list of incomplete tasks sorted by nearest due date.
- `QuickActions`: High-priority shortcuts (+ New Task, View Tasks, Profile Settings).
- `UserSummary`: Authenticated user profile card showing fallback avatar initials, name, email, and current role.
- `DashboardSkeleton`: Loading placeholder layout matching dashboard structure.

## 4. API Integration
- `taskService.getTasks(params)`: Calls `GET /api/v1/tasks` returning `PageResponse<Task>`.
- `taskService.createTask(data)`: Calls `POST /api/v1/tasks` with `CreateTaskInput`.
- `taskService.completeTask(id)`: Calls `PATCH /api/v1/tasks/{id}/complete`.
- `authService.getCurrentUser()`: Calls `GET /api/v1/users/me`.

## 5. Responsive Design Strategy
- Desktop (`≥ 1024px`): Sidebar navigation + 3-column dashboard grid.
- Tablet (`≥ 640px`): 2-column metric cards and stacked layout.
- Mobile (`< 640px`): Single-column stacked cards with horizontal scroll safety.

## 6. Accessibility & Performance
- Built with semantic HTML elements (`<header>`, `<main>`, `<section>`, `<nav>`, `<button>`).
- Direct single-fetch task loading avoids duplicate network requests.
- Lightweight UI using Lucide React SVG icons and CSS transitions.
