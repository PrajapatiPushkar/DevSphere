# ADR 0041: Public Resume Presentation & Shareability Foundation

## Status
Accepted

## Context
In previous lessons (Lesson 40 & 41), DevSphere introduced public resume read models and published version management. To make public resumes production-ready for sharing, the system requires a presentation-safe public resume contract (`PublicResumeResponse`) that ensures:
1. Public resume URLs (`GET /api/v1/public/resumes/{publicResumeId}`) work unauthenticated.
2. The public response strictly resolves through the currently `PUBLISHED` immutable snapshot.
3. No internal database IDs, user IDs, version IDs, audit fields, draft/archived versions, or private developer platform metrics (goals, tasks, planner, DSA progress) are exposed.
4. Consistent `404 NOT_FOUND` responses are returned for invalid, missing, draft-only, or archived-only public resumes.

## Decision
1. **Public Resume Presentation Contract**:
   - Harden `PublicResumeResponse` and create dedicated public DTOs (`PublicExperienceResponse`, `PublicEducationResponse`, `PublicSkillItemResponse`, `PublicSkillsResponse`, `PublicCertificationResponse`, `PublicProjectResponse`, `PublicSummaryResponse`, `PublicResumeSectionResponse`).
   - Completely exclude internal database entity primary keys (`id`, `userId`, `resumeProfileId`, `versionId`) from public JSON responses.
   - Expose presentation fields (`name`, `targetRole`, `template`, visible sections).

2. **Snapshot Resolution**:
   - Map `publicResumeId` -> `ResumeProfile` -> `PUBLISHED` `ResumeVersion` -> deserialized snapshot data -> `PublicResumeResponse`.
   - Modifying live profile/career history data does not mutate the public resume; only publishing a new version updates the public representation.

3. **Security Boundary & 404 Uniformity**:
   - `GET /api/v1/public/resumes/**` is permitted without authentication in both API Gateway (`JwtAuthenticationFilter`) and User Service (`SecurityConfig`).
   - Private endpoints under `/api/v1/resumes/**` remain protected by JWT authentication.
   - Return `PUBLIC_RESUME_NOT_FOUND` (HTTP 404) uniformly for non-existent IDs, draft-only profiles, and archived-only profiles.

4. **Observability**:
   - Low-cardinality metric counter `devsphere_public_resume_access_total` with tags `status=success|not_found|failure` and timer `devsphere_public_resume_access_duration`. High-cardinality `publicResumeId` is excluded from tags.

5. **Future Caching Strategy**:
   - Caching via Redis is documented as future work.

## Consequences
- Developers can share public resume links securely without risking exposure of internal database architecture or private platform data.
- The contract is deterministic and presentation-safe.
- Immutability of published resume versions is preserved.
