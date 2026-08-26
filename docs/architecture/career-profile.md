# Career Profile & Resume Foundation Architecture

## Overview
The Career Profile domain enables software developers using DevSphere to maintain structured career positioning information (`professionalSummary`, `currentTitle`, `targetRole`, `yearsOfExperience`, `preferredLocation`, `workPreference`, `availability`).

This domain is implemented inside `services/user-service` as a singleton resource per user (`UNIQUE(user_id)`), serving as the data foundation for future resume generation, portfolio building, and career analytics.

## Domain Model
```
CareerProfile (Singleton per user)
├── id: Long (PK)
├── userId: Long (NOT NULL, UNIQUE)
├── professionalSummary: String (TEXT, optional)
├── currentTitle: String (optional, max 255)
├── targetRole: String (optional, max 255)
├── yearsOfExperience: Integer (optional, >= 0, <= 70)
├── preferredLocation: String (optional, max 255)
├── workPreference: WorkPreference (STRING) [REMOTE, HYBRID, ONSITE, FLEXIBLE]
├── availability: Availability (STRING) [OPEN_TO_WORK, ACTIVELY_LOOKING, NOT_LOOKING, OPEN_TO_OPPORTUNITIES]
├── createdAt: Instant (NOT NULL, server managed)
└── updatedAt: Instant (NOT NULL, server managed)
```

## Developer Profile vs. Career Profile Relationship
- **Developer Profile** (`UserProfile`): Manages personal identity details (`firstName`, `lastName`, `displayName`, `headline`, `bio`, social/portfolio links).
- **Career Profile** (`CareerProfile`): Manages job-readiness and career positioning details (`professionalSummary`, `currentTitle`, `targetRole`, `yearsOfExperience`, `preferredLocation`, `workPreference`, `availability`).
- The two models remain separate bounded resources under `user-service`.

## API Endpoints
All endpoints are user-scoped and exposed through API Gateway under `/api/v1/career-profile/**`.

| Method | Endpoint | Description | Response Code |
|---|---|---|---|
| `GET` | `/api/v1/career-profile` | Fetch authenticated user's career profile | `200 OK` (or `404 Not Found`) |
| `PUT` | `/api/v1/career-profile` | Create or update authenticated user's career profile (Idempotent) | `200 OK` |
| `DELETE` | `/api/v1/career-profile` | Delete authenticated user's career profile | `204 No Content` |

## Ownership & IDOR Isolation
- `userId` is extracted exclusively from authenticated JWT context (`UserPrincipal` or Gateway `X-Authenticated-User-Id` header).
- No arbitrary profile IDs are exposed in URI paths, preventing IDOR access vectors.

## Database Schema & Migration
Flyway migration: `V8__create_career_profiles.sql`
Schema:
```sql
CREATE TABLE career_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    professional_summary TEXT NULL,
    current_title VARCHAR(255) NULL,
    target_role VARCHAR(255) NULL,
    years_of_experience INT NULL,
    preferred_location VARCHAR(255) NULL,
    work_preference VARCHAR(50) NULL,
    availability VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_career_profiles_user_id UNIQUE (user_id)
);
```

## Observability
Micrometer metrics:
- `devsphere_career_profile_created_total`
- `devsphere_career_profile_updated_total`
- `devsphere_career_profile_deleted_total`

## Future Extensions
- Structured resume sections (Education, Experience, Skills, Certifications).
- Automated resume compilation (PDF/DOCX).
- Developer portfolio generation.
- Career goal alignment and skill gap analytics.
