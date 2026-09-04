# STAR Stories

## 1. Overcoming Mocking Constraints in JDK 21
**Situation:** While implementing the OIDC integration tests, the CI/CD pipeline failed because Mockito could not mock JwtEncodingContext.
**Task:** I needed to fix the test suite quickly without reverting the project's JDK 21 compatibility.
**Action:** I investigated the root cause and found that our mock-maker-subclass configuration (required for JDK 21) disabled final class mocking. Instead of fighting the framework or creating complex wrappers, I rewrote the test to utilize Spring's native JwtEncodingContext.with() builder pattern.
**Result:** The test compiled and passed successfully, and the pipeline was restored. It taught me that relying on native framework builders is often more resilient than aggressive mocking.

## 2. Implementing Secure Multi-Tenant IAM
**Situation:** The enterprise required a centralized IAM solution capable of isolating multiple organizations while providing single sign-on (SSO).
**Task:** I had to design a scalable session and authentication architecture.
**Action:** I implemented a hybrid approach: stateless JWTs for fast, scalable API access, combined with hashed refresh tokens stored in PostgreSQL for secure, revocable sessions. I integrated Spring Authorization Server to handle the OAuth2/OIDC protocols.
**Result:** We successfully demonstrated cross-application SSO (Journey D) where a user could log into the IAM portal once and seamlessly access CRM and HR applications without re-entering credentials.
