#!/usr/bin/env bash
# smoke-test.sh — Quick HTTP smoke check for flow-orchestrator endpoints.
# Usage: scripts/smoke-test.sh [BASE_URL]
# Default BASE_URL: http://localhost:8080
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
RESULTS=""

check() {
  local name="$1" method="$2" url="$3" expected_status="$4"
  shift 4
  local actual_status
  actual_status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$@" "$url") || true
  if [[ "$actual_status" == "$expected_status" ]]; then
    RESULTS+="  PASS  $name (HTTP $actual_status)\n"
    PASS=$((PASS + 1))
  else
    RESULTS+="  FAIL  $name (expected $expected_status, got $actual_status)\n"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Smoke Test: ${BASE_URL} ==="

# Health / Actuator
check "GET /actuator/health" GET "${BASE_URL}/actuator/health" 200

# Issues API — default request (empty body)
check "POST /api/issues/search (default)" POST "${BASE_URL}/api/issues/search" 200 \
  -H "Content-Type: application/json" -d '{}'

# Issues API — filtered request
check "POST /api/issues/search (filtered)" POST "${BASE_URL}/api/issues/search" 200 \
  -H "Content-Type: application/json" \
  -d '{"pagination":{"page":1,"perPage":5},"filters":{"state":"opened"}}'

# Issues API — create request
SMOKE_CREATE_TITLE="Smoke test issue $(date -u +%Y%m%d%H%M%S)"
check "POST /api/issues (create)" POST "${BASE_URL}/api/issues" 201 \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"${SMOKE_CREATE_TITLE}\",\"description\":\"Created by smoke test\",\"labels\":[\"smoke\",\"api\"]}"

# Issues API — create validation failure
check "POST /api/issues (create validation error)" POST "${BASE_URL}/api/issues" 400 \
  -H "Content-Type: application/json" \
  -d '{"title":"   "}'

echo ""
printf "%b" "$RESULTS"
echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed ==="

if [[ "$FAIL" -gt 0 ]]; then
  exit 1
fi
