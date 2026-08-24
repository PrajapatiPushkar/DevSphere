# 17. Distributed Rate Limiting and API Protection with Redis

Date: 2026-08-24

## Status

Accepted

## Context

DevSphere is a distributed microservices platform with traffic entering through the API Gateway (`DEVSPHERE-API-GATEWAY`). In a multi-instance deployment (Gateway 1, Gateway 2, Gateway 3), single-client abuse, credential stuffing on auth routes, or runaway client loops can consume entire Gateway capacity and degrade service for all users.

Local in-memory rate counters are insufficient because global limits would be effectively multiplied by the number of running Gateway instances. To enforce consistent, platform-wide API protection, DevSphere requires distributed rate limiting backed by a shared data store.

## Decision

We adopt **Redis-backed Spring Cloud Gateway Rate Limiting** with customized key resolution and resilient filter execution:

1. **Distributed Token Bucket Enforcement**:
   - Utilize Spring Cloud Gateway's token-bucket algorithm executed via atomic Redis Lua scripts.
   - Separate rate-limiting state (`rate_limit:*`) logically from user profile cache (`user_profile:*`).

2. **Context-Aware Rate Limit Key Strategy**:
   - **Authenticated Routes**: Derive keys from validated user identity (`rate_limit:user:{userId}`). Identity is extracted strictly from `X-Authenticated-User-Id` populated by `JwtAuthenticationFilter` after JWT cryptographic verification.
   - **Public & Unauthenticated Routes**: Derive keys from client IP (`rate_limit:ip:{normalizedIp}`).
   - **Security Guarantee**: Un-authenticated requests or spoofed client headers (`X-Authenticated-User-Id`, `X-Role`) are stripped before authentication, preventing key spoofing attacks.

3. **Endpoint-Specific Policies**:
   - Apply strict limits for sensitive authentication endpoints (`/api/v1/auth/register`, `/api/v1/auth/login`) to prevent brute force and registration abuse.
   - Apply generous per-user limits for protected API endpoints (`/api/v1/users/**`).
   - Preserve unrestricted access for health probes (`/actuator/health`) and Prometheus scrapers (`/actuator/prometheus`).

4. **Standardized 429 Response & Retry-After**:
   - Return HTTP status `429 Too Many Requests` with `Retry-After: 1` header and a clean JSON body (`status`, `error`, `message`) without exposing internal Redis keys or bucket state.

5. **Configurable Fail-Open / Fail-Closed Policy**:
   - Execute Redis operations with bounded timeouts (`2000ms`).
   - Default local development to `app.rate-limit.fail-open=true` to maintain application availability when Redis is unreachable.
   - Allow production environments to configure `app.rate-limit.fail-open=false` when infrastructure protection takes priority.

6. **Observability & Low-Cardinality Rules**:
   - Expose Prometheus metrics: `devsphere_rate_limit_requests_total{result="allowed|rejected|error"}` and `devsphere_rate_limit_rejected_total{route="..."}`.
   - Enforce low-cardinality label rules (never tag by user ID, email, IP, or JWT).
   - Inject OpenTelemetry trace span attribute `rate_limit.result`.

## Consequences

### Positive
- **Multi-Instance Consistency**: Centralized Redis counters guarantee identical limit enforcement regardless of which Gateway instance receives the request.
- **Early Protection**: Rejects abusive traffic at the Gateway before touching downstream microservices or databases.
- **Security & Integrity**: Spoofed identity headers cannot manipulate rate limit keys.

### Negative / Trade-offs
- **Redis Path Dependency**: Redis becomes an operational dependency on the Gateway request path.
- **Tuning Requirement**: Burst capacities and refill rates require ongoing load testing and capacity planning.

## Future Extensions
- Introduce dynamic quota management per tier (e.g. standard vs enterprise API limits).
- Integrate edge/CDN WAF protection for volumetric DDoS mitigation.
