#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODULE_DIR="${REPO_ROOT}/flow-orchestrator"

cd "${MODULE_DIR}"

echo "[verify-quick] Running clean compile check"
mvn -B -q -DskipTests clean compile

echo "[verify-quick] Running test suite"
mvn -B test
