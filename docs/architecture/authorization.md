# Production-Grade Authorization and Role-Based Access Control (RBAC) Architecture

## Overview

DevSphere establishes a clear architectural distinction between **Authentication** and **Authorization**:

- **Authentication**: *"Who are you?"* — Verified via identity credentials in `auth-service` and validated through signed JSON Web Tokens (JWT).
- **Authorization**: *"What are you allowed to do?"* — Enforced through Role-Based Access Control (RBAC) at both the API Gateway perimeter and independent downstream microservices, combined with domain resource ownership checks.

```
Client
  │
  ▼
API Gateway (Port 8080)
  │
  ├── 1. Signature & Expiration Validation
  ├── 2. Header Sanitization (Strip untrusted X-Role, X-User-Role, X-Admin)
  ├── 3. Coarse Route Authorization (e.g., /api/v1/admin/** -> ROLE_ADMIN)
  │
  ▼
Eureka Service Discovery
  │
  ▼
Microservices (e.g., User Service Port 8082)
  │
  ├── 1. Independent JWT Validation (Defense in Depth)
  ├── 2. Spring SecurityContext Population (ROLE_USER, ROLE_ADMIN)
  ├── 3. Method-Level Security (@PreAuthorize)
  ├── 4. Resource Ownership Verification (authenticatedUserId == targetUserId OR ROLE_ADMIN)
  │
  ▼
Database
```

---

## Role Model

DevSphere defines a minimalist production role model:

| Role | Description | Capabilities |
| :--- | :--- | :--- |
| `USER` | Normal authenticated user | Access own user profile, update own data, interact with standard service endpoints. |
| `ADMIN` | System administrator | Access administrative operations, view broader domain profiles, perform system management. |

### Server-Controlled Role Assignment

- Every newly registered user automatically receives the `USER` role server-side.
- Clients CANNOT self-assign the `ADMIN` role via registration requests.
- Admin credentials are managed securely via controlled database initialization (`AdminUserInitializer` for local development/test environments).

---

## JWT Security Claims & Authority Conventions

### JWT Payload Structure

JWTs issued by `auth-service` contain identity and authorization data only:

```json
{
  "sub": "101",
  "email": "user@example.com",
  "roles": ["USER"],
  "iat": 1787534000,
  "exp": 1787537600
}
```

> [!IMPORTANT]
> **Zero Sensitive Data in Tokens:**
> JWTs **never** contain passwords, password hashes, secrets, private tokens, or database internals.

### Authority Mapping Convention

- In Database: `role = "USER"` or `"ADMIN"`.
- In JWT Claim: `roles = ["USER"]` or `["ADMIN"]`.
- In Spring Security: Converted to `GrantedAuthority` as `ROLE_USER` and `ROLE_ADMIN`.

This convention aligns directly with Spring Security's `hasRole('ADMIN')` and `hasRole('USER')` expressions.

---

## Defense in Depth Security Architecture

### 1. API Gateway Layer (Perimeter Security)
- Performs initial token validation (signature, expiration, format).
- Strips any untrusted client-supplied identity/role headers (`X-Role`, `X-User-Role`, `X-Admin`, `X-Authenticated-User-Id`).
- Enforces coarse route-level protection (e.g. non-admin users attempting to reach admin paths receive `403 Forbidden`).
- Forwards validated user context downstream.

### 2. Microservice Layer (Domain Security)
- Microservices (e.g., `user-service`) **independently validate JWT tokens** from the `Authorization: Bearer <token>` header.
- Bypassing the API Gateway to hit microservices directly on internal ports (e.g., port 8082) **cannot bypass authorization**.
- Populates Spring `SecurityContextHolder` with `UserPrincipal` and `GrantedAuthority` list.
- Enforces fine-grained method-level security (`@PreAuthorize`) and resource ownership checks.

---

## Ownership Authorization

Resource ownership checks verify that a user can only read or mutate their own private resources:

```java
// UserSecurity helper bean
public boolean isOwnerOrAdmin(Long targetUserId) {
    Long currentUserId = getCurrentUserId();
    boolean isAdmin = hasRole("ROLE_ADMIN");
    return isAdmin || (currentUserId != null && currentUserId.equals(targetUserId));
}
```

Applied via Spring Security method annotations:

```java
@GetMapping("/{userId}")
@PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
public ResponseEntity<UserProfileResponse> getUserProfileById(@PathVariable("userId") Long userId) { ... }
```

---

## Safe HTTP Security Semantics & Response Payload

### 401 Unauthorized vs 403 Forbidden

| HTTP Status | Reason | Json Response Structure |
| :--- | :--- | :--- |
| **401 Unauthorized** | Unauthenticated (missing, invalid, or expired JWT) | `{"status": 401, "error": "UNAUTHORIZED", "message": "Authentication is required"}` |
| **403 Forbidden** | Authenticated but insufficient role or non-owner access | `{"status": 403, "error": "FORBIDDEN", "message": "You do not have permission to access this resource"}` |

---

## Observability & Security Monitoring

- Failed authorization attempts increment low-cardinality metric `devsphere_auth_authorization_denied_total` with tag `reason` (`unauthenticated` or `forbidden`).
- Metric tags **never** contain high-cardinality values such as `userId`, `email`, or `JWT`.
- Security failures are logged at `WARN` level without leaking token secrets or sensitive parameters.

---

## Future Security Considerations

1. **Token Revocation & Blacklisting**: Distributed Redis token revocation lists.
2. **Refresh Token Lifecycle**: Short-lived access tokens with secure refresh token rotation.
3. **Fine-Grained Permissions**: Transition to permission-based ABAC or policy-driven engines (OPA) when domain complexity requires it.
