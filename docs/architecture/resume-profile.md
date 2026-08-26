# Resume Profile & Compilation Foundation Architecture

## Overview
The Resume Profile & Compilation domain enables software developers to define multiple structured resume configurations (`ResumeProfile`), manage section visibility and ordering (`ResumeSection`), and select specific career history records (`Experience`, `Education`, `Skill`, `Certification`) and projects (`DeveloperProject`) to compile into a tailored resume representation.

All resume configuration domains reside inside `services/user-service` and are directly owned by `userId`.

## Domain Models

### 1. ResumeProfile
```
ResumeProfile (Multiple per user)
├── id: Long (PK)
├── userId: Long (NOT NULL)
├── name: String (NOT NULL, max 100)
├── targetRole: String (NOT NULL, max 255)
├── summaryOverride: String (TEXT, optional, max 4000)
├── template: ResumeTemplate (NOT NULL) [PROFESSIONAL, MODERN, MINIMAL]
├── status: ResumeStatus (NOT NULL, default DRAFT) [DRAFT, ACTIVE, ARCHIVED]
├── createdAt: Instant (NOT NULL)
└── updatedAt: Instant (NOT NULL)
```
- **Single Active Resume Rule**: A user may have multiple resumes, but maximum 1 `ACTIVE` resume. Activating a resume atomically archives any previous active resume for that user.

### 2. ResumeSection
```
ResumeSection
├── id: Long (PK)
├── resumeProfileId: Long (NOT NULL)
├── sectionType: ResumeSectionType (NOT NULL) [SUMMARY, EXPERIENCE, EDUCATION, SKILLS, CERTIFICATIONS, PROJECTS]
├── displayOrder: Integer (NOT NULL, min 1)
├── visible: Boolean (NOT NULL, default true)
├── createdAt: Instant (NOT NULL)
└── updatedAt: Instant (NOT NULL)
```
- **Default Sections**: Automatically generated upon resume creation (orders 1..6, all `visible = true`).
- **Uniqueness Constraint**: `UNIQUE(resume_profile_id, section_type)`.

### 3. Selection Reference Entities
```
ResumeExperience   (UNIQUE: resume_profile_id, experience_id)
ResumeEducation    (UNIQUE: resume_profile_id, education_id)
ResumeSkill        (UNIQUE: resume_profile_id, skill_id)
ResumeCertification(UNIQUE: resume_profile_id, certification_id)
ResumeProject      (UNIQUE: resume_profile_id, project_id)
```
- Selections store foreign IDs and display ordering only. Source records remain the single source of truth.

## API Endpoints
Exposed via API Gateway under `/api/v1/resumes/**`.

| Method | Endpoint | Description | Response Code |
|---|---|---|---|
| `POST` | `/api/v1/resumes` | Create resume profile (creates default 6 sections) | `201 Created` |
| `GET` | `/api/v1/resumes` | List authenticated user's resumes | `200 OK` |
| `GET` | `/api/v1/resumes/{id}` | Get owned resume profile | `200 OK` |
| `PUT` | `/api/v1/resumes/{id}` | Update owned resume profile | `200 OK` |
| `DELETE` | `/api/v1/resumes/{id}` | Logically archive resume profile | `204 No Content` |
| `POST` | `/api/v1/resumes/{id}/archive` | Set status to ARCHIVED | `200 OK` |
| `POST` | `/api/v1/resumes/{id}/activate` | Atomically activate resume | `200 OK` |
| `GET` | `/api/v1/resumes/{id}/sections` | List resume sections | `200 OK` |
| `PUT` | `/api/v1/resumes/{id}/sections/{sectionId}` | Update section visibility / order | `200 OK` |
| `POST` | `/api/v1/resumes/{id}/experiences` | Add experience selection | `200 OK` |
| `GET` | `/api/v1/resumes/{id}/experiences` | List selected experiences | `200 OK` |
| `DELETE` | `/api/v1/resumes/{id}/experiences/{expId}` | Remove experience selection | `204 No Content` |
| `POST` | `/api/v1/resumes/{id}/education` | Add education selection | `200 OK` |
| `GET` | `/api/v1/resumes/{id}/education` | List selected education | `200 OK` |
| `DELETE` | `/api/v1/resumes/{id}/education/{eduId}` | Remove education selection | `204 No Content` |
| `POST` | `/api/v1/resumes/{id}/skills` | Add skill selection | `200 OK` |
| `GET` | `/api/v1/resumes/{id}/skills` | List selected skills | `200 OK` |
| `DELETE` | `/api/v1/resumes/{id}/skills/{skillId}` | Remove skill selection | `204 No Content` |
| `POST` | `/api/v1/resumes/{id}/certifications` | Add certification selection | `200 OK` |
| `GET` | `/api/v1/resumes/{id}/certifications` | List selected certifications | `200 OK` |
| `DELETE` | `/api/v1/resumes/{id}/certifications/{certId}` | Remove certification selection | `204 No Content` |
| `POST` | `/api/v1/resumes/{id}/projects` | Add project selection | `200 OK` |
| `GET` | `/api/v1/resumes/{id}/projects` | List selected projects | `200 OK` |
| `DELETE` | `/api/v1/resumes/{id}/projects/{projId}` | Remove project selection | `204 No Content` |

## Ownership & IDOR Protection
- Identity is derived exclusively from JWT authentication context (`UserPrincipal` or Gateway `X-Authenticated-User-Id` header).
- Selections verify that both `resume.userId == authenticatedUserId` and `sourceEntity.userId == authenticatedUserId`.
- Unauthorized access or cross-user selection attempts return HTTP `404 Not Found`.

## Database Schema & Migration
Flyway migration: `V10__create_resume_profiles.sql`

## Observability
Micrometer metrics:
- `devsphere_resume_created_total`, `devsphere_resume_updated_total`, `devsphere_resume_archived_total`, `devsphere_resume_activated_total`, `devsphere_resume_deleted_total`
- `devsphere_resume_section_updated_total`
- `devsphere_resume_experience_selected_total`, `devsphere_resume_education_selected_total`, `devsphere_resume_skill_selected_total`, `devsphere_resume_certification_selected_total`, `devsphere_resume_project_selected_total`

## Why Compilation Configuration is Separate from Document Generation
Separating resume configuration (`ResumeProfile`) from document rendering allows developers to maintain multiple tailored resume profiles backed by a single live career history dataset. Future compilation services can render HTML, PDF, or DOCX formats dynamically without duplicating source data.
