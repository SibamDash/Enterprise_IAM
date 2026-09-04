# Git & Collaboration History

## Branching Strategy
- Direct to main for foundational phases (1-9) to rapidly establish the monolith structure.
- Future changes will utilize feature branches.

## Major Refactors
- **Phase 7-8:** Major integration of spring-security-oauth2-authorization-server. Required significant rewiring of the existing SecurityFilterChain to accommodate OIDC alongside standard API authentication.
