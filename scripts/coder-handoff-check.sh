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

# 2. Flow-log status — parse JSON checks via node one-liner for reliability
STATUS_JSON=$(node flow-log/flow-log.mjs status --feature "${FEATURE}" 2>/dev/null) || {
  echo "  FAIL: Could not read flow-log status for ${FEATURE}"
  exit 1
}

FINAL_CHECK=$(echo "${STATUS_JSON}" | node -e "process.stdin.resume(); let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{const s=JSON.parse(d).status; console.log(s.finalCheck ?? 'MISSING')})" 2>/dev/null) || FINAL_CHECK="MISSING"

# finalCheck
if [[ "${FINAL_CHECK}" == "PASS" ]]; then
  echo "  PASS: finalCheck recorded as PASS"
else
  echo "  FAIL: finalCheck is '${FINAL_CHECK}' — must be PASS"
  ERRORS=$((ERRORS + 1))
fi

# 3. Changed files tracked (non-empty array)
FILE_COUNT=$(node -e "const s=JSON.parse(require('fs').readFileSync('${STATE_FILE}','utf8')); console.log(s.changes.files.length)" 2>/dev/null) || FILE_COUNT="0"
if [[ "${FILE_COUNT}" -gt 0 ]]; then
  echo "  PASS: ${FILE_COUNT} changed file(s) tracked in flow-log"
else
  echo "  WARN: No changed files recorded — run flow-log add-change before handoff"
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
