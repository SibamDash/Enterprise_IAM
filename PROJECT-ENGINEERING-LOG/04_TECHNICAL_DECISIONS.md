# Technical Decision Log

## DECISION-001: Session Management Architecture
- **Decision:** Implement stateless sessions via JWT and hashed Refresh Tokens.
- **Context:** The frontend is a SPA requiring robust authentication. We needed to support SSO and OAuth2 seamlessly while allowing explicit session invalidation.
- **Alternatives:** 
  1. Traditional Cookie-based `JSESSIONID`.
  2. Pure stateless JWT without server-side invalidation.
- **Final choice:** JWT for access, hashed UUID refresh tokens stored in PostgreSQL for session state.
- **Reason:** Provides the scalability of JWT while maintaining the security capability to immediately revoke a compromised session.
- **Consequences:** We had to implement custom logic to pass the `access_token` query parameter into the `JwtAuthenticationFilter` during OAuth2 redirects, as there was no traditional cookie session.

## DECISION-002: Multi-Tenancy Strategy
- **Decision:** Logical Isolation (Row-Level Security via `tenant_id` column).
- **Context:** The IAM system needed to serve multiple organizations seamlessly without database sprawl.
- **Final choice:** Shared Database, Shared Schema with a strictly enforced `tenant_id` column filtered at the application/repository level.
- **Reason:** Simplest to maintain and scale for an IAM service where organizations share the same exact data model.
