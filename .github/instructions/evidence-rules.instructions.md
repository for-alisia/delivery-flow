---
applyTo: "**/flow-orchestrator/**"
---

## Evidence And Verification Rules

- Verification commands must be recorded with the exact command and observed outcome.
- Evidence must identify: branch or worktree reference, head commit SHA, and changed files reviewed.
- Status is `BLOCKED` (not `PASS`) when a required check could not be executed.
- Claims require code, test, or executed-command evidence — prose self-certification is not sufficient.
- No acceptance criterion may be marked `Verified` from manual `curl` alone if it can and should be covered by an automated test.
- Each applicable gate must be marked `PASS`, `FAIL`, `BLOCKED`, or `Approved deviation` with supporting evidence.
