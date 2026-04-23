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
STATUS_JSON=$(scripts/flow-log.sh status --feature "${FEATURE}" 2>/dev/null) || {
  echo "  FAIL: Could not read flow-log status for ${FEATURE}"
  exit 1
}
SUMMARY_JSON=$(scripts/flow-log.sh summary --feature "${FEATURE}" 2>/dev/null) || {
  echo "  FAIL: Could not read flow-log summary for ${FEATURE}"
  exit 1
}

parse_check() {
  local check_name="$1"
  echo "${STATUS_JSON}" | node -e "process.stdin.resume(); let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{const s=JSON.parse(d).status; console.log(s.${check_name} ?? 'MISSING')})" 2>/dev/null || echo "MISSING"
}

parse_status_flag() {
  local flag_name="$1"
  echo "${STATUS_JSON}" | node -e "process.stdin.resume(); let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{const s=JSON.parse(d).status; console.log(String(s.${flag_name} ?? 'MISSING'))})" 2>/dev/null || echo "MISSING"
}

parse_changed_file_count() {
  echo "${SUMMARY_JSON}" | node -e "process.stdin.resume(); let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{const s=JSON.parse(d).summary; const batchFiles=Array.isArray(s?.batches?.current?.changedFiles)?s.batches.current.changedFiles:[]; const files=batchFiles.length>0?batchFiles:(Array.isArray(s.changedFiles)?s.changedFiles:[]); console.log(files.length)})" 2>/dev/null || echo "0"
}

# verifyQuick
VERIFY_QUICK=$(parse_check verifyQuick)
if [[ "${VERIFY_QUICK}" == "PASS" ]]; then
  echo "  PASS: verifyQuick recorded as PASS"
else
  echo "  FAIL: verifyQuick is '${VERIFY_QUICK}' — must be PASS"
  ERRORS=$((ERRORS + 1))
fi

# finalCheck
FINAL_CHECK=$(parse_check finalCheck)
FINAL_CHECK_STALE=$(parse_status_flag finalCheckStale)
if [[ "${FINAL_CHECK}" == "PASS" ]]; then
  if [[ "${FINAL_CHECK_STALE}" == "true" ]]; then
    echo "  FAIL: finalCheck PASS is stale — re-run verification before handoff"
    ERRORS=$((ERRORS + 1))
  else
    echo "  PASS: finalCheck recorded as fresh PASS"
  fi
else
  echo "  FAIL: finalCheck is '${FINAL_CHECK}' — must be PASS"
  ERRORS=$((ERRORS + 1))
fi

# karate
KARATE_CHECK=$(parse_check karate)
KARATE_STALE=$(parse_status_flag karateStale)
if [[ "${KARATE_CHECK}" == "PASS" ]]; then
  if [[ "${KARATE_STALE}" == "true" ]]; then
    echo "  FAIL: karate PASS is stale — re-run verification before handoff"
    ERRORS=$((ERRORS + 1))
  else
    echo "  PASS: karate recorded as fresh PASS"
  fi
else
  echo "  FAIL: karate is '${KARATE_CHECK}' — must be PASS"
  ERRORS=$((ERRORS + 1))
fi

# 3. Changed files tracked (non-empty array)
FILE_COUNT=$(parse_changed_file_count)
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
