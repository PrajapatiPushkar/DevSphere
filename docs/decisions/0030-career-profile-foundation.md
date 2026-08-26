# ADR 0030: Career Profile & Resume Foundation Domain in User Service

## Status
Accepted

## Date
2026-08-27

## Context
DevSphere is a Developer Life Management and Career Growth platform. The current product domain foundation includes:
- Developer Profile
- Goals
- Tasks
- Daily Planner
- DSA Progress Tracking
- Developer Projects

To support future resume generation, portfolio creation, ATS scoring, and career intelligence features, DevSphere requires a structured career positioning foundation.

## Decision
We introduce the **Career Profile Domain** directly inside `services/user-service` as a singleton resource per user (`UNIQUE(user_id)`). We explicitly decide **NOT** to split this into a standalone `career-service`, `resume-service`, or `portfolio-service` microservice at this stage.

### Key Rationale
1. **Personal Identity & Career Positioning**: Career profile information represents personal user-owned data and resides naturally alongside the existing Developer Profile and productivity domains.
2. **Singleton Resource Model**: Each user has exactly one career profile (`UNIQUE(user_id)`), accessible via user-scoped endpoints (`GET /api/v1/career-profile`, `PUT /api/v1/career-profile`, `DELETE /api/v1/career-profile`).
3. **Idempotent Upsert Design**: `PUT /api/v1/career-profile` performs an idempotent create-or-update operation without requiring separate POST endpoints or client-supplied profile IDs.
4. **No Premature Complexity**: No AI generation, external scraping, or resume rendering (PDF/DOCX) is added in Lesson 31; those are deferred to specialized future lessons.

## Architecture & Principles
- **Entity**: `CareerProfile` with fields `professionalSummary`, `currentTitle`, `targetRole`, `yearsOfExperience`, `preferredLocation`, `workPreference` (`REMOTE`, `HYBRID`, `ONSITE`, `FLEXIBLE`), and `availability` (`OPEN_TO_WORK`, `ACTIVELY_LOOKING`, `NOT_LOOKING`, `OPEN_TO_OPPORTUNITIES`).
- **Identity & IDOR Protection**: Identity is derived exclusively from JWT (`UserPrincipal`). No arbitrary profile ID path variables are exposed to prevent IDOR vulnerabilities.
- **Observability**: Metrics `devsphere_career_profile_created_total`, `devsphere_career_profile_updated_total`, `devsphere_career_profile_deleted_total`.

## Consequences
### Positive
- Unified career positioning foundation inside `user-service`.
- Clean singleton resource semantics and database constraints (`V8__create_career_profiles.sql`).
- Idempotent API operations.
- Zero extra microservice overhead.

### Tradeoffs
- `user-service` continues growing; if career intelligence, resume compilation, or job parsing demands intensive AI or PDF processing in future lessons, decomposition into specialized microservices will be evaluated.
