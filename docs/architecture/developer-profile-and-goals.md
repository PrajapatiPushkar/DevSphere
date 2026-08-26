# Architecture Specification — Developer Profile & Goal Management Domain

## 1. Domain Ownership & Bounded Context

The **User Service** (`services/user-service`) owns the Developer Profile and Goal Management product domains in DevSphere.

```
+--------------------------+         +-------------------------------------+
|      Auth Service        |         |            User Service             |
| (services/auth-service)  |         |        (services/user-service)      |
+--------------------------+         +-------------------------------------+
| - Credentials & Hashing  |         | - Developer Profile Metadata        |
| - JWT Issuance & Login   |         |   (headline, bio, social links,     |
| - User Identity Source   |         |    role, experience)                |
+--------------------------+         | - Goal Management & Progress        |
                                     |   (DAILY, WEEKLY, LONG_TERM goals)  |
                                     +-------------------------------------+
```

- **Auth Service**: Remains sole owner of authentication credentials, JWT token signing, and login/registration.
- **User Service**: Owns developer profiles and goals. No cross-service database calls or foreign key constraints exist between microservice databases. Identity is strictly passed via JWT headers (`X-Authenticated-User-Id` / Spring Security `UserPrincipal`).

---

## 2. Profile Domain

### Database Schema (`user_profiles`)
```sql
ALTER TABLE user_profiles ADD COLUMN headline VARCHAR(250);
ALTER TABLE user_profiles ADD COLUMN location VARCHAR(100);
ALTER TABLE user_profiles ADD COLUMN github_url VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN linkedin_url VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN portfolio_url VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN `current_role` VARCHAR(100);
ALTER TABLE user_profiles ADD COLUMN years_of_experience INT;
```

### Endpoints
- `GET /api/v1/profile`: Returns authenticated user's profile metadata. Lazily creates default profile if absent.
- `PUT /api/v1/profile`: Updates current profile fields with field validations (`@Size`, `@Min(0)`, `@Pattern` for URL structure).

---

## 3. Goal Management Domain

### Database Schema (`goals`)
```sql
CREATE TABLE goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    goal_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    target_value INT,
    current_value INT DEFAULT 0,
    target_date DATE,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_goals_user_id (user_id),
    INDEX idx_goals_user_status (user_id, status),
    INDEX idx_goals_user_type (user_id, goal_type)
);
```

### Enums & Lifecycle
- `GoalType`: `DAILY`, `WEEKLY`, `LONG_TERM`
- `GoalStatus`: `ACTIVE`, `COMPLETED`, `ARCHIVED`

#### Lifecycle Transitions
- Creation: Status defaults to `ACTIVE`, `currentValue` defaults to `0`.
- Transition to `COMPLETED`: Sets `completedAt = Instant.now()` and increments Micrometer metric `devsphere_goals_completed_total`.
- Transition away from `COMPLETED`: Clears `completedAt = null`.
- Deletion (`DELETE /api/v1/goals/{id}`): Logical archival (`status = ARCHIVED`, returns 204 No Content). No physical deletion.

### Progress Calculation Logic
Progress percentage is dynamically computed in memory (`GoalResponse.calculateProgressPercentage`):
- `targetValue == null` or `targetValue <= 0` -> `progressPercentage = null`
- `currentValue == null` -> `currentValue = 0`
- `currentValue >= targetValue` -> `progressPercentage = 100.0`
- Otherwise -> `round((currentValue * 100.0) / targetValue, 2)`

---

## 4. Security & IDOR Isolation Model

Identity is derived strictly from JWT claims in Spring Security context (`UserPrincipal` or validated `X-Authenticated-User-Id` header).

All repository queries use user-scoped methods:
`goalRepository.findByIdAndUserId(goalId, userId)`

If User B attempts `GET`, `PUT`, or `DELETE` on User A's Goal ID:
- Query returns `Optional.empty()`.
- Service throws `ResourceNotFoundException("Goal not found")`.
- API returns HTTP `404 Not Found`.
- **Zero Information Leakage**: User B cannot infer resource existence.

---

## 5. Endpoints Summary

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/api/v1/profile` | Get authenticated developer profile | `200 OK` (`UserProfileResponse`) |
| `PUT` | `/api/v1/profile` | Update profile attributes | `200 OK` (`UserProfileResponse`) |
| `POST` | `/api/v1/goals` | Create new goal | `201 Created` (`GoalResponse`) |
| `GET` | `/api/v1/goals` | List paginated goals (supports `status`, `goalType`, `page`, `size`) | `200 OK` (`PageResponse<GoalResponse>`) |
| `GET` | `/api/v1/goals/{id}` | Get goal by ID (IDOR protected) | `200 OK` / `404 Not Found` |
| `PUT` | `/api/v1/goals/{id}` | Update goal details & status | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/v1/goals/{id}` | Archive goal logically | `204 No Content` / `404 Not Found` |
