---
applyTo: "**/flow-orchestrator/**"
---

## Local Quality Gate

- Shared source of truth for commands, execution order, and report paths:
  `artifacts/reference-docs/local-quality-flow-orchestrator.md`
- Default fast verification command: `scripts/verify-quick.sh`
- Default full static-analysis command: `scripts/quality-check.sh`
- Reviewer Phase 2 can pass only when the shared local-quality workflow succeeds and report paths are captured
- If Maven or plugin execution is unavailable, status is `BLOCKED`, not `PASS`
- Evidence should include the executed command and the generated report paths under `flow-orchestrator/target/`
- The local toolchain does not replace Reviewer judgment on project-specific rules it cannot encode
