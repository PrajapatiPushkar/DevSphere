# DevSphere Frontend Authentication Architecture

This document specifies the technical design, API interaction contracts, token lifecycle, protected route navigation, error handling, and security boundaries for the DevSphere Frontend Authentication and Onboarding system.

---

## 1. Authentication Interaction Flow

```text
  [ User ]
     │
     ├──────► Sign In (/login)  ───► POST /api/v1/auth/login ─────► [ Auth Service ]
     │                                                                   │
     │   ◄─── Returns accessToken ◄──────────────────────────────────────┘
     │            │
     │            ▼
     │       Store token in localStorage
     │            │
     │            ▼
     ├──────► GET /api/v1/users/me (Header: Authorization: Bearer <token>) ──► [ User Service ]
     │                                                                               │
     │   ◄─── Returns User Profile ◄─────────────────────────────────────────────────┘
     │            │
     │            ▼
     └──────► Navigate to /dashboard (/protected)
```

---

## 2. API Contracts & Interfaces

### 1. Login Request & Response
- **Endpoint**: `POST /api/v1/auth/login` (Auth Service)
- **Request Body**:
  ```json
  {
    "email": "user@devsphere.io",
    "password": "SecurePassword123!"
  }
  ```
- **Response (HTTP 200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

### 2. Register Request & Response
- **Endpoint**: `POST /api/v1/auth/register` (Auth Service)
- **Request Body**:
  ```json
  {
    "email": "newuser@devsphere.io",
    "password": "SecurePassword123!"
  }
  ```
- **Response (HTTP 201 Created)**:
  ```json
  {
    "id": 101,
    "email": "newuser@devsphere.io",
    "createdAt": "2026-09-04T05:00:00Z"
  }
  ```

### 3. Current User Profile
- **Endpoint**: `GET /api/v1/users/me` (User Service)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Response (HTTP 200 OK)**:
  ```json
  {
    "id": 101,
    "userId": 101,
    "email": "newuser@devsphere.io",
    "displayName": "New User",
    "role": "USER"
  }
  ```

---

## 3. Session & Token Lifecycle

1. **Storage**: On successful authentication, the returned JWT string is stored in `localStorage` under key `devsphere_token`.
2. **Request Attachment**: `src/services/apiClient.ts` inspects `localStorage` on every outbound HTTP request and appends `Authorization: Bearer <token>`.
3. **Session Restoration**: On app initialization, `AuthProvider` reads `devsphere_token` and executes `authService.getCurrentUser()`. If valid, session is restored; if invalid or expired (401/403), token is removed and user remains unauthenticated.
4. **Logout**: Executing `logout()` removes `devsphere_token` from `localStorage` and resets `AuthContext` state to `{ user: null, token: null, isAuthenticated: false }`.

---

## 4. Protected Routing Strategy

The `ProtectedRoute` wrapper enforces access boundaries:
- **Unauthenticated Navigation**: Users attempting to access `/dashboard`, `/tasks`, `/resumes`, etc., without a valid session are redirected to `/login` with `state: { from: location }`.
- **Post-Login Redirect**: Upon successful authentication, the user is navigated directly back to their target `from` route (defaulting to `/dashboard`).
- **Authenticated Access to Auth Screens**: Authenticated users opening `/login` or `/register` are automatically redirected to `/dashboard`.

---

## 5. Password Reset Deferral

`auth-service` currently implements authentication registration and login endpoints. Password recovery / forgot-password functionality is **intentionally deferred** until backend password reset APIs are developed in a future lesson.
