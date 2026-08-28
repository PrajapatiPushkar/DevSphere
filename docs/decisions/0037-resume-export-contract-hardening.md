# ADR 0037: Resume Export Quality & Contract Hardening

## Status
Accepted

## Context
Following Lessons 34–37 (Resume Compilation Engine, HTML Renderer, PDF Renderer, and DOCX Renderer), DevSphere supports multi-format resume generation. However, each export endpoint handled headers, filename sanitization, error responses, size validation, and observability independently.

To ensure production-grade reliability and API stability across all formats, the export contract required unified orchestration and contract hardening.

## Decision
We introduce `ResumeExportService` as a clean export orchestration service in `user-service` and standardize export behavior across HTML, PDF, and DOCX formats.

### Core Architecture & Rules:
1. **Unified Export Orchestration**: `ResumeExportService` manages compilation, format dispatching, size guardrail validation, filename sanitization, and metric recording.
2. **Renderer Separation**: Renderers (`HtmlResumeRenderer`, `OpenHtmlToPdfResumeRenderer`, `ApachePoiDocxResumeRenderer`) remain presentation-only and consume `CompiledResumeResponse` directly. No intermediate HTML -> PDF -> DOCX format conversions are permitted.
3. **Single Compilation Execution**: Data is compiled once per export request. Renderers do not query database repositories.
4. **Header & Filename Standardization**:
   - `HTML`: `text/html;charset=UTF-8` (Preview, no attachment header)
   - `PDF`: `application/pdf` with `Content-Disposition: attachment; filename="<name>.pdf"`
   - `DOCX`: `application/vnd.openxmlformats-officedocument.wordprocessingml.document` with `Content-Disposition: attachment; filename="<name>.docx"`
5. **Configurable Export Guardrails**: Configured `app.resume.export.max-size-bytes: 10485760` (10MB default). Empty or oversized document generations are rejected with HTTP 400.
6. **Security & IDOR Isolation**: Cross-user non-owned resume requests return HTTP `404 Not Found`. Unauthenticated requests return HTTP `401 Unauthorized`.

### Explicit Exclusions:
This decision explicitly does NOT include:
- AI/LLM resume generation
- ATS scoring or job matching
- External job platform integrations
- Resume tailoring
- Asynchronous document generation
- Kafka export events
- S3 or persistent document storage
- Redis export caching
- New microservices

## Consequences
- **Positive**: Consistent, production-hardened API contracts for HTML preview and PDF/DOCX downloads; unified filename security and error responses; comprehensive test coverage.
- **Trade-offs**: Synchronous byte generation limits single-request payload sizes; document bytes are generated in memory and returned immediately without persistence.
