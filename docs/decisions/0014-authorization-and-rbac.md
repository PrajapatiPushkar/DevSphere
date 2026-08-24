# 14. Production Authorization and Role-Based Access Control (RBAC)

Date: 2026-08-24

## Status

Accepted

## Context

In previous lessons, DevSphere introduced authentication (JWT generation in `auth-service` and JWT validation in `api-gateway`). However, authentication alone answers *"Who are you?"*, but does not answer *"What are you allowed to do?"*.

Without role-based authorization and resource ownership verification:
1. Any authenticated user could access or mutate any other user's private data simply by altering resource IDs in API paths (`/api/v1/users/123` -> `/api/v1/users/456`).
2. There was no distinction between standard users and system administrators.
3. If a request bypassed the API Gateway directly to an internal microservice port, microservices lacked independent authorization enforcement.

## Decision

We introduce a production-grade Role-Based Access Control (RBAC) and Defense-in-Depth Authorization architecture:

1. **Role Model**: Minimalist `USER` and `ADMIN` roles stored in `auth-service` database and issued as `roles` claim in JWT payload.
2. **Server-Side Assignment**: Public registration server-side assigns `USER` role. Clients cannot self-assign `ADMIN` role.
3. **Authority Convention**: Consistent mapping from DB/JWT roles (`USER`, `ADMIN`) to Spring Security GrantedAuthorities (`ROLE_USER`, `ROLE_ADMIN`).
4. **Gateway Perimeter Defense**: Gateway validates JWT, strips untrusted client headers (`X-Role`, `X-User-Role`, `X-Admin`), enforces coarse route access, and forwards trusted identity context downstream.
5. **Independent Microservice Validation**: `user-service` integrates Spring Security for independent JWT validation, populating `SecurityContextHolder`. Direct service access cannot bypass authorization.
6. **Domain Ownership Enforcement**: Endpoints verifying resource ownership (`authenticatedUserId == targetUserId OR ROLE_ADMIN`) via Spring `@PreAuthorize` method-level security.
7. **HTTP Security Semantics**: Strict 401 Unauthorized (unauthenticated) vs 403 Forbidden (authenticated with insufficient privileges) status codes and safe error payloads.

## Consequences

### Positive

- **Defense in Depth**: Gateway and domain microservices independently enforce security.
- **Resource Protection**: Ownership checks prevent horizontal privilege escalation.
- **Clear Security Semantics**: Standardized 401 vs 403 responses remove ambiguity for clients.
- **No Sensitive Leakage**: JWTs carry minimal identity claims without passwords, secrets, or internal database metadata.

### Negative / Tradeoffs

- **Validation Overhead**: Microservices perform independent JWT signature verification.
- **Configuration Maintenance**: Security filters and Spring Security bean configurations across microservices must remain synchronized.
