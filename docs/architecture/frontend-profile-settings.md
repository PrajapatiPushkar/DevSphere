# Frontend Profile & Settings Architecture

This document describes the architectural implementation of **Lesson 75 — Profile & Settings** in DevSphere.

---

## 1. Overview

Lesson 75 adds a full-featured Developer Profile (`/profile`) and Settings (`/settings`) console experience consuming existing REST endpoints provided by `user-service` and `auth-service`.

---

## 2. Architecture & Data Flow

```
[ User Browser ]
       │
       ├──> ProtectedRoute + AppLayout
       │        │
       │        ├──> ProfilePage
       │        │       ├──> ProfileHeader (Edit & Refresh actions)
       │        │       ├──> ProfileAvatar (Initials fallback & photo policy)
       │        │       ├──> ProfileStats (Total/Completed tasks, completion velocity)
       │        │       ├──> ProfileInformation (2-column responsive credentials)
       │        │       └──> EditProfileModal (Validates inputs, submits PUT)
       │        │
       │        └──> SettingsPage
       │                └──> SettingsLayout (Tabbed sidebar)
       │                        ├──> AccountSettings (Personal info, read-only email)
       │                        ├──> SecuritySettings (JWT token security governance)
       │                        ├──> PreferencesSettings (Page size, compact view, toasts)
       │                        └──> AppearanceSettings (Theme switcher: Dark/Light/System)
       │
       └──> userService & taskService
                ├──> GET /api/v1/users/me
                ├──> PUT /api/v1/users/me
                └──> GET /api/v1/tasks
```

---

## 3. Backend REST Capabilities Integration

| Capability | Backend Endpoint | Status | UI Handling |
| :--- | :--- | :--- | :--- |
| **Get My Profile** | `GET /api/v1/users/me` | Fully Supported | Loads `UserProfileResponse` into `ProfilePage` & `AuthContext`. |
| **Update Profile** | `PUT /api/v1/users/me` | Fully Supported | `EditProfileModal` and `AccountSettings` submit validated payloads. |
| **Avatar Image Upload** | N/A | Unsupported | Renders polished initials fallback with Gravatar/GitHub image error handling. |
| **Email Modification** | N/A (In auth-service) | Read-Only | Email displayed with informational badge explaining central auth policy. |
| **Password Change** | N/A (Self-service disabled) | Informational | Security section displays JWT token policies and session active status. |
| **Theme / Preferences** | N/A | Frontend LocalStorage | Instant DOM class toggling and `devsphere_theme` / `devsphere_user_preferences` persistence. |

---

## 4. AuthContext Synchronization

When profile fields are updated in `ProfilePage` or `SettingsPage`:
1. `userService.updateMyProfile(payload)` issues a REST call to `user-service`.
2. Upon success, `updateUser(updatedFields)` is called on `AuthContext`.
3. Header user avatar/initials, display name, and sidebar profile status sync immediately across all pages without requiring a full browser refresh.

---

## 5. Security & Accessibility Controls

- **Zero Plaintext Password Exposure**: Password inputs/storage are strictly excluded.
- **XSS & Injection Protection**: Inputs validated client-side with regex and character length constraints matching backend validation annotations.
- **Accessibility**: Semantic HTML headings, labeled form controls, keyboard navigation (ESC key to dismiss modals), focus outlines, and responsive breakpoints.
