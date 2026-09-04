# Development Progress Log

> **Rule:** Never erase history. Append new phases sequentially.

## Phase 1: Admin UX Journey
- **Date:** 2026-08
- **What was planned:** Multi-tenant DB schema, backend CRUD, frontend views.
- **What was implemented:** `V2__multi_tenant_schema.sql`, Organizations/Users/Roles APIs, React scaffolding.
- **Current Status:** Completed.

## Phase 2-4: Authentication, Sessions, Roles
- **Date:** 2026-08
- **What was planned:** Login, stateless sessions with refresh tokens, RBAC.
- **What was implemented:** JWT issuance, Refresh Token hashing and DB storage, Role checking via `@PreAuthorize`.
- **Current Status:** Completed.

## Phase 5-6: Policy Engine & MFA
- **Date:** 2026-08
- **What was planned:** ABAC rules evaluation and TOTP MFA.
- **What was implemented:** MFA setup using `warrenstrange/googleauth`, recovery codes, and JWT claims specifying if MFA is pending.
- **Current Status:** Completed.

## Phase 7-8: OAuth 2.0 & OIDC
- **Date:** 2026-09
- **What was planned:** OAuth2 Authorization Server setup and OpenID Connect Discovery.
- **What was implemented:** `AuthorizationServerConfig.java`, Token Customizers to inject ID token claims.
- **Current Status:** Completed.

## Phase 9: SSO
- **Date:** 2026-09
- **What was planned:** Cross-application SSO E2E validation.
- **What was implemented:** `V8__add_sso_test_clients.sql` (CRM and HR apps), Playwright test `phase-9-sso.spec.ts`.
- **Current Status:** Completed.
