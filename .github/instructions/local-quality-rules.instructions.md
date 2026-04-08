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
| `scripts/karate-test.sh` | Karate API smoke tests (`mvn failsafe -Pkarate`) | After starting the application; isolated from `mvn test` and `mvn verify` |
| `scripts/format-code.sh` | Formatting only | Supporting; called by `final-check.sh` |
| `scripts/quality-check.sh` | Static analysis only (`mvn clean verify`) | Supporting; called by `final-check.sh` |

Prefer these scripts over raw Maven commands. Use raw `mvn` only for focused debugging.

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
