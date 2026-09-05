# Frontend Task Management UI Architecture

## 1. Overview
The DevSphere Task Management UI (`/tasks`) enables authenticated developers to manage microservice tasks across their lifecycle. It integrates directly with existing Spring Boot microservice REST APIs (`user-service` via `/api/v1/tasks`), providing comprehensive searching, filtering, sorting, task creation, editing, status transitions (Start, Complete, Reopen, Cancel), deletion, pagination, and fallback states.

## 2. Architecture & Data Flow

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                               React Frontend                                  │
│                                                                               │
│  [ TasksPage ] (/tasks protected route)                                      │
│        │                                                                      │
│        ├── [ taskService ] ──(HTTP REST APIs)──┐                             │
│        │                                       │                              │
│        ├── State:                              │                              │
│        │    ├── tasks, page, totalPages, totalElements                        │
│        │    ├── filters (search, status, priority, dueDateFilter, sort)      │
│        │    └── modals (isFormModalOpen, taskToEdit, isDeleteModalOpen)       │
│        │                                       │                              │
│        ├── [ TaskFilters ] (Toolbar: search, status, priority, sort)          │
│        ├── [ TaskTable ] (Desktop table & mobile card views)                  │
│        ├── [ TaskFormModal ] (Create & Edit task modal)                       │
│        ├── [ DeleteTaskModal ] (Destructive confirmation modal)              │
│        └── [ TaskListSkeleton ] (Loading placeholder)                        │
└────────────────────────────────────────────────┬──────────────────────────────┘
                                                 │
                                       [ API Gateway :8080 ]
                                                 │
                                       [ user-service :8082 ]
                                         - POST   /api/v1/tasks
                                         - GET    /api/v1/tasks
                                         - PUT    /api/v1/tasks/{id}
                                         - DELETE /api/v1/tasks/{id}
                                         - PATCH  /api/v1/tasks/{id}/complete
                                         - PATCH  /api/v1/tasks/{id}/start
                                         - PATCH  /api/v1/tasks/{id}/reopen
                                         - PATCH  /api/v1/tasks/{id}/cancel
```

## 3. Component Hierarchy
- `TasksPage`: Top-level page component orchestrating state, API triggers, filters, pagination, and modal dialogs.
- `TaskFilters`: Filter bar combining debounced keyword search input, Status dropdown filter (`TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `ARCHIVED`), Priority filter (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), Due Date filter (`OVERDUE`, `DUE_TODAY`, `UPCOMING`), Sort selector (`createdAt,desc`, `dueDate,asc`, etc.), and a "Clear Filters" button.
- `TaskTable`: Dual-layout table component rendering full desktop table with Action dropdown menus and responsive mobile card layout.
- `TaskFormModal`: Unified modal form for Creating and Editing task items with client-side validation, submit spinner, error handling, and toast feedback.
- `DeleteTaskModal`: Destructive operation confirmation modal for deleting/archiving tasks with task summary details.
- `TaskListSkeleton`: Animated skeleton loader for task list data fetching.

## 4. Task Lifecycle Operations & API Integration
- **Create**: `POST /api/v1/tasks` (`CreateTaskInput`: `title`, `description`, `priority`, `dueDate`, `goalId`).
- **Edit / Update**: `PUT /api/v1/tasks/{id}` (`UpdateTaskInput`: `title`, `description`, `priority`, `dueDate`, `goalId`).
- **Start**: `PATCH /api/v1/tasks/{id}/start` (Transitions status to `IN_PROGRESS`).
- **Complete**: `PATCH /api/v1/tasks/{id}/complete` (Transitions status to `COMPLETED` and sets `completedAt`).
- **Reopen**: `PATCH /api/v1/tasks/{id}/reopen` (Transitions status back to `TODO`).
- **Cancel**: `PATCH /api/v1/tasks/{id}/cancel` (Transitions status to `CANCELLED`).
- **Delete / Archive**: `DELETE /api/v1/tasks/{id}` (Archives/deletes task from system).

## 5. Filter, Search & Pagination Strategy
- **Search**: Client-side filtering over title and description fields without resetting active filters.
- **Filters**: Server-side status & priority filtering combined with client-side due date filters (`OVERDUE`, `DUE_TODAY`, `UPCOMING`).
- **Sorting**: Server-side `sort` parameter (`field,direction`).
- **Pagination**: Uses `PageResponse<Task>` pagination attributes (`pageNumber`, `pageSize`, `totalElements`, `totalPages`, `first`, `last`).

## 6. Responsive & Accessibility Strategy
- Responsive grid & flex layouts adapt seamlessly between desktop table and mobile cards.
- HTML labels associated with form controls via `id` and `htmlFor` attributes.
- Keyboard navigation (Escape key closes modals) and visible focus rings conforming to WCAG 2.1 AA guidelines.
