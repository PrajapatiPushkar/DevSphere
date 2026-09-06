# 0072. Notifications & Activity Architecture

- **Status**: Accepted
- **Deciders**: DevSphere Core Engineering Team
- **Date**: 2026-09-06

---

## Context

Lesson 76 requires implementing a Notification Center popover and Activity Timeline feed for DevSphere while respecting backend boundaries and using genuine application data.

## Decision Drivers

1. **No Fake Data**: No synthetic or hardcoded notification records.
2. **Backend Scope Preservation**: The backend does not expose a public `/api/v1/notifications` REST endpoint. Therefore, no fake microservices, WebSockets, SSE, or DB schemas are added.
3. **Entity Timestamp Derivation**: Transform real task (`createdAt`, `completedAt`, `updatedAt`, `overdue`) and profile (`updatedAt`) records into chronologically ordered event models.
4. **Header Integration**: Replace static toast bell button in `Header.tsx` with an interactive `NotificationBell` component.

## Decision

1. **Service Layer**: Implemented `activityService.ts` to transform real REST responses from `taskService` and `userService` into notification and activity models.
2. **Local Read State**: Persisted read notification IDs in `localStorage` (`devsphere_read_notifications`).
3. **UI Components**:
   - `NotificationBell` + `NotificationPanel` + `NotificationItem` in Header.
   - `ActivityPage` (`/activity`) + `ActivityTimeline` + `ActivityItem`.

## Consequences

- 100% production-oriented implementation without backend scope creep or mock data.
- Full test coverage for notification and activity interactions.
