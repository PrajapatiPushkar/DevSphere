# Public Resume Access Tracking & Analytics Architecture

This document describes the design, privacy mechanisms, asynchronous view event recording, data model, Redis caching, IDOR protection, and observability metrics of DevSphere's **Public Resume Access Tracking & Analytics Engine**.

---

## 1. Executive Summary

Lesson 54 introduces a production-grade **Public Resume Access Tracking & Analytics Engine** across `user-service`.

When public resumes are viewed by third parties (e.g. recruiters, hiring managers, public web visitors via `/api/v1/public/resumes/{publicId}`), DevSphere records view access events asynchronously to ensure zero read performance degradation. Profile owners gain access to privacy-compliant analytics APIs displaying view counts, unique visitors, access trends over time, and top referral channels.

```text
               Public Client View Request
                           │
             ┌─────────────▼─────────────┐
             │  PublicResumeController   │
             └─────────────┬─────────────┘
                           │ Returns HTTP 200 / 304 (Fast Read)
                           │ Publishes PublicResumeViewEvent (Asynchronous)
             ┌─────────────▼─────────────┐
             │ ApplicationEventPublisher │
             └─────────────┬─────────────┘
                           │ @Async @EventListener
             ┌─────────────▼─────────────┐
             │PublicResumeAnalyticsServ. │
             └─────────────┬─────────────┘
                           │ Hashing: Salt + IP (SHA-256)
                           │ Referrer Domain Sanitization
                           │
  ┌────────────────────────┴────────────────────────┐
  │                                                 │
┌─▼──────────────────────┐               ┌──────────▼─────────────┐
│public_resume_view_logs │               │ Redis Analytics Cache  │
│      (MySQL DB)        │               │   (5-Min TTL Evict)    │
└────────────────────────┘               └────────────────────────┘
```

---

## 2. Asynchronous View Tracking

1. **Non-Blocking Execution**:
   - `PublicResumeService.getPublicResume` publishes a `PublicResumeViewEvent` containing client IP (`X-Forwarded-For` or `remoteAddr`), `Referer`, `User-Agent`, `publicId`, `resumeProfileId`, and timestamp.
   - `PublicResumeAnalyticsService.onPublicResumeView` handles events asynchronously using Spring `@Async` and `@EventListener`.
   - Logging or persistence errors in analytics processing NEVER block or fail the main public HTTP response thread.

2. **GDPR / Privacy Compliance**:
   - Raw IP addresses are NEVER stored in the database.
   - Client IPs are salted and hashed using SHA-256 (`ipHash = SHA-256("devsphere-privacy-salt-2026:" + ip)`).
   - `ipHash` is used to calculate `countDistinctIpHashByResumeProfileId` for unique visitor metrics without violating user privacy laws.

3. **Referrer & User-Agent Sanitization**:
   - `Referer` headers are parsed to extract normalized host domains (e.g., `linkedin.com`, `github.com`, `twitter.com`, or `direct`).
   - `User-Agent` strings are truncated to max 500 characters to prevent SQL payload overflow.

---

## 3. Database Schema & Flyway Migration (`V17`)

```sql
CREATE TABLE public_resume_view_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL,
    resume_profile_id BIGINT NOT NULL,
    accessed_at TIMESTAMP NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    referrer VARCHAR(512),
    user_agent VARCHAR(512),
    CONSTRAINT fk_prvl_resume_profile FOREIGN KEY (resume_profile_id) REFERENCES resume_profiles (id) ON DELETE CASCADE
);

CREATE INDEX idx_prvl_public_id ON public_resume_view_logs (public_id);
CREATE INDEX idx_prvl_profile_access ON public_resume_view_logs (resume_profile_id, accessed_at);
```

---

## 4. Owner Analytics API & IDOR Guardrails

### Endpoints

- `GET /api/v1/resumes/{resumeId}/analytics`
- `GET /api/v1/resumes/{resumeId}/public/analytics`

### Response Payload

```json
{
  "resumeId": 10,
  "publicId": "pub-uuid-12345",
  "totalViews": 142,
  "uniqueVisitors": 89,
  "lastAccessedAt": "2026-08-31T05:20:00Z",
  "viewsByDay": {
    "2026-08-01": 5,
    "2026-08-02": 12,
    "2026-08-31": 15
  },
  "topReferrers": {
    "linkedin.com": 68,
    "direct": 45,
    "github.com": 29
  }
}
```

### Security & IDOR Verification

- Requests require `X-Authenticated-User-Id` header (populated by API Gateway JWT authentication).
- `PublicResumeAnalyticsService.getResumeAnalytics` verifies `ResumeProfile.findByIdAndUserId(resumeId, userId)`.
- If the resume profile does not exist or belong to the requesting caller, HTTP 404 `RESOURCE_NOT_FOUND` is thrown to prevent resource enumeration.

---

## 5. Caching & Invalidation Strategy

- **Redis Key**: `public-resume-analytics:{resumeId}`
- **TTL**: 5 minutes (`app.cache.public-resume-analytics-ttl: 5m`).
- **Transaction-Aware Post-Commit Eviction**:
  - Evicted asynchronously when a new view event is recorded.
  - Evicted post-commit when public sharing is revoked or public token rotated via `TransactionAwareCacheInvalidator`.

---

## 6. Observability & Micrometer Metrics

- **Counters**:
  - `devsphere_public_resume_views_total{status="recorded"}`
  - `devsphere_public_resume_views_total{status="error"}`
  - `devsphere.cache.hits.total{cache="public_resume_analytics"}`
  - `devsphere.cache.misses.total{cache="public_resume_analytics"}`
- **Tag Cardinality Guardrail**: `publicId`, `ipHash`, `resumeId`, and `userId` are strictly prohibited as metric tags.
