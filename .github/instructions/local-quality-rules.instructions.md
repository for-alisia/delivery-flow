---
applyTo: "**/flow-orchestrator/**"
description: "Local verification commands, execution order, expected reports, and artifact evidence rules for the flow-orchestrator module."
---

# Local Quality Rules For `flow-orchestrator`

## Commands

| Command | Purpose | When to use |
|---------|---------|-------------|
| `scripts/verify-quick.sh` | Compile + tests | After each code/test change |
| `scripts/final-check.sh` | Format + full quality verification | Before considering work complete |
| `scripts/karate-test.sh` | Karate API smoke tests (`mvn failsafe -Pkarate`) | Use for runtime smoke verification; reuses a healthy local app when already running or starts it automatically for local `BASE_URL`s; isolated from `mvn test` and `mvn verify` |
| `scripts/format-code.sh` | Formatting only | Supporting; called by `final-check.sh` |
| `scripts/quality-check.sh` | Static analysis only (`mvn clean verify`) | Supporting; called by `final-check.sh` |

Prefer these scripts over raw Maven commands. Use raw `mvn` only for focused debugging.

## Agent Verification Via flow-log

Agents must use `flow-log run-check` or `flow-log verify` instead of running scripts directly. This executes the script and records the result in one atomic step:

```bash
scripts/flow-log.sh run-check --feature <name> --name verifyQuick --by <agent>
scripts/flow-log.sh run-check --feature <name> --name finalCheck --by <agent>
scripts/flow-log.sh run-check --feature <name> --name karate --by <agent>
```

Or run the standard sequences through one command:

```bash
scripts/flow-log.sh verify --feature <name> --profile slice --by <agent>
scripts/flow-log.sh verify --feature <name> --profile full --by <agent>
```

`verify --profile slice` runs `verifyQuick` then `finalCheck`. `verify --profile full` runs `verifyQuick`, `finalCheck`, then `karate`. In the active workflow, use the `slice` profile for coder handoff work and reserve the `full` profile for exceptional combined reruns only. `run-check` maps each check name to the correct script automatically. On failure, the JSON output includes `outputTail` (last 80 lines) plus `logPath` for the persisted redacted check log. Use `scripts/flow-log.sh check-log --feature <name> --name <verifyQuick|finalCheck|karate> --lines 80` before rerunning a non-obvious failure or when review / smoke evidence needs deeper inspection. Do not run scripts separately and then call `set-check` — that pattern is error-prone and deprecated for agent use.

Active workflow ownership:

- `Java Coder` uses `verify --profile slice` for every slice-run.
- When `e2eMode = SCENARIOS_REQUIRED`, `E2E Tester` owns `run-check --name karate` after the final slice-run and after any later source change that makes `karateStale=true`.
- When `e2eMode = REUSE_EXISTING`, Team Lead runs `run-check --name karate` directly against the existing Karate suite.
- `verify --profile full` remains available for exceptional combined reruns, but it is not the default handoff path in the active workflow.

### Source Staleness Detection

`run-check` and `verify` record a source fingerprint when a check passes. The `status` command exposes `*Stale` fields (`verifyQuickStale`, `finalCheckStale`, `karateStale`) — these are `true` when source files changed after the check last passed. Agents should check staleness before trusting prior PASS results.

## Evidence Recording

- Record the exact command, observed result, and generated report paths.
- `PASS` or `FAIL` + last 10 terminal lines. Do not build custom parsing pipelines.
- If a script cannot run in the environment, mark `BLOCKED`, not `PASS`.
- Tool findings and test failures are `FAIL`, not `BLOCKED`.

## Report Outputs

After `scripts/final-check.sh` or `mvn clean verify`:

- `flow-orchestrator/target/checkstyle-result.xml`
- `flow-orchestrator/target/pmd.xml`
- `flow-orchestrator/target/cpd.xml`
- `flow-orchestrator/target/spotbugsXml.xml`
- `flow-orchestrator/target/site/jacoco/jacoco.xml`

Reference these paths in implementation, review, and sign-off artifacts.

## Quality Config

- Checkstyle: `flow-orchestrator/config/quality/checkstyle.xml`
- PMD: `flow-orchestrator/config/quality/pmd-ruleset.xml`
- SpotBugs: `flow-orchestrator/config/quality/spotbugs-exclude.xml` (keep empty; add exclusions only for understood false positives)
- Java formatter: Spotless with `palantir-java-format`
- TypeScript formatter: Prettier via `mcp-server/package.json`
