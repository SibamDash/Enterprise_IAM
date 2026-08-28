# Enterprise IAM Platform

A production-style, multi-tenant Enterprise Identity and Access Management (IAM) platform built with Spring Boot, React, and PostgreSQL.

## Features
- Multi-tenant organizations
- User lifecycle management
- Password authentication & MFA
- RBAC and Policy-based authorization (ABAC)
- OAuth 2.0 & OpenID Connect (OIDC)
- SSO & Client/Application management
- Audit logging & Security events
- Fully containerized

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21 (for local backend development)
- Node.js 20+ (for local frontend development)

### Running with Docker
1. Clone the repository.
2. Run `docker compose build && docker compose up -d` to start the full stack (PostgreSQL, Redis, Backend, Frontend).
3. The backend API is available at `http://localhost:8080`.
4. The frontend application is available at `http://localhost:3000`.

### Development
Backend:
```bash
cd backend
./mvnw spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
npm run dev
```

### Running Tests
A script is provided to run the full regression test suite:
```bash
./scripts/run-all-tests.sh
```
