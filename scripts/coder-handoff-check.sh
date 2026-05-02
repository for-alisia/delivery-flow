#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

# Usage: scripts/coder-handoff-check.sh <feature-name> [--state-path <path>]
if [[ $# -lt 1 ]]; then
  echo "[handoff-check] Usage: $0 <feature-name> [--state-path <path>]"
  exit 1
fi

FEATURE="$1"
shift

STATE_ARGS=()
STATE_FILE="artifacts/flow-logs/${FEATURE}.json"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --state-path)
      if [[ $# -lt 2 ]]; then
        echo "[handoff-check] Missing value for --state-path"
        exit 1
      fi
      STATE_FILE="$2"
      STATE_ARGS+=(--state-path "$2")
      shift 2
      ;;
    *)
      echo "[handoff-check] Unknown argument: $1"
      echo "[handoff-check] Usage: $0 <feature-name> [--state-path <path>]"
      exit 1
      ;;
  esac
done

ERRORS=0

echo "[handoff-check] Checking handoff readiness for feature: ${FEATURE}"

# 1. Flow-log state file exists
if [[ ! -f "${STATE_FILE}" ]]; then
  echo "  FAIL: State file missing: ${STATE_FILE}"
  ERRORS=$((ERRORS + 1))
else
  echo "  PASS: State file exists"
fi

# 2. Flow-log status - parse JSON checks via node one-liner for reliability
STATUS_JSON=$(scripts/flow-log.sh status --feature "${FEATURE}" "${STATE_ARGS[@]}" 2>/dev/null) || {
  echo "  FAIL: Could not read flow-log status for ${FEATURE}"
  exit 1
}
SUMMARY_JSON=$(scripts/flow-log.sh summary --feature "${FEATURE}" "${STATE_ARGS[@]}" 2>/dev/null) || {
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
  echo "${SUMMARY_JSON}" | node -e "process.stdin.resume(); let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{const s=JSON.parse(d).summary; const files=Array.isArray(s?.sliceRuns?.current?.changedFiles)?s.sliceRuns.current.changedFiles:[]; console.log(files.length)})" 2>/dev/null || echo "0"
}

parse_slice_run_type() {
  echo "${SUMMARY_JSON}" | node -e "process.stdin.resume(); let d=''; process.stdin.on('data',c=>d+=c); process.stdin.on('end',()=>{const s=JSON.parse(d).summary; console.log(s?.sliceRuns?.current?.type ?? 'MISSING')})" 2>/dev/null || echo "MISSING"
}

SLICE_RUN_TYPE=$(parse_slice_run_type)
case "${SLICE_RUN_TYPE}" in
  intermediate|final)
    echo "  PASS: active slice-run type is '${SLICE_RUN_TYPE}'"
    ;;
  MISSING)
    echo "  FAIL: No active slice-run found - Team Lead must start one before coder handoff"
    ERRORS=$((ERRORS + 1))
    ;;
  *)
    echo "  FAIL: Unsupported slice-run type '${SLICE_RUN_TYPE}'"
    ERRORS=$((ERRORS + 1))
    ;;
esac

# verifyQuick
VERIFY_QUICK=$(parse_check verifyQuick)
if [[ "${VERIFY_QUICK}" == "PASS" ]]; then
  echo "  PASS: verifyQuick recorded as PASS"
else
  echo "  FAIL: verifyQuick is '${VERIFY_QUICK}' - must be PASS"
  ERRORS=$((ERRORS + 1))
fi

# finalCheck
FINAL_CHECK=$(parse_check finalCheck)
FINAL_CHECK_STALE=$(parse_status_flag finalCheckStale)
if [[ "${FINAL_CHECK}" == "PASS" ]]; then
  if [[ "${FINAL_CHECK_STALE}" == "true" ]]; then
    echo "  FAIL: finalCheck PASS is stale - re-run verification before handoff"
    ERRORS=$((ERRORS + 1))
  else
    echo "  PASS: finalCheck recorded as fresh PASS"
  fi
else
  echo "  FAIL: finalCheck is '${FINAL_CHECK}' - must be PASS"
  ERRORS=$((ERRORS + 1))
fi

# karate is owned by the smoke owner selected by e2eMode after coder handoff
E2E_MODE=$(parse_status_flag e2eMode)
KARATE_CHECK=$(parse_check karate)
case "${E2E_MODE}" in
  SCENARIOS_REQUIRED)
    echo "  PASS: karate not required for coder handoff; post-handoff smoke owner is E2E Tester for SCENARIOS_REQUIRED (current: ${KARATE_CHECK})"
    ;;
  REUSE_EXISTING)
    echo "  PASS: karate not required for coder handoff; post-handoff smoke owner is Team Lead for REUSE_EXISTING (current: ${KARATE_CHECK})"
    ;;
  *)
    echo "  PASS: karate not required for coder handoff; post-handoff smoke owner depends on Team Lead e2eMode decision (current mode: ${E2E_MODE}, check: ${KARATE_CHECK})"
    ;;
esac

# 3. Active slice-run changed files tracked (non-empty array)
FILE_COUNT=$(parse_changed_file_count)
if [[ "${FILE_COUNT}" -gt 0 ]]; then
  echo "  PASS: ${FILE_COUNT} changed file(s) tracked for active slice-run"
else
  echo "  FAIL: No changed files recorded for active slice-run - run flow-log add-change before handoff"
  ERRORS=$((ERRORS + 1))
fi

# Summary
echo ""
if [[ ${ERRORS} -eq 0 ]]; then
  echo "[handoff-check] ALL CHECKS PASSED - ready for handoff"
  exit 0
else
  echo "[handoff-check] ${ERRORS} CHECK(S) FAILED - fix before returning to Team Lead"
  exit 1
fi
