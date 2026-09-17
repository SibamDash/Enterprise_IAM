# Enterprise IAM Platform

A robust, enterprise-grade Identity and Access Management (IAM) platform built with Spring Boot and React.

## Architecture Summary

This project implements a modular monolith structure with strict tenant isolation at the data layer.

### Backend (Spring Boot 3 + Java 21)
- **Security**: Spring Security, JWT (Auth/Access/Refresh tokens), OAuth 2.0 Authorization Server.
- **Data Access**: Spring Data JPA with PostgreSQL, leveraging Hibernate filters for strict row-level multitenancy (`tenant_id`).
- **Caching & Sessions**: Redis (via Spring Data Redis) for session management, token revocation tracking, and caching.
- **Modules**: Authentication, Users, Roles, Organizations (Tenants), OAuth Clients, Audit Logs, and Admin Dashboard.

### Frontend (React 19 + TypeScript + Vite)
- **State Management**: React Context / Hooks (`useAuth`).
- **Routing**: React Router v7.
- **Styling**: Vanilla CSS with modern, responsive, and dynamic UI paradigms (glassmorphism, CSS variables).
- **Tooling**: Playwright for End-to-End Testing.

---

## Setup Instructions

This platform utilizes Docker for local development. Make sure you have Docker Desktop (or equivalent) installed.

### 1. Start the Data Stack
Start PostgreSQL and Redis via Docker Compose:
```bash
docker compose up -d
```

### 2. Run the Backend
You can run the Spring Boot application using Maven:
```bash
cd backend
./mvnw spring-boot:run
```
*(The backend will automatically start on port 8080. Ensure the database is running first).*

### 3. Run the Frontend
In a separate terminal, install dependencies and start the Vite dev server:
```bash
cd frontend
npm install
npm run dev
```
*(The frontend will start on port 5173).*

---

## Testing & Verification

### Running the End-to-End Demo Script
A bash script is provided to demonstrate the core 17 IAM capabilities (from organization creation to token replay and tenant isolation).
With the backend running, execute:
```bash
./scripts/demo.sh
```

### Running the Backend Integration Suite
The backend contains a full suite of unit and integration tests (using Testcontainers).
```bash
cd backend
./mvnw test
```
*Or use the provided script:* `./scripts/run-all-tests.sh`

### Running the Frontend E2E Suite (Playwright)
To run the automated UI journeys (A-E):
```bash
cd frontend
npx playwright test
```
