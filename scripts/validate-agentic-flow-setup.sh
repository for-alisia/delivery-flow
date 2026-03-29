#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

required_tracked_files=(
  ".env.local.example"
  ".github/agents/team-lead.agent.md"
  ".github/agents/product-manager.md"
  ".github/agents/java-architect.agent.md"
  ".github/agents/java-coder.agent.md"
  ".github/agents/reviewer.agent.md"
  ".github/prompts/agentic-orchestration.prompt.md"
  "artifacts/code-guidance.md"
  "artifacts/review-reports/.gitkeep"
  "artifacts/templates/implementation-plan-template.md"
  "artifacts/templates/review-report-template.md"
  "artifacts/templates/implementation-signoff-template.md"
  "artifacts/reference-docs/sonar-flow-orchestrator.md"
  "scripts/run-flow-orchestrator-sonar.sh"
  "scripts/validate-agentic-flow-setup.sh"
)

missing=()

cd "${REPO_ROOT}"

for path in "${required_tracked_files[@]}"; do
  if ! git ls-files --error-unmatch "${path}" >/dev/null 2>&1; then
    missing+=("${path}")
  fi
done

if ! git check-ignore -q .env.local; then
  missing+=(".env.local must be gitignored")
fi

if [[ ! -x scripts/run-flow-orchestrator-sonar.sh ]]; then
  missing+=("scripts/run-flow-orchestrator-sonar.sh must be executable")
fi

if [[ ${#missing[@]} -gt 0 ]]; then
  printf 'Agentic flow setup validation failed:\n' >&2
  for item in "${missing[@]}"; do
    printf ' - %s\n' "${item}" >&2
  done
  exit 1
fi

printf 'Agentic flow setup validation passed.\n'
