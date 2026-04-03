---
applyTo: "**/flow-orchestrator/**"
---

# Local Quality Tooling For `flow-orchestrator`

This file is the single source of truth for how agents and humans run local verification for `flow-orchestrator`.
Use it for command choice, execution order, expected reports, and artifact evidence.

## Preferred Commands

- Default fast verification command: `scripts/verify-quick.sh`
- Default final verification command: `scripts/final-check.sh`
- Supporting formatting command: `scripts/format-code.sh`
- Supporting static-analysis command: `scripts/quality-check.sh`
- Default smoke-test command: `scripts/smoke-test.sh` (requires a running application; verifies health + API endpoints with `curl`)
- Prefer these scripts over raw Maven commands when recording implementation, review, or sign-off evidence.
- Use raw `mvn` commands only for focused debugging when the scripts are not enough.

## Toolchain

- `scripts/verify-quick.sh` runs the fast developer gate: compile + tests.
- `scripts/final-check.sh` is the user-facing final gate: it applies formatting and then runs the full quality verification.
- `scripts/format-code.sh` is the supporting formatter script used by `scripts/final-check.sh`.
- `scripts/quality-check.sh` is the supporting verification script that runs Maven `clean verify`.
- Maven `verify` executes:
  - Spotless via `spotless-maven-plugin` `3.0.0`
  - Checkstyle via `maven-checkstyle-plugin` `3.6.0`
  - PMD + CPD via `maven-pmd-plugin` `3.28.0`
  - SpotBugs via `spotbugs-maven-plugin` `4.9.8.2`
  - JaCoCo report + coverage check via `jacoco-maven-plugin` `0.8.14`

## Repository-Owned Quality Config

- Checkstyle config: `flow-orchestrator/config/quality/checkstyle.xml`
- PMD ruleset: `flow-orchestrator/config/quality/pmd-ruleset.xml`
- SpotBugs exclude filter: `flow-orchestrator/config/quality/spotbugs-exclude.xml`

The quality config is intentionally repo-owned so the same rules run in local terminals, agent workflows, and future CI.

## Coverage Gate

- JaCoCo enforces bundle-level line coverage of at least `85%`.
- The build fails during `verify` when coverage is below the threshold.

## Expected Agent Workflow

- During slice-by-slice implementation, run `scripts/verify-quick.sh`.
- Before coder handoff, run `scripts/final-check.sh`.
- After the coder claims tests passed, Team Lead independently reruns `mvn test` from `flow-orchestrator/` before Reviewer Phase 2.
- During Reviewer Phase 2, rerun both scripts plus startup and required smoke checks.
- Reviewer Phase 2 can pass only when the shared local-quality workflow succeeds and report paths are captured.
- Record the exact command, the observed result, and the generated report paths in the implementation, review, or sign-off artifact.
- If Maven or plugin execution is unavailable, mark the check `BLOCKED`, not `PASS`.
- If the scripts run and report findings, test failures, or coverage failures, mark the check `FAIL`.
- Evidence should include the executed command and the generated report paths under `flow-orchestrator/target/`.

## Expected Report Outputs

After `scripts/final-check.sh`, `scripts/quality-check.sh`, or `mvn clean verify`, the module writes:

- `flow-orchestrator/target/checkstyle-result.xml`
- `flow-orchestrator/target/pmd.xml`
- `flow-orchestrator/target/cpd.xml`
- `flow-orchestrator/target/spotbugsXml.xml`
- `flow-orchestrator/target/site/jacoco/jacoco.xml`

These report paths should be referenced in implementation, review, and sign-off artifacts when static-analysis evidence is required.

## Formatting Expectations

- The Java formatter source of truth is Spotless with `palantir-java-format`.
- The TypeScript formatter source of truth is Prettier via `mcp-server/package.json`.
- `scripts/final-check.sh` is the default agent-facing command before final handoff.
- `scripts/format-code.sh apply` is the underlying formatting action used by `scripts/final-check.sh`.
- `scripts/format-code.sh check` is the read-only formatting validation command when only verification is needed.

## PMD Ruleset Guidance

- The PMD ruleset is intentionally conservative and focuses on high-signal bug-prone patterns.
- Prefer adding rules in small batches and rerunning `scripts/quality-check.sh` after each change.
- Avoid duplicating checks already enforced clearly by Checkstyle unless PMD adds materially different value.

## SpotBugs Exclusion Guidance

- Keep `spotbugs-exclude.xml` empty by default.
- Add an exclusion only for a specific, understood false positive.
- Each exclusion should be narrow by class or bug pattern and should be documented in the related implementation/review artifacts.

## Review Guidance

- Use `scripts/verify-quick.sh` during slice-by-slice implementation.
- Use `scripts/final-check.sh` before implementation handoff and during Reviewer Phase 2.
- Treat a local-tool execution failure as `BLOCKED` only when the command truly cannot run in the environment. Tool findings themselves are `FAIL`, not `BLOCKED`.
- The local toolchain does not replace Reviewer judgment on project-specific rules it cannot encode.
