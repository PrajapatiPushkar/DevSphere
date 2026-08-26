# ADR 0032: Resume Profile & Compilation Foundation in User Service

## Status
Accepted

## Date
2026-08-27

## Context
DevSphere is a Developer Life Management and Career Growth platform. Following the establishment of Career Profile (Lesson 31) and Career History collections (Lesson 32), software developers need a structured resume configuration model to group, customize, order, and selectively include career history records (`Experience`, `Education`, `Skill`, `Certification`) and projects (`DeveloperProject`).

## Decision
We introduce the **Resume Profile & Compilation Foundation** directly inside `services/user-service`. We explicitly decide **NOT** to create a separate `resume-service`, `career-service`, or `document-service` microservice at this stage.

### Key Rationale
1. **Direct User Ownership**: `ResumeProfile` entities are directly owned by `userId` (authenticated developer identity). A developer can create multiple resume profiles (e.g. Java Backend Resume, Full Stack Resume, Cloud Engineer Resume).
2. **Selection References without Data Duplication**: Selections (`ResumeExperience`, `ResumeEducation`, `ResumeSkill`, `ResumeCertification`, `ResumeProject`) store only foreign references (`sourceEntityId`) and display ordering. Source records in `experiences`, `educations`, `skills`, `certifications`, and `developer_projects` remain the single source of truth.
3. **Single Active Resume Rule**: Maximum 1 `ACTIVE` resume per user is enforced transactionally (`activateResumeProfile` atomically archives any previously active resume for that user).
4. **Default Sections**: Creating a `ResumeProfile` automatically populates default sections (`SUMMARY`, `EXPERIENCE`, `EDUCATION`, `SKILLS`, `CERTIFICATIONS`, `PROJECTS`) with default display order 1 to 6 and `visible = true`.
5. **No Premature Document Generation**: Lesson 33 focuses exclusively on the structured compilation model. Document generation (PDF, DOCX, HTML) and AI features are explicitly deferred to future lessons.

## Architecture & Principles
- **ResumeProfile**: Fields `name`, `targetRole`, `summaryOverride`, `template` (`PROFESSIONAL`, `MODERN`, `MINIMAL`), `status` (`DRAFT`, `ACTIVE`, `ARCHIVED`).
- **ResumeSection**: Fields `resumeProfileId`, `sectionType`, `displayOrder`, `visible`. Unique constraint `UNIQUE(resume_profile_id, section_type)`.
- **Selections**: Unique constraints `UNIQUE(resume_profile_id, source_id)`. Prevents duplicate selections (returning HTTP `409 Conflict`).
- **IDOR Protection**: All APIs require JWT authentication and enforce `userId` scoping across resume profiles and source selection entities. Cross-user selection attempts return `404 Not Found`.
- **Observability**: Low-cardinality Prometheus metrics `devsphere_resume_*_total`.

## Consequences
### Positive
- Flexible multi-resume model per developer.
- Zero duplicate data between career history and resume profiles.
- Strong transactional single-active resume enforcement.
- Clean separation between compilation configuration and document rendering.

### Tradeoffs
- `user-service` model expands; if high-volume PDF/DOCX rendering is introduced later, a dedicated document compilation microservice will be evaluated.
