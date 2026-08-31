# 51. Production Error Handling & API Error Contract

* **Status**: Accepted
* **Impacted Components**: `user-service`, `auth-service`, `api-gateway`
* **Date**: 2026-08-31

---

## Context

As DevSphere expands, disparate error responses across microservices and API gateway components can lead to integration friction for frontend clients, poor diagnostic visibility during operational incidents, and potential exposure of sensitive internal infrastructure details (e.g. SQL exceptions, stack traces, Redis connection state).

We needed a standardized, enterprise-grade error contract and diagnostic foundation that guarantees:
1. Consistent, machine-readable JSON error payloads across all public and internal APIs.
2. Stable, client-friendly error codes (`code`) mapped to clear HTTP status codes.
3. Correlation-aware diagnostics that propagate distributed trace IDs (`traceId`) into error responses.
4. Strict security guardrails preventing leakage of database errors, connection strings, credentials, or stack traces.
5. Unified gateway error propagation that prevents framework-generated HTML error pages.

---

## Decision

1. **Standardized API Error Response Contract**:
   - Standardized error structure: `{ timestamp, status, error, code, message, path, errors (optional), traceId }`.
   - Guaranteed formatting across `user-service`, `auth-service`, and `api-gateway`.

2. **Error Code Standardization**:
   - Field validation failures map to HTTP 400 with `code = "VALIDATION_ERROR"` and structured `errors` map.
   - Authentication failures map to HTTP 401 with `code = "UNAUTHORIZED"` or `"INVALID_CREDENTIALS"` / `"INVALID_TOKEN"`.
   - Authorization failures map to HTTP 403 with `code = "FORBIDDEN"`.
   - Resource non-existence maps to HTTP 404 with `code = "RESOURCE_NOT_FOUND"`.
   - Resilience failures map to HTTP 503 (`code = "DOWNSTREAM_SERVICE_UNAVAILABLE"`, `"BULKHEAD_LIMIT_EXCEEDED"`) or HTTP 504 (`code = "DOWNSTREAM_TIMEOUT"`).
   - Unhandled runtime failures map to HTTP 500 with `code = "INTERNAL_SERVER_ERROR"`.

3. **Distributed Trace Correlation**:
   - All handled errors safely extract the active correlation ID (`MDC`, `X-Trace-Id`, or `traceparent`) and populate the `traceId` field in the response body.
   - `X-Trace-Id` headers remain echoed in HTTP response headers.

4. **Security Shielding**:
   - Internal exception messages containing SQL keywords, stack traces, or connection strings are caught by `GlobalExceptionHandler` and masked under generic client-safe messages.
   - `traceId` is excluded from Micrometer metric tags to prevent metric cardinality explosion.

5. **Gateway Error Standardization**:
   - Updated `JwtAuthenticationFilter`, `DistributedRateLimiterGatewayFilterFactory`, and `FallbackController` in `api-gateway` to return standard JSON payloads.
   - Introduced WebFlux `GlobalErrorWebExceptionHandler` to catch unhandled gateway errors and prevent raw Spring HTML responses.

---

## Consequences

* **Positive**:
  - Consistent client error handling experience with stable error codes and field validation maps.
  - Faster incident diagnosis using correlation `traceId` in error responses.
  - Zero leakage of sensitive database schemas, stack traces, or connection credentials.
* **Negative / Trade-offs**:
  - Small maintenance obligation to ensure future endpoints and custom exceptions conform to the standard error contract.
