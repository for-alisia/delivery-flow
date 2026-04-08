---
applyTo: "**"
description: "Delivery flow rules for Team Lead, Product Manager, Java Architect, Java Coder, and Reviewer. Apply during agentic delivery workflow execution."
---

## Delivery Flow Rules

### Workflow Control

- Never read or modify `.github/agentic-flow/logs/`.
- Subagents return structured summaries, not file contents.
- Do not advance on chat-only claims; the on-disk artifact is the source of truth.
- Use `<<ESCALATE_TO_ARCHITECT>>` when a fundamental plan flaw blocks progress.
- After 2 Java Coder false-positive red cards on the same feature, route back to Java Architect for plan revision.
- Use `/memories/session/<feature-name>-checkpoint.json` as the shared context source.

### Blocker Escalation

- If required input artifacts are missing, **REPORT A BLOCKER** — do not proceed with assumptions.
- If requirements conflict with `documentation/constitution.md`, **REPORT A BLOCKER**.
- If locked request constraints would need to be silently changed, **REPORT A BLOCKER** instead of redesigning.
- If verification cannot run in the current environment, mark the item `BLOCKED` with the exact command attempted and observed error.
- If the artifact set is internally contradictory and the source of truth is unclear, **REPORT A BLOCKER** instead of guessing.
- Never improvise around a blocker. Surface it immediately with evidence.

### Evidence And Verification

- Verification commands must be recorded with the exact command and observed outcome.
- Evidence must identify: branch or worktree reference, head commit SHA, and changed files reviewed.
- Status is `BLOCKED` (not `PASS`) when a required check could not be executed.
- Claims require code, test, or executed-command evidence — prose self-certification is not sufficient.
- No acceptance criterion may be marked `Verified` from manual `curl` alone if it can and should be covered by an automated test.
- Each applicable gate must be marked `PASS`, `FAIL`, `BLOCKED`, or `Approved deviation` with supporting evidence.
