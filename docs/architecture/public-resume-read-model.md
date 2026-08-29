# Architecture: Published Resume Access & Public Resume Read Model

## Overview

DevSphere Public Resume Read Model (`GET /api/v1/public/resumes/{publicResumeId}`) allows published developer resumes to be publicly accessed and viewed safely without requiring authentication, JWT tokens, or exposure of private platform entities.

---

## Data Flow & Architecture Diagram

```
Client (Unauthenticated Browser / Link Visitor)
  │
  │ GET /api/v1/public/resumes/{publicResumeId}
  ▼
API Gateway (`DEVSPHERE-API-GATEWAY`)
  │ (Bypasses JWT authentication via PUBLIC_PATH_PREFIXES)
  ▼
User Service (`DEVSPHERE-USER-SERVICE`)
  │ (`SecurityConfig` permitAll("/api/v1/public/**"))
  ▼
`PublicResumeController`
  │
  ▼
`PublicResumeService`
  ├── 1. `ResumeProfileRepository.findByPublicId(publicResumeId)`
  ├── 2. `ResumeVersionRepository.findByResumeProfileIdAndStatus(profileId, PUBLISHED)`
  ├── 3. Read immutable snapshot JSON (`snapshot_data`)
  └── 4. Build presentation-safe `PublicResumeResponse`
  │
  ▼
HTTP 200 OK (Public Resume Presentation Data)
```

---

## Core Guarantees & Privacy Boundaries

1. **Unpredictable Public Identifier (`public_id`)**
   - Public resume sharing uses an opaque, server-side generated UUID (`VARCHAR(36)`).
   - Generated automatically on `ResumeProfile` creation via `@PrePersist` (`UUID.randomUUID().toString()`).
   - Indexed via a database unique constraint (`uk_resume_profiles_public_id`).
   - Internal database primary keys (`id`), user sequence IDs (`userId`), resume version IDs (`versionId`), and audit fields are **never** exposed.

2. **Published-Only Snapshot Access**
   - The public endpoint resolves **only** the active `PUBLISHED` version snapshot.
   - If a resume profile is in `DRAFT` status or has no `PUBLISHED` version, the endpoint returns `404 Not Found`.
   - `DRAFT` and `ARCHIVED` versions are strictly inaccessible over the public read model.

3. **Complete Immutability Against Live Edits**
   - Public resume reads directly from the frozen JSON snapshot (`snapshot_data`) of the published version.
   - Live mutations to developer career profiles, experience records, or skill items do not retroactively alter the public resume view until the developer explicitly publishes a new version snapshot.

4. **Security & IDOR Immunity**
   - The public read model exposes presentation data (`name`, `targetRole`, `template`, `sections`) stripped of internal entity database IDs.
   - Any query for an unknown `publicResumeId` or an unpublished resume profile yields an identical `404 Not Found` (`PUBLIC_RESUME_NOT_FOUND`) error response, preventing user probing or entity existence leakage.
   - All management, versioning, compilation, and export endpoints (`/api/v1/resumes/**`) remain strictly protected by JWT authentication and ownership checks.

---

## Observability & Metrics

- `devsphere_public_resume_access_total{status="success|not_found"}`
- `devsphere_public_resume_access_duration` (Timer)
