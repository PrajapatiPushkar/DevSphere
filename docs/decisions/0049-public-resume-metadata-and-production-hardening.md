# 49. Public Resume Metadata, Share Preview & Production Hardening

* **Status**: Accepted
* **Impacted Components**: `user-service`, `PublicResumeResponse`, `PublicResumeService`, `PublicResumeController`, `RedisPublicResumeCache`
* **Date**: 2026-08-30

---

## Context

Lesson 49 implemented secure public resume sharing with owner access control and token rotation. However, to support rich share previews (e.g. social meta cards, browser titles, search crawlers) and production-grade HTTP bandwidth efficiency, the public resume representation required deterministic title/description metadata and conditional HTTP ETag caching.

We needed a hardened public resume contract that:
1. Enriches `PublicResumeResponse` with safe presentation metadata (`title`, `description`, `publicResumeId`, `publishedVersion`).
2. Generates deterministic titles (`"{Name} — {Target Role}"`) and plain-text descriptions with HTML sanitization and length bounds.
3. Strictly prevents exposure of internal sequence IDs, database primary keys, or private timestamps.
4. Introduces HTTP `ETag` conditional request handling (`If-None-Match` -> `304 Not Modified`) and `Cache-Control` headers.

---

## Decision

1. **Public DTO Metadata Enrichment**:
   - Extended `PublicResumeResponse` with `title`, `description`, `publicResumeId`, and `publishedVersion`.
   - Implemented null-safe, HTML-stripped, whitespace-normalized helper functions `generateTitle(...)` (max 255 chars) and `generateDescription(...)` (max 300 chars, extracted from `SUMMARY` section with fallback).

2. **HTTP ETag & Conditional Caching**:
   - `PublicResumeController` computes a SHA-256 digest ETag for `GET /api/v1/public/resumes/{publicResumeId}` and attaches `Cache-Control: public, max-age=60, must-revalidate`.
   - Supports `If-None-Match` request header; returns `304 Not Modified` when content is unchanged.

3. **Cache Model Consistency**:
   - Cached objects in `RedisPublicResumeCache` contain only the enriched `PublicResumeResponse`.
   - Preserves transaction-aware cache invalidation (`TransactionAwareCacheInvalidator`) on publish/archive/revoke/rotate events.

---

## Consequences

* **Positive**:
  - Presentation-ready title and preview description for frontend and social sharing.
  - Zero exposure of internal database primary keys or sensitive user state.
  - Significant bandwidth savings via HTTP 304 Not Modified conditional responses.
* **Negative / Trade-offs**:
  - Adds minor payload overhead for metadata strings (`title`, `description`).
