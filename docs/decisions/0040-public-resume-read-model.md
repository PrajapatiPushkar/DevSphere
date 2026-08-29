# ADR 0040: Public Resume Read Model & Unauthenticated Access Boundary

## Status
Accepted

## Context
In DevSphere, developers want to share their active published resume with external viewers (employers, portfolio visitors, recruiters) via a public link. The public endpoint must allow unauthenticated access while strictly preventing IDOR vulnerability, guessing of internal database IDs, exposure of live draft data, or leakage of private platform management information (goals, tasks, DSA, planner, audit metadata).

## Decision
1. **Public Identifier**:
   - Added database Flyway migration `V13__add_public_id_to_resume_profiles.sql` adding `public_id VARCHAR(36)` with a unique index `uk_resume_profiles_public_id`.
   - Server-generated 128-bit random UUID (`UUID.randomUUID().toString()`) assigned on profile creation via `@PrePersist`.
2. **Dedicated Read Model (`PublicResumeResponse`)**:
   - Strips all internal database sequence IDs (`id`, `userId`, `resumeProfileId`, `versionId`), internal audit timestamps, and private management data. Exposes only presentation fields (`name`, `targetRole`, `template`, `sections`).
3. **Published-Only Snapshot Source**:
   - `PublicResumeService.getPublicResume(publicResumeId)` resolves the active `PUBLISHED` snapshot JSON (`snapshot_data`). Draft or archived versions return HTTP `404 Not Found`.
4. **Security & Gateway Routing**:
   - `GET /api/v1/public/resumes/{publicResumeId}` is added to `PUBLIC_PATH_PREFIXES` in API Gateway and `permitAll()` in User Service `SecurityConfig`.
   - Private endpoints under `/api/v1/resumes/**` remain strictly authenticated via JWT.

## Consequences
- **Positive**: Enables safe public resume sharing without security risks or entity leakage.
- **Positive**: Guarantees snapshot immutability so live profile edits do not unexpectedly alter the public presentation until re-published.
- **Trade-off**: Requires dedicated public DTO mapping and public path routing in API Gateway.
