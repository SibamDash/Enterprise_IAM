#!/usr/bin/env bash
set -e

echo "==> Starting Redis and Postgres for backend tests"
docker compose -f docker-compose.yml -f docker-compose.test.yml up -d redis postgres

echo "==> Backend unit + integration tests"
MSYS_NO_PATHCONV=1 docker compose -f docker-compose.yml -f docker-compose.test.yml run --rm test-runner ./mvnw -q test -Dspring.data.redis.host=redis

echo "==> Frontend unit tests"
(cd frontend && npm run test -- --run)

echo "==> Full stack up for E2E"
docker compose -f docker-compose.yml -f docker-compose.test.yml up -d --build

echo "==> End-to-end tests"
(cd e2e && npm ci && npx playwright test)

echo "==> Tear down"
docker compose -f docker-compose.yml -f docker-compose.test.yml down -v

echo "ALL TESTS PASSED"