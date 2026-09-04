# Architecture

## Current Architecture

The system uses a monolithic Spring Boot backend serving a React SPA frontend. The backend acts as both an API for the frontend and an OAuth2 Authorization Server.

**Key Components:**
1. **Frontend SPA:** React application (Vite) handling Admin UI and Login/Consent screens.
2. **Backend API:** Spring Boot REST API for managing Organizations, Users, Roles, and Policies.
3. **Authorization Server:** Spring Security Authorization Server implementing OAuth2/OIDC.
4. **Database:** PostgreSQL handling all relational data, utilizing `tenant_id` for logical isolation.
5. **Session Management:** Stateless session design leveraging JWTs and Refresh Tokens stored in the DB (hashed).

## Architecture Evolution History

### Initial Setup (Phase 1)
- **Architecture:** Basic Spring Boot + React structure with a generic Postgres DB.
- **Reason:** To establish the foundation for the multi-tenant system.

### Introduction of Authorization Server (Phase 7-9)
- **Previous Architecture:** Standard JWT based API authentication.
- **Problem:** Could not act as an identity provider for *other* applications in the enterprise.
- **New Architecture:** Integrated Spring Authorization Server.
- **Reason for change:** The goal was to provide OAuth2/OIDC and SSO.
- **Impact:** Required complex configuration (`JwtEncodingContext`, `JwtAuthenticationFilter` integration) to allow the SPA to handle the login screen while the Authorization Server handled the protocol.
