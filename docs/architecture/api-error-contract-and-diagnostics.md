# Production Error Handling, API Error Contract & Correlation-Aware Diagnostics

This document outlines the production-grade error handling architecture, machine-readable error contracts, HTTP status mappings, trace correlation guidelines, gateway error handling, and security rules across DevSphere (`api-gateway`, `user-service`, `auth-service`).

---

## 1. Executive Summary

Lesson 53 hardens DevSphere's error handling architecture to ensure that all APIs expose a consistent, secure, machine-readable error payload structure while retaining distributed trace correlation (`traceId`) for production debugging.

```text
                                 Client
                                   │
                     Standard JSON Error Response
           { timestamp, status, error, code, message, path, traceId }
                                   │
                      ┌────────────▼────────────┐
                      │       API Gateway       │ (TracePropagationGlobalFilter /
                      └────────────┬────────────┘  GlobalErrorWebExceptionHandler)
                                   │
                Propagated X-Trace-Id / W3C traceparent
                                   │
             ┌─────────────────────┴─────────────────────┐
             │                                           │
  ┌──────────▼──────────┐                     ┌──────────▼──────────┐
  │    user-service     │                     │    auth-service     │
  │ GlobalExceptionHdr  │                     │ GlobalExceptionHdr  │
  └─────────────────────┘                     └─────────────────────┘
```

---

## 2. Standard API Error Response Contract

All microservices and the API Gateway produce error responses adhering to the following JSON schema:

### Standard Error Payload Schema

```json
{
  "timestamp": "2026-08-31T05:20:00.000Z",
  "status": 404,
  "error": "NOT_FOUND",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Requested resource was not found",
  "path": "/api/v1/resumes/999",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

### Standard Validation Error Payload Schema

When request field validation fails (e.g. `@Valid` binding or `@Validated` constraint violations), the payload includes field-level details:

```json
{
  "timestamp": "2026-08-31T05:20:00.000Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/auth/register",
  "errors": {
    "email": "must be a valid email address",
    "password": "must be at least 8 characters long"
  },
  "traceId": "1ca04e826e344d7e98ec9ae5f9dfce77"
}
```

### Field Definitions

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `timestamp` | String (ISO-8601) | Yes | UTC timestamp when error was generated. |
| `status` | Integer | Yes | HTTP status code (e.g., 400, 401, 403, 404, 409, 429, 500, 503, 504). |
| `error` | String | Yes | Standard HTTP status name (e.g., `NOT_FOUND`, `BAD_REQUEST`). |
| `code` | String | Yes | Machine-readable, stable error code for client handling. |
| `message` | String | Yes | Safe, sanitized human-readable error description. |
| `path` | String | Yes | Request URI path. |
| `errors` | Map<String, String> | No | Map of invalid field names to error messages (validation failures only). |
| `traceId` | String | No | Active distributed trace / correlation ID for debugging. |

---

## 3. Standard HTTP Status & Error Code Mappings

| HTTP Status | Error Name | Error Code (`code`) | Trigger Condition |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `BAD_REQUEST` | `VALIDATION_ERROR` | Request payload or parameter validation failure (`@Valid`, constraint violations). |
| `400 Bad Request` | `BAD_REQUEST` | `MALFORMED_REQUEST` | Unparseable or malformed JSON payload (`HttpMessageNotReadableException`). |
| `400 Bad Request` | `BAD_REQUEST` | `INVALID_PARAMETER` | Method parameter type mismatch (`MethodArgumentTypeMismatchException`). |
| `400 Bad Request` | `BAD_REQUEST` | `BAD_REQUEST` | Invalid input or argument (`IllegalArgumentException`, `IllegalStateException`). |
| `401 Unauthorized` | `UNAUTHORIZED` | `UNAUTHORIZED` | Unauthenticated request or missing authorization credentials. |
| `401 Unauthorized` | `UNAUTHORIZED` | `INVALID_CREDENTIALS` | Authentication failure (incorrect email/password). |
| `401 Unauthorized` | `UNAUTHORIZED` | `INVALID_TOKEN` | Access token signature invalid or expired JWT. |
| `403 Forbidden` | `FORBIDDEN` | `FORBIDDEN` | Authenticated user lacks required role/permission (`AccessDeniedException`). |
| `404 Not Found` | `NOT_FOUND` | `RESOURCE_NOT_FOUND` | Requested entity or route does not exist (`ResourceNotFoundException`). |
| `405 Method Not Allowed` | `METHOD_NOT_ALLOWED` | `METHOD_NOT_ALLOWED` | HTTP method not supported for targeted endpoint (`HttpRequestMethodNotSupportedException`). |
| `409 Conflict` | `CONFLICT` | `EMAIL_ALREADY_EXISTS` | Registration with pre-existing email address. |
| `409 Conflict` | `CONFLICT` | `DUPLICATE_*` | Unique constraint violation (`DuplicatePlannerEntryException`, `DuplicateSkillException`, etc.). |
| `409 Conflict` | `CONFLICT` | `RESOURCE_VERSION_CONFLICT` | Optimistic locking failure during concurrent updates. |
| `409 Conflict` | `CONFLICT` | `DATABASE_CONSTRAINT_VIOLATION` | DB integrity constraint failure. |
| `409 Conflict` | `CONFLICT` | `LOCK_ACQUISITION_TIMEOUT` | Pessimistic locking failure. |
| `429 Too Many Requests` | `TOO_MANY_REQUESTS` | `RATE_LIMIT_EXCEEDED` | Distributed or local rate limit threshold exceeded (`RequestNotPermitted`). |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | `INTERNAL_SERVER_ERROR` | Unhandled runtime exception (generic exception fallback). |
| `503 Service Unavailable` | `SERVICE_UNAVAILABLE` | `DOWNSTREAM_SERVICE_UNAVAILABLE` | Downstream circuit breaker is OPEN (`CallNotPermittedException`). |
| `503 Service Unavailable` | `SERVICE_UNAVAILABLE` | `BULKHEAD_LIMIT_EXCEEDED` | Bulkhead concurrency queue is full (`BulkheadFullException`). |
| `504 Gateway Timeout` | `GATEWAY_TIMEOUT` | `DOWNSTREAM_TIMEOUT` | Downstream operation timed out (`TimeoutException`). |

---

## 4. Distributed Trace Correlation & Diagnostics

1. **Trace ID Extraction Hierarchy**:
   - Primary: Active MDC context (`traceId` or `trace_id`).
   - Secondary: Request header `X-Trace-Id`.
   - Tertiary: Request header `traceparent` (extracted 128-bit span trace ID).
2. **Response Echoing**:
   - Every error response body echoes `traceId` when present.
   - The API Gateway `TracePropagationGlobalFilter` ensures `X-Trace-Id` is echoed in HTTP response headers.
3. **Metric Cardinality Rule**:
   - `traceId` MUST NEVER be used as a Micrometer tag or Prometheus metric label to prevent memory exhaustion.

---

## 5. Security & Sensitive Information Protection

- **Stack Trace Suppression**: Internal exception stack traces are NEVER serialized into HTTP response bodies.
- **SQL & Infrastructure Shielding**: Raw SQL error messages, database table names, constraint syntax, and Redis/Kafka host names are caught by `GlobalExceptionHandler` and masked under generic messages (e.g. `"An unexpected internal error occurred"`).
- **Credential Masking**: Password hashes, JWT tokens, refresh tokens, and Authorization header strings MUST NEVER be logged or returned in error payloads.
- **Enumeration Prevention**: User profiles or IDOR resources not owned by the caller return standard 404 `RESOURCE_NOT_FOUND` or 403 `FORBIDDEN` without revealing sensitive resource existence.

---

## 6. Gateway Error Handling

- **JWT Denial**: `JwtAuthenticationFilter` intercepts unauthenticated or unauthorized calls and returns standard JSON with 401 `UNAUTHORIZED` / `INVALID_TOKEN` or 403 `FORBIDDEN`.
- **Rate Limit Denial**: `DistributedRateLimiterGatewayFilterFactory` formats 429 `RATE_LIMIT_EXCEEDED` JSON payloads with `Retry-After` header.
- **Service Degradation Fallback**: `FallbackController` handles downstream circuit breaker trips with 503 `DOWNSTREAM_SERVICE_UNAVAILABLE`.
- **Unhandled Gateway Exceptions**: `GlobalErrorWebExceptionHandler` catches reactive WebFlux errors and guarantees JSON output adhering to the standard schema.
