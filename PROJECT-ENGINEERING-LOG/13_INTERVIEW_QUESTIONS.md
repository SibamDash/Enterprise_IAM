# Interview Question Bank

## Basic
- **What does this component do?**
  *Answer:* The IAM platform serves as the central identity provider for the enterprise. It handles user authentication, MFA, and dispenses OAuth2 tokens for other microservices and applications.
- **Why did you use Spring Authorization Server?**
  *Answer:* It is the modern, supported standard for Spring applications replacing the deprecated Spring Security OAuth library, and seamlessly integrates with our existing Spring Boot 3 backend.

## Technical
- **How does authentication work?**
  *Answer:* The user authenticates via a React SPA. The backend issues a short-lived JWT access token and a hashed refresh token stored in PostgreSQL. The JWT is passed as a Bearer token for API access.
- **How does cross-application SSO work?**
  *Answer:* Once a user logs into the IAM platform, the SPA holds their access token. When an external application (like a CRM) redirects the user to the IAM authorize endpoint, the SPA intercepts this, appends the access token, and the backend instantly issues an authorization code without prompting for a password.

## Deep-Dive
- **What happens internally when a user logs in with MFA?**
  *Answer:* The backend verifies the password, sees MFA is enabled, and issues a temporary JWT with an mfa=true claim. The JwtAuthenticationFilter recognizes this claim and blocks access to protected APIs. The user must submit a TOTP code to the /mfa endpoint, which is verified against the googleauth library. Only then is the final, full-access JWT issued.

## Problem-Solving
- **What was the hardest bug?**
  *Answer:* Dealing with Mockito and JDK 21 final class constraints during the OIDC integration phase. 
- **How did you identify the root cause?**
  *Answer:* I traced the MockitoException regarding final classes back to the mockito-extensions/org.mockito.plugins.MockMaker file which was set to mock-maker-subclass to bypass a JDK 21 bug, preventing inline mocking.
- **What alternatives did you try?**
  *Answer:* I considered writing a wrapper class, but eventually refactored the test to use the framework's native JwtEncodingContext.with() builder, which is cleaner and more resilient.

## Design
- **How would you scale this to 1 million users?**
  *Answer:* The stateless JWT access tokens already scale perfectly. The bottleneck would be the Postgres database handling Refresh Tokens. I would implement Redis for session caching and read-replicas for the user/policy tables, as IAM is heavily read-biased.

## Security
- **How do you prevent unauthorized access to other tenants?**
  *Answer:* Every API request extracts the 	enantId from the JWT and places it in a TenantContextHolder. The Repository layer filters all database queries by this 	enantId, enforcing row-level security logically.
- **What vulnerabilities did you consider for sessions?**
  *Answer:* Token replay and DB leaks. We hash refresh tokens in the DB so a leak doesn't compromise active sessions, and we use Token Family revocation so if a refreshed token is reused, the entire family is killed to stop replay attacks.
