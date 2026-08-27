# Resume Compilation Engine Architecture

## Overview
The Resume Compilation Engine transforms user-configured resume profiles (`ResumeProfile`) and section selections into an immutable, presentation-ready JSON representation (`CompiledResumeResponse`). This compiled model acts as the input layer for future rendering engines (HTML, PDF, DOCX, ATS export).

## Pipeline Flow

```
Authenticated User (JWT / X-Authenticated-User-Id)
       │
       ▼
GET /api/v1/resumes/{resumeId}/compile
       │
       ▼
ResumeCompilationService.compileResume(resumeId, userId)
       │
       ├── 1. Validate Resume Ownership (resumeProfile.userId == userId)
       ├── 2. Load ResumeSections (Ordered by displayOrder ASC)
       ├── 3. Filter Invisible Sections (visible == false skipped)
       ├── 4. Batch-Resolve Source Records (IN queries avoiding N+1)
       ├── 5. Enforce Source Ownership (source.userId == userId)
       ├── 6. Resolve Summary (summaryOverride -> CareerProfile summary -> null)
       ├── 7. Sort Section Items (Selection displayOrder ASC)
       └── 8. Construct & Return CompiledResumeResponse
```

## DTO Models

### CompiledResumeResponse
```json
{
  "id": 50,
  "resumeProfileId": 50,
  "name": "Java Backend Resume",
  "targetRole": "Senior Java Developer",
  "template": "PROFESSIONAL",
  "sections": [
    {
      "sectionType": "SUMMARY",
      "displayOrder": 1,
      "visible": true,
      "content": {
        "text": "Executive summary..."
      }
    },
    {
      "sectionType": "EXPERIENCE",
      "displayOrder": 2,
      "visible": true,
      "content": {
        "items": [
          {
            "id": 10,
            "companyName": "Tech Corp",
            "jobTitle": "Backend Engineer",
            "employmentType": "FULL_TIME",
            "location": "Remote",
            "startDate": "2021-01-01",
            "endDate": null,
            "currentlyWorking": true,
            "description": "Java & Spring microservices",
            "displayOrder": 1
          }
        ]
      }
    }
  ]
}
```

## Summary Resolution Logic
1. If `ResumeProfile.summaryOverride` is non-null and non-blank $\rightarrow$ use `summaryOverride.trim()`.
2. Else if `CareerProfile.professionalSummary` exists $\rightarrow$ use `professionalSummary`.
3. Else $\rightarrow$ return `null`.

## Section & Item Ordering
- **Section Order**: Controlled strictly by `ResumeSection.displayOrder ASC`.
- **Item Order**: Controlled strictly by selection table `displayOrder ASC` (`ResumeExperience.displayOrder`, `ResumeEducation.displayOrder`, `ResumeSkill.displayOrder`, `ResumeCertification.displayOrder`, `ResumeProject.displayOrder`).

## Missing Source Record Tolerance
If a selection reference points to an `experienceId`, `educationId`, `skillId`, `certificationId`, or `projectId` that was deleted or archived from the database, the compilation engine safely ignores the missing record and continues assembling the remaining section items without crashing or throwing database exceptions.

## Read-Only Transactions & N+1 Prevention
- Execution runs inside `@Transactional(readOnly = true)`.
- Source records are batch-fetched via `findAllById(ids)` IN queries.

## Observability
- Micrometer Timer: `devsphere_resume_compilation_duration`
- Micrometer Counter: `devsphere_resume_compilation_total` (tags: `status=success|failure`, `template=professional|modern|minimal`)
