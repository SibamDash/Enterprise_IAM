# Security Log

## 1. Authentication & JWT Handling
- Implemented a short-lived Access Token (15 minutes) and a long-lived Refresh Token (7 days).
- The Refresh Token is hashed via SHA-256 before being stored in the database to prevent database-leak compromises.
- Implemented Refresh Token Rotation: on every refresh, the old token is invalidated. If a revoked token is used, a token replay attack is assumed, and the entire token family is immediately revoked.

## 2. Multi-Factor Authentication (MFA)
- Implemented TOTP-based MFA using the `googleauth` library.
- If MFA is enabled, the initial login returns an intermediate token with an `mfa=true` claim. The `JwtAuthenticationFilter` specifically checks for this claim and rejects access to standard API endpoints until the second factor is verified and a full access token is issued.
- Enforced strong password requirements prior to MFA setup.
