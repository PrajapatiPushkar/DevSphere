# ADR 0039: Resume Publishing and Active Version Management

## Status
Accepted

## Context
In DevSphere, developers require controlled management over published resume versions. When a developer publishes a new iteration of their resume, the platform must guarantee that at most ONE version per resume profile is active (`PUBLISHED`), previously published versions are safely archived, concurrent publish requests do not produce multiple published versions, and historical published snapshots remain 100% immutable.

## Decision
1. **Single Published Version Invariant**:
   - Database level: Added Flyway migration `V12__add_published_version_constraint.sql` introducing generated column `published_profile_id = CASE WHEN status = 'PUBLISHED' THEN resume_profile_id ELSE NULL END` and a unique index `uk_resume_versions_published_profile`.
   - Application level: Applied `PESSIMISTIC_WRITE` row locking on `ResumeProfile` (`findByIdAndUserIdForUpdate`) during publishing.
2. **Automated Archival Strategy**:
   - Publishing a `DRAFT` version inside `@Transactional` automatically transitions any existing `PUBLISHED` version for that profile to `ARCHIVED` (`archivedAt = Instant.now()`), then transitions the requested `DRAFT` to `PUBLISHED` (`publishedAt = Instant.now()`).
3. **State Machine Strictness**:
   - Only `DRAFT` versions can transition to `PUBLISHED`.
   - Attempting `PUBLISHED` -> `PUBLISHED` or `ARCHIVED` -> `PUBLISHED` returns `400 Bad Request`.
4. **Active Published Endpoint**:
   - Implemented `GET /api/v1/resumes/{resumeId}/versions/published` for user-scoped retrieval of the active published version.
5. **IDOR & Security**:
   - Ownership checks (`findByIdAndUserId`) ensure users can only publish or retrieve versions belonging to their authenticated `userId`. Cross-user calls return `404 Not Found`.

## Consequences
- **Positive**: Hard database and transaction guarantees prevent race conditions and duplicate active published resumes.
- **Positive**: Clear lifecycle state machine prevents accidental modification or invalid state transitions.
- **Trade-off**: Requires database pessimistic write lock during publish execution, serialized per resume profile without affecting independent profiles.
