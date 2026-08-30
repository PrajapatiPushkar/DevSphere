# Public Resume Sharing & Access Control Architecture

This document describes the public resume sharing lifecycle, security boundaries, access control model, and cache invalidation mechanics in DevSphere `user-service`.

---

## 1. Executive Summary

DevSphere enables software engineers to share their published resume publicly via an opaque, unguessable URL (`GET /api/v1/public/resumes/{publicResumeId}`) without exposing private user identifiers, internal database primary keys, or unpublished draft state.

Public sharing requires explicit owner authorization (`public_enabled == true`). When public sharing is enabled, the public endpoint dynamically resolves the profile's active `PUBLISHED` resume version and returns a presentation-safe public DTO.

---

## 2. Public Sharing Lifecycle & State Machine

```
              ┌──────────────────────────────────────────────┐
              │                 PRIVATE                      │
              │         (public_enabled = false)            │
              └──────────────────────┬───────────────────────┘
                                     │
                 POST /api/v1/resumes/{resumeId}/public/share
              (Requires active PUBLISHED version)
                                     │
                                     ▼
              ┌──────────────────────────────────────────────┐
              │                  PUBLIC                      │
              │          (public_enabled = true)             │
              └──────────────────────┬───────────────────────┘
                                     │
                 POST /api/v1/resumes/{resumeId}/public/revoke
                                     │
                                     ▼
              ┌──────────────────────────────────────────────┐
              │                 PRIVATE                      │
              │         (public_enabled = false)            │
              └──────────────────────────────────────────────┘
```

### Deterministic Lifecycle Rules

1. **Default Privacy**: All newly created resume profiles are `PRIVATE` (`public_enabled = false`).
2. **Pre-requisite for Enablement**: Enabling public sharing (`POST /api/v1/resumes/{resumeId}/public/share`) is rejected with HTTP `400 Bad Request` if no active `PUBLISHED` resume version exists.
3. **Active Version Dynamic Resolution**: The public URL resolves whichever version is currently marked `PUBLISHED`. When the owner publishes a newer version, the public URL automatically presents the new published version.
4. **Immediate Revocation**: Disabling public sharing (`POST /api/v1/resumes/{resumeId}/public/revoke`) immediately renders the public URL invalid (HTTP `404 Not Found`) and evicts cached entries from Redis.
5. **Token Rotation**: The owner can rotate the opaque identifier (`POST /api/v1/resumes/{resumeId}/public/rotate`). This immediately invalidates the old public identifier and generates a fresh cryptographically strong UUID.

---

## 3. Security & Information Leakage Prevention

### Uniform 404 Not Found Strategy
To prevent enumeration, timing attacks, and information leakage, `GET /api/v1/public/resumes/{publicResumeId}` returns an identical `404 Not Found` (`PUBLIC_RESUME_NOT_FOUND`) response for:
- Invalid or unknown `publicResumeId`
- Public sharing disabled (`public_enabled = false`)
- Archived resume profile
- Missing active published version

### Opaque Public Identifiers
Public links utilize server-generated UUID v4 tokens (`public_id`) backed by `java.security.SecureRandom`. The public API contract never accepts internal database sequence IDs or private version IDs.

### IDOR Protection
Owner management endpoints (`/api/v1/resumes/{resumeId}/public/*`) strictly enforce JWT authentication and verify that the authenticated user matches the resume profile owner. Attempting to query or modify another user's public sharing status returns `404 Not Found` to prevent ID enumeration.

---

## 4. Redis Caching & Transaction Isolation

Public resume lookups use the cache key `public-resume:{publicResumeId}` with a default TTL of 10 minutes.

```
Request ──► PublicResumeCache (Redis) ──[ HIT ]──► PublicResumeResponse
                 │
             [ MISS ]
                 │
                 ▼
         Database Lookup & Auth Check
                 │
                 ▼
         Compile Published Snapshot
                 │
                 ▼
      Cache Put & Return Response
```

### Post-Commit Cache Invalidation
To guarantee cache consistency without risking database transaction rollbacks:
- Evictions use `TransactionAwareCacheInvalidator.executeAfterCommit(...)`.
- Invalidation triggers:
  - Revoking public sharing
  - Rotating public token
  - Publishing a new resume version
  - Archiving a published resume version
  - Archiving or deleting a resume profile
- Redis network errors or outages do not cause primary database transactions to fail.

---

## 5. Micrometer Metrics

Operational metrics track public resume access and management:

- `devsphere_public_resume_access_total`: Tags `status=success|not_found|failure` and `cache=hit|miss`.
- `devsphere_public_resume_sharing_total`: Tag `action=enable|revoke|rotate`.
- `devsphere_public_resume_access_duration`: Access latency timer.
