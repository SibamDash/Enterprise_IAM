# Project Overview

**Project Name:** Enterprise IAM
**Problem Statement:** Enterprises need a centralized identity, authentication, and access management platform capable of handling multi-tenancy, strict security boundaries, robust role-based access control, SSO, and MFA across all internal applications.
**Business/Use-Case:** A centralized Auth provider used by all microservices and external tools in the corporate ecosystem.
**Target Users:** End users logging into applications, Organization Admins managing their tenants, System Admins maintaining the IAM platform.

## Architecture
- **Backend:** Java 21, Spring Boot 3.x, Spring Security, Spring Authorization Server
- **Frontend:** React, TypeScript, Vite
- **Database:** PostgreSQL (Multi-tenant via tenant_id column isolation)
- **Infrastructure:** Docker, Docker Compose for local environments

## Major Features
- Tenant-isolated User and Role Management (RBAC)
- Fine-Grained Policy Engine (ABAC)
- Multi-Factor Authentication (MFA/TOTP)
- OAuth 2.0 / OpenID Connect Authorization Server
- Cross-Application Single Sign-On (SSO)

## Current Project Status
- Completed up to Phase 9 (SSO Cross-Application)

## Project Goals and Milestones
- [x] Phase 1-9: Core Identity, Auth, MFA, OAuth2, SSO
- [ ] Phase 10-12: Client/Application Management, Service Accounts, Audit Logging
- [ ] Phase 13-16: Security hardening, Dashboard, Production readiness
