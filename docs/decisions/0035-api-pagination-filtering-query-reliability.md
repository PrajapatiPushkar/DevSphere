# ADR 0035: API Pagination, Filtering & Query Reliability Hardening

## Status
Accepted

## Date
2026-08-29

## Context
Following Lesson 43's establishment of standardized API error handling, existing collection/list endpoints (Goals, Tasks, Planner Entries, DSA Problems, Developer Projects) required production hardening to ensure bounded result sets, deterministic sorting, valid parameter handling, query efficiency, and strict IDOR security protection.

## Decision
We establish standard API pagination, filtering, and query reliability rules across all collection endpoints in `services/user-service`.

### Key Architectural Rationale
1. **Standardized Response Contract (`PageResponse<T>`)**:
   Collection endpoints return a unified response schema:
   ```json
   {
     "content": [...],
     "page": 0,
     "size": 20,
     "totalElements": 125,
     "totalPages": 7,
     "first": true,
     "last": false
   }
   ```
   Both `page`/`size` and `pageNumber`/`pageSize` are supported for JSON clarity and backward compatibility.

2. **Bounded Page Size Guardrails**:
   Page sizes are bounded between `1` and `100` (`DEFAULT_SIZE = 20`, `MAX_PAGE_SIZE = 100`). Requesting `size <= 0` or `size > 100` triggers HTTP `400 Bad Request`.

3. **Strict Parameter Validation & Error Handling**:
   - `page < 0`: Throws `IllegalArgumentException` ("Page index must not be negative") $\rightarrow$ HTTP `400 Bad Request`.
   - `size <= 0`: Throws `IllegalArgumentException` ("Page size must be greater than zero") $\rightarrow$ HTTP `400 Bad Request`.
   - `size > 100`: Throws `IllegalArgumentException` ("Page size must not exceed 100") $\rightarrow$ HTTP `400 Bad Request`.
   - Invalid sort field: Throws `IllegalArgumentException` ("Invalid sort field: ...") $\rightarrow$ HTTP `400 Bad Request`.
   - Invalid sort direction: Throws `IllegalArgumentException` ("Invalid sort direction: ...") $\rightarrow$ HTTP `400 Bad Request`.
   All parameter errors return the Lesson 43 standardized `ErrorResponse` schema.

4. **Sort Field Allowlists & Deterministic Ordering**:
   Sorting parameter (`?sort=field,direction`) is validated against domain-specific field allowlists (e.g. `createdAt`, `title`, `status`, `id`). If primary sort is non-ID, a secondary sort of `id DESC` is automatically appended to prevent non-deterministic page splits.

5. **Database-Level Execution & Indexing**:
   - Query filtering, sorting, and pagination happen entirely at SQL level (`WHERE`, `ORDER BY`, `LIMIT / OFFSET`) via Spring Data JPA `Pageable` and `Specification`. No in-memory entity filtering.
   - Composite table indexes added for frequent filter/sort fields (`user_id, status`, `user_id, created_at`, `user_id, planned_date`).
   - Removed hardcoded `query.orderBy(...)` inside JPA specifications to allow `Pageable` `Sort` to dynamically drive database ordering.

6. **User Scoping & IDOR Safety**:
   All collection queries enforce mandatory `userId` equality predicates (`WHERE user_id = ?`). Changing page index, page size, sort parameters, or filters can never expose records of another authenticated user.

7. **Empty Collection Behavior**:
   Empty result sets return HTTP `200 OK` with `content: []`, `totalElements: 0`, `totalPages: 0`, `first: true`, `last: true` rather than `404 Not Found`.

## Consequences
### Positive
- Production-safe collection endpoints protected against memory exhaustion and full-table scans.
- Consistent sorting, filtering, and pagination contract across all domain endpoints.
- Full IDOR security guarantees intact under arbitrary combinations of query parameters.

### Tradeoffs
- Rejection of invalid page parameters (HTTP 400) requires clients to strictly conform to page limits (`size <= 100`).
