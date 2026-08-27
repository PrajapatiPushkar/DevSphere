# ADR 0034: Resume HTML Rendering Foundation

## Status
Accepted

## Date
2026-08-27

## Context
Following Lesson 34's introduction of the `ResumeCompilationEngine`, software developers require a clean rendering layer to view their compiled resumes as HTML documents (`GET /api/v1/resumes/{id}/render/html`). Rendering must remain strictly decoupled from entity persistence and compilation business logic.

## Decision
We introduce the **Resume HTML Rendering Foundation** (`ResumeRenderer` and `HtmlResumeRenderer`) inside `services/user-service`.

### Key Architectural Rationale
1. **Strict Separation of Concerns**:
   $$\text{Career Data} \rightarrow \text{Resume Profile} \rightarrow \text{Compilation Engine} \rightarrow \text{Compiled Model} \rightarrow \text{Resume Renderer} \rightarrow \text{HTML}$$
   The renderer receives an already compiled `CompiledResumeResponse` DTO and performs **zero database repository, JPA, Redis, or network access**.
2. **Deterministic Output**: Rendering the exact same `CompiledResumeResponse` twice yields byte-for-byte identical HTML. Output contains no random UUIDs, dynamic timestamps, or non-deterministic generators.
3. **Template-Aware Styling**: Embedded CSS supports `PROFESSIONAL`, `MODERN`, and `MINIMAL` templates via CSS root classes (e.g., `class="template-modern"`). Includes print-friendly `@media print` rules.
4. **HTML Escaping & XSS Protection**: All user-provided strings (name, target role, summary, descriptions, skill names, URLs) are strictly HTML-escaped.
5. **URL Sanitization**: Clickable links (`<a href="...">`) require explicit `http://` or `https://` protocol schemes. Unsafe URI schemes (`javascript:`, `data:`, `vbscript:`) are not rendered as clickable anchors.
6. **Direct HTML Response**: REST endpoint `GET /api/v1/resumes/{id}/render/html` returns `Content-Type: text/html;charset=UTF-8` directly (no raw JSON wrapping).
7. **IDOR Protection & Ownership**: Inherits strict ownership checks from `ResumeCompilationService`. Non-owned resume requests return HTTP `404 Not Found`.

## What is Intentionally NOT Implemented
- PDF or DOCX file generation.
- AI/LLM resume tailoring or ATS scoring.
- Asynchronous background rendering or Kafka rendering events.
- HTTP or CDN caching of rendered HTML.

## Consequences
### Positive
- Clean architectural boundary allowing future PDF/DOCX renderers to consume `CompiledResumeResponse` independently.
- Robust defense against stored XSS attacks.
- High-performance, memory-only rendering with zero database locks.

### Tradeoffs
- Additional rendering abstraction layer (`com.devsphere.user.renderer.*`).
