# Architectural Decision Record — 0074 Frontend ↔ Backend Integration

## Status
Accepted

## Date
2026-09-06

## Context
Following the completion of Lessons 71–77, DevSphere required full end-to-end integration and contract verification connecting the React 18 SPA frontend to the microservices backend via the API Gateway (`DEVSPHERE-API-GATEWAY` on port 8080).

## Key Decisions

1. **Gateway-Centric Communication**:
   - All frontend API traffic is routed through the API Gateway on `http://localhost:8080`.
   - Direct service bypass was avoided to enforce centralized rate-limiting, circuit-breaking, and JWT authentication filters.

2. **Frontend Adaptation to Backend Contracts**:
   - Adapted frontend API client interfaces strictly to existing backend REST DTOs (`LoginResponse`, `RegisterResponse`, `UserProfileResponse`, `TaskResponse`, `PageResponse`).
   - Maintained existing status transition endpoints (`/start`, `/complete`, `/reopen`, `/cancel`).

3. **CORS Enablement in API Gateway**:
   - Added standard Spring Cloud Gateway `globalcors` configuration in `config-repo/api-gateway.yml` allowing requests from local development origins (`http://localhost:5173`, `http://localhost:3000`).

4. **JWT Security & Token Lifecycle**:
   - Maintained JWT Bearer token storage in `localStorage` under `devsphere_token`.
   - Propagated token via Axios request interceptor (`Authorization: Bearer <token>`).
   - Centralized 401 Unauthorized token cleanup and unauthenticated state redirection.

5. **No Architectural Scope Creep**:
   - Avoided introducing fake APIs, mock data, refresh-token complexity, or unnecessary backend refactoring.

## Consequences
The DevSphere frontend and backend microservices communicate seamlessly through the API Gateway with verified contract alignment, security token propagation, and error management.
