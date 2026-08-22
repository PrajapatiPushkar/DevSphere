# ADR 0004: Stateless JWT Authentication

* **Status**: Accepted
* **Date**: 2026-08-22
* **Context**: Establishing a secure, stateless authentication mechanism for user sessions across DevSphere microservices.

---

## Decision

We will use **JSON Web Tokens (JWT)** signed with **HMAC SHA-256 (HS256)** for stateless user authentication.

---

## Rationale & Benefits

1. **Stateless Scalability**: The Auth Service issues cryptographically signed JWT tokens upon successful login (`POST /api/v1/auth/login`). Downstream services and API Gateway can verify tokens independently without querying a central session database.
2. **Standardized Claims**:
   - `sub`: Stores the stable numeric User ID (`Long`).
   - `email`: User email address for quick claim extraction.
   - `iat`: Token issuance timestamp.
   - `exp`: Expiration timestamp (default: 3600 seconds / 1 hour).
3. **Decoupled Security**: Downstream services only require the public or shared signing key to validate incoming tokens.

---

## Security Considerations

- **Secret Management**: `JWT_SECRET` must be supplied via environment variables and MUST be at least 256 bits (32 characters) long for HS256 compliance. Hardcoded secrets in source control are strictly forbidden.
- **Fail-Fast Validation**: `JwtService` validates secret length on application startup and fails fast if the secret is insecure or missing.
- **Minimal Claims Payload**: Passwords, password hashes, and sensitive personal information are strictly excluded from JWT payload claims.
- **Generic Error Responses**: Failed authentication attempts return `401 UNAUTHORIZED` with a generic message (`Invalid email or password`) to prevent user enumeration.

---

## Tradeoffs & Future Evolution

- **Token Revocation Tradeoff**: Stateless JWT tokens cannot be revoked instantly before expiration without a distributed token blacklist (e.g. Redis).
- **Future Improvements**:
  - Introduction of Refresh Tokens for session extension without long-lived access tokens.
  - API Gateway JWT validation filter for edge-level security before forwarding requests to internal services.
