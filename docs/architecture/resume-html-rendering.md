# Resume HTML Rendering Architecture

## Overview
The Resume HTML Rendering subsystem transforms compiled resume DTO representations (`CompiledResumeResponse`) into deterministic, semantic HTML documents. It establishes a clear boundary between compilation ("What data belongs in this resume?") and visual rendering ("How should that data be displayed?").

## Data & Processing Flow

```
Authenticated Client (Browser / Gateway)
       │
       ▼
GET /api/v1/resumes/{resumeId}/render/html
       │
       ▼
ResumeRenderingController
       │
       ▼
ResumeRenderingService.renderHtmlResume(resumeId, userId)
       │
       ├── 1. ResumeCompilationService.compileResume(resumeId, userId)
       │      └─ [Validates ownership, queries DB, returns CompiledResumeResponse]
       │
       └── 2. HtmlResumeRenderer.render(compiledResume)
              ├─ [Zero DB calls - Pure in-memory transformation]
              ├─ [HTML Entity Escaping & Safe URL Sanitization]
              ├─ [Injects Embedded Print-Friendly CSS]
              └─ Returns Complete HTML String (<!DOCTYPE html>...)
```

## Security & XSS Protection
- **HTML Entity Escaping**: Converts `<`, `>`, `&`, `"`, `'` to HTML entities (`&lt;`, `&gt;`, `&amp;`, `&quot;`, `&#39;`) for all user-provided fields.
- **URL Scheme Filtering**: Validates link protocols before rendering `<a href="...">` elements. Only `http://` and `https://` schemes are permitted. Unsafe URIs (`javascript:`, `data:`, `vbscript:`) are suppressed.

## Template Strategy & Styling
The renderer injects a self-contained `<style>` block supporting template variations:
- **`PROFESSIONAL`**: Traditional typography with deep navy accents (`#1e3a8a`).
- **`MODERN`**: Modern font stack with vibrant teal section headers (`#0d9488`).
- **`MINIMAL`**: Monospace monochrome layout with subtle border lines.
- **Print Optimization**: Includes `@media print` rules for clean paper/PDF browser printouts.

## Observability Metrics
- **Counter**: `devsphere_resume_render_total` (tags: `status=success|failure`, `format=html`, `template=professional|modern|minimal`).
- **Timer**: `devsphere_resume_render_duration`.
