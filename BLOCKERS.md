# Current Blockers

## Local Testcontainers Docker Environment
- **Status:** BLOCKED
- **Description:** The `session-bootstrap.sh` script fails during the backend regression suite. Specifically, the Mockito JDK 21 issue was resolved via `mock-maker-subclass`, but now `Testcontainers` fails to start with:
  `java.lang.IllegalStateException: Could not find a valid Docker environment. Please see logs and check configuration`
- **Root Cause:** The Antigravity IDE agent runs on Windows and lacks direct socket access to the Docker Desktop daemon (which runs in WSL2). The tests try to spin up a Postgres container via Testcontainers but cannot connect to `\\.\pipe\docker_engine` or `/var/run/docker.sock` from this restricted environment.
- **Required Action:** Please verify the full regression suite by either:
  1. Checking the GitHub Actions CI pipeline for the latest commit (`f607a61` or `9fd399e`).
  2. Running `./scripts/session-bootstrap.sh` manually from your native WSL2 terminal where Docker socket access is available.
- **Next Steps:** Once you verify the tests pass, please provide explicit approval to proceed to **Phase 4: RBAC + Permission System**.
