# Frontend ↔ Backend Integration Architecture

This document describes the end-to-end integration architecture, API Gateway routing, authentication flows, JWT propagation, REST endpoint contracts, and error handling mechanisms connecting the React frontend with the DevSphere backend microservices.

---

## 1. System Topology

```text
React 18 SPA (Vite)
  │ (http://localhost:5173)
  │  Axios Client + JWT Interceptor
  ▼
Spring Cloud API Gateway
  │ (http://localhost:8080)
  │  CORS + Rate Limiting + Circuit Breakers + JwtAuthenticationFilter
  │
  ├──► Auth Service (DEVSPHERE-AUTH-SERVICE on port 8082)
  │      ├── POST /api/v1/auth/login
  │      └── POST /api/v1/auth/register
  │
  └──► User Service (DEVSPHERE-USER-SERVICE on port 8081)
         ├── GET  /api/v1/users/me
         ├── PUT  /api/v1/users/me
         ├── GET  /api/v1/tasks
         ├── POST /api/v1/tasks
         ├── PUT  /api/v1/tasks/{id}
         ├── DELETE /api/v1/tasks/{id}
         └── PATCH /api/v1/tasks/{id}/{start|complete|reopen|cancel}
```

---

## 2. Environment Configuration

- **Frontend API Base URL**: Configured via Vite environment variable `VITE_API_BASE_URL` (defaulting to `http://localhost:8080`).
- **Development `.env` File**: `frontend/.env` sets `VITE_API_BASE_URL=http://localhost:8080`.
- **CORS Configuration**: API Gateway config (`config-repo/api-gateway.yml`) specifies `globalcors` rules permitting `http://localhost:5173` and `http://localhost:3000` with methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`).

---

## 3. Authentication & JWT Security Workflow

1. **Login & Registration**:
   - `POST /api/v1/auth/login` accepts `{ email, password }` and returns `{ accessToken, tokenType: "Bearer", expiresIn }`.
   - `POST /api/v1/auth/register` accepts `{ email, password }` and returns `{ id, email, createdAt }`.
2. **Token Persistence**:
   - Frontend stores the JWT in `localStorage` under `devsphere_token`.
3. **Request Interceptor (`apiClient.ts`)**:
   - Every outgoing HTTP request injects `Authorization: Bearer <token>` into the headers.
4. **Gateway Token Validation & Header Forwarding**:
   - `JwtAuthenticationFilter` on `api-gateway` validates the token using secret `JWT_SECRET`.
   - Upon successful verification, the Gateway forwards security metadata downstream in headers:
     - `X-Authenticated-User-Id`
     - `X-Authenticated-User-Roles`
5. **Session Initialization & 401 Expiration**:
   - On app startup, `AuthContext` executes `checkAuth()` by calling `GET /api/v1/users/me`.
   - If an HTTP 401 Unauthorized occurs, `apiClient` interceptor removes `devsphere_token` from `localStorage` and resets `AuthContext` state to unauthenticated, redirecting the user to `/login`.

---

## 4. Protected Domain APIs

### User Domain
- `GET /api/v1/users/me`: Fetches authenticated user profile (`UserProfileResponse`).
- `PUT /api/v1/users/me`: Updates user profile metadata (`UpdateUserProfileRequest`).

### Task Management Domain
- `GET /api/v1/tasks`: Lists tasks with pagination (`page`, `size`, `sort`) and filters (`status`, `priority`). Returns `PageResponse<TaskResponse>`.
- `POST /api/v1/tasks`: Creates a task (`CreateTaskRequest`).
- `PUT /api/v1/tasks/{id}`: Updates a task (`UpdateTaskRequest`).
- `DELETE /api/v1/tasks/{id}`: Archives/deletes a task.
- `PATCH /api/v1/tasks/{id}/start`: Transitions status to `IN_PROGRESS`.
- `PATCH /api/v1/tasks/{id}/complete`: Transitions status to `COMPLETED`.
- `PATCH /api/v1/tasks/{id}/reopen`: Transitions status to `TODO`.
- `PATCH /api/v1/tasks/{id}/cancel`: Transitions status to `CANCELLED`.
