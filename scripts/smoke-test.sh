#!/usr/bin/env bash
# smoke-test.sh — Quick HTTP smoke check for flow-orchestrator endpoints.
# DEPRECATED: Prefer scripts/karate-test.sh for structured smoke verification.
# This script remains as a fallback during transition.
# Usage: scripts/smoke-test.sh [BASE_URL]
# Default BASE_URL: http://localhost:8080
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
RESULTS=""

check_field() {
  local name="$1" method="$2" url="$3" field="$4"
  shift 4
  local body
  body=$(curl -s -X "$method" "$@" "$url") || true
  if echo "$body" | grep -q "\"${field}\""; then
    RESULTS+="  PASS  $name (field '${field}' present)\n"
    PASS=$((PASS + 1))
  else
    RESULTS+="  FAIL  $name (field '${field}' missing)\n"
    FAIL=$((FAIL + 1))
  fi
}

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

extract_issue_id() {
  local response_body="$1"
  echo "$response_body" | sed -n 's/.*"issueId"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -n 1
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

# Issues API — delete flow (create -> delete -> delete again)
SMOKE_DELETE_TITLE="Smoke delete issue $(date -u +%Y%m%d%H%M%S)"
DELETE_CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/issues" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"${SMOKE_DELETE_TITLE}\",\"description\":\"Created for delete smoke flow\",\"labels\":[\"smoke\",\"delete\"]}") || true
DELETE_ISSUE_ID=$(extract_issue_id "$DELETE_CREATE_RESPONSE")

if [[ -n "$DELETE_ISSUE_ID" ]]; then
  RESULTS+="  PASS  POST /api/issues (create for delete flow) issueId=${DELETE_ISSUE_ID}\n"
  PASS=$((PASS + 1))
  check "DELETE /api/issues/{issueId} (existing)" DELETE "${BASE_URL}/api/issues/${DELETE_ISSUE_ID}" 204
  check "DELETE /api/issues/{issueId} (deleted again)" DELETE "${BASE_URL}/api/issues/${DELETE_ISSUE_ID}" 404
else
  RESULTS+="  FAIL  POST /api/issues (create for delete flow) issueId missing\n"
  FAIL=$((FAIL + 1))
fi

check "DELETE /api/issues/0 (validation error)" DELETE "${BASE_URL}/api/issues/0" 400

# issueId field presence — verify additive field appears in both endpoints
check_field "POST /api/issues/search issueId present" POST "${BASE_URL}/api/issues/search" "issueId" \
  -H "Content-Type: application/json" -d '{}'

SMOKE_FIELD_TITLE="Smoke field check $(date -u +%Y%m%d%H%M%S)"
check_field "POST /api/issues issueId present" POST "${BASE_URL}/api/issues" "issueId" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"${SMOKE_FIELD_TITLE}\",\"description\":\"Created by smoke test\",\"labels\":[\"smoke\",\"api\"]}"

echo ""
printf "%b" "$RESULTS"
echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed ==="

if [[ "$FAIL" -gt 0 ]]; then
  exit 1
fi
