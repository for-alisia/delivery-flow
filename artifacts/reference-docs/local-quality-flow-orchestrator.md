# Local Quality Tooling For `flow-orchestrator`

This document is the shared source of truth for how agents and humans run local verification for `flow-orchestrator`.
Use it for command choice, execution order, expected reports, and artifact evidence.

## Preferred Commands

- Run from the repository root: `scripts/verify-quick.sh`
- Run from the repository root: `scripts/quality-check.sh`
- Prefer these scripts over raw Maven commands when recording implementation, review, or sign-off evidence.
- Use raw `mvn` commands only for focused debugging when the scripts are not enough.

## Toolchain

- `scripts/verify-quick.sh` runs the fast developer gate: compile + tests.
- `scripts/quality-check.sh` runs the full local quality gate with Maven `clean verify`.
- Maven `verify` executes:
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
- Before coder handoff, run `scripts/quality-check.sh`.
- After the coder claims tests passed, Team Lead independently reruns `mvn test` from `flow-orchestrator/` before Reviewer Phase 2.
- During Reviewer Phase 2, rerun both scripts plus startup and required smoke checks.
- Record the exact command, the observed result, and the generated report paths in the implementation, review, or sign-off artifact.
- If Maven or plugin execution is unavailable, mark the check `BLOCKED`.
- If the scripts run and report findings, test failures, or coverage failures, mark the check `FAIL`.

## Expected Report Outputs

After `scripts/quality-check.sh` or `mvn clean verify`, the module writes:

- `flow-orchestrator/target/checkstyle-result.xml`
- `flow-orchestrator/target/pmd.xml`
- `flow-orchestrator/target/cpd.xml`
- `flow-orchestrator/target/spotbugsXml.xml`
- `flow-orchestrator/target/site/jacoco/jacoco.xml`

These report paths should be referenced in implementation, review, and sign-off artifacts when static-analysis evidence is required.

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
- Use `scripts/quality-check.sh` before implementation handoff and during Reviewer Phase 2.
- Treat a local-tool execution failure as `BLOCKED` only when the command truly cannot run in the environment. Tool findings themselves are `FAIL`, not `BLOCKED`.
