# 70. Frontend Authentication & Onboarding Integration

* Status: Accepted
* Date: 2026-09-04

## Context and Problem Statement

Following the establishment of the modern frontend foundation and design system in Lesson 71, DevSphere requires full frontend integration with the existing Spring Boot backend authentication services (`auth-service` and `user-service`). The frontend must connect login (`POST /api/v1/auth/login`) and registration (`POST /api/v1/auth/register`) endpoints, manage JWT sessions, guard protected console routes (`/dashboard`, `/tasks`, etc.), provide real-time form validation, and handle error states gracefully.

## Decision Drivers

* Strict adherence to existing backend contracts (`LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`, `UserProfileResponse`).
* Client-side JWT session handling using `localStorage` token storage and automatic `Authorization: Bearer <accessToken>` header injection in Axios API requests.
* Protected routing using `ProtectedRoute` component to prevent unauthorized access to application console pages.
* Password visibility toggles, client-side validation, and backend error message translation (e.g. `Invalid email or password`, `An account with this email already exists`).
* Password reset deferral: Explicitly defer password recovery UI since `auth-service` currently does not provide a forgot-password API.

## Considered Options

1. **Direct Integration with Existing Backend REST APIs (Selected)**
2. Client-only Mock Authentication Server
3. Redesigning Backend Authentication System

## Decision Outcome

Option 1 was chosen. The frontend integrates directly with the existing Spring Boot microservice REST endpoints without altering any backend logic.

### Key Architectural Choices

1. **Token & Session Handling**:
   - `AuthContext` checks `localStorage.getItem('devsphere_token')` on startup and calls `GET /api/v1/users/me`.
   - On 401 unauthenticated response, `apiClient` response interceptor automatically purges invalid tokens.

2. **Protected Route Boundary**:
   - `ProtectedRoute` inspects `isAuthenticated` and `isLoading`.
   - Unauthenticated users attempting to visit protected routes are redirected to `/login` with `state: { from: location }`.
   - Authenticated users visiting `/login` or `/register` are redirected to `/dashboard`.

3. **Password Recovery Deferral**:
   - Password reset functionality is intentionally deferred until backend password recovery APIs are introduced in future iterations.

## Security Considerations

* Zero secrets or plain-text passwords are saved in frontend state or environment variables.
* Tokens are stored in `localStorage` and attached only to requests targeting `VITE_API_BASE_URL`.
* Form inputs are validated on the client side before sending HTTP requests to reduce unnecessary backend server load.

## Consequences

* Positive: Smooth, production-quality user onboarding and authentication workflow connecting React frontend to Spring Boot microservices.
* Positive: Type-safe DTO contracts between frontend TypeScript models and backend Java models.
* Negative: `localStorage` token storage is susceptible to XSS if third-party scripts are injected (mitigated by strict Content Security Policy headers).
