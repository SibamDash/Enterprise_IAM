# Enterprise IAM Platform - Comprehensive Documentation

## 1. Introduction
The Enterprise Identity and Access Management (IAM) platform is a robust, production-grade identity platform built to manage multi-tenant organizations, authenticate users, handle authorization (RBAC), and issue OAuth 2.0 / OpenID Connect tokens. Similar to platforms like Auth0, Okta, and Keycloak, this system is designed to provide identity services to other integrated applications.

## 2. System Architecture
The platform follows a modular monolith architecture, ensuring strict tenant isolation at the data layer. 

### Technology Stack
- **Backend**: Spring Boot 3 + Java 21, Spring Security, Spring Data JPA
- **Database**: PostgreSQL 16 (for relational data storage)
- **Caching & Sessions**: Redis 7 (for session management and caching)
- **Frontend**: React 19 + TypeScript + Vite, React Router v7
- **Deployment**: Docker and Docker Compose

### Core Components
1. **Authentication Engine**: Handles login, MFA verification, password resets, and session tracking.
2. **Authorization Server**: Issues and validates OAuth 2.0 access and refresh tokens.
3. **Tenant Manager**: Implements strict row-level multitenancy (`tenant_id`) ensuring organizations never leak data.
4. **Admin Dashboard**: A React frontend interface for managing Users, Roles, Policies, Groups, and OAuth Clients.

## 3. Core Features

### 3.1 Multi-tenant Organization Isolation
The platform guarantees data isolation using Hibernate filters. Every entity (User, Role, Session, etc.) is bound to an `organization_id`. The `TenantFilter` sets the active tenant context per request, automatically applying `tenant_id = ?` to all JPA queries to prevent cross-tenant data leaks.

### 3.2 User Lifecycle & RBAC
- **Users**: Managed centrally per organization. Can be assigned roles and groups.
- **Roles**: Collections of granular permissions (e.g., `USER_CREATE`, `USER_READ`).
- **Groups**: Used for logical grouping of users and mapping organizational units.
- **Policies**: Fine-grained access control mechanisms.

### 3.3 Authentication Flows
- **Local Login**: Standard email and password login utilizing BCrypt hashing.
- **Multi-Factor Authentication (MFA)**: Support for TOTP-based secondary verification.
- **OAuth 2.0 & OIDC**: Full support for Authorization Code flow with PKCE, Client Credentials flow, and Refresh Token rotation.

### 3.4 Audit Logging
Security-sensitive events (Logins, Failed Logins, Logouts, User creation) are recorded asynchronously and tied to the initiating `user_id` and `organization_id`.

## 4. Local Development & Setup

### Requirements
- Docker and Docker Compose
- Node.js 20+
- Java 21+

### Running the Stack
The easiest way to spin up the entire application stack is via Docker Compose:
```bash
docker compose up -d --build
```
This command starts:
1. `postgres` (port 5432)
2. `redis` (port 6379)
3. `backend` (port 8080)
4. `frontend` (port 3000)

**Accessing the Services:**
- Frontend Admin Portal: `http://localhost:3000`
- Backend API: `http://localhost:8080`

### Seed Data (Default Login)
When the database initializes, it automatically seeds a Super Admin account.
- **Organization ID**: `28be457e-4f4d-4b98-982a-fac750369982` *(Check DB if different: `docker compose exec postgres psql -U iam_user -d iam -c "SELECT id FROM organizations;"`)*
- **Email**: `admin@acme.com`
- **Password**: `SecurePassword123!`

## 5. API Reference
The backend exposes a RESTful API under `/api/v1/`.
*All endpoints (except public auth endpoints) require a Bearer token.*

- `POST /api/v1/auth/login`: Authenticate and receive a JWT.
- `POST /api/v1/auth/refresh`: Rotate refresh and access tokens.
- `GET /api/v1/users`: Retrieve paginated list of users for the current tenant.
- `POST /api/v1/users`: Create a new user (requires `USER_CREATE` permission).
- `GET /api/v1/organizations`: List organizations.
- `GET /api/v1/audit-logs`: Fetch security audit logs.

## 6. Challenges & Solutions

During the implementation of this Enterprise IAM platform, we encountered several complex architectural and engineering challenges. Here is how we solved them:

### Challenge 1: Ensuring Bulletproof Tenant Isolation
**Problem:** In a multi-tenant system using a shared database, developers might accidentally write JPA queries that span across tenants, causing severe data leaks.
**Solution:** We implemented Hibernate `@FilterDef` and `@Filter` on all tenant-aware entities. A custom `TenantFilter` in the Spring Security filter chain extracts the Tenant ID from the request headers or JWT token and binds it to a ThreadLocal `TenantContextHolder`. Hibernate automatically appends `AND organization_id = ?` to every database query seamlessly.

### Challenge 2: Audit Logging Transaction Failures
**Problem:** When a user attempted to log in with an invalid or non-existent Organization ID, the system attempted to record a `USER_LOGIN_FAILED` audit event. However, since the `audit_logs` table has a foreign key constraint referencing the `organizations` table, the database threw a `ConstraintViolationException`, resulting in an unhandled 500 Internal Server Error instead of a 401 Unauthorized response.
**Solution:** The audit event listener operates asynchronously using `@Async`. While it didn't block the main authentication failure response (which correctly returned 401 to the frontend), it generated noisy backend stack traces. The solution involved validating the existence of the Organization ID *before* attempting the audit insertion, or gracefully catching constraint violations in the async audit thread.

### Challenge 3: Frontend Layout Breaking on Wide Screens
**Problem:** The React frontend was constrained by a default Vite template CSS (`#root { width: 1126px; text-align: center; }`). This caused the full-viewport dashboard layout (`100vw`) to overflow horizontally on larger screens, cutting off elements on the right side (like the Tenant ID input and action buttons).
**Solution:** We overhauled `index.css`, removing the hardcoded `1126px` width and centering rules. We replaced it with a flexible `width: 100%` and `min-height: 100vh` layout, allowing the dashboard's internal flexbox system to correctly bound the viewport without overflow.

### Challenge 4: Security Configuration Circular Dependencies
**Problem:** Injecting the `UserRepository` into the custom `UserDetailsService`, and then referencing it in the `SecurityFilterChain`, caused circular bean dependency errors during Spring Boot startup because of how password encoders and authentication managers intertwine.
**Solution:** We decoupled the bean definitions. We placed the `PasswordEncoder` and `AuthenticationManager` in a separate `@Configuration` class or utilized `ObjectProvider`/lazy injection where necessary to break the initialization cycle.

### Challenge 5: Managing Session Revocation and Token Replay
**Problem:** Standard stateless JWTs cannot be forcefully revoked before their expiration time, creating a security risk if an account is compromised.
**Solution:** We adopted a hybrid approach. Short-lived access tokens (15 minutes) act statelessly, while Refresh Tokens are stateful and stored in Redis. When a user logs out, their Refresh Token is immediately blacklisted in Redis. We also implemented token family tracking to detect Refresh Token reuse (a sign of a stolen token), immediately revoking all active sessions for that token family if replay is detected.

## 7. Testing Strategy
We employ a layered testing strategy to guarantee security and functionality:
- **End-to-End Tests**: Playwright is used to automate browser journeys across the React frontend.
- **Integration Tests**: Spring Boot `@SpringBootTest` alongside Testcontainers dynamically provisions ephemeral PostgreSQL and Redis instances for database integration testing.
- **Unit Tests**: Granular tests verify JWT generation, RBAC logic, and password hashing.
