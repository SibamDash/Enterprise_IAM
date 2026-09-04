# Issues & Bugs Register

## ISSUE-001: Mockito Final Class Mocking Error (JDK 21)
- **Date:** 2026-09-04
- **Component:** `OidcIntegrationTest.java` (Backend)
- **Problem:** Mockito failed to mock `JwtEncodingContext` because it is a final class.
- **Expected behavior:** Test compiles and runs, verifying ID token claims.
- **Actual behavior:** `MockitoException: Cannot mock/spy class org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext because: final class`.
- **Root cause:** The project is on JDK 21. A previous workaround for JDK 21 compatibility enforced the `mock-maker-subclass` configuration, which explicitly disables mocking of final classes (a feature otherwise available via `mock-maker-inline`).
- **Investigation process:** Initially considered enabling `mockito-inline`, but discovered a documented blocker indicating `mock-maker-subclass` was strictly required to fix a JDK 21 issue. Thus, mocking was impossible.
- **Solutions considered:** 
  1. Re-enable inline mocking (rejected due to JDK 21 regression).
  2. Use a wrapper class (rejected due to framework constraints).
  3. Instantiate the context properly using its native Builder.
- **Solution selected:** Use `JwtEncodingContext.with()` builder.
- **Why selected:** It avoids mocking entirely, adheres to Spring Security best practices, and bypasses the JDK 21 limitation.
- **Exact fix:** Replaced `mock(JwtEncodingContext.class)` with `JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claimsBuilder)...build()`.
- **Lessons learned:** Framework builders are often more robust for tests than mocking, especially when dealing with strict final classes in modern JDKs.
- **Interview Question:** "Tell me about a time you faced a compatibility issue with testing tools."

## ISSUE-002: Docker Socket Pathing on Windows WSL
- **Date:** 2026-09-04
- **Component:** E2E Regression Suite (`run-all-tests.sh`)
- **Problem:** The bash script fails in the IDE terminal because the Testcontainers library cannot resolve the Docker daemon socket correctly on Windows.
- **Actual behavior:** `Cannot connect to the Docker daemon at unix:///var/run/docker.sock`.
- **Root cause:** Windows IDEs inject specific environment variables (`MSYS_NO_PATHCONV`) and handle paths differently than native WSL2, causing the integration tests to fail at Docker discovery.
- **Solution selected:** Documented the constraint in `BLOCKERS.md` and mandated that tests must be run in the native WSL2 terminal by the user rather than through the automated IDE agent.
- **Interview Question:** "How do you handle environment-specific integration testing failures in a containerized application?"
