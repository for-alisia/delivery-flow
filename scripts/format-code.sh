#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODE="${1:-apply}"

case "${MODE}" in
    apply)
        JAVA_GOAL="spotless:apply"
        NODE_SCRIPT="format"
        ;;
    check)
        JAVA_GOAL="spotless:check"
        NODE_SCRIPT="format:check"
        ;;
    *)
        echo "Usage: scripts/format-code.sh [apply|check]" >&2
        exit 1
        ;;
esac

echo "[format-code] Running ${MODE} for flow-orchestrator"
(
    cd "${REPO_ROOT}/flow-orchestrator"
    mvn -B "${JAVA_GOAL}"
)

echo "[format-code] Running ${MODE} for mcp-server"
(
    cd "${REPO_ROOT}/mcp-server"
    npm run "${NODE_SCRIPT}"
)
