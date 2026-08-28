# Enterprise IAM — Implementation Specification for Antigravity

Repository: https://github.com/SibamDash/Enterprise_IAM.git

> **Read this entire document before writing any code.** Sections 1–7 are operating
> rules — testing, Docker, and CI/CD are not optional add-ons, they are binding
> constraints that gate every phase. Sections 8+ contain the technical/phase plan.

---

## 0. Mission

Build a production-style, multi-tenant Enterprise Identity and Access Management (IAM) platform.

This must **not** become a basic JWT + RBAC tutorial. The final system should behave like a lightweight Auth0/Okta/Keycloak-style identity platform that other applications can authenticate against.

The platform must provide:

- Multi-tenant organizations
- User lifecycle management
- Authentication
- MFA
- RBAC and fine-grained permissions
- Policy-based authorization / ABAC foundation
- OAuth 2.0
- OpenID Connect (OIDC)
- Authorization Code flow with PKCE
- Client Credentials flow
- Refresh-token rotation and revocation
- SSO
- Application/client management
- Sessions and device management
- Password policies and account lockout
- Audit logging
- Security events
- API/service-account access
- Admin portal
- A layered automated test suite (unit, integration, security, end-to-end, regression)
- A fully containerized local/production-style environment (Docker)
- An automated CI/CD pipeline that gates every merge
- Secure configuration and secrets handling

**Definition of "complete":** the project is not finished when Phase 15's tests pass.
It is finished when Phase 16 (Section 27) — the whole-system verification gate — passes
with every phase's functionality still working together, the full Docker stack running
cleanly end to end, and CI green on the final commit.

**Working autonomously across restarts:** Antigravity has no memory between sessions.
Section 2.4's Autonomous Resume Protocol is what makes unattended, multi-session work
safe — it re-verifies real system state against `PROGRESS.md` at the start of every
session, automatically, so a restart, crash, or new chat can never silently continue
from an incorrect assumption about what's actually done.

---

# 1. Non-Negotiable Implementation Rules

## Rule 1 — Never mark a feature complete without verification

For **every feature**, Antigravity must follow this loop:

```text
Understand requirement
      ↓
Design data/API/UI
      ↓
Implement
      ↓
Run unit tests
      ↓
Run integration tests
      ↓
Run security/negative tests
      ↓
Run application
      ↓
Manually verify the user flow
      ↓
Bug found?
   ┌──┴──┐
  YES    NO
   │      │
   └─ Fix  │
      ↓   │
   Re-run─┘
      ↓
Only then mark COMPLETE
```

### Loop requirement

Do **not** stop after the first implementation attempt if:

- tests fail
- compilation fails
- migrations fail
- API responses are incorrect
- authorization can be bypassed
- tenant isolation is broken
- frontend/backend integration fails
- refresh-token behavior is incorrect
- security tests fail
- Docker build/compose fails
- CI pipeline fails
- the documented user journey cannot be completed

**Keep looping — implement, run, observe failure, fix, re-run — until the feature works
end-to-end.** There is no maximum retry count. A feature is not "attempted," it is
either working and verified, or it is not done.

## Rule 2 — Test both success and failure paths

Every security-sensitive feature must have:

- Happy-path tests
- Invalid-input tests
- Unauthorized tests
- Forbidden tests
- Boundary tests
- Tenant-isolation tests where applicable
- Replay/reuse tests where applicable
- Rate-limit/lockout tests where applicable

Example:

```text
DELETE /api/users/123

Authenticated + correct permission
→ 204

Authenticated + missing permission
→ 403

Unauthenticated
→ 401

User from Tenant B attempting Tenant A resource
→ 403 / 404 according to resource policy
```

## Rule 3 — Never weaken security to make tests pass

Do NOT:

- disable authorization
- hard-code tokens
- store plaintext passwords
- skip signature validation
- accept arbitrary redirect URIs
- expose client secrets
- bypass tenant checks
- remove failing security tests
- silently catch security exceptions
- use insecure development behavior in production configuration
- disable/skip a CI test step to make a pipeline run go green

If a security test fails, fix the implementation — never fix the test to accept the
insecure behavior, and never delete/skip/comment-out a failing test to get to green.

## Rule 4 — One phase at a time, and never move on while it is broken

This governs every other section:

1. Only ever work on **one phase** at a time, in the exact order phases are listed in
   this document (Phase 0 → Phase 1 → … → Phase 15 → **Phase 16: Final System
   Verification**).
2. Never begin implementation for a later phase — even a "quick" piece of it — while
   the current phase has not been marked `DONE` per Section 3.
3. Never skip a phase, reorder phases, or merge two phases into one pass.
4. At the end of every phase: run the full layered test suite (Section 5), rebuild and
   run the complete Docker stack (Section 6), confirm the CI pipeline is green
   (Section 7), fix all failures, update documentation and `PROGRESS.md`, commit, and
   push — in that order.
5. If any of the above fails, **do not move to the next phase.** Fix → test → fix →
   test → repeat until it passes, however many iterations that takes.
6. Phase 15 finishing does **not** mean the project is done. The project is only done
   once Phase 16 (Section 27) — which re-verifies the entire system as a whole — is
   also `DONE`.

---

# 2. Phase Gate Protocol (read before starting any phase)

## 2.1 Maintain `PROGRESS.md` at the repository root

Create this file during Phase 0 and keep it updated for the rest of the project. It is
the single source of truth for where the project stands.

```markdown
# IAM Build Progress

| Phase | Name                                    | Status      | Verified On | Commit |
|------:|-------------------------------------------|-------------|-------------|--------|
| 0     | Project Foundation                        | NOT_STARTED | -           | -      |
| 1     | Multi-Tenant Org + User Management        | NOT_STARTED | -           | -      |
| 2     | Password Authentication                   | NOT_STARTED | -           | -      |
| 3     | Token + Session Management                | NOT_STARTED | -           | -      |
| 4     | RBAC + Permission System                  | NOT_STARTED | -           | -      |
| 5     | Policy Engine / ABAC Foundation           | NOT_STARTED | -           | -      |
| 6     | MFA                                       | NOT_STARTED | -           | -      |
| 7     | OAuth 2.0 Authorization Server            | NOT_STARTED | -           | -      |
| 8     | OpenID Connect                            | NOT_STARTED | -           | -      |
| 9     | SSO                                       | NOT_STARTED | -           | -      |
| 10    | Client/Application Management             | NOT_STARTED | -           | -      |
| 11    | Client Credentials + Service Accounts     | NOT_STARTED | -           | -      |
| 12    | Audit Logging + Security Events           | NOT_STARTED | -           | -      |
| 13    | Security Controls                         | NOT_STARTED | -           | -      |
| 14    | Admin Dashboard                           | NOT_STARTED | -           | -      |
| 15    | Testing & End-to-End Validation           | NOT_STARTED | -           | -      |
| 16    | Final System Verification & Release       | NOT_STARTED | -           | -      |

Status values: `NOT_STARTED`, `IN_PROGRESS`, `BLOCKED (<reason>)`, `DONE`.
A phase may only be set to `DONE` once every item in Section 2.3 is checked.
```

## 2.2 Phase start protocol

Before writing a single line of code in a session, Antigravity must:

1. Open `PROGRESS.md`.
2. Find the **first row from the top that is not `DONE`**. That row is the only phase
   you are allowed to work on right now.
3. If that phase is `NOT_STARTED`, set it to `IN_PROGRESS` and commit that change
   alone (`chore: start phase N`) before writing feature code.
4. If any prior row is not `DONE`, stop and fix/finish that earlier phase instead —
   do not jump ahead.
5. Re-read the phase's section in this document in full before implementing anything.

## 2.3 Definition of Done (applies to every phase, including Phase 16)

A phase may be marked `DONE` in `PROGRESS.md` only when **all applicable** boxes are
true:

- [ ] Feature(s) for this phase implemented
- [ ] Database migrations complete and applied cleanly from a fresh database
- [ ] Unit tests written and passing (Section 5.1)
- [ ] Integration tests written and passing (Section 5.2)
- [ ] API/contract tests written and passing for every new endpoint (Section 5.3)
- [ ] Security/negative tests written and passing (Section 5.4)
- [ ] End-to-end UI test(s) added/updated for this phase's user journey (Section 5.5)
- [ ] **Full regression suite** re-run: every test from every previously completed
      phase still passes, not just this phase's new tests (Section 5.6)
- [ ] Frontend works for the relevant flow
- [ ] API behaves per spec (correct status codes for all documented cases)
- [ ] Error handling implemented (no unhandled exceptions leaking stack traces)
- [ ] Unauthorized path tested (401)
- [ ] Forbidden path tested (403)
- [ ] Tenant isolation tested, where applicable
- [ ] Security implications reviewed against Rule 3
- [ ] This phase's acceptance criteria (listed in its section) all pass
- [ ] `docker compose build && docker compose up` succeeds from a clean checkout with
      this phase's changes (Section 6)
- [ ] CI pipeline is green on the commit about to be pushed (Section 7)
- [ ] Documentation updated (README and/or module docs)
- [ ] `PROGRESS.md` row updated with date and commit hash
- [ ] Git commit created
- [ ] GitHub push to `SibamDash/Enterprise_IAM.git` successful and `git status` is clean

If **any** box is unchecked:

```text
DO NOT MOVE TO THE NEXT PHASE.
Fix → Test → Fix → Test → Repeat, for as many iterations as it takes.
```

## 2.4 Autonomous Resume Protocol (runs automatically — do not wait to be asked)

Antigravity has no memory between sessions or restarts. Every single time a session
starts — whether that's the very first run, a restart after a crash, a new day, or a
brand-new chat — Antigravity must run this bootstrap **on its own, before doing
anything else**, without the user needing to say "check PROGRESS.md" or "resume where
we left off." A user simply saying "continue the build" (or nothing at all) is enough
to trigger this.

### 2.4.1 `scripts/session-bootstrap.sh` (created in Phase 0, never skipped)

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "==> Syncing with remote"
git fetch origin
git pull origin main

echo "==> Checking for leftover local changes from an interrupted session"
if [[ -n "$(git status --porcelain)" ]]; then
  echo "WARNING: uncommitted changes found. These are from an interrupted session."
  echo "Do NOT discard them blindly — inspect, finish, or intentionally stash them"
  echo "before proceeding. Treat any code they touch as UNVERIFIED until tested."
  git status
fi

echo "==> Reading PROGRESS.md"
if [[ ! -f PROGRESS.md ]]; then
  echo "PROGRESS.md missing. This should only be possible before Phase 0 exists."
  exit 1
fi
cat PROGRESS.md

echo "==> Running full regression suite to verify PROGRESS.md matches reality"
./scripts/run-all-tests.sh
REGRESSION_EXIT_CODE=$?

if [[ $REGRESSION_EXIT_CODE -ne 0 ]]; then
  echo "STATE DRIFT DETECTED: PROGRESS.md claims phases are DONE, but the"
  echo "regression suite failed. Do not trust the DONE labels. Stop and fix"
  echo "before starting any new feature work (see Section 2.4.2)."
  exit 1
fi

echo "==> Bootstrap complete. PROGRESS.md is confirmed consistent with a passing build."
echo "==> Proceed to Section 2.2 (Phase Start Protocol)."
```

Antigravity runs this script (or performs the equivalent steps manually if the script
itself doesn't exist yet, e.g. during early Phase 0) at the start of every session,
automatically, with no user prompt required.

### 2.4.2 Decision rules based on the bootstrap result

| Situation found | Required action |
|---|---|
| Bootstrap passes cleanly, no uncommitted changes | Proceed directly to Section 2.2 — resume the first non-`DONE` phase. |
| Uncommitted changes exist, belonging to the current `IN_PROGRESS` phase | Do not assume they are correct or finished. Re-run that phase's tests specifically, then the full regression suite, before continuing new work or marking anything `DONE`. |
| Uncommitted changes exist but don't obviously match the `IN_PROGRESS` phase | Stop and inspect before writing any new code. Do not silently delete or silently keep — understand what they are first. If they're clearly abandoned scratch work, remove them only after confirming the tree returns to a passing state without them. |
| A phase marked `DONE` now fails the regression suite (**state drift**) | Treat it as broken, not done. Immediately set its `PROGRESS.md` status to `BLOCKED (regression failure — see <date>)` and fix it before any further phase work, even if a "later" phase looks tempting to jump to. This is a Rule 4 violation if ignored. |
| `PROGRESS.md` says a phase is `IN_PROGRESS` but Git history shows it was actually pushed complete | Re-run its tests to confirm before trusting either signal; update `PROGRESS.md` to match whichever the tests confirm. |
| Docker stack fails to build/start during bootstrap | Treat exactly like a regression failure — fix before any new feature work. |

**The governing principle:** `PROGRESS.md` is a *claim*, not a source of truth by
itself — the regression suite passing is the source of truth. The bootstrap's job is
to make sure the claim and the truth agree before any new work is layered on top. This
is what lets Antigravity work autonomously across restarts without a human having to
manually re-establish context every time.

---

# 3. Antigravity Operating Loop

Apply this loop to every individual feature within the current phase, and treat the
phase itself as complete only once every feature's loop has finished successfully and
Section 2.3's checklist is fully checked.

```text
┌──────────────────────────────────────┐
│ RUN SESSION BOOTSTRAP (Section 2.4)  │
│ automatically, every session start   │
└──────────────────┬───────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ READ PHASE REQUIREMENTS              │
└──────────────────┬───────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ CONFIRM PROGRESS.md ALLOWS THIS PHASE│
└──────────────────┬───────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ INSPECT EXISTING CODE                │
└──────────────────┬───────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ DESIGN BEFORE IMPLEMENTATION         │
└──────────────────┬───────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ IMPLEMENT ONE FEATURE                │
└──────────────────┬───────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ RUN UNIT + INTEGRATION TESTS         │
└──────────────────┬───────────────────┘
                   ▼
             ┌────────────┐
             │ ALL PASS?  │
             └─────┬──────┘
              NO   │   YES
              │    │
              ▼    └──────────────┐
       ┌───────────────┐          │
       │ FIX THE ISSUE │          │
       └───────┬───────┘          │
               │                  │
               └──→ RE-RUN ←──────┘
                                  │
                                  ▼
                    ┌────────────────────────┐
                    │ MANUAL USER FLOW TEST  │
                    └───────────┬────────────┘
                                ▼
                           ┌───────────┐
                           │ WORKS?    │
                           └─────┬─────┘
                            NO   │   YES
                            │    │
                            ▼    └─────────────┐
                         FIX                   │
                            │                 │
                            └→ TEST AGAIN ←───┘
                                              │
                                              ▼
                                  ┌──────────────────┐
                                  │ SECURITY REVIEW  │
                                  └────────┬─────────┘
                                           ▼
                                  ┌──────────────────┐
                                  │ MORE FEATURES IN │
                                  │ THIS PHASE?      │
                                  └────────┬─────────┘
                                     YES   │   NO
                                      │    │
                          back to IMPLEMENT│
                                           ▼
                              ┌─────────────────────────┐
                              │ RUN FULL REGRESSION     │
                              │ SUITE (Section 5.6)     │
                              └────────────┬────────────┘
                                     FAIL  │  PASS
                                      │    │
                                FIX → LOOP │
                                           ▼
                              ┌─────────────────────────┐
                              │ REBUILD DOCKER STACK    │
                              │ (Section 6)             │
                              └────────────┬────────────┘
                                     FAIL  │  PASS
                                      │    │
                                FIX → LOOP │
                                           ▼
                              ┌─────────────────────────┐
                              │ PUSH + CONFIRM CI GREEN │
                              │ (Section 7)             │
                              └────────────┬────────────┘
                                     FAIL  │  PASS
                                      │    │
                                FIX → LOOP │
                                           ▼
                              ┌─────────────────────────┐
                              │ RUN SECTION 2.3 DoD     │
                              └────────────┬────────────┘
                                ANY UNCHECKED? YES → FIX → LOOP
                                           │ NO
                                           ▼
                              MARK PHASE DONE IN PROGRESS.md,
                              ONLY THEN MOVE TO NEXT PHASE
```

**Critical instruction:** Antigravity must not interpret a partially working feature,
a red CI run, a failing Docker build, or a partially complete phase, as finished. Keep
looping until everything above passes — then, and only then, advance.

---

# 4. Git Workflow

Repository:

```text
https://github.com/SibamDash/Enterprise_IAM.git
```

All code for this project is pushed to this repository, and only this repository.

## 4.1 One-time setup

```bash
git clone https://github.com/SibamDash/Enterprise_IAM.git
cd Enterprise_IAM
```

## 4.2 Before starting work in any session

```bash
git pull origin main
```

Then follow the Phase Start Protocol in Section 2.2 before touching code.

## 4.3 After completing a phase (only after Section 2.3 is fully checked)

```bash
git status
git add .
git commit -m "<phase-specific message>"
git push origin main
```

Verify the push:

```bash
git status
git log -1
```

Then confirm the GitHub Actions run for this commit is green (Section 7) before
marking the phase `DONE`. The working tree must be clean after the phase is pushed.

## 4.4 Commit message convention

```text
feat: ...
fix: ...
security: ...
test: ...
docker: ...
ci: ...
refactor: ...
chore: ...
docs: ...
```

Each phase section lists its expected commit message(s) as a "Git checkpoint" — use
these as a baseline, adding more granular commits within a phase if useful, but never
skip the final phase-completion commit and push.

---

# 5. Testing Strategy (required every phase — no exceptions)

Every phase, without exception, must produce tests in **all** of the following layers
for the functionality it adds. This is what makes "the feature works properly" a
checkable fact instead of an opinion.

## 5.1 Unit tests

- Scope: pure business logic, service classes, validators, token/permission
  calculators, policy evaluators — isolated from the database, network, and framework
  where practical.
- Tooling: JUnit 5 + Mockito.
- Location: `backend/src/test/java/.../unit/...`
- Bar: every new class with non-trivial logic gets a unit test file; every branch
  (including error branches) is exercised at least once.

## 5.2 Integration tests

- Scope: repository layer, controller-to-database flows, Flyway migrations, Redis
  interactions, and any multi-component interaction within the backend.
- Tooling: JUnit 5 + Spring Boot Test + **Testcontainers** (real PostgreSQL/Redis
  containers, not mocks or in-memory substitutes).
- Location: `backend/src/test/java/.../integration/...`
- Bar: every new repository method and every new REST controller has at least one
  integration test hitting a real containerized database.

## 5.3 API / contract tests

- Scope: every new or changed endpoint, verifying request/response schema and every
  documented status code (`200/201/204/400/401/403/404/409/429`, as applicable).
- Tooling: Spring `MockMvc`/`WebTestClient`, or a Postman/Newman or REST-assured
  collection kept under `backend/src/test/java/.../api/` or `tests/api/`.
- Bar: for each endpoint, at minimum test the happy path, one invalid-input case, the
  unauthenticated case, and the unauthorized/forbidden case.

## 5.4 Security / negative tests

- Scope: the abuse cases relevant to the phase — auth bypass attempts, tenant
  boundary violations, token tampering/replay, privilege escalation, rate-limit and
  lockout behavior, injection attempts on new inputs.
- Tooling: same test frameworks as above, organized under
  `backend/src/test/java/.../security/...`.
- Bar: every checklist item under each phase's "Acceptance tests" / "Security
  requirement" section has a corresponding automated test — not just a manual check.

## 5.5 End-to-end (UI) tests

- Scope: the phase's documented user journey, driven through the real frontend
  against the real backend (via Docker Compose in a test profile).
- Tooling: Playwright (preferred) or Cypress.
- Location: `e2e/tests/phase-<N>-<name>.spec.ts`
- Bar: at least one E2E spec per phase that walks the primary UX flow documented in
  that phase's section (e.g. login → MFA → dashboard for Phase 6).

## 5.6 Regression suite (runs at the end of every phase, mandatory)

The regression suite is the **entire accumulated test suite from Phase 0 through the
current phase** — not just the current phase's new tests. A single script must be able
to run it all:

```bash
# from repo root
./scripts/run-all-tests.sh
```

`run-all-tests.sh` (created in Phase 0, extended every phase) should, at minimum:

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "==> Backend unit + integration tests"
(cd backend && ./mvnw -q test)

echo "==> Frontend unit tests"
(cd frontend && npm run test -- --run)

echo "==> Full stack up for E2E"
docker compose -f docker-compose.yml -f docker-compose.test.yml up -d --build

echo "==> End-to-end tests"
(cd e2e && npm ci && npx playwright test)

echo "==> Tear down"
docker compose -f docker-compose.yml -f docker-compose.test.yml down -v

echo "ALL TESTS PASSED"
```

Rule: **no phase may be marked `DONE` if running this script produces any failure**,
including failures in tests written for *earlier* phases. If a later phase's change
broke an earlier phase's behavior, that is a regression and must be fixed before
proceeding — this is exactly the mechanism that guarantees the finished system (Phase
16) still has every earlier feature working.

## 5.7 Phase → required new test artifacts (minimum)

| Phase | Minimum new test additions |
|---|---|
| 0 | Health check test, Docker Compose smoke test |
| 1 | Org/user CRUD unit + integration tests, tenant-isolation tests |
| 2 | Login/lockout/reset unit + integration + security tests |
| 3 | Token rotation/reuse-detection tests, session revocation tests |
| 4 | RBAC permission-matrix tests (role/group/permission combinations) |
| 5 | Policy conflict-resolution tests (ALLOW vs DENY precedence) |
| 6 | TOTP enroll/verify/recovery tests, MFA E2E journey |
| 7 | OAuth2 authorize/token/PKCE tests, redirect URI validation tests |
| 8 | OIDC discovery/JWKS/ID-token validation tests |
| 9 | SSO cross-application E2E journey (Journey D) |
| 10 | Application CRUD + secret-rotation tests |
| 11 | Client-credentials grant tests, service-account permission tests |
| 12 | Audit-event-emitted-for-every-action tests |
| 13 | Full security checklist (Section "Security test checklist" in Phase 13) automated |
| 14 | Dashboard metrics-match-backend-data tests |
| 15 | All 5 end-to-end journeys (A–E) automated and passing together |
| 16 | Full-system regression + demonstration script (Section 27) |

---

# 6. Docker & Containerization

The project must run as a fully containerized stack from a clean checkout with a
single command, from Phase 0 onward. Every later phase that adds a new service,
environment variable, or port must update these files as part of that phase's work —
this is part of the Definition of Done, not a separate task.

## 6.1 `docker-compose.yml` (root of repo)

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-iam}
      POSTGRES_USER: ${POSTGRES_USER:-iam_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-iam_password}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-iam_user}"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    restart: unless-stopped
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-iam}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-iam_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-iam_password}
      SPRING_REDIS_HOST: redis
      SPRING_PROFILES_ACTIVE: docker
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/api/v1/health"]
      interval: 10s
      timeout: 5s
      retries: 10

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    restart: unless-stopped
    depends_on:
      - backend
    environment:
      VITE_API_BASE_URL: http://localhost:8080
    ports:
      - "3000:80"

volumes:
  pgdata:
```

`docker-compose.test.yml` (overlay used only in CI/E2E runs) should point the
frontend/backend at a disposable test database and expose whatever ports Playwright
needs — created in Phase 0, extended as needed.

## 6.2 Backend `Dockerfile` (multi-stage)

```dockerfile
# ---- build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S iam && adduser -S iam -G iam
COPY --from=build /app/target/*.jar app.jar
USER iam
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## 6.3 Frontend `Dockerfile` (multi-stage, served via nginx)

```dockerfile
# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- run stage ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

## 6.4 `.dockerignore` (both backend and frontend)

```text
target/
node_modules/
dist/
.git/
.env
*.log
```

## 6.5 Docker acceptance criteria (checked every phase)

```bash
docker compose down -v          # clean slate
docker compose build            # must succeed with no errors
docker compose up -d            # must succeed
docker compose ps               # all services healthy
curl -f http://localhost:8080/api/v1/health   # 200 OK
```

If any of the above fails, the phase is not done — fix the Docker configuration before
proceeding, per Rule 4.

---

# 7. CI/CD Pipeline (GitHub Actions)

A CI pipeline must exist from Phase 0 and must go green on every push before a phase
can be marked `DONE` (Section 2.3). It is the automated enforcement of Section 5 and
Section 6 — nothing merges without unit, integration, and container verification
passing.

## 7.1 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: iam_test
          POSTGRES_USER: iam_user
          POSTGRES_PASSWORD: iam_password
        ports: ["5432:5432"]
        options: >-
          --health-cmd "pg_isready -U iam_user"
          --health-interval 5s
          --health-timeout 5s
          --health-retries 10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - name: Run backend unit + integration tests
        working-directory: backend
        run: ./mvnw -B verify
      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-reports
          path: backend/target/surefire-reports

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install dependencies
        working-directory: frontend
        run: npm ci
      - name: Lint
        working-directory: frontend
        run: npm run lint
      - name: Unit tests
        working-directory: frontend
        run: npm run test -- --run
      - name: Build
        working-directory: frontend
        run: npm run build

  docker-and-e2e:
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    steps:
      - uses: actions/checkout@v4
      - name: Build and start full stack
        run: docker compose -f docker-compose.yml -f docker-compose.test.yml up -d --build
      - name: Wait for backend health
        run: |
          for i in $(seq 1 30); do
            curl -sf http://localhost:8080/api/v1/health && break
            sleep 2
          done
      - uses: actions/setup-node@v4
        with:
          node-version: "20"
      - name: Run end-to-end tests
        working-directory: e2e
        run: |
          npm ci
          npx playwright install --with-deps
          npx playwright test
      - name: Tear down
        if: always()
        run: docker compose -f docker-compose.yml -f docker-compose.test.yml down -v
```

## 7.2 Optional `.github/workflows/release.yml` (from Phase 16 onward)

Once Phase 16 is reached, add a tag-triggered workflow that builds and pushes the
backend/frontend images to GHCR (or another registry) so the finished system is
demoable as built artifacts, not just source:

```yaml
name: Release

on:
  push:
    tags: ["v*"]

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: true
          tags: ghcr.io/sibamdash/enterprise-iam-backend:${{ github.ref_name }}
      - uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: ghcr.io/sibamdash/enterprise-iam-frontend:${{ github.ref_name }}
```

## 7.3 CI acceptance criteria (checked every phase)

- The `CI` workflow run for the pushed commit shows all jobs green
  (`backend`, `frontend`, `docker-and-e2e`).
- A red/failing run blocks marking the phase `DONE` — fix and re-push until green.

---

# 8. Recommended Technology Stack

Use the following unless an existing repository already establishes a different stack.

## Backend

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Bean Validation
- JUnit 5
- Mockito
- Testcontainers
- Maven

## Security

- OAuth 2.0
- OpenID Connect
- JWT
- RSA/ECDSA signing keys
- BCrypt or Argon2id for password hashing
- Secure random token generation
- Refresh-token rotation
- Token revocation
- PKCE

## Infrastructure

- Docker
- Docker Compose
- GitHub Actions (CI/CD)
- PostgreSQL
- Redis for rate limiting/session/cache use cases where appropriate

## Frontend

- React
- TypeScript
- Vite
- React Router
- A component/UI library if useful
- API client with centralized authentication handling

## Testing

- JUnit 5, Mockito, Testcontainers (backend unit/integration)
- Vitest or Jest (frontend unit)
- Playwright (end-to-end)

Do not introduce unnecessary microservices initially. Build a **modular monolith**
with clear domain boundaries. It should be possible to split modules into services
later.

---

# 9. High-Level Architecture

```text
                        ┌───────────────────────┐
                        │       End Users       │
                        └───────────┬───────────┘
                                    │
                         Browser / Mobile / API
                                    │
                                    ▼
                    ┌───────────────────────────┐
                    │       IAM Platform        │
                    │                           │
                    │  Authentication           │
                    │  Authorization            │
                    │  OAuth2 / OIDC            │
                    │  User Management           │
                    │  Tenant Management         │
                    │  Policy Engine             │
                    │  Session Management        │
                    │  Audit & Security Events   │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    ▼             ▼             ▼
               PostgreSQL       Redis       Key Store
                    │
                    ▼
          ┌───────────────────────────┐
          │ Registered Applications   │
          ├───────────────────────────┤
          │ CRM                       │
          │ HR                        │
          │ Finance                   │
          │ Internal Admin            │
          │ External APIs             │
          └───────────────────────────┘
```

Everything above runs inside Docker containers orchestrated by `docker-compose.yml`
(Section 6), and every change to it is validated by the CI pipeline (Section 7) before
a phase is considered done.

---

# 10. Core Domain Model

Start with these entities and expand only when a feature requires it.

```text
Organization
 ├── Users
 ├── Groups
 ├── Roles
 ├── Permissions
 ├── Policies
 ├── Applications / OAuth Clients
 ├── Sessions
 ├── Service Accounts
 └── Audit Events

User
 ├── Roles
 ├── Groups
 ├── MFA Factors
 ├── Sessions
 └── Organization

Role
 └── Permissions

Group
 └── Users + Roles

Application / OAuth Client
 ├── Redirect URIs
 ├── Allowed Grant Types
 ├── Scopes
 └── Client Credentials

Policy
 └── Conditions + Effect

Session
 └── User + Device + Token metadata

AuditEvent
 └── Actor + Action + Resource + Result + Timestamp + IP
```

---

# 11. Phase 0 — Project Foundation

## Objective

Create a clean repository, development environment, Docker stack, CI pipeline, and
testing harness that every later phase builds on.

## Implement

- Backend project
- Frontend project
- Database connection
- Flyway migrations
- `docker-compose.yml`, `docker-compose.test.yml`, backend/frontend `Dockerfile`s
  (Section 6)
- `.github/workflows/ci.yml` (Section 7)
- `scripts/run-all-tests.sh` (Section 5.6)
- `scripts/session-bootstrap.sh` (Section 2.4)
- Environment configuration
- Global error handling
- Logging
- API versioning
- Health endpoint
- README
- `PROGRESS.md` (Section 2.1)
- `.env.example`
- `.gitignore`, `.dockerignore`

## Required endpoints

```text
GET /api/v1/health
```

## Acceptance criteria

- Application starts from a clean checkout.
- `docker compose up --build` starts Postgres, Redis, backend, and frontend
  successfully (Section 6.5).
- Database migrations execute successfully.
- Health endpoint returns success both locally and through the container.
- `./scripts/run-all-tests.sh` runs and passes (even with a minimal test at this
  stage).
- `./scripts/session-bootstrap.sh` runs cleanly end to end (Section 2.4).
- The GitHub Actions `CI` workflow runs and is green on the initial commit.
- `PROGRESS.md` exists with all 17 phases (0–16) listed as `NOT_STARTED`.

## Loop

```text
Build → Test → Dockerize → Run CI → Verify → Fix → Repeat
```

## Git checkpoint

```bash
git add .
git commit -m "chore: establish IAM project foundation, docker stack, and CI pipeline"
git push origin main
```

Mark Phase 0 `DONE` in `PROGRESS.md` only once Section 2.3 is fully satisfied.

---

# 12. Phase 1 — Multi-Tenant Organization + User Management

## Objective

Create the identity and tenant foundation.

## Admin UX

```text
Admin Login
   ↓
Dashboard
   ↓
Organizations
   ↓
Create Organization
   ↓
Create User
   ↓
Assign Role
```

## Implement

### Organization

- Create
- Read
- Update
- Deactivate
- Organization status
- Tenant isolation

### Users

- Create
- Read
- Update
- Deactivate
- Search
- Pagination
- User status
- Organization membership

### Roles

- Create
- Update
- Delete where safe
- Assign to users

### Groups

- Create
- Add/remove users
- Group role assignment

## Security requirement

Every tenant-owned resource must be associated with an organization.

Never trust a tenant ID supplied by the client.

Derive tenant context from the authenticated identity/session wherever possible.

## Acceptance tests

```text
Tenant A user → Tenant A user → ALLOW
Tenant A user → Tenant B user → DENY
Tenant A admin → Tenant A user → ALLOW
Tenant A user → Admin operation → DENY
Deactivated user → protected endpoint → DENY
```

Per Section 5.7: add unit + integration tests for org/user CRUD and tenant isolation,
and an E2E spec for the admin UX flow above.

## Git checkpoint

```bash
git add .
git commit -m "feat: add multi-tenant user and organization management"
git push origin main
```

Do not proceed until the full regression suite (Section 5.6) passes and Section 2.3 is
fully satisfied.

---

# 13. Phase 2 — Password Authentication

## User UX

```text
Application
   ↓
Sign in
   ↓
Email
Password
   ↓
Login
```

## Implement

- Password hashing
- Login endpoint
- Credential verification
- Account status verification
- Failed login tracking
- Account lockout
- Password strength policy
- Password change
- Password reset
- Reset token expiration
- Generic authentication errors
- Rate limiting

## Important security behavior

Do not reveal whether an email address exists.

Bad:

```text
User does not exist.
```

Good:

```text
If an account exists, reset instructions have been sent.
```

## Login flow

```text
Credentials
    ↓
Find identity
    ↓
Check account
    ↓
Check lockout
    ↓
Verify password
    ↓
Update login metadata
    ↓
Continue authentication
```

## Failure scenarios

| Problem | Required behavior |
|---|---|
| Wrong password | Reject + record failure |
| Repeated failures | Rate limit / lock account |
| Disabled account | Reject |
| Expired reset token | Reject + allow new request |
| Unknown email | Generic response |
| Password too weak | Reject |

Per Section 5.7: add unit + integration + security tests for login, lockout, and
password reset, per the table above.

## Git checkpoint

```bash
git add .
git commit -m "feat: implement secure password authentication"
git push origin main
```

---

# 14. Phase 3 — Token + Session Management

## Objective

Create secure application sessions.

## Implement

- Short-lived access tokens
- Refresh tokens
- Refresh-token rotation
- Token family tracking
- Revocation
- Session records
- Logout
- Logout-all-devices
- Session timeout
- Device/session metadata

## Flow

```text
Login
 ↓
Access Token + Refresh Token
 ↓
API requests
 ↓
Access Token expires
 ↓
Refresh Token
 ↓
Rotate Refresh Token
 ↓
New Access Token
```

## Refresh-token reuse detection

If an old refresh token is reused:

```text
Reuse detected
     ↓
Invalidate token family
     ↓
Terminate associated sessions
     ↓
Create security event
```

## User UX

```text
Settings
  ↓
Active Sessions

Chrome / Windows
Last active: 2 minutes ago
[Revoke]

Android
Last active: 1 hour ago
[Revoke]

[Logout all devices]
```

Per Section 5.7: add token-rotation and reuse-detection tests, plus session-revocation
tests.

## Git checkpoint

```bash
git add .
git commit -m "feat: add secure token and session management"
git push origin main
```

---

# 15. Phase 4 — RBAC + Permission System

## Objective

Implement enterprise authorization.

## Permission format

Use granular permissions:

```text
USER_READ
USER_CREATE
USER_UPDATE
USER_DELETE

ROLE_READ
ROLE_CREATE
ROLE_UPDATE
ROLE_DELETE

APPLICATION_READ
APPLICATION_CREATE
APPLICATION_UPDATE
APPLICATION_DELETE

AUDIT_READ
```

## Authorization model

```text
User
 ↓
Groups
 ↓
Roles
 ↓
Permissions
 ↓
Authorization Check
 ↓
ALLOW / DENY
```

## API example

```text
GET /api/v1/users
```

Required permission:

```text
USER_READ
```

## Failure behavior

```text
Unauthenticated → 401
Authenticated but unauthorized → 403
```

## Admin UX

```text
Roles
 └── Sales Manager
      ├── USER_READ
      ├── USER_UPDATE
      ├── REPORT_READ
      └── CUSTOMER_READ
```

## Acceptance tests

Test:

- Direct role assignment
- Group-derived permissions
- Missing permission
- Multiple roles
- Role removal
- Permission removal
- Cross-tenant access
- Admin permissions

Per Section 5.7: build a permission-matrix test suite covering every combination
above, not just one example of each.

## Git checkpoint

```bash
git add .
git commit -m "feat: implement enterprise RBAC and permissions"
git push origin main
```

---

# 16. Phase 5 — Policy Engine / ABAC Foundation

## Objective

Move beyond static RBAC.

Authorization should eventually support:

```text
WHO
+
WHAT
+
WHICH RESOURCE
+
WHICH ACTION
+
CONTEXT
=
ACCESS DECISION
```

Example:

```text
User:
department = Finance

Resource:
department = Finance

Action:
READ

Policy:
Finance employees may read Finance reports.

Result:
ALLOW
```

Another example:

```text
User:
role = Employee

Action:
DELETE

Policy:
Employees cannot delete records.

Result:
DENY
```

## Policy model

```text
Policy
 ├── Name
 ├── Effect: ALLOW / DENY
 ├── Actions
 ├── Resources
 ├── Conditions
 └── Priority
```

## Policy evaluation

```text
Request
   ↓
Authentication
   ↓
RBAC
   ↓
ABAC / Policy Engine
   ↓
Final Decision
```

## Acceptance tests

Include conflicting policies:

```text
ALLOW policy
+
DENY policy
=
deterministic documented result
```

Never leave policy precedence ambiguous. Add a dedicated test suite of conflicting
policy combinations (Section 5.7).

## Git checkpoint

```bash
git add .
git commit -m "feat: add policy-based authorization engine"
git push origin main
```

---

# 17. Phase 6 — MFA

## Objective

Add strong authentication.

## Initial MFA method

Implement TOTP first.

Later support:

- Recovery codes
- Email OTP if justified
- WebAuthn/passkeys as an advanced phase

## Enrollment UX

```text
Account Settings
     ↓
Security
     ↓
Enable MFA
     ↓
Show QR code
     ↓
Authenticator App
     ↓
Enter code
     ↓
MFA enabled
```

## Login UX

```text
Email
 ↓
Password
 ↓
MFA challenge
 ↓
TOTP
 ↓
Authenticated
```

## Recovery

Generate one-time recovery codes.

Never store recovery codes in plaintext.

## Acceptance tests

- Correct TOTP
- Incorrect TOTP
- Expired/invalid code
- MFA disabled
- MFA reset
- Recovery code
- Reused recovery code
- Rate limiting

Per Section 5.7: automate all of the above plus one full E2E "enroll → logout → login
with MFA" journey.

## Git checkpoint

```bash
git add .
git commit -m "feat: implement TOTP multi-factor authentication"
git push origin main
```

---

# 18. Phase 7 — OAuth 2.0 Authorization Server

This is a major differentiator of the project.

Other applications should authenticate against **our IAM**.

## Registered application

Admin creates:

```text
Application:
Acme CRM

Client ID:
generated

Client Secret:
generated for confidential clients

Redirect URI:
https://crm.acme.com/oauth/callback

Grant Types:
authorization_code

Scopes:
openid
profile
email
```

## Authorization Code + PKCE flow

```text
User
 ↓
CRM
 ↓
IAM /authorize
 ↓
Login
 ↓
MFA if required
 ↓
Consent if required
 ↓
Authorization Code
 ↓
CRM
 ↓
POST /oauth/token
 ↓
Access Token
 + ID Token
 + Refresh Token where applicable
```

## Required endpoints

```text
GET  /oauth/authorize
POST /oauth/token
POST /oauth/revoke
GET  /.well-known/openid-configuration
GET  /oauth/jwks
```

Exact endpoint naming may be adjusted to protocol conventions.

## Security requirements

- Validate client ID
- Validate redirect URI exactly
- Validate response type
- Validate scopes
- Validate PKCE
- Protect authorization codes
- Short authorization-code lifetime
- One-time authorization-code use
- Sign tokens
- Publish public keys
- Never expose client secrets

Per Section 5.7: automate authorize/token/PKCE flows and redirect-URI validation as
both integration and security tests.

## Git checkpoint

```bash
git add .
git commit -m "feat: implement OAuth2 authorization server"
git push origin main
```

---

# 19. Phase 8 — OpenID Connect

## Objective

Allow applications to authenticate users and receive identity claims.

Implement:

- OIDC discovery
- ID tokens
- `openid` scope
- Standard claims
- UserInfo endpoint
- JWKS endpoint
- Nonce validation
- Audience validation
- Issuer validation

## Example

```text
CRM
 ↓
IAM
 ↓
User authenticates
 ↓
ID Token
 ↓
CRM validates:
 ├── signature
 ├── issuer
 ├── audience
 ├── expiry
 └── nonce
 ↓
User logged in
```

Per Section 5.7: automate discovery-document, JWKS, and ID-token validation tests.

## Git checkpoint

```bash
git add .
git commit -m "feat: implement OpenID Connect identity provider"
git push origin main
```

---

# 20. Phase 9 — SSO

## Objective

Demonstrate the main enterprise SSO experience.

Scenario:

```text
User logs into CRM
      ↓
IAM session created
      ↓
User opens HR application
      ↓
HR redirects to IAM
      ↓
IAM sees existing session
      ↓
No password required
      ↓
User returns to HR authenticated
```

## Acceptance test

The same user must be able to access two registered applications without entering
credentials twice during the valid IAM session. Automate this as an E2E journey
(Section 5.5, Journey D in Section 26).

## Git checkpoint

```bash
git add .
git commit -m "feat: implement cross-application SSO"
git push origin main
```

---

# 21. Phase 10 — Client/Application Management

## Admin UX

```text
Applications
   ↓
Register Application
   ↓
Name
Protocol
Redirect URIs
Grant Types
Scopes
   ↓
Create
```

## Implement

- Application CRUD
- Public/confidential clients
- Client credentials
- Client secret rotation
- Secret revocation
- Redirect URI management
- Allowed grant types
- Allowed scopes
- Application status

## Security

Never return a client secret after initial creation unless deliberately designed as a
secure one-time reveal.

Never log secrets.

Per Section 5.7: add CRUD and secret-rotation/revocation tests, including a test that
asserts secrets never appear in logs or repeated API responses.

## Git checkpoint

```bash
git add .
git commit -m "feat: add OAuth application and client management"
git push origin main
```

---

# 22. Phase 11 — Client Credentials + Service Accounts

## Objective

Support machine-to-machine authentication.

Example:

```text
Reporting Service
      ↓
POST /oauth/token
grant_type=client_credentials
      ↓
IAM
      ↓
Access Token
      ↓
Reporting API
```

## Implement

- Service accounts
- Client credentials grant
- Scopes
- Service-account permissions
- Credential rotation
- Revocation
- Audit events

Never treat service accounts as normal human users.

Per Section 5.7: add client-credentials-grant tests and service-account permission
tests.

## Git checkpoint

```bash
git add .
git commit -m "feat: add machine-to-machine authentication"
git push origin main
```

---

# 23. Phase 12 — Audit Logging + Security Events

## Objective

Every security-sensitive action must be traceable.

## Audit event fields

At minimum:

```text
id
timestamp
tenantId
actorId
action
resourceType
resourceId
result
ipAddress
userAgent
metadata
```

## Examples

```text
USER_CREATED
USER_DISABLED
LOGIN_SUCCESS
LOGIN_FAILED
MFA_ENABLED
MFA_FAILED
ROLE_ASSIGNED
ROLE_REMOVED
PERMISSION_DENIED
TOKEN_REVOKED
SESSION_REVOKED
CLIENT_CREATED
CLIENT_SECRET_ROTATED
POLICY_CHANGED
```

## Admin UX

```text
Audit Logs

Time        Actor    Action          Result
10:32       Admin    USER_CREATED    SUCCESS
10:35       Rahul    LOGIN_SUCCESS   SUCCESS
10:40       Rahul    USER_DELETE     DENIED
10:41       Admin    ROLE_CHANGED    SUCCESS
```

Per Section 5.7: add a test asserting that every security-sensitive action implemented
so far (from Phases 1–11) actually emits the corresponding audit event — this doubles
as a regression check on earlier phases.

## Git checkpoint

```bash
git add .
git commit -m "feat: implement enterprise audit logging and security events"
git push origin main
```

---

# 24. Phase 13 — Security Controls

Implement:

- API rate limiting
- Login rate limiting
- Account lockout
- Password policies
- CORS policy
- CSRF protection where applicable
- Secure cookies where applicable
- Security headers
- Input validation
- Output/error sanitization
- Secret management
- Key rotation design
- Token expiration
- Revocation
- Replay protection
- Structured security logging

## Security test checklist

Attempt:

```text
Brute force
Credential stuffing simulation
JWT modification
Expired token
Wrong issuer
Wrong audience
Wrong signature
Refresh-token replay
Unauthorized tenant access
Unauthorized role escalation
Invalid redirect URI
Authorization-code replay
Missing PKCE
Invalid scope
Disabled user access
Revoked session access
```

All must be handled correctly, and **every item in this checklist must be an
automated test**, not a manual pass — this is the phase where Section 5.7's "full
security checklist automated" bar applies.

## Git checkpoint

```bash
git add .
git commit -m "security: harden IAM platform security controls"
git push origin main
```

---

# 25. Phase 14 — Admin Dashboard

Build a usable enterprise dashboard.

## Main navigation

```text
Dashboard
Organizations
Users
Groups
Roles
Permissions
Applications
Policies
Sessions
Audit Logs
Security Events
Settings
```

## Dashboard metrics

Examples:

```text
Total Users
Active Users
Locked Accounts
Active Sessions
Registered Applications
Failed Logins
Recent Security Events
```

Do not fabricate metrics. All dashboard values must come from the backend. Add a test
that asserts each displayed metric matches a direct backend query (Section 5.7).

## Git checkpoint

```bash
git add .
git commit -m "feat: build enterprise admin dashboard"
git push origin main
```

---

# 26. Phase 15 — Testing & End-to-End Validation

## Unit tests

Test domain logic independently.

## Integration tests

Use a real PostgreSQL container where appropriate.

## API tests

Verify:

```text
401
403
200
201
204
400
404
409
429
```

as applicable.

## End-to-end journeys

### Journey A — New employee

```text
Admin
 ↓
Create User
 ↓
Assign Role
 ↓
Invitation
 ↓
User accepts
 ↓
Password created
 ↓
MFA enrolled
 ↓
Login
 ↓
Access granted
```

### Journey B — Authorization

```text
User logs in
 ↓
Requests protected resource
 ↓
Permission evaluated
 ↓
Allowed
 ↓
Action performed
 ↓
Audit logged
```

### Journey C — Authorization denial

```text
User
 ↓
Protected resource
 ↓
Permission missing
 ↓
403
 ↓
Audit event
```

### Journey D — SSO

```text
Login to App A
 ↓
IAM session
 ↓
Open App B
 ↓
IAM recognizes session
 ↓
No second password
 ↓
App B authenticated
```

### Journey E — Token replay

```text
Refresh Token #1
 ↓
Refresh
 ↓
Token #1 rotated
 ↓
Token #1 reused
 ↓
Replay detected
 ↓
Token family revoked
 ↓
Security event
```

## Objective for this phase

All five journeys (A–E) must be automated as Playwright/API test specs and run
together, in addition to the full regression suite from Section 5.6.

## Git checkpoint

```bash
git add .
git commit -m "test: complete end-to-end validation suite"
git push origin main
```

---

# 27. Phase 16 — Final System Verification & Release

## Objective

Prove the finished product actually is the finished product: every phase's feature
still works, together, in the containerized system, with CI green — not just the
individually-passing phase tests along the way. **The build is not "complete" until
this phase is `DONE`.**

## Implement / perform

1. **Full regression pass**: run `./scripts/run-all-tests.sh` (Section 5.6) one final
   time against a completely clean checkout and a freshly built Docker stack. Every
   test from every phase (0–15) must pass together, in one run.
2. **Full demonstration script**: write and run a single scripted walkthrough
   (`scripts/demo.sh` or an E2E spec) that exercises, in order, all 17 items from the
   Final Project Standard demonstration list (Section 29):
   organization creation → user creation → role assignment → permission configuration
   → application registration → IAM authentication → MFA → OAuth/OIDC token issuance →
   application access → RBAC/ABAC enforcement → a denied-access case → SSO across two
   applications → session management → token revocation → audit log inspection → a
   triggered security event → a tenant-isolation check.
3. **Docker production build check**: `docker compose build --no-cache && docker
   compose up`, confirm all services report healthy, and confirm the full demo script
   passes against the container stack (not just local dev servers).
4. **CI green on final commit**: confirm the `CI` workflow (Section 7.1) is green, and
   optionally cut a `v1.0.0` tag to trigger the release workflow (Section 7.2) and
   publish backend/frontend images.
5. **Documentation pass**: update the README with setup instructions, architecture
   summary, and a link to/inclusion of the demo script output, so a third party can
   clone the repo and verify the system themselves.
6. **Known-issues review**: if anything from earlier phases was deferred or stubbed,
   either finish it now or explicitly document it in `PROGRESS.md`/README as a known
   limitation — do not leave silently broken functionality.

## Acceptance criteria

- [ ] `./scripts/run-all-tests.sh` passes with zero failures on a clean checkout
- [ ] All 17 demonstration items in Section 29 are shown working in one continuous
      run (script or recorded walkthrough)
- [ ] `docker compose build --no-cache && docker compose up` succeeds and all services
      are healthy
- [ ] GitHub Actions `CI` workflow is green on the final commit
- [ ] No phase in `PROGRESS.md` is anything other than `DONE`
- [ ] README accurately describes how to run and verify the whole system

## Git checkpoint

```bash
git add .
git commit -m "chore: final system verification — all phases integrated and passing"
git push origin main
# optional:
git tag v1.0.0
git push origin v1.0.0
```

Only once every acceptance criterion above is checked may Phase 16 be marked `DONE` in
`PROGRESS.md`. At that point, and not before, the Enterprise IAM Platform is complete.

---

# 28. Final End-to-End User Flow

```text
                         ┌───────────────┐
                         │     USER      │
                         └───────┬───────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │  APPLICATION    │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ OAuth / Login   │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ Authentication  │
                        │ Password + MFA  │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │   Identity      │
                        │ User + Tenant   │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ Authorization   │
                        │ RBAC + ABAC     │
                        └────────┬────────┘
                                 │
                          ┌──────┴──────┐
                          │             │
                       DENY           ALLOW
                          │             │
                          ▼             ▼
                       403        Create Session
                                        │
                                        ▼
                                 Access Token
                                        │
                                        ▼
                                 Application API
                                        │
                                        ▼
                                  User Action
                                        │
                                        ▼
                                   Audit Log
                                        │
                                        ▼
                                     Logout
```

---

# 29. Final Project Standard

The final portfolio project should demonstrate that the developer understands:

- Identity
- Authentication
- Authorization
- Multi-tenancy
- RBAC
- ABAC/policy evaluation
- OAuth 2.0
- OIDC
- SSO
- PKCE
- JWT
- Token lifecycle
- Refresh-token rotation
- Session management
- MFA
- Password security
- API security
- Service accounts
- Auditability
- Security event handling
- Database design
- REST APIs
- Frontend integration
- Layered automated testing (unit, integration, security, E2E, regression)
- Docker containerization
- CI/CD
- Secure software engineering

The final demonstration should be able to show:

```text
1. Create organization
2. Create users
3. Assign roles
4. Configure permissions
5. Register an application
6. Authenticate through IAM
7. Complete MFA
8. Issue OAuth/OIDC tokens
9. Access the application
10. Enforce RBAC/ABAC
11. Demonstrate denied access
12. Demonstrate SSO
13. Manage sessions
14. Revoke tokens
15. Show audit logs
16. Demonstrate a security event
17. Demonstrate tenant isolation
```

All 17 of the above must be shown working **together, in the Dockerized system, after
Phase 16 is complete** — this is the target system, not merely a login service.

---

# Appendix — Quick Reference Card

Keep this near you as a one-glance summary of Sections 1–7:

```text
0. At the START of every session, automatically (never wait to be told): run
   scripts/session-bootstrap.sh — pull latest, check for leftover uncommitted work,
   and run the full regression suite to confirm PROGRESS.md matches reality. If it
   doesn't (state drift), fix that before anything else.
1. Read PROGRESS.md → find first non-DONE phase (0 through 16) → that is your ONLY
   phase.
2. Implement one feature at a time inside that phase.
3. For each feature: implement → unit test → integration test → fix → re-test → loop
   until it truly works.
4. Never weaken security to pass a test or a CI run.
5. When every feature in the phase passes, run the FULL regression suite (Section 5.6)
   — every earlier phase's tests must still pass, not just this phase's.
6. Rebuild and run the full Docker stack (Section 6) — it must come up healthy.
7. Push and confirm the CI pipeline (Section 7) is green on that commit.
8. Run the full Definition of Done checklist (Section 2.3). If anything is unchecked:
   fix → test → repeat. Do not proceed.
9. If everything is checked: update docs + PROGRESS.md → commit → push → verify
   `git status` is clean and CI is green.
10. Only now may you move to the next phase. Repeat from step 1.
11. After Phase 15, do NOT stop — Phase 16 (Section 27) re-verifies the entire system
    end to end. The project is only complete once Phase 16 is DONE.
```