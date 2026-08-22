# User Service Architecture Document

## Overview

The **User Service** owns authenticated application profile information in DevSphere. It is an independently deployable microservice running on port `8082` backed by its own dedicated database (`devsphere_user`).

```
[ Client ]
    │
    ▼
[ API Gateway ] (Port 8080)
    │  - Validates Bearer JWT
    │  - Strips client X-Authenticated-User-Id
    │  - Injects verified X-Authenticated-User-Id: <userId>
    ▼
[ User Service ] (Port 8082)
    │  - Reads trusted identity header
    │  - Manages profile persistence
    ▼
[ User Database ] (MySQL: devsphere_user)
```

---

## Microservice Domain Boundaries

| Boundary | Auth Service (`:8081`) | User Service (`:8082`) |
|---|---|---|
| **Domain** | Credentials & Authentication | Profile & Application Identity |
| **Database** | `devsphere_auth` | `devsphere_user` |
| **Table** | `users` | `user_profiles` |
| **Data Owned** | `id`, `email`, `password_hash`, `created_at` | `user_id`, `first_name`, `last_name`, `display_name`, `bio`, `phone_number` |
| **Security Role** | Password hashing, verification, JWT signing | Reads trusted `X-Authenticated-User-Id` propagated by Gateway |

---

## Architectural Principles & Rules

1. **Zero Database Table Sharing**: Auth Service and User Service MUST NOT share database schemas, tables, or foreign keys.
2. **Identity Correlation**: `user_profiles.user_id` acts as the foreign identity reference originating from Auth Service's canonical user ID.
3. **No Direct Microservice Database Queries**: User Service must never connect to `devsphere_auth`, and Auth Service must never connect to `devsphere_user`.
4. **Gateway Identity Trust Boundary**:
   - External clients cannot supply `/users/{userId}` to query arbitrary user profiles.
   - User Service trusts only identity propagated via `X-Authenticated-User-Id` header from API Gateway.
   - Direct public access to port `8082` is prohibited in production deployments.
