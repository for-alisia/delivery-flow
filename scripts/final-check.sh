#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

echo "[final-check] Applying repository formatting"
"${SCRIPT_DIR}/format-code.sh"

echo "[final-check] Running full quality verification"
"${SCRIPT_DIR}/quality-check.sh"
