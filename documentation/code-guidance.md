# Code Guidance

This document defines the required coding and verification expectations for `flow-orchestrator`.
Treat it as a gate, not as optional advice.

Every non-trivial change must be validated against these rules in both:

- the implementation report
- the reviewer report

Each applicable gate must be marked `PASS`, `FAIL`, `BLOCKED`, or `Approved deviation`.
A gate is not passed by assertion alone. It needs code evidence, test evidence, or executed-command evidence.

## Code Standards Gate

- Local variables and fields are `final` unless mutation is required.
- Boundary models are immutable.
- Records are used for simple immutable data carriers when they improve clarity.
- Dependencies use constructor injection.
- Methods are small and single-purpose.
- Names express business intent.
- Validation happens early at the correct boundary.
- Logging is structured with SLF4J and includes relevant context for efficient debugging without exposing sensitive information.
- Lombok is used only when it reduces boilerplate without hiding important behavior.
- `@RequiredArgsConstructor` is required for constructor injection when Lombok is on the classpath. Do not write explicit constructors for injection-only classes.
- `@Slf4j` is used for structured logging when Lombok is used.
- `@Builder` is used only when it improves readability for object construction.
- Prefer builder-style object construction over direct `new Class(...)` calls when creating non-trivial objects, especially when there are multiple arguments, optional fields, or a risk of argument-order mistakes.

## Naming Gate

- Names must be short, clear, and business-oriented.
- Do not use numeric suffixes without a real semantic reason. Avoid names like `issue1`, `issue2`, `test1`, or similar placeholder naming.
- Prefer `get...` over `list...` for retrieval-oriented names unless `list` is required by an external API, protocol, or established domain term.
- Do not name code after collection types or implementation mechanics. Pick the shortest name that stays unambiguous and understandable.

## Null Handling Gate

- Prefer `Optional.ofNullable(...)` inside method bodies for short null-safe extraction, mapping, or defaulting when it is clearer than repeated ternary checks.
- Avoid repeated `value == null ? ... : ...` chains for field extraction or default resolution.
- A simple `if (value == null)` is still preferred when it is the clearest form.
- Do not use `Optional` for fields, record components, or public API return types unless there is a strong reason.

## Collections And Streams Gate

- Streams are used for clear transformations.
- Streams are not used when a loop is simpler and clearer.
- Stream pipelines have no hidden side effects.
- Collections are returned empty, not `null`.
- Ordering is explicit when it matters.
- Public methods that can return no value are annotated with `@Nullable` and return `null` explicitly instead of `Optional`.
- `Optional` is used only inside method bodies when it makes nested null-safe access or transformation clearer than chained explicit `null` checks.

## Testing Matrix

All added or updated tests must follow
[test-instructions.instructions.md](../.github/instructions/test-instructions.instructions.md).

That instruction is mandatory to follow for writing/updating tests for the `flow-orchestrator` module. Deviations require explicit approval.

Reviewer must treat violations as `FAIL` unless an approved deviation is recorded.

## Evidence Expectations

- The implementation report must point each acceptance criterion to concrete implementation evidence and verification evidence.
- The implementation report must include a code-guidance ledger with evidence or an approved deviation for each applicable gate.
- The reviewer report must explicitly mark each applicable review item `PASS`, `FAIL`, `BLOCKED`, or `N/A`.
- Every failed or blocked item must explain what failed, where it failed, and what remains to be done.
- Verification commands must be recorded exactly enough to be reproducible, together with the observed outcome, but secrets must be redacted.
- Tokens, passwords, `.env.local` contents, and expanded secret-bearing commands must never be copied into implementation reports, review reports, or sign-off artifacts.
- Review and sign-off evidence must identify the verified revision and scope: branch or worktree reference, head commit SHA when available, comparison base or reference when available, and changed files reviewed.
- If a required check could not be executed, the status is `BLOCKED`, not `PASS`.

## Local Static Analysis Gate

For `flow-orchestrator`, the local static-analysis gate is required before Reviewer Phase 2 can pass.

Execution details, command order, and report locations are auto-injected via `.github/instructions/local-quality-rules.instructions.md` for any agent working on `flow-orchestrator`.

- Preferred command: `scripts/quality-check.sh`
- Accepted raw command shape: `mvn clean verify` or `mvn verify`
- The local quality gate covers Checkstyle, PMD, CPD, SpotBugs, and JaCoCo coverage check.
- If Maven or plugin execution is unavailable in the current environment, the status is `BLOCKED`, not `PASS`.
- Local-tool findings must be reviewed before startup and smoke-check sign-off.
- The local toolchain is not treated as a replacement for Reviewer judgment on project-specific rules it does not encode.
- Static-analysis evidence must include the executed command and the generated report paths under `flow-orchestrator/target/`.
- Repo-owned quality configuration files under `flow-orchestrator/config/quality/` are the source of truth for Checkstyle, PMD, and SpotBugs behavior.
- PMD rule additions and SpotBugs exclusions must stay narrow, justified, and reviewable. Do not silence findings broadly to force a green build.

See `.github/instructions/local-quality-rules.instructions.md` for the current setup and report locations.

## Final Verification

- All applicable code standards and collections/streams gates are `PASS` or `Approved deviation`.
- The testing matrix is satisfied for the change.
- All acceptance criteria from the implementation plan are satisfied.
- `scripts/verify-quick.sh` passes.
- For `flow-orchestrator`, `scripts/quality-check.sh` passes and produces current local quality reports.
- The repository-supported startup command starts without errors and logs expected startup messages.
- If an API is added or changed, the corresponding `<api-name>.http` file is created or updated in `/flow-orchestrator/http`.
- API-facing changes are smoke-tested with `curl` by verifying the API returns expected responses for valid and invalid requests.
- If no API contract was changed directly, the existing API endpoints most likely affected by the change were identified and verified with `curl`, with commands and observed responses recorded.
- Team Lead sign-off is blocked while Reviewer reports any applicable `FAIL` or `BLOCKED` item.
