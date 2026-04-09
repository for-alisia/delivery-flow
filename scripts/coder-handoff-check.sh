#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

# Usage: scripts/coder-handoff-check.sh <feature-name>
if [[ $# -lt 1 ]]; then
  echo "[handoff-check] Usage: $0 <feature-name>"
  exit 1
fi

FEATURE="$1"
STATE_FILE="artifacts/flow-logs/${FEATURE}.json"
ERRORS=0

echo "[handoff-check] Checking handoff readiness for feature: ${FEATURE}"

# 1. Flow-log state file exists
if [[ ! -f "${STATE_FILE}" ]]; then
  echo "  FAIL: State file missing: ${STATE_FILE}"
  ERRORS=$((ERRORS + 1))
else
  echo "  PASS: State file exists"
fi

# 2. Flow-log status — checks recorded
STATUS_OUTPUT=$(node flow-log/flow-log.mjs status --feature "${FEATURE}" 2>&1) || true

# Check verifyQuick
if echo "${STATUS_OUTPUT}" | grep -q '"verifyQuick"' && echo "${STATUS_OUTPUT}" | grep -q '"finalCheck": "PASS"'; then
  echo "  PASS: finalCheck recorded as PASS"
else
  echo "  FAIL: finalCheck not recorded as PASS in flow-log"
  ERRORS=$((ERRORS + 1))
fi

# 3. Changed files tracked
CHANGES=$(echo "${STATUS_OUTPUT}" | grep -o '"changes"' || true)
if node flow-log/flow-log.mjs get --feature "${FEATURE}" 2>&1 | grep -q '"files"'; then
  FILE_COUNT=$(node flow-log/flow-log.mjs get --feature "${FEATURE}" 2>&1 | grep -o '"files"' | wc -l || echo "0")
  echo "  PASS: Changed files tracked in flow-log"
else
  echo "  WARN: No changed files recorded — verify via flow-log add-change"
  ERRORS=$((ERRORS + 1))
fi

# 4. Run final-check.sh to verify code compiles and passes quality gates
echo "[handoff-check] Running final-check.sh..."
if "${SCRIPT_DIR}/final-check.sh"; then
  echo "  PASS: final-check.sh succeeded"
else
  echo "  FAIL: final-check.sh failed"
  ERRORS=$((ERRORS + 1))
fi

# Summary
echo ""
if [[ ${ERRORS} -eq 0 ]]; then
  echo "[handoff-check] ALL CHECKS PASSED — ready for handoff"
  exit 0
else
  echo "[handoff-check] ${ERRORS} CHECK(S) FAILED — fix before returning to Team Lead"
  exit 1
fi
