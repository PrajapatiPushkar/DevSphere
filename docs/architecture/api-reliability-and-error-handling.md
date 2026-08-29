# API Reliability & Production Error Handling Architecture

## Overview
Lesson 43 establishes the **API Reliability & Production Error Handling Architecture** across DevSphere microservices.
The goal of this architectural hardening is to make backend APIs consistent, predictable, trace-correlated, and production-ready when errors occur, while strictly preserving security, IDOR isolation, and zero-information-leakage boundaries.

---

## Standardized Error Response Contract

All REST microservices (`user-service`, `auth-service`, `api-gateway`) return a standardized JSON error response structure when an API call fails:

```json
{
  "timestamp": "2026-08-29T19:20:00.123456Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "code": "VALIDATION_FAILED",
  "message": "title: Title is required",
  "path": "/api/v1/goals",
  "errors": {
    "title": "Title is required"
  },
  "traceId": "352cfdaf789ac6a5ee32bb0427f493f5"
}
```

### Contract Properties

| Property | Type | Presence | Description |
|---|---|---|---|
| `timestamp` | String | Mandatory | ISO-8601 UTC timestamp of error occurrence |
| `status` | Integer | Mandatory | HTTP status code (e.g., 400, 401, 403, 404, 409, 500, 503) |
| `error` | String | Mandatory | Standardized HTTP status reason phrase (e.g., `BAD_REQUEST`, `NOT_FOUND`) |
| `code` | String | Mandatory | Machine-readable domain error code (e.g., `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`) |
| `message` | String | Mandatory | Human-readable error summary |
| `path` | String | Mandatory | Target request URI path |
| `errors` | Map<String, String> | Optional | Field-level validation errors (omitted if empty/null via `@JsonInclude(NON_NULL)`) |
| `traceId` | String | Optional | OpenTelemetry W3C trace/correlation identifier extracted from MDC context |

---

## Centralized Exception Hierarchy & HTTP Status Mapping

`GlobalExceptionHandler` handles exception categories across microservices with strict HTTP status mappings:

```
                          Throwable
                              │
       ┌──────────────────────┼──────────────────────┐
       ▼                      ▼                      ▼
Client Request Error    Business Conflict      Unexpected Server Error
(400 Bad Request)       (409 Conflict)         (500 Internal Error)
MethodArgumentNotValid  DuplicatePlannerEntry  Generic Exception / SQL / NPE
ConstraintViolation     DuplicateSkill         [Masked client message,
IllegalArgumentEx       EmailAlreadyExists     full log on server]
```

### Exception Mapping Matrix

| Category | Exception Types | HTTP Status | Error Code | Client Message Strategy |
|---|---|---|---|---|
| Resource Not Found | `ResourceNotFoundException` | 404 NOT_FOUND | Exception `code` / `RESOURCE_NOT_FOUND` | Explicit non-leaking entity message |
| Unauthenticated | `UnauthorizedException`, Unauth Filter | 401 UNAUTHORIZED | `UNAUTHORIZED` / `INVALID_TOKEN` | "Authentication is required" |
| Authorization Denied | `AccessDeniedException` | 403 FORBIDDEN | `FORBIDDEN` | "You do not have permission to access this resource" |
| Validation Failure | `MethodArgumentNotValidException`, `ConstraintViolationException` | 400 BAD_REQUEST | `VALIDATION_FAILED` | Structured field-level error map in `errors` |
| Malformed Payload | `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` | 400 BAD_REQUEST | `MALFORMED_REQUEST` / `INVALID_PARAMETER` | Clear payload formatting guidance |
| Invalid Business Input | `IllegalArgumentException`, `IllegalStateException` | 400 BAD_REQUEST | `BAD_REQUEST` | Business rule validation message |
| Duplicate / Conflict | `DuplicatePlannerEntryException`, `DuplicateDsaProblemException`, `DuplicateSkillException`, `DuplicateResumeSelectionException`, `EmailAlreadyExistsException` | 409 CONFLICT | Exception `code` / `EMAIL_ALREADY_EXISTS` | Specific resource conflict details |
| Unsupported Method | `HttpRequestMethodNotSupportedException` | 405 METHOD_NOT_ALLOWED | `METHOD_NOT_ALLOWED` | Unsupported HTTP verb indicator |
| Gateway Fallback | Resilience4j CircuitBreaker fallback | 503 SERVICE_UNAVAILABLE | `SERVICE_UNAVAILABLE` | Downstream availability message |
| Unexpected Failure | `Exception.class` | 500 INTERNAL_SERVER_ERROR | `INTERNAL_SERVER_ERROR` | Masked: "An unexpected internal error occurred" |

---

## Security Guardrails & Zero Information Leakage

### 1. Unexpected Exception Masking (500 Guardrail)
Internal exceptions (e.g. `SQLException`, `NullPointerException`, `HibernateException`, system file paths) are intercepted by the generic `Exception` handler.
- **Client Payload**: Masked generic message (`"An unexpected internal error occurred"`).
- **Server Logs**: Full exception stack trace logged at `ERROR` level with active `traceId`.

### 2. Preserved IDOR & Resource Hiding (404 Boundary)
Attempting to access non-owned entities (e.g., another user's goal, task, planner entry, or resume profile) continues returning `404 NOT_FOUND` rather than `403 FORBIDDEN`, preventing resource existence disclosure.

### 3. Log Sanitization
Operational logs log context using structured SLF4J MDC without logging:
- Passwords or plain-text credentials
- JWT bearer tokens or signature keys
- Authorization headers
- Database connection strings or secrets

---

## Traceability & Log Correlation

Every error response automatically includes a W3C OpenTelemetry `traceId` when present in the thread MDC context (`MDC.get("traceId")`). This allows site reliability engineers to immediately correlate client-side API error reports with server-side tracing tools (Zipkin / Jaeger / Grafana Tempo).
