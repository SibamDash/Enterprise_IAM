#!/usr/bin/env bash
set -euo pipefail

echo "==> Backend unit + integration tests"
(cd backend && docker run --rm -v "/$(pwd)":/app -v m2_cache:/root/.m2 -w /app -v /var/run/docker.sock:/var/run/docker.sock eclipse-temurin:21-jdk-jammy ./mvnw -q test)

echo "==> Frontend unit tests"
(cd frontend && npm run test -- --run)

echo "==> Full stack up for E2E"
docker compose -f docker-compose.yml -f docker-compose.test.yml up -d --build

echo "==> End-to-end tests"
(cd e2e && npm ci && npx playwright test)

echo "==> Tear down"
docker compose -f docker-compose.yml -f docker-compose.test.yml down -v

echo "ALL TESTS PASSED"
