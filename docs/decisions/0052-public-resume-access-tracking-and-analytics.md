# 52. Public Resume Access Tracking & Analytics Engine

* **Status**: Accepted
* **Impacted Components**: `user-service`
* **Date**: 2026-08-31

---

## Context

With public resume sharing enabled in DevSphere, developers share public profile links with recruiters, prospective employers, and peers. However, resume owners previously had no visibility into view performance, visitor engagement, access trends, or traffic referral sources.

We needed a scalable, production-grade tracking and analytics engine that fulfills four core principles:
1. **Asynchronous Non-Blocking Execution**: Access logging MUST NOT slow down public resume HTTP GET read response times.
2. **User Privacy & GDPR Compliance**: Raw client IP addresses MUST NOT be stored in plain text.
3. **IDOR & Security Protection**: Analytics metrics MUST be accessible strictly to the resume owner.
4. **Cache & Performance Isolation**: Aggregated analytics queries MUST NOT saturate primary database connection pools under high read load.

---

## Decision

1. **Asynchronous Event Driven Architecture**:
   - `PublicResumeService.getPublicResume` publishes `PublicResumeViewEvent` containing request metadata (`clientIp`, `referrer`, `userAgent`, `publicId`, `resumeProfileId`).
   - `PublicResumeAnalyticsService` processes events in an `@Async @EventListener` thread pool, persisting records to `public_resume_view_logs` table (Flyway migration `V17`).

2. **Privacy-Preserving Salted IP Hashing**:
   - Client IP addresses are salted and hashed using SHA-256 (`ipHash`) prior to storage.
   - Unique visitor counts are calculated using `COUNT(DISTINCT ip_hash)` without storing raw IP strings.

3. **Owner-Facing Analytics API**:
   - Exposed endpoints `GET /api/v1/resumes/{resumeId}/analytics` and `GET /api/v1/resumes/{resumeId}/public/analytics`.
   - Enforced strict IDOR checks matching `resume_profile_id` and `user_id`.

4. **Redis Analytics Caching**:
   - Cached aggregated `PublicResumeAnalyticsResponse` objects in Redis (`public-resume-analytics:{resumeId}`) with 5-minute TTL.
   - Utilized `TransactionAwareCacheInvalidator` for post-commit eviction on token rotation or access status updates.

5. **Observability**:
   - Tracked low-cardinality Micrometer metrics (`devsphere_public_resume_views_total{status="recorded|error"}`).

---

## Consequences

* **Positive**:
  - Developers receive real-time visibility into public resume performance, unique visitors, and referral sources.
  - Zero performance degradation on public resume reads due to `@Async` decoupled logging.
  - 100% GDPR compliant privacy protection via salted SHA-256 IP hashing.
* **Negative / Trade-offs**:
  - Requires database storage for `public_resume_view_logs` records (mitigated by cascading cleanup and composite indexes).
