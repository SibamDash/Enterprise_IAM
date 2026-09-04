# Deployment & DevOps Log

## Environment Setup
- **Docker Compose:** Used for spinning up the PostgreSQL database (docker-compose.yml).
- **Constraint:** Windows IDE environments (like the one used for initial dev) inject WSL/Docker socket mismatches (MSYS_NO_PATHCONV), causing Testcontainers to fail during automated E2E runs. 
- **Resolution:** Fallback to native WSL2 terminal for execution of full E2E suites.
