# 0071. Profile and Settings Architecture

- **Status**: Accepted
- **Deciders**: DevSphere Core Engineering Team
- **Date**: 2026-09-06

---

## Context

Lesson 75 requires adding a Developer Profile and Settings experience to the DevSphere production SaaS UI while strictly reusing existing Spring Boot backend REST endpoints (`user-service`, `auth-service`, `task-service`).

## Decision Drivers

1. **Strict Scope Rule**: No new backend endpoints, schema migrations, or synthetic APIs allowed.
2. **Honest Capabilities**: Features unsupported by the backend (such as binary avatar upload or self-service password updates) must be gracefully represented rather than faked or mocked.
3. **Design System Reuse**: Reuse existing UI primitives (`Card`, `Button`, `Input`, `Textarea`, `Modal`, `Badge`, `Alert`, `Skeleton`) and Tailwind CSS design tokens established in Lessons 71–74.
4. **Real-Time State Consistency**: Profile updates must immediately sync to `AuthContext` to update Header and Sidebar components.

## Decision

1. **Service Layer**: Created `userService.ts` mapping to `GET /api/v1/users/me` and `PUT /api/v1/users/me`.
2. **Auth Context Sync**: Extended `AuthContext` with an `updateUser(Partial<User>)` method for atomic in-memory state updates.
3. **Avatar & Security Handling**:
   - Avatars fall back gracefully to user initials when no image URL exists or if photo fails loading.
   - Password/Security settings clearly detail JWT token session architecture without introducing non-existent backend APIs.
4. **Theme Persistence**: Implemented theme switching (Dark/Light/System) stored in `localStorage` (`devsphere_theme`) and updating `document.documentElement` class list.

## Consequences

- Clean separation between frontend UI and existing backend REST contracts.
- Seamless developer experience matching the existing Dashboard and Task Management modules.
- 100% test coverage for profile and settings interactions.
