# Resume PDF Rendering & Export Architecture

This document describes the design, implementation, security policy, and operational semantics of the PDF Resume Rendering and Export engine in DevSphere (`user-service`).

## Pipeline Architecture

The resume export pipeline follows a strict, single-direction flow with explicit boundary separation:

```
Career Data
    ↓
Resume Profile
    ↓
Resume Compilation Engine (ResumeCompilationService)
    ↓
CompiledResume (CompiledResumeResponse)
    ↓
HTML Renderer (HtmlResumeRenderer)
    ↓
HTML Document
    ↓
PDF Renderer (OpenHtmlToPdfResumeRenderer)
    ↓
PDF Bytes
```

### Responsibility Breakdown
1. **Compilation (`ResumeCompilationService`)**: Determines "What belongs in the resume?" by aggregating user career data according to active selections, section ordering, and visibility filters.
2. **HTML Rendering (`HtmlResumeRenderer`)**: Determines "How is the resume represented visually as HTML?" generated from the compiled response with selected CSS templates.
3. **PDF Rendering (`OpenHtmlToPdfResumeRenderer`)**: Determines "How is the HTML resume exported into PDF bytes?" without accessing database entities, repositories, JPA, Redis, Kafka, or network resources.

---

## PDF Library Selection & Rationale

- **Library**: `OpenHTMLtoPDF` (`com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10`)
- **Test PDF Parser**: `Apache PDFBox` (`org.apache.pdfbox:pdfbox:2.0.24`)

### Why OpenHTMLtoPDF was Selected
1. **In-Process Java Execution**: Pure Java library executing directly within the Spring Boot JVM without requiring external headless browser binaries (e.g., Playwright, Puppeteer, Chromium).
2. **Standard CSS & Page Media Support**: Parses W3C standard HTML5/CSS2.1+ and supports page formatting via `@page` rules (`@page { size: A4 portrait; margin: 15mm; }`).
3. **Strict Resource Isolation**: Can be executed with restricted resource resolvers (baseUri = `null`), preventing SSRF and arbitrary local file system access.
4. **Fast Mode Efficiency**: Utilizes Jsoup parsing to convert clean HTML into a W3C DOM tree, producing lightweight PDFs in milliseconds.

---

## Security & Resource Isolation Policy

1. **Strict HTML Pre-Escaping**: All user content (names, job titles, descriptions) is HTML-escaped by `HtmlResumeRenderer` prior to PDF conversion.
2. **XSS Payload Protection**: Script tags or injected HTML tags (`<script>alert(1)</script>`) are escaped into plain text string literals in the PDF document rather than evaluated as markup.
3. **URL Sanitization**: Unsafe URL schemes (`javascript:`, `data:`, `file:`) are stripped during HTML generation so they are never converted into clickable PDF links.
4. **No External Network/File Fetching**: `PdfRendererBuilder` operates with null baseUri to prevent resolving remote HTTP/HTTPS resources, localhost endpoints, or `file://` URIs.
5. **Filename Security**: `ResumeFilenameSanitizer` cleans input resume names before injecting into HTTP `Content-Disposition` attachment headers:
   - Removes path separators (`/`, `\`)
   - Removes control characters (`\r`, `\n`, `\0`, `\t`)
   - Strips consecutive dot sequences (`..`) to prevent path traversal
   - Enforces a 100-character maximum length limit
   - Fallback to `resume.pdf` if empty or invalid

---

## PDF Document Configuration & Page Breaks

- **Format**: Default A4 Portrait (`size: A4 portrait; margin: 15mm;`).
- **Page Break Rules**: CSS print rules (`page-break-inside: avoid; break-inside: avoid;`) prevent splitting section headings and individual experience, education, certification, or project item blocks across page boundaries unnecessarily.

---

## PDF Metadata Policy

- PDF metadata title is derived strictly from the developer's resume name (`<title>`).
- Sensitive internal data (User IDs, database primary keys, JWT tokens, system internal details) are explicitly omitted from PDF metadata.

---

## Observability & Operational Semantics

### Metrics
- **Counter**: `devsphere_resume_pdf_export_total`
  - Tags: `status=success|failure`, `format=pdf`, `template=<professional|modern|minimal>`
- **Timer**: `devsphere_resume_pdf_export_duration`

### Logging & Memory
- PDF rendering logs operational context (`resumeId`, `userId`, size in bytes).
- PDF byte arrays are generated directly into memory (`ByteArrayOutputStream`) and returned in the HTTP response body without persisting generated PDF files to local disk, database, S3, or Redis cache.
