# 69. Main Dashboard Implementation & Frontend Architecture

Date: 2026-09-05

## Status

Accepted

## Context

Lesson 73 requires the implementation of the main **DevSphere Dashboard UI** at `/dashboard`. The dashboard must deliver a clean, modern SaaS experience for authenticated developers while strictly reusing existing backend microservice REST APIs without inventing fake data or unrequested backend features.

## Decision

1. **Frontend-Driven Metric Aggregation**:
   - Instead of creating synthetic backend summary endpoints, the frontend fetches task records via `taskService.getTasks()` and dynamically computes task metrics (`total`, `pending`, `completed`, `overdue`, `completionPercentage`).
   - Handles `0` total tasks gracefully without generating `NaN%`.

2. **Derived Activity Stream**:
   - In the absence of a dedicated `/api/v1/activities` endpoint, recent activity is derived from task mutation timestamps (`createdAt`, `updatedAt`, `completedAt`). An empty state ("No recent activity") is rendered when no task activity exists.

3. **Modular Component Architecture**:
   - Structured the dashboard into modular components (`WelcomeSection`, `StatsGrid`, `StatCard`, `CompletionOverview`, `UpcomingTasks`, `RecentActivity`, `QuickActions`, `UserSummary`, `DashboardSkeleton`).

4. **Robust UX & Fallbacks**:
   - Implemented explicit loading skeleton states (`DashboardSkeleton`), retryable error boundaries (`ErrorState`), and empty states (`EmptyState`) for zero-task scenarios.

## Consequences

### Positive
- Fully compatible with existing Spring Boot `user-service` and `api-gateway` microservices.
- Zero extra backend overhead or unnecessary database queries.
- Clean component separation simplifies future module integrations (e.g. Resume Compiler, Observability).

### Negative
- Task statistics calculation is bounded by the size parameter of the fetched task list. For high-volume enterprise scenarios, dedicated aggregation endpoints can be added in future iterations.
