# Public Resume Presentation & Shareability Architecture

## Overview
Lesson 42 establishes the **Public Resume Presentation & Shareability Foundation** for DevSphere.
This feature enables developers to share an unauthenticated, presentation-safe public resume link (`GET /api/v1/public/resumes/{publicResumeId}`) while strictly guaranteeing snapshot immutability, zero exposure of internal database identifiers, and maintaining robust security boundaries between public and private API surfaces.

---

## Key Principles & Security Boundaries

### 1. Immutable Published Snapshot Resolution
Public resume requests resolve exclusively through published immutable snapshots:
```
publicResumeId
    ↓
ResumeProfile (lookup by public_id UUID)
    ↓
ResumeVersion (status = PUBLISHED)
    ↓
snapshot_data (JSON snapshot)
    ↓
PublicResumeResponse (Sanitized Presentation DTO)
```

- **Live Data Isolation**: Mutating live profile or career history records after publishing does **NOT** alter the public resume.
- **Controlled Updates**: The public representation changes only when a developer explicitly publishes a new `ResumeVersion`.

### 2. Information Disclosure Prevention (Zero-Leakage Contract)
The public contract (`PublicResumeResponse`) exposes **only** presentation-safe domain fields:
- **Exposed Fields**: Name, target role, template style, and visible sections (summary, experience, education, skills, certifications, projects).
- **Hidden / Stripped Fields**:
  - Internal Database Primary Keys (`id`, `userId`, `resumeProfileId`, `versionId`, section item entity `id`s)
  - Private Platform Data (goals, tasks, daily planner entries, DSA progress, private notes)
  - Audit Metadata (`createdAt`, `updatedAt`, `publishedAt`, `archivedAt`)
  - Authentication / Authorization tokens or credentials

### 3. Uniform 404 Behavior (Anti-Enumeration)
To prevent timing side-channels and resource enumeration, the endpoint `GET /api/v1/public/resumes/{publicResumeId}` returns an identical `404 NOT_FOUND` (`PUBLIC_RESUME_NOT_FOUND`) response for:
- Non-existent or invalid `publicResumeId` UUIDs
- Profiles with only `DRAFT` versions
- Profiles with only `ARCHIVED` versions

### 4. API Gateway & Security Policy
- **User Service SecurityConfig**: `/api/v1/public/**` configured with `permitAll()`.
- **API Gateway (`JwtAuthenticationFilter`)**: Bypasses JWT validation for `/api/v1/public/**` and strips incoming untrusted authentication headers (`X-Authenticated-User-Id`, `X-Authenticated-User-Roles`).
- **Private API Protection**: All endpoints under `/api/v1/resumes/**` remain strictly authenticated via JWT.

---

## Presentation DTO Architecture

The public contract maps compiled section contents to presentation-only DTOs:

| Presentation DTO | Included Fields | Omitted Fields |
|---|---|---|
| `PublicResumeResponse` | `name`, `targetRole`, `template`, `sections` | `id`, `userId`, `resumeProfileId`, `publishedAt` |
| `PublicResumeSectionResponse` | `sectionType`, `displayOrder`, `visible`, `content` | `id`, section database IDs |
| `PublicSummaryResponse` | `summary` | — |
| `PublicExperienceResponse` | `companyName`, `jobTitle`, `employmentType`, `location`, `startDate`, `endDate`, `currentlyWorking`, `description`, `displayOrder` | `id` (Experience PK) |
| `PublicEducationResponse` | `institutionName`, `degree`, `fieldOfStudy`, `location`, `startDate`, `endDate`, `currentlyAttending`, `description`, `displayOrder` | `id` (Education PK) |
| `PublicSkillItemResponse` | `name`, `category`, `proficiencyLevel`, `displayOrder` | `id` (Skill PK) |
| `PublicSkillsResponse` | `items` | — |
| `PublicCertificationResponse` | `name`, `issuingOrganization`, `issueDate`, `expirationDate`, `credentialId`, `credentialUrl`, `description`, `displayOrder` | `id` (Certification PK) |
| `PublicProjectResponse` | `title`, `description`, `technologies`, `projectUrl`, `repositoryUrl`, `startDate`, `endDate`, `displayOrder` | `id` (Project PK) |

---

## Observability & Metrics

Low-cardinality Micrometer metrics track public resume access:
- Counter: `devsphere_public_resume_access_total` with tag `status=success|not_found|failure`
- Timer: `devsphere_public_resume_access_duration`

> [!NOTE]
> High-cardinality parameters such as `publicResumeId` are intentionally excluded from metric tags to protect monitoring infrastructure.

---

## Future Considerations & Caching

Distributed caching (e.g. Redis caching for public resume reads) is intentionally out of scope for Lesson 42 and documented as potential future work to optimize read latency for viral public links.
