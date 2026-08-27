# ADR 0033: Resume Compilation Engine & Preview Model

## Status
Accepted

## Date
2026-08-27

## Context
Following the creation of `ResumeProfile` configurations and career history selections in Lesson 33, software developers need a deterministic engine to compile these configurations into an immutable, presentation-ready format (`CompiledResumeResponse`). Querying database entities directly during future rendering would couple rendering engines (PDF/DOCX/HTML) to database schemas.

## Decision
We introduce the **Resume Compilation Engine** inside `services/user-service`. We explicitly decide **NOT** to render PDF, DOCX, or HTML documents at this stage, nor introduce AI/LLM resume scoring.

### Key Rationale
1. **Clean Architectural Boundary**: The compilation engine translates database domain entities (`ResumeProfile`, `ResumeSection`, `ResumeExperience`, `Experience`, etc.) into compiled DTOs (`CompiledResumeResponse`, `CompiledResumeSectionResponse`). Future rendering engines will consume this DTO structure exclusively.
2. **Deterministic Pipeline**: Compilation follows a fixed pipeline:
   $$\text{User ID Validation} \rightarrow \text{Load Profile} \rightarrow \text{Load Sections} \rightarrow \text{Filter Invisible Sections} \rightarrow \text{Sort Sections by Display Order} \rightarrow \text{Resolve Source Records} \rightarrow \text{Sort Items by Selection Order}$$
3. **Summary Resolution Priority**: Prefers `ResumeProfile.summaryOverride` if non-blank; falls back to `CareerProfile.professionalSummary`.
4. **Tolerance of Missing Source Records**: If a selected source record (e.g. `Experience`) was deleted, compilation safely skips the missing record without failing or throwing database exceptions.
5. **N+1 Query Avoidance**: Source records are batch-resolved via repository `findAllById(...)` IN queries instead of single-record database calls.
6. **IDOR Isolation & Dual Ownership**: Verifies that both the `ResumeProfile` and resolved source records belong to the authenticated `userId`.
7. **Read-Only Transactions**: `@Transactional(readOnly = true)` guarantees zero database mutations during compilation.

## Architecture & Data Flow
- Endpoint: `GET /api/v1/resumes/{resumeId}/compile`
- Service: `ResumeCompilationService.compileResume(Long resumeId, Long userId)`
- Observability: Micrometer Timer `devsphere_resume_compilation_duration` and Counter `devsphere_resume_compilation_total`.

## Consequences
### Positive
- Clean separation between compilation configuration and presentation rendering.
- Deterministic, high-performance compilation without N+1 query overhead.
- Safe tolerance of deleted or modified source records.
- Zero risk of cross-user IDOR data leakage.

### Tradeoffs
- Additional DTO mapping layer (`com.devsphere.user.dto.compilation.*`).
