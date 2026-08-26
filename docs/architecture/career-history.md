# Structured Career History Foundation Architecture

## Overview
The Structured Career History domain expands DevSphere's career capabilities by providing user-owned collections for **Experience**, **Education**, **Skills**, and **Certifications**. These entities store detailed resume data supporting future automated resume compilation, developer portfolios, and career analytics.

All four sub-domains reside inside `services/user-service` and are directly owned by `userId`.

## Domain Models

### 1. Experience
```
Experience (Collection per user)
├── id: Long (PK)
├── userId: Long (NOT NULL)
├── companyName: String (NOT NULL, max 255)
├── jobTitle: String (NOT NULL, max 255)
├── employmentType: EmploymentType (NOT NULL) [FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, FREELANCE, OTHER]
├── location: String (optional, max 255)
├── startDate: LocalDate (NOT NULL)
├── endDate: LocalDate (optional; must be null if currentlyWorking = true)
├── currentlyWorking: Boolean (NOT NULL, default false)
├── description: String (TEXT, optional, max 4000)
├── displayOrder: Integer (NOT NULL, default 0)
├── createdAt: Instant (NOT NULL)
└── updatedAt: Instant (NOT NULL)
```
- **Ordering**: `displayOrder ASC, startDate DESC`

### 2. Education
```
Education (Collection per user)
├── id: Long (PK)
├── userId: Long (NOT NULL)
├── institutionName: String (NOT NULL, max 255)
├── degree: String (NOT NULL, max 255)
├── fieldOfStudy: String (optional, max 255)
├── location: String (optional, max 255)
├── startDate: LocalDate (NOT NULL)
├── endDate: LocalDate (optional; must be null if currentlyStudying = true)
├── currentlyStudying: Boolean (NOT NULL, default false)
├── description: String (TEXT, optional, max 4000)
├── displayOrder: Integer (NOT NULL, default 0)
├── createdAt: Instant (NOT NULL)
└── updatedAt: Instant (NOT NULL)
```
- **Ordering**: `displayOrder ASC, startDate DESC`

### 3. Skill
```
Skill (Collection per user)
├── id: Long (PK)
├── userId: Long (NOT NULL)
├── name: String (NOT NULL, max 100)
├── category: SkillCategory (NOT NULL) [PROGRAMMING_LANGUAGE, FRAMEWORK, DATABASE, CLOUD, DEVOPS, TESTING, TOOLS, SOFT_SKILL, OTHER]
├── proficiency: Proficiency (NOT NULL) [BEGINNER, INTERMEDIATE, ADVANCED, EXPERT]
├── yearsOfExperience: Integer (optional, min 0, max 70)
├── displayOrder: Integer (NOT NULL, default 0)
├── createdAt: Instant (NOT NULL)
└── updatedAt: Instant (NOT NULL)
```
- **Uniqueness Constraint**: `UNIQUE(user_id, name)` (case-insensitive check prevents `Java` / `java` duplicates).
- **Ordering**: `displayOrder ASC, name ASC`

### 4. Certification
```
Certification (Collection per user)
├── id: Long (PK)
├── userId: Long (NOT NULL)
├── name: String (NOT NULL, max 255)
├── issuingOrganization: String (NOT NULL, max 255)
├── issueDate: LocalDate (optional)
├── expirationDate: LocalDate (optional; expirationDate >= issueDate)
├── credentialId: String (optional, max 255)
├── credentialUrl: String (optional, max 1000, valid HTTP/HTTPS URL)
├── description: String (TEXT, optional, max 2000)
├── displayOrder: Integer (NOT NULL, default 0)
├── createdAt: Instant (NOT NULL)
└── updatedAt: Instant (NOT NULL)
```
- **Ordering**: `displayOrder ASC, issueDate DESC`

## API Endpoints
Exposed via API Gateway under `/api/v1/experience/**`, `/api/v1/education/**`, `/api/v1/skills/**`, and `/api/v1/certifications/**`.

| Domain | Method | Endpoint | Description | Status Code |
|---|---|---|---|---|
| Experience | `POST` | `/api/v1/experience` | Create experience | `201 Created` |
| Experience | `GET` | `/api/v1/experience` | List authenticated user's experiences | `200 OK` |
| Experience | `GET` | `/api/v1/experience/{id}` | Get owned experience by ID | `200 OK` |
| Experience | `PUT` | `/api/v1/experience/{id}` | Update owned experience | `200 OK` |
| Experience | `DELETE` | `/api/v1/experience/{id}` | Delete owned experience | `204 No Content` |
| Education | `POST` | `/api/v1/education` | Create education | `201 Created` |
| Education | `GET` | `/api/v1/education` | List authenticated user's education | `200 OK` |
| Education | `GET` | `/api/v1/education/{id}` | Get owned education by ID | `200 OK` |
| Education | `PUT` | `/api/v1/education/{id}` | Update owned education | `200 OK` |
| Education | `DELETE` | `/api/v1/education/{id}` | Delete owned education | `204 No Content` |
| Skills | `POST` | `/api/v1/skills` | Create skill | `201 Created` |
| Skills | `GET` | `/api/v1/skills` | List authenticated user's skills | `200 OK` |
| Skills | `GET` | `/api/v1/skills/{id}` | Get owned skill by ID | `200 OK` |
| Skills | `PUT` | `/api/v1/skills/{id}` | Update owned skill | `200 OK` |
| Skills | `DELETE` | `/api/v1/skills/{id}` | Delete owned skill | `204 No Content` |
| Certifications | `POST` | `/api/v1/certifications` | Create certification | `201 Created` |
| Certifications | `GET` | `/api/v1/certifications` | List authenticated user's certifications | `200 OK` |
| Certifications | `GET` | `/api/v1/certifications/{id}` | Get owned certification by ID | `200 OK` |
| Certifications | `PUT` | `/api/v1/certifications/{id}` | Update owned certification | `200 OK` |
| Certifications | `DELETE` | `/api/v1/certifications/{id}` | Delete owned certification | `204 No Content` |

## Ownership & IDOR Protection
- `userId` is extracted exclusively from JWT context (`UserPrincipal` or Gateway `X-Authenticated-User-Id` header).
- `findByIdAndUserId(id, userId)` ensures non-owned resource requests return `404 Not Found` without leaking resource existence.

## Database Schema & Indexing
Flyway migration: `V9__create_career_history.sql`
Indexes:
- `idx_experiences_user_order (user_id, display_order)`
- `idx_educations_user_order (user_id, display_order)`
- `idx_skills_user_order (user_id, display_order)`
- `idx_certifications_user_order (user_id, display_order)`

## Observability
Micrometer metrics:
- `devsphere_experience_created_total`, `devsphere_experience_updated_total`, `devsphere_experience_deleted_total`
- `devsphere_education_created_total`, `devsphere_education_updated_total`, `devsphere_education_deleted_total`
- `devsphere_skill_created_total`, `devsphere_skill_updated_total`, `devsphere_skill_deleted_total`
- `devsphere_certification_created_total`, `devsphere_certification_updated_total`, `devsphere_certification_deleted_total`

## Future Extensions
- Automated resume compilation (PDF/DOCX/JSON Resume format).
- Tailored resume generation based on target job description.
- Skill gap analysis and learning path recommendations.
