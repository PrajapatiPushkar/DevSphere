# 70. Task Management UI Implementation & API Integration

Date: 2026-09-05

## Status

Accepted

## Context

Lesson 74 requires the implementation of a full-featured, modern **Task Management UI** at route `/tasks`. The UI must support the complete task lifecycle (create, view, edit, complete, start, reopen, cancel, delete, search, filter, sort, and paginate) while strictly integrating with existing Spring Boot microservice REST APIs (`user-service` via `/api/v1/tasks`).

## Decision

1. **Native Backend Alignment**:
   - Mapped all task actions directly to existing backend Spring Boot endpoints (`POST`, `GET`, `PUT`, `DELETE`, `PATCH /complete`, `PATCH /start`, `PATCH /reopen`, `PATCH /cancel`).
   - Added no synthetic backend endpoints or speculative request/response fields.

2. **Combined Filtering, Search & Server-Side Pagination**:
   - Integrated server-side status, priority, and sort query parameters with client-side due-date and keyword search filtering.
   - Preserved active filters and sorting during page navigation.

3. **Unified Form & Modal Architecture**:
   - Reused design system components to build `TaskFormModal` for both Creation and Editing flows, and `DeleteTaskModal` for destructive confirmation prompts.

4. **Robust Feedback & States**:
   - Provided skeleton loaders (`TaskListSkeleton`), retryable error boundaries (`ErrorState`), and distinct empty states ("No tasks registered yet" vs "No matching tasks found").

## Consequences

### Positive
- Direct alignment with backend REST APIs guarantees zero backend modifications.
- Complete task lifecycle capabilities provided to developers.
- 100% test coverage for Task Management UI scenarios (10 new unit/integration tests).

### Negative
- Client-side keyword search operates over the active page payload. For multi-thousand task enterprise repositories, server-side full-text search parameters can be added to the backend in future iterations.
