# Public Resume Metadata & Production Hardening Architecture

This document describes the public resume response contract, metadata generation algorithms, HTTP ETag caching mechanics, security boundaries, and rate-limiting readiness in DevSphere `user-service`.

---

## 1. Executive Summary

Lesson 50 hardens the public resume presentation domain (`GET /api/v1/public/resumes/{publicResumeId}`) for real-world production usage.

The public JSON response payload (`PublicResumeResponse`) is enriched with presentation-ready metadata (`title`, `description`, `publicResumeId`, `publishedVersion`) generated via null-safe, deterministic, and HTML-sanitized algorithms. Internal database identifiers, primary keys, audit metadata, and private timestamps remain strictly excluded.

HTTP conditional requests (`If-None-Match`) and caching headers (`ETag`, `Cache-Control: public, max-age=60, must-revalidate`) are introduced to minimize bandwidth usage and eliminate redundant processing for unchanged public resumes.

---

## 2. Public Resume Response Contract

```json
{
  "title": "Jane Doe — Senior Backend Engineer",
  "description": "Experienced backend developer specializing in Java microservices and distributed systems.",
  "publicResumeId": "c4b12345-6789-4abc-def0-1234567890ab",
  "publishedVersion": 2,
  "name": "Jane Doe",
  "targetRole": "Senior Backend Engineer",
  "template": "PROFESSIONAL",
  "sections": [
    {
      "sectionType": "SUMMARY",
      "displayOrder": 1,
      "visible": true,
      "content": {
        "text": "Experienced backend developer specializing in Java microservices and distributed systems."
      }
    }
  ]
}
```

### Privacy & Boundary Enforcement
- **Included**: Presentation fields, display title, summary description, public token ID, active version number, template, visible sections.
- **Excluded**: `id`, `userId`, `resumeProfileId`, `versionId`, internal sequence primary keys, `createdAt`, `updatedAt`, `publishedAt`, audit metadata, private status fields.

---

## 3. Metadata Generation Algorithms

### Title Generation (`PublicResumeResponse.generateTitle`)
1. Sanitize `name` and `targetRole`: strip HTML tags (`<[^>]*>`), trim, and normalize multiple whitespace characters into single spaces.
2. Resolution rules:
   - Both `name` & `targetRole` present: `"{name} — {targetRole}"`
   - Only `name` present: `"{name} — Resume"`
   - Only `targetRole` present: `"{targetRole} — Resume"`
   - Neither present: `"Professional Resume"`
3. Bounded length: Maximum 255 characters (truncated with `...` if exceeded).

### Description Generation (`PublicResumeResponse.generateDescription`)
1. Locate `SUMMARY` section from compiled sections.
2. Extract plain text content from `CompiledSummaryResponse.text` or string content.
3. Sanitize: strip HTML tags (`<[^>]*>`), replace newlines/tabs with spaces, trim.
4. Fallback: If summary text is missing or blank, use `"Professional resume and career profile."`
5. Bounded length: Maximum 300 characters (truncated with `...` if exceeded).

---

## 4. HTTP ETag & Conditional Requests

```
Client                                  PublicResumeController                      Redis / DB
  │                                                │                                    │
  │─── GET /api/v1/public/resumes/pub-123 ────────►│                                    │
  │                                                │─── Get Public Resume Payload ─────►│
  │                                                │◄── Return PublicResumeResponse ────│
  │                                                │
  │                                      Compute SHA-256 ETag
  │◄── 200 OK + ETag: "abc..." + Body ─────────────│
  │                                                │
  │─── GET /api/v1/public/resumes/pub-123 ────────►│
  │    If-None-Match: "abc..."                     │
  │                                                │─── Get Public Resume Payload ─────►│
  │                                                │◄── Return PublicResumeResponse ────│
  │                                                │
  │                                      Compute SHA-256 ETag (Matches!)
  │◄── 304 Not Modified + ETag: "abc..." ──────────│
```

- **Header**: `ETag: "<sha256-hex-digest>"`
- **Cache-Control**: `public, max-age=60, must-revalidate`
- **Conditional Handling**: When `If-None-Match` matches the computed ETag, the API returns HTTP `304 Not Modified` without serializing or transferring the JSON payload body.
- **Cache Synergy**: Redis caching handles DB isolation (`PublicResumeCache`), while HTTP ETags handle bandwidth and client re-validation.

---

## 5. Security & Rate-Limiting Readiness

- **Uniform 404 Behavior**: Kept 100% consistent with Lesson 49. Invalid token, revoked sharing, missing published version, or archived profile returns uniform HTTP `404 Not Found`.
- **Rate-Limiting Readiness**: High-volume public requests are served from Redis cache (sub-millisecond latency). API Gateway rate-limiting filters (e.g. Spring Cloud Gateway `RequestRateLimiter` with Redis token bucket) can be attached directly to `/api/v1/public/resumes/**` routes in production.
