# ADR 0003: Auth Service Data Ownership & Credential Isolation

* **Status**: Accepted
* **Date**: 2026-08-22
* **Context**: Defining the domain boundary and data ownership rules for authentication credentials versus user profile data.

---

## Decision

The **Auth Service** cleanly owns user authentication credentials (`email`, BCrypt `password_hash`, credential creation timestamps) and its dedicated MySQL database (`devsphere_auth`).

---

## Rationale & Drivers

1. **Security Isolation**: Keeping credentials and password hashes strictly isolated inside a dedicated auth database minimizes exposure risks and attack vectors.
2. **Clear Domain Ownership**: Authentication logic is decoupled from user profiles, tasks, and career management domains.
3. **Independent Service Scaling**: Authentication load (logins, token validations, registration) can scale independently from profile or task read/write workloads.
4. **Reduced Coupling**: Modifying user profile fields or adding career features in the future will not impact the core authentication schema or security model.
5. **Evolving Auth Standards**: Simplifies future introduction of OAuth2, OIDC, or Multi-Factor Authentication (MFA) without altering user profile domains.

---

## Strict Architectural Rules

* **Database Isolation**: Other microservices (API Gateway, User Service, Task Service, etc.) MUST NOT directly query or access the Auth Service database (`devsphere_auth`).
* **Zero Credential Exposure**: Plaintext passwords and BCrypt hashes must never be exposed via API responses or application logs.
* **Minimal Identity Schema**: The Auth Service database stores only authentication identity. User profile details (names, bio, skills, resume) belong strictly to the future User Service.
