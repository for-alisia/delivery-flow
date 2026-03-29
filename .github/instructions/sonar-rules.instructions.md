---
applyTo: "**/flow-orchestrator/**"
---

## Sonar Quality Gate

- Default Sonar command: `scripts/run-flow-orchestrator-sonar.sh`
- Fallback: raw Maven command from `artifacts/reference-docs/sonar-flow-orchestrator.md` only if the script is unavailable
- SonarCloud organization: `for-alisia`, project: `com.gitlabflow:flow-orchestrator`
- Sonar quality gate must pass before Reviewer Phase 2 can pass
- If Sonar environment variables or server access are unavailable, status is `BLOCKED`, not `PASS`
- Sonar evidence must include: dashboard URL, CE task URL or ID, and analysis ID when available
- Sonar does not replace Reviewer judgment on project-specific rules the default Sonar profile cannot enforce
