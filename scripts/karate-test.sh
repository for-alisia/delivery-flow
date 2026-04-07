#!/usr/bin/env bash
# karate-test.sh — Run Karate API tests against a running application.
# Usage: scripts/karate-test.sh [BASE_URL] [additional maven args...]
# Default BASE_URL: http://localhost:8080
# Requires: running flow-orchestrator application with SPRING_PROFILES_ACTIVE=local
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODULE_DIR="${REPO_ROOT}/flow-orchestrator"

BASE_URL="${1:-http://localhost:8080}"
if [[ $# -gt 0 ]]; then
  shift
fi

cd "${MODULE_DIR}"
echo "[karate-test] Running Karate smoke suite against ${BASE_URL}"
mvn -B failsafe:integration-test failsafe:verify -Pkarate -Dkarate.env="${KARATE_ENV:-local}" -DbaseUrl="${BASE_URL}" "$@"
