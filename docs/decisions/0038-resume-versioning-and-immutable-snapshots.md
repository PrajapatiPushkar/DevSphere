# ADR 0038: Resume Versioning and Immutable Snapshots

## Status
Accepted

## Context
Developers in DevSphere build and compile custom resumes based on their career profile, experience entries, educations, skills, certifications, and developer projects. As developers update their career details over time, previous iterations of exported resumes need to be preserved without risk of retroactively mutating previously submitted or published resumes.

## Decision
1. **JSON Snapshot Storage**: When a user creates a resume version, `ResumeCompilationService` compiles the current resume configuration into `CompiledResumeResponse`, which is serialized as a JSON document into `snapshot_data` in the `resume_versions` table.
2. **Profile-Scoped Version Numbers**: Version numbers increment per `resume_profile_id` starting from 1. A database-level constraint `UNIQUE(resume_profile_id, version_number)` prevents duplicate version numbers.
3. **State Machine & Lifecycle**:
   - `DRAFT`: Newly created version.
   - `PUBLISHED`: Official version snapshot. Cannot be reverted to `DRAFT`.
   - `ARCHIVED`: Soft-archived version snapshot. Cannot be republished.
4. **Decoupled Version Exports**: Version rendering endpoints (`GET /render/html`, `pdf`, `docx`) read from `snapshot_data` rather than live relational career tables, ensuring 100% immutability.
5. **IDOR & Security Isolation**: Ownership checks enforce that users can only query or export resume versions belonging to their authenticated `userId`. Cross-user access returns `404 Not Found`.

## Consequences
- **Positive**: Complete immutability of historical resume versions. Edits to live career profiles or experience items have zero impact on published version snapshots.
- **Positive**: Seamless integration with existing `ResumeRenderer`, `PdfResumeRenderer`, and `DocxResumeRenderer` without duplicating rendering logic.
- **Trade-off**: Storage footprint grows linearly with version snapshots, managed via size guardrails and strict snapshot schema normalization.
