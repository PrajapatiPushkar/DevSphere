# API Gateway Architecture Document

## Overview

The **API Gateway** acts as the single external gateway, reverse proxy, and perimeter security boundary for all DevSphere client applications.

---

## Evolution of Gateway Security Architecture

### Before Lesson 6 (Routing Only)
```
[ Client ] ──► [ API Gateway ] ──► [ Downstream Services ]
```

### After Lesson 6 (Perimeter JWT Validation)
```
                                ┌── Public Route ───────────► [ Auth Service / Health ]
                                │
[ Client ] ──► [ API Gateway ] ─┤
                                │                              [ Validated Token ]
                                └── Protected Route ──► [ JWT Validation ] ─────────► [ Downstream Service ]
                                                               Filter                    (with X-Authenticated-User-Id)
```

---

## Key Highlights

1. **Perimeter Authentication Boundary**: Public endpoints (`/api/v1/auth/register`, `/api/v1/auth/login`, `/actuator/health`) bypass JWT validation. Protected endpoints require a valid `Authorization: Bearer <token>` header.
2. **Stateless Local Validation**: `JwtAuthenticationFilter` validates token signature (HS256) and expiration locally using `JwtValidator` without querying the Auth Service or any database.
3. **Authentication vs. Business Authorization**: The API Gateway strictly performs **perimeter authentication** (identity verification). Fine-grained **business authorization** (domain permissions) remains the responsibility of downstream microservices.
4. **Header Sanitization**: Untrusted client-supplied `X-Authenticated-User-Id` headers are stripped, and trusted user IDs extracted from validated JWT `sub` claims are attached before forwarding requests.
