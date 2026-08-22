# ADR 0005: API Gateway Perimeter JWT Validation

* **Status**: Accepted
* **Date**: 2026-08-22
* **Context**: Establishing a centralized perimeter security boundary at the API Gateway to validate incoming request identity before routing to downstream microservices.

---

## Decision

We will implement reactive **JWT Validation** inside the **API Gateway** (`services/api-gateway`) as a global reactive filter (`JwtAuthenticationFilter`). The Gateway acts as the external security perimeter for all incoming client traffic.

---

## Rationale & Benefits

1. **Centralized Perimeter Security**: Public routes (`/api/v1/auth/register`, `/api/v1/auth/login`, `/actuator/health`) pass through freely, while protected routes (`/api/demo/protected` and future domain routes) are authenticated at the perimeter before hitting internal microservices.
2. **Stateless Request Processing**: The Gateway validates the JWT signature (HS256) and expiration locally without making synchronous database queries or calling the Auth Service for every incoming request.
3. **Reduced Downstream Security Overhead**: Downstream microservices receive pre-authenticated requests containing trusted identity headers (`X-Authenticated-User-Id`), preventing untrusted client header spoofing.
4. **Consistent 401 Responses**: Standardized JSON error responses (`UNAUTHORIZED`, `INVALID_TOKEN`) are returned at the Gateway boundary, hiding internal exceptions or stack traces from external callers.

---

## Security Tradeoffs & Limitations

- **Security-Critical Single Point**: The API Gateway becomes critical security infrastructure. Any misconfiguration or compromise directly impacts perimeter protection.
- **Symmetric Key Sharing**: In this phase, Auth Service and API Gateway share a symmetric secret key (`JWT_SECRET`). Secret rotation requires coordinated environment updates.
- **Perimeter vs. Business Authorization**: The Gateway enforces **authentication** (who the user is), but does NOT perform fine-grained domain **authorization** (what permissions the user has inside a specific microservice).

---

## Future Improvements

- **Asymmetric Signing Keys (RS256 / EdDSA)**: Auth Service signs tokens using a private key; Gateway and downstream services verify using a public key.
- **JSON Web Key Sets (JWKS)**: Dynamic key retrieval and zero-downtime key rotation.
- **Service-to-Service Mutual Auth (mTLS)**: Securing internal service-to-service communication behind the Gateway perimeter.
- **Fine-Grained Role-Based Access Control (RBAC)**: Delegating domain authorization policies to downstream microservices.
