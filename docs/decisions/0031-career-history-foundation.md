# ADR 0031: Structured Career History Foundation in User Service

## Status
Accepted

## Date
2026-08-27

## Context
DevSphere is a Developer Life Management and Career Growth platform. Following the introduction of the Career Profile summary domain in Lesson 31, DevSphere requires structured career history supporting data (`Experience`, `Education`, `Skills`, `Certifications`) to power future automated resume generation, portfolio building, and career intelligence features.

## Decision
We introduce four independent structured career history collection domains (`Experience`, `Education`, `Skill`, `Certification`) directly inside `services/user-service`. We explicitly decide **NOT** to split these into a separate `career-service`, `resume-service`, `profile-service`, or `portfolio-service` microservice at this stage.

### Key Rationale
1. **Direct User Ownership**: `Experience`, `Education`, `Skill`, and `Certification` entities are directly owned by `userId` (authenticated developer identity), rather than being foreign-keyed to `CareerProfile.id`. This supports clean domain isolation and flexible queries.
2. **User-Scoped CRUD & IDOR Isolation**: All API operations (`/api/v1/experience/**`, `/api/v1/education/**`, `/api/v1/skills/**`, `/api/v1/certifications/**`) enforce strict user identity scoping via repository calls (`findByIdAndUserId(id, userId)`). Non-owned accesses return `404 Not Found`.
3. **Case-Insensitive Skill Uniqueness**: Skill names enforce database and service uniqueness per user `(user_id, name)` case-insensitively to prevent duplicates like `Java`, `java`, and `JAVA`.
4. **No Premature Complexity**: No automated resume rendering (PDF/DOCX), AI generation, or external integrations are implemented in Lesson 32.

## Architecture & Principles
- **Experience**: Fields `companyName`, `jobTitle`, `employmentType` (`FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERNSHIP`, `FREELANCE`, `OTHER`), `location`, `startDate`, `endDate`, `currentlyWorking`, `description`, `displayOrder`. Validates `currentlyWorking=true` => `endDate=null`. Default sort: `displayOrder ASC, startDate DESC`.
- **Education**: Fields `institutionName`, `degree`, `fieldOfStudy`, `location`, `startDate`, `endDate`, `currentlyStudying`, `description`, `displayOrder`. Validates `currentlyStudying=true` => `endDate=null`. Default sort: `displayOrder ASC, startDate DESC`.
- **Skill**: Fields `name`, `category` (`PROGRAMMING_LANGUAGE`, `FRAMEWORK`, `DATABASE`, `CLOUD`, `DEVOPS`, `TESTING`, `TOOLS`, `SOFT_SKILL`, `OTHER`), `proficiency` (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`), `yearsOfExperience` (`0` to `70`), `displayOrder`. Enforces case-insensitive `UNIQUE(user_id, name)`. Default sort: `displayOrder ASC, name ASC`.
- **Certification**: Fields `name`, `issuingOrganization`, `issueDate`, `expirationDate`, `credentialId`, `credentialUrl`, `description`, `displayOrder`. Validates `expirationDate >= issueDate`. Default sort: `displayOrder ASC, issueDate DESC`.
- **Observability**: Metrics `devsphere_experience_*_total`, `devsphere_education_*_total`, `devsphere_skill_*_total`, `devsphere_certification_*_total`.

## Consequences
### Positive
- Structured resume data foundation built cleanly inside `user-service`.
- Strict user-scoped IDOR security across all 4 collections.
- Database migration `V9__create_career_history.sql` with efficient indexes.
- Zero extra microservice complexity.

### Tradeoffs
- `user-service` context expands; if high-volume resume compilation or document rendering is introduced later, microservice extraction will be evaluated.
