# Frontend Notifications & Activity Architecture

This document describes the architectural implementation of **Lesson 76 — Notifications & Activity** in DevSphere.

---

## 1. Overview

Lesson 76 adds an interactive **Notification Center** popover in the global Header and a dedicated **Activity Timeline** (`/activity`) feed consuming real task and profile entity data without adding non-existent microservices or synthetic backend APIs.

---

## 2. Component Structure & Data Flow

```
[ User Browser ]
       │
       ├──> Header (NotificationBell)
       │        └──> NotificationPanel Popover
       │                ├──> NotificationItem (Overdue, Completed, Created, Profile)
       │                ├──> Filter Tabs ("All", "Unread")
       │                └──> Actions ("Mark as read", "Mark all as read")
       │
       └──> ActivityPage (/activity)
                └──> ActivityTimeline
                        ├──> ActivityItem (Chronological event node + timeline line)
                        └──> Filter Controls ("All", "Tasks", "Profile", Search query)
```

---

## 3. Real Event Derivation & Data Sources

| Event Category | Real Data Source Field | Derived Notification / Activity Item | Target Navigation |
| :--- | :--- | :--- | :--- |
| **Task Overdue** | `task.overdue \|\| (task.dueDate < now && status != COMPLETED)` | "Task Overdue Notice: Task X is overdue" | `/tasks` |
| **Task Completed** | `task.status === 'COMPLETED'` & `task.completedAt` | "Task Completed: Task X was completed" | `/tasks` |
| **Task Created** | `task.createdAt` | "Task Created: Task X was created" | `/tasks` |
| **Task Updated** | `task.updatedAt > task.createdAt` & `status != COMPLETED` | "Task Updated: Task X was updated" | `/tasks` |
| **Profile Updated** | `user.updatedAt` | "Developer Profile Updated" | `/profile` |

---

## 4. Toast vs Notification Center Distinction

- **Toasts (`useToast`)**: Transient (4s timeout) action feedback (e.g. "Task created successfully"). Not stored in notification history.
- **Notification Center**: Persistent application events derived from entity timestamps and status. Supports read/unread status badge and navigation.

---

## 5. Read/Unread Local Persistence & Backend Scope

- Read notification IDs are tracked client-side in `localStorage` (`devsphere_read_notifications`).
- Unread count badge on the Bell icon updates dynamically based on unread IDs.
- No WebSockets, SSE, or Kafka frontend streaming added, adhering strictly to Lesson 76 backend scope rules.
