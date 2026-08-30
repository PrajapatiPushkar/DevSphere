# 48. Resume Public Sharing, Access Control & Secure Public Resume Lifecycle

* **Status**: Accepted
* **Impacted Components**: `user-service`, `api-gateway`, `PublicResumeService`, `ResumePublicShareController`, `RedisPublicResumeCache`
* **Date**: 2026-08-30

---

## Context

While Lesson 41 established the public read model for published resumes (`GET /api/v1/public/resumes/{publicResumeId}`), resumes previously lacked an explicit owner-controlled public sharing lifecycle mechanism. A user could not toggle public visibility off, nor rotate their public sharing URL token without mutating internal state.

We needed a secure, production-grade public sharing model that satisfies:
1. **Explicit Owner Control**: Resumes are private by default and require explicit owner enablement (`public_enabled = true`).
2. **Prerequisite Validation**: Public sharing cannot be enabled unless an active `PUBLISHED` resume version exists.
3. **Information Leakage Prevention**: Uniform HTTP 404 responses for invalid tokens, revoked sharing, archived profiles, or un-published versions.
4. **Token Security & Rotation**: Opaque UUID token support with explicit rotation capabilities.
5. **Transactional Consistency & Cache Isolation**: Transaction-aware Redis cache invalidation.

---

## Decision

1. **Database Model Extension**:
   - Added `public_enabled` (`BOOLEAN NOT NULL DEFAULT FALSE`) and `public_enabled_at` (`TIMESTAMP NULL`) columns to `resume_profiles`.
   - Indexed `public_enabled` for fast access control queries.

2. **Public Sharing Lifecycle API**:
   - Owner Endpoints (`/api/v1/resumes/{resumeId}/public/*`):
     - `POST /share`: Validates ownership & published version existence; sets `public_enabled = true`.
     - `POST /revoke`: Validates ownership; sets `public_enabled = false` and evicts cache post-commit.
     - `GET /status`: Returns `PublicShareStatusResponse` containing `publicResumeId`, `publicEnabled`, `publicEnabledAt`, and `shareUrl`.
     - `POST /rotate`: Generates a new secure UUID `publicId` and invalidates old cached links.
   - Public Endpoint (`GET /api/v1/public/resumes/{publicResumeId}`):
     - Unauthenticated access requiring `public_enabled == true` and an active `PUBLISHED` version.

3. **Cache & Resiliency Integration**:
   - Integrated with `TransactionAwareCacheInvalidator` so Redis eviction executes only after database transaction commit.
   - Redis failures fail open to database queries without failing database transactions.

---

## Consequences

* **Positive**:
  - Full developer control over public visibility of published resumes.
  - Complete security against timing attacks and internal ID enumeration via uniform 404 responses.
  - Zero performance impact on database when cache hits occur.
* **Negative / Trade-offs**:
  - Requires explicit call to `/share` after publishing a version for the public link to resolve.
