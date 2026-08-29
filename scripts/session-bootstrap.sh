#!/usr/bin/env bash
set -euo pipefail

echo "==> Verifying remote identity"
EXPECTED="https://github.com/SibamDash/Enterprise_IAM.git"
ACTUAL="$(git remote get-url origin)"
if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "FATAL: origin is '$ACTUAL', expected '$EXPECTED'. Refusing to proceed." >&2
  exit 1
fi

echo "==> Checking working tree before sync"
if [[ -n "$(git status --porcelain)" ]]; then
  echo "UNCOMMITTED WORK DETECTED."
  echo "Inspect and verify the interrupted work before syncing."
  git status --short
else
  echo "==> Working tree clean; syncing with remote using fast-forward only"
  git fetch origin
  git pull --ff-only origin main
fi

echo "==> Reading PROGRESS.md"
if [[ ! -f PROGRESS.md ]]; then
  echo "FATAL: PROGRESS.md missing. Refusing to continue."
  exit 1
fi
cat PROGRESS.md

echo "==> Running full regression suite to verify PROGRESS.md matches reality"
set +e
bash ./scripts/run-all-tests.sh
REGRESSION_EXIT_CODE=$?
set -e

if [[ $REGRESSION_EXIT_CODE -ne 0 ]]; then
  echo "STATE DRIFT / REGRESSION DETECTED."
  echo "Do not start new feature work. Fix the failure and rerun the suite."
  exit "$REGRESSION_EXIT_CODE"
fi

echo "==> Bootstrap complete. Actual system state is verified."
echo "==> Proceed to Section 2.2 (Phase Start Protocol)."
