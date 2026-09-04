# Testing & Quality

## Test Strategy
1. **Unit Tests:** Java/JUnit for isolated service logic (e.g., `AuthServiceTest.java`).
2. **Integration Tests:** Spring Boot Test + Testcontainers (PostgreSQL) to validate database interactions and API endpoints (`OidcIntegrationTest.java`).
3. **End-to-End Tests:** Playwright for full user journeys.

## Major Test Discoveries
- Discovered issues with Docker socket pathing on Windows, resulting in a strict environmental testing requirement (WSL2 native).
- Found that `JwtEncodingContext` cannot be mocked on JDK 21.

## E2E Journeys Automated
- **Journey A:** Admin UX (Phase 1)
- **Journey B:** Authentication (Phase 2)
- **Journey C:** Session Management (Phase 3)
- **Journey D:** SSO Cross-Application (Phase 9) - The test explicitly simulates `crm-client` and `hr-client` utilizing the same IAM session for seamless authentication.
