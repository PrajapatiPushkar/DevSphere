# Architecture: Published Resume & Active Version Management

## Overview

DevSphere Resume Publishing & Active Version Management ensures production-grade consistency, immutability, and transactional safety when transitioning resume versions to the `PUBLISHED` state. It guarantees that any single `ResumeProfile` has **at most one active `PUBLISHED` version** at any point in time.

---

## Architecture Guarantees & Principles

1. **Single Published Version Invariant**
   - A `ResumeProfile` can have at most **one** version with status `PUBLISHED`.
   - Enforced dual-layer protection:
     - **Database Layer**: Flyway migration (`V12__add_published_version_constraint.sql`) adds a generated virtual column `published_profile_id = CASE WHEN status = 'PUBLISHED' THEN resume_profile_id ELSE NULL END` with a database unique index `uk_resume_versions_published_profile`. Since SQL `UNIQUE` constraints ignore multiple `NULL` values but enforce uniqueness on non-null values, any attempt to insert or update a second `PUBLISHED` record for the same profile triggers a hard `DataIntegrityViolationException`.
     - **Application Transaction Layer**: `ResumeProfileRepository.findByIdAndUserIdForUpdate(...)` applies a `PESSIMISTIC_WRITE` row lock (`SELECT ... FOR UPDATE`) on the parent `ResumeProfile` record during publish operations, serializing concurrent publish requests gracefully.

2. **Automated Archival of Previously Published Version**
   - When a user publishes a `DRAFT` version, the system queries for any existing `PUBLISHED` version for that resume profile.
   - If an existing `PUBLISHED` version exists, it is automatically transitioned: `PUBLISHED` → `ARCHIVED` (with `archivedAt = Instant.now()`).
   - The requested `DRAFT` version is then transitioned: `DRAFT` → `PUBLISHED` (with `publishedAt = Instant.now()`).
   - Both status transitions complete atomically within a single Spring `@Transactional` boundary.

3. **Lifecycle State Machine**

```
        DRAFT
       /     \
      /       \
     v         v
PUBLISHED ---> ARCHIVED
```

| Current Status | Target Status | Validity | Description |
|---|---|---|---|
| `DRAFT` | `PUBLISHED` | **VALID** | Newly published active version; previous published version is archived |
| `DRAFT` | `ARCHIVED` | **VALID** | Soft-archived draft version |
| `PUBLISHED` | `ARCHIVED` | **VALID** | Replaced or retired version |
| `PUBLISHED` | `PUBLISHED` | **INVALID** | Rejected with HTTP 400 Bad Request |
| `ARCHIVED` | `PUBLISHED` | **VALID** (via new version creation) / **INVALID** (direct reactivation rejected with HTTP 400) | Historical versions remain frozen |
| `ARCHIVED` | `DRAFT` | **INVALID** | Re-opening archived versions is disallowed; create a new version snapshot instead |
| `PUBLISHED` | `DRAFT` | **INVALID** | Reverting published versions to draft is disallowed |

4. **Active Published Version Retrieval**
   - `GET /api/v1/resumes/{resumeId}/versions/published` retrieves the current active published version for a resume profile.
   - Returns HTTP `200 OK` with the `ResumeVersionResponse` and compiled snapshot.
   - Returns HTTP `404 Not Found` if the resume profile does not exist, belongs to another user (IDOR isolation), or has no published version.

5. **Immutable Snapshot & Export Integrity**
   - Historical versions (`PUBLISHED` or `ARCHIVED`) store frozen JSON snapshots (`snapshot_data`).
   - Rendering endpoints (`GET /render/html`, `/render/pdf`, `/render/docx`) export directly from the frozen snapshot, ensuring mutations to live career data do not retroactively alter published versions.

---

## Observability & Metrics

- `devsphere_resume_version_publish_total{status="success|failure",transition="publish"}`
- `devsphere_resume_versions_published_total{status="success"}`
