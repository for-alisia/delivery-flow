#!/usr/bin/env bash
# karate-test.sh — Run Karate API tests against a running application or start it if needed.
# Usage: scripts/karate-test.sh [BASE_URL] [additional maven args...]
# Default BASE_URL: http://localhost:8080
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODULE_DIR="${REPO_ROOT}/flow-orchestrator"

BASE_URL="${1:-http://localhost:8080}"
if [[ $# -gt 0 ]]; then
  shift
fi

HEALTH_URL="${BASE_URL%/}/actuator/health"
AUTO_START="${KARATE_AUTO_START:-true}"
START_TIMEOUT_SEC="${KARATE_START_TIMEOUT_SEC:-180}"
POLL_INTERVAL_SEC="${KARATE_POLL_INTERVAL_SEC:-2}"
APP_LOG_FILE="${MODULE_DIR}/target/karate-app.log"
APP_STARTED_BY_SCRIPT=false
APP_PID=""

is_local_base_url() {
  [[ "${BASE_URL}" =~ ^https?://(localhost|127\.0\.0\.1)(:[0-9]+)?(/.*)?$ ]]
}

is_app_ready() {
  local response

  response="$(curl --silent --show-error --fail "${HEALTH_URL}" 2>/dev/null)" || return 1
  [[ "${response}" == *'"status":"UP"'* || "${response}" == *'"status": "UP"'* ]]
}

wait_for_app_ready() {
  local elapsed=0

  while (( elapsed < START_TIMEOUT_SEC )); do
    if is_app_ready; then
      return 0
    fi

    if [[ -n "${APP_PID}" ]] && ! kill -0 "${APP_PID}" 2>/dev/null; then
      echo "[karate-test] Application process exited before becoming ready"
      tail -n 40 "${APP_LOG_FILE}" || true
      return 1
    fi

    sleep "${POLL_INTERVAL_SEC}"
    elapsed=$((elapsed + POLL_INTERVAL_SEC))
  done

  echo "[karate-test] Timed out waiting for ${HEALTH_URL}"
  if [[ -f "${APP_LOG_FILE}" ]]; then
    tail -n 40 "${APP_LOG_FILE}" || true
  fi
  return 1
}

start_app() {
  mkdir -p "${MODULE_DIR}/target"
  : > "${APP_LOG_FILE}"

  echo "[karate-test] Starting flow-orchestrator with local profile"
  (
    cd "${MODULE_DIR}"
    SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}" mvn -B spring-boot:run
  ) > "${APP_LOG_FILE}" 2>&1 &

  APP_PID=$!
  APP_STARTED_BY_SCRIPT=true
}

cleanup() {
  if [[ "${APP_STARTED_BY_SCRIPT}" == true ]] && [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" 2>/dev/null; then
    echo "[karate-test] Stopping application started by this script"
    kill "${APP_PID}" 2>/dev/null || true
    wait "${APP_PID}" 2>/dev/null || true
  fi
}

trap cleanup EXIT

if is_app_ready; then
  echo "[karate-test] Reusing running application at ${BASE_URL}"
else
  if [[ "${AUTO_START}" != "true" ]]; then
    echo "[karate-test] Application is not ready at ${HEALTH_URL} and auto-start is disabled"
    exit 1
  fi

  if ! is_local_base_url; then
    echo "[karate-test] Refusing to auto-start for non-local BASE_URL=${BASE_URL}"
    echo "[karate-test] Start the target app manually or rerun with a local BASE_URL"
    exit 1
  fi

  start_app
  wait_for_app_ready
fi

cd "${MODULE_DIR}"
echo "[karate-test] Running Karate smoke suite against ${BASE_URL}"
mvn -B failsafe:integration-test failsafe:verify -Pkarate -Dkarate.env="${KARATE_ENV:-local}" -DbaseUrl="${BASE_URL}" "$@"
