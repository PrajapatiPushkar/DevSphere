# Auth Service Architecture Document

## Overview

The **Auth Service** is the core identity microservice in the DevSphere platform. It owns authentication credentials, password hashes, user registration, and stateless JWT authentication.

```
[ Frontend Client ]
        │
        ▼
  [ API Gateway ] (Port 8080)
        │
        ▼
  [ Auth Service ] (Port 8081)
        │
        ▼
 [ Auth Database ] (MySQL: devsphere_auth)
```

---

## Authentication Workflows

### 1. User Registration Flow

```
Client ──► API Gateway ──► Auth Service ──► BCrypt Hashing ──► MySQL Database
```

### 2. User Login & Token Issuance Flow

```
Client ──► API Gateway ──► Auth Service ──► BCrypt Verification ──► JWT Generation (HS256) ──► Return Bearer Token
```

### 3. Protected API Request Flow (Future Lessons)

```
Client (Authorization: Bearer <JWT>) ──► API Gateway (JWT Filter) ──► Protected Microservice
```

> **Note**: Gateway-side token validation filter will be introduced in a later lesson.

---

## Domain Ownership & Boundary

1. **Authentication Credentials Only**: The Auth Service strictly owns credential identity (`id`, `email`, `password_hash`, `created_at`, `updated_at`).
2. **Stateless JWT Issuance**: Upon successful credential verification, Auth Service signs a JWT token containing:
   - `sub`: User ID (`Long`)
   - `email`: User email address
   - `iat`: Issued-at timestamp
   - `exp`: Expiration timestamp
3. **Database Isolation**: Other microservices (API Gateway, User Service, Task Service, etc.) must NEVER query the `devsphere_auth` database directly.
