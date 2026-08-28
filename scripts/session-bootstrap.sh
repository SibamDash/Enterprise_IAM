#!/usr/bin/env bash
set -euo pipefail

echo "==> Verifying remote identity"
EXPECTED="https://github.com/SibamDash/Enterprise_IAM.git"
ACTUAL="$(git remote get-url origin)"
if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "FATAL: origin is '$ACTUAL', expected '$EXPECTED'. Refusing to proceed." >&2
  exit 1
fi

echo "==> Syncing with remote"
git fetch origin || echo "Skipping fetch, no remote"
git pull origin main || echo "Skipping pull, maybe no remote or branch"

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
