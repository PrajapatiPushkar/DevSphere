# ADR 0025: Developer Profile and Goal Management Domain Architecture

- **Status**: Accepted
- **Date**: 2026-08-26
- **Authors**: DevSphere Core Engineering Team

---

## Context

DevSphere is transitioning from core platform infrastructure into product domain capabilities. The first product module requires implementing **Developer Profile Management** and **Goal Management** to enable software engineers to maintain rich professional identity metadata and track actionable career and technical goals.

## Decision

1. **Domain Isolation in User Service**:
   - `services/user-service` is established as the sole domain owner for developer profiles and goals.
   - Authentication remains exclusively owned by `services/auth-service`.

2. **Database Migration Strategy**:
   - Created Flyway migration `V3__create_goals_and_update_profiles.sql` adding fields (`headline`, `location`, `github_url`, `linkedin_url`, `portfolio_url`, `current_role`, `years_of_experience`) to `user_profiles` and creating the `goals` table.

3. **Goal Lifecycle & Logical Archival**:
   - Goals support types (`DAILY`, `WEEKLY`, `LONG_TERM`) and statuses (`ACTIVE`, `COMPLETED`, `ARCHIVED`).
   - `DELETE /api/v1/goals/{id}` executes logical archival (`status = ARCHIVED`, returns 204 No Content). Physical deletion is forbidden.
   - `completedAt` timestamps are managed automatically on transition to/from `COMPLETED`.

4. **Dynamic Progress Percentage Calculation**:
   - `progressPercentage` is computed in-memory based on `currentValue` and `targetValue` and returned in API DTOs without database persistence.

5. **IDOR Protection & User Scoping**:
   - Identity is derived strictly from JWT headers.
   - Database operations enforce `findByIdAndUserId(id, userId)`. Unowned access attempts return HTTP `404 Not Found` to prevent resource enumeration.

6. **API Gateway Routing**:
   - Forwarded `/api/v1/profile/**` and `/api/v1/goals/**` through API Gateway with distributed rate limiting.

## Consequences

- Clear separation of auth and user product domain metadata.
- Robust user isolation and security against IDOR vulnerabilities.
- Complete Flyway database schema evolution and historical goal tracking without data loss.
