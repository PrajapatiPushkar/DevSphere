# Resume Export Quality & Contract Hardening Architecture

## Overview
Lesson 38 standardizes and hardens the multi-format resume export API contract in DevSphere `user-service`. The export layer unifies HTTP response contracts, header disponsitions, filename sanitization, guardrails, and observability across HTML, PDF, and DOCX document formats, while preserving renderer independence.

## Architecture & Data Flow

```
Career Data
    ↓
Resume Profile
    ↓
Resume Compilation Engine (ResumeCompilationService)
    ↓
CompiledResumeResponse
    ↓
ResumeExportService (Orchestration & Guardrails)
    ├── HTML Renderer (HtmlResumeRenderer)             → text/html;charset=UTF-8 (Preview)
    ├── PDF Renderer  (OpenHtmlToPdfResumeRenderer)    → application/pdf (Attachment)
    └── DOCX Renderer (ApachePoiDocxResumeRenderer)    → application/vnd.openxmlformats-... (Attachment)
```

## Unified Export Contracts

| Export Format | HTTP Method & URL | Content-Type | Content-Disposition | Behavior |
|---|---|---|---|---|
| **HTML** | `GET /api/v1/resumes/{id}/render/html` | `text/html;charset=UTF-8` | None | Inline HTML preview |
| **PDF** | `GET /api/v1/resumes/{id}/render/pdf` | `application/pdf` | `attachment; filename="<sanitized>.pdf"` | Download PDF file |
| **DOCX** | `GET /api/v1/resumes/{id}/render/docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | `attachment; filename="<sanitized>.docx"` | Download editable Word file |

## Key Architectural Principles

1. **Single Compilation Snapshot**:
   - Resume compilation (`resumeCompilationService.compileResume(resumeId, userId)`) executes exactly once per export request. Renderers receive a deterministic `CompiledResumeResponse` snapshot and remain purely presentation-focused without database access.

2. **Unified Export Orchestration**:
   - `ResumeExportService` encapsulates common export logic: format selection, size validation, filename sanitization, header resolution, and Micrometer metrics.

3. **Format Independence**:
   - Format renderers (`HtmlResumeRenderer`, `OpenHtmlToPdfResumeRenderer`, `ApachePoiDocxResumeRenderer`) remain completely independent. Renderers consume `CompiledResumeResponse` directly without converting between document formats.

4. **Sanitized Filenames**:
   - `ResumeFilenameSanitizer` strips control characters, directory separators, path traversal dot sequences (`..`), script tags, and Windows drive letters (`C:`). Enforces 100-character maximum base name lengths and format-appropriate extensions (`.html`, `.pdf`, `.docx`).

5. **Size Guardrails**:
   - Defensive maximum document size guardrail configured via `app.resume.export.max-size-bytes` (default `10485760` bytes / 10MB). Requests producing empty output or exceeding limits are rejected with HTTP 400.

6. **IDOR & Security Isolation**:
   - Requests for non-owned resumes return HTTP `404 Not Found` (rather than 403), preventing unauthorized resource enumeration. Unauthenticated requests are rejected with HTTP `401 Unauthorized`.

7. **Observability & Safe Operational Logging**:
   - Micrometer counter `devsphere_resume_export_total` with tags `status=success|failure`, `format=html|pdf|docx`, `template=professional|modern|minimal`.
   - Micrometer timer `devsphere_resume_export_duration`.
   - Logging records only low-cardinality metadata (resumeId, format, size, filename). Document bytes and personal data are never logged.
