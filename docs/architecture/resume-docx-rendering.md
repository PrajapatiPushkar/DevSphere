# Resume DOCX Rendering & Export Architecture

## 1. Responsibilities & Objectives

The DOCX rendering engine in `user-service` produces production-quality, editable Microsoft Word (`.docx`) documents directly from compiled resume data.

Key objectives:
- Direct rendering from deterministic `CompiledResumeResponse`
- Strict independence from HTML and PDF renderers
- Generation of fully editable OOXML Word documents (`XWPFDocument`)
- Support for `PROFESSIONAL`, `MODERN`, and `MINIMAL` templates
- Robust security: XSS prevention (plain-text treatment) and safe URL hyperlinking
- Operational observability via Micrometer counters and timers

---

## 2. Compilation vs HTML vs PDF vs DOCX Architecture

DevSphere decouples resume data compilation from output format rendering:

```
Career Data (Experience, Education, Skills, Certifications, Projects)
                        ↓
                 Resume Profile
                        ↓
            Resume Compilation Engine
                        ↓
              CompiledResumeResponse
                        ↓
      ┌─────────────────┼─────────────────┐
      ↓                 ↓                 ↓
HTML Renderer     PDF Renderer      DOCX Renderer
      ↓                 ↓                 ↓
HTML Preview       PDF Export        DOCX Export
```

### Architectural Principles:
1. **Single Source of Truth**: `CompiledResumeResponse` guarantees identical resume content regardless of output format.
2. **Format Independence**: DOCX rendering does NOT rely on HTML-to-DOCX conversion or PDF-to-DOCX conversion.
3. **No Cross-Renderer Dependencies**: HTML, PDF, and DOCX renderers can evolve or be replaced independently.

---

## 3. Apache POI Selection Rationale & Limitations

### Selected Library:
- **`org.apache.poi:poi-ooxml:5.2.5`**

### Selection Rationale:
- **Industry Standard**: Pure Java library for reading and writing Office Open XML (OOXML) documents.
- **In-Memory Generation**: Direct document stream construction (`ByteArrayOutputStream`) with minimal footprint.
- **Native Editability**: Generates real Word paragraph (`w:p`), run (`w:r`), and hyperlink (`w:hyperlink`) XML nodes, ensuring users can open and edit the document in Microsoft Word, Google Docs, or LibreOffice.
- **No External Daemon Required**: Operates completely in-process without requiring headless LibreOffice or MS Word installations.

### DOCX vs HTML/PDF Layout Limitations:
- **Flow Layout vs Fixed Layout**: DOCX relies on Microsoft Word's reflow engine; pixel-exact placement is not achievable across different Word viewers.
- **CSS Incompatibility**: Modern CSS flexbox/grid and custom borders cannot be directly mapped; POI paragraph styling and native borders are used instead.
- **Font Rendering Differences**: Font substitution depends on client system fonts.

---

## 4. DOCX Layout Strategy & Template Styling

### Layout Configuration:
- **Page Size**: A4 portrait (`11906` twips width x `16838` twips height).
- **Margins**: Managed via `DocxTemplateStyle` (e.g. `1080` twips = 0.75 in for Professional/Modern; `1440` twips = 1.0 in for Minimal).

### Centralized Template Styling (`DocxTemplateStyle`):
| Template | Document Font | Heading Font | Title Size | Primary Color | Secondary Color |
|---|---|---|---|---|---|
| **PROFESSIONAL** | Calibri | Calibri | 22 pt | `#1E40AF` (Deep Blue) | `#1F2937` (Dark Charcoal) |
| **MODERN** | Segoe UI | Segoe UI | 24 pt | `#0D9488` (Teal) | `#0F766E` (Dark Teal) |
| **MINIMAL** | Courier New | Courier New | 20 pt | `#374151` (Dark Gray) | `#4B5563` (Gray) |

---

## 5. Security & Safety Model

### XSS & Plain-Text Protection:
DOCX is an XML-based format, not HTML. All user-supplied strings are treated as plain text and passed to `XWPFRun.setText(text)`. Apache POI automatically XML-escapes text nodes, preventing injection of malicious markup or script execution.

### Safe URL Hyperlinks:
- Only `http://` and `https://` schemes are rendered as clickable Word hyperlinks (`XWPFHyperlinkRun`).
- Dangerous schemes (`javascript:`, `file:`, `data:`, `vbscript:`) are suppressed to prevent protocol handler exploits.

### Filename Sanitization:
Filenames are sanitized via `ResumeFilenameSanitizer`:
- Removes path separators (`/`, `\`), control characters, and dot sequences (`..`).
- Strips existing extensions before enforcing a 100-character maximum length.
- Enforces `.docx` extension.

---

## 6. Memory & Performance Considerations

- DOCX generation uses in-memory `ByteArrayOutputStream` wrapped in Java `try-with-resources`.
- `XWPFDocument` instances are closed promptly to release POI XML objects.
- Documents are generated synchronously on-demand and are not persisted to database, local disk, S3, or Redis cache.
