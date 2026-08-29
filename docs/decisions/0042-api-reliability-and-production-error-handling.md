# ADR 0042: API Reliability & Production Error Handling

## Status
Accepted

## Context
Across DevSphere's microservices architecture, API failure responses previously varied in JSON structure and error field naming depending on the exception source and layer. To make DevSphere backend APIs production-ready, predictable, trace-correlated, and secure, a standardized error contract and centralized exception handling policy are required.

## Decision
1. **Standardized REST Error Response Contract**:
   - Standardize `ErrorResponse` DTO across microservices with mandatory properties: `timestamp`, `status`, `error`, `code`, `message`, `path`, and optional `errors` (validation map) and `traceId` (OpenTelemetry correlation token).
   - Use `@JsonInclude(NON_NULL)` to ensure clean serialization without empty/null keys.

2. **Centralized Exception Handling Policy**:
   - `ResourceNotFoundException` → HTTP 404 NOT_FOUND (`code`: `RESOURCE_NOT_FOUND` / domain specific code).
   - `UnauthorizedException` / Security EntryPoints → HTTP 401 UNAUTHORIZED (`code`: `UNAUTHORIZED`).
   - `AccessDeniedException` → HTTP 403 FORBIDDEN (`code`: `FORBIDDEN`).
   - Validation Exceptions (`MethodArgumentNotValidException`, `ConstraintViolationException`) → HTTP 400 BAD_REQUEST (`code`: `VALIDATION_FAILED`) with structured field errors map in `errors`.
   - Conflict Exceptions (`DuplicatePlannerEntryException`, `DuplicateDsaProblemException`, `DuplicateSkillException`, `DuplicateResumeSelectionException`, `EmailAlreadyExistsException`) → HTTP 409 CONFLICT (`code`: domain specific code).
   - Malformed Requests (`HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`) → HTTP 400 BAD_REQUEST (`code`: `MALFORMED_REQUEST` / `INVALID_PARAMETER`).
   - Method Not Supported (`HttpRequestMethodNotSupportedException`) → HTTP 405 METHOD_NOT_ALLOWED (`code`: `METHOD_NOT_ALLOWED`).
   - Unhandled Exceptions (`Exception.class`) → HTTP 500 INTERNAL_SERVER_ERROR (`code`: `INTERNAL_SERVER_ERROR`, masked message `"An unexpected internal error occurred"`).

3. **Security & Information Disclosure Guardrails**:
   - Mask internal unexpected exception details (500) from HTTP response payloads while logging full stack traces at `ERROR` level on the server.
   - Preserve IDOR 404 resource-hiding behavior for non-owned entities.
   - Ensure logs never log plain-text passwords, JWT signature keys, bearer tokens, or connection strings.

4. **Trace Correlation**:
   - Automatically attach W3C OpenTelemetry `traceId` from MDC context to error responses.

## Consequences
- Backend REST APIs present a consistent, predictable error contract to frontend clients and API Gateway.
- Operational debugging is streamlined by correlating API error payloads with server log traces via `traceId`.
- System security is hardened against information disclosure and resource enumeration attacks.
