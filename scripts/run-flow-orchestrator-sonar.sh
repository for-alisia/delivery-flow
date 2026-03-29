#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env.local"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

: "${SONAR_HOST_URL:=https://sonarcloud.io}"

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "SONAR_TOKEN is not set. Put it in ${ENV_FILE} or export it before running this script." >&2
  exit 1
fi

cd "${REPO_ROOT}/flow-orchestrator"

CMD=(
  mvn
  verify
  sonar:sonar
  "-Dsonar.host.url=${SONAR_HOST_URL}"
)

if [[ -n "${SONAR_ORGANIZATION:-}" ]]; then
  CMD+=("-Dsonar.organization=${SONAR_ORGANIZATION}")
fi

if [[ $# -gt 0 ]]; then
  CMD+=("$@")
fi

exec "${CMD[@]}"
