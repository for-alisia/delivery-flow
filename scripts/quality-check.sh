#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODULE_DIR="${REPO_ROOT}/flow-orchestrator"

cd "${MODULE_DIR}"

echo "[quality-check] Running full local quality gate"
mvn -B clean verify

echo "[quality-check] Reports generated:"
echo "  - ${MODULE_DIR}/target/checkstyle-result.xml"
echo "  - ${MODULE_DIR}/target/pmd.xml"
echo "  - ${MODULE_DIR}/target/cpd.xml"
echo "  - ${MODULE_DIR}/target/spotbugsXml.xml"
echo "  - ${MODULE_DIR}/target/site/jacoco/jacoco.xml"
