# ADR 0028: DSA Progress Tracking Domain Architecture

- **Status**: Accepted
- **Date**: 2026-08-26
- **Authors**: DevSphere Core Engineering Team

---

## Context

Following Developer Profiles, Goals, Tasks, and Daily Planner in Lessons 26–28, DevSphere requires a DSA Progress Tracking domain to enable developers to log, categorize, and track Data Structures and Algorithms problem-solving activity.

## Decision

1. **Domain Ownership in User Service**:
   - DSA Progress Tracking is embedded inside `services/user-service` as part of the personal productivity and developer growth context. No separate `dsa-service` microservice is created.

2. **Core Model & Categorization**:
   - `DsaProblem` records problem title, description, platform (`LEETCODE`, `CODEFORCES`, etc.), problem URL, difficulty (`EASY`, `MEDIUM`, `HARD`), topic (`ARRAY`, `STRING`, `GRAPH`, `DYNAMIC_PROGRAMMING`, etc.), notes, time spent, and attempt count.

3. **Status Lifecycle & Timestamping**:
   - Statuses: `TODO`, `IN_PROGRESS`, `SOLVED`, `REVISIT`, `ARCHIVED`.
   - `solvedAt` timestamp is populated when transitioning to `SOLVED` and preserved when transitioning to `REVISIT`.
   - Logical archival (`status = ARCHIVED`, returns `204 No Content`) is used instead of physical database deletion.

4. **Attempt Tracking**:
   - `attemptCount` starts at `0` and is incremented exclusively via `POST /api/v1/dsa/problems/{id}/attempt`.

5. **Dynamic Progress & Statistics**:
   - Daily progress (`/progress/daily`) and overall statistics (`/statistics`) are computed dynamically via database queries rather than persisted counter tables.

6. **IDOR Isolation & Security**:
   - Identity is derived from verified JWT (`UserPrincipal`). Access attempts on non-owned problems, tasks, or goals yield `404 Not Found`.

7. **API Gateway Routing**:
   - `/api/v1/dsa/**` routes are configured under `user-service-route` in API Gateway with distributed rate limiting and OpenTelemetry tracing.

## Consequences

- Direct integration between developer tasks, goals, and DSA problem-solving.
- Strict resource isolation and zero duplicate state.
- Flexible foundation for future DSA streak tracking, spaced repetition, and competitive coding platform sync.
