# Architecture: Resume Versioning & Immutable Snapshot Management

## Overview

DevSphere Resume Versioning introduces structured, immutable resume snapshots within `user-service`. It enables developers to capture explicit point-in-time versions of their compiled resumes, transition versions through state lifecycles (`DRAFT`, `PUBLISHED`, `ARCHIVED`), and render multi-format exports (HTML, PDF, DOCX) directly from immutable version snapshots.

---

## Architectural Principles

1. **Immutable Point-In-Time Snapshots**
   - Each resume version contains a JSON blob (`snapshot_data`) representing the full `CompiledResumeResponse` at the exact moment of version creation.
   - Any subsequent edits to career profiles, experiences, educations, skills, certifications, developer projects, or resume section configurations DO NOT affect existing published versions.

2. **Scoped Version Numbering & Database Integrity**
   - Version numbers (`version_number`) are profile-scoped integer sequences starting at 1 (`UNIQUE(resume_profile_id, version_number)`).
   - High concurrency is safely handled at the database layer via unique constraints and transaction isolation.

3. **Lifecycle State Machine**
   - State transitions are strictly governed:
     - `DRAFT` → `PUBLISHED` (sets `published_at`)
     - `DRAFT` → `ARCHIVED` (sets `archived_at`)
     - `PUBLISHED` → `ARCHIVED` (sets `archived_at`)
   - Invalid transitions (e.g. `PUBLISHED` → `DRAFT`) return HTTP `400 Bad Request`.

4. **Multi-Format Export Compatibility**
   - Version rendering endpoints (`GET /api/v1/resumes/{resumeId}/versions/{versionId}/render/html|pdf|docx`) deserialize the immutable `snapshot_data` and execute the exact same HTML, PDF, and DOCX rendering engines used for live compilation.

5. **IDOR Protection & User Isolation**
   - Version lookups require both `resumeProfileId` and `userId` (`findByIdAndResumeProfileIdAndUserId`). Accessing another user's version returns HTTP `404 Not Found`.

---

## Entity Relationship

```
+---------------------+        1:N       +------------------------+
|   resume_profiles   | ----------------> |    resume_versions     |
+---------------------+                  +------------------------+
| id (PK)             |                  | id (PK)                |
| user_id             |                  | resume_profile_id (FK) |
| name                |                  | user_id                |
| target_role         |                  | version_number         |
| template            |                  | name                   |
+---------------------+                  | status (DRAFT/PUB/ARCH)|
                                         | snapshot_data (JSON)   |
                                         | created_at, published_at|
                                         +------------------------+
```

---

## Key REST Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/resumes/{resumeId}/versions` | Create a new point-in-time resume version snapshot |
| `GET` | `/api/v1/resumes/{resumeId}/versions` | List all versions for a resume profile (descending) |
| `GET` | `/api/v1/resumes/{resumeId}/versions/{versionId}` | Retrieve specific version details and snapshot |
| `POST` | `/api/v1/resumes/{resumeId}/versions/{versionId}/publish` | Publish a version |
| `POST` | `/api/v1/resumes/{resumeId}/versions/{versionId}/archive` | Archive a version |
| `GET` | `/api/v1/resumes/{resumeId}/versions/{versionId}/render/html` | Render version as HTML |
| `GET` | `/api/v1/resumes/{resumeId}/versions/{versionId}/render/pdf` | Render version as downloadable PDF |
| `GET` | `/api/v1/resumes/{resumeId}/versions/{versionId}/render/docx` | Render version as downloadable DOCX |

---

## Metrics & Observability

- `devsphere_resume_versions_created_total` (`status=success|failure`)
- `devsphere_resume_versions_published_total` (`status=success|failure`)
- `devsphere_resume_export_total` (`format=html|pdf|docx`, `status=success|failure`, `template=...`)
