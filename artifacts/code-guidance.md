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
- `@RequiredArgsConstructor` is preferred for constructor injection when Lombok is used.
- `@Slf4j` is used for structured logging when Lombok is used.
- `@Builder` is used only when it improves readability for object construction.
- Prefer builder-style object construction over direct `new Class(...)` calls when creating non-trivial objects, especially when there are multiple arguments, optional fields, or a risk of argument-order mistakes.

## Collections And Streams Gate

- Streams are used for clear transformations.
- Streams are not used when a loop is simpler and clearer.
- Stream pipelines have no hidden side effects.
- Collections are returned empty, not `null`.
- Ordering is explicit when it matters.
- Public methods that can return no value are annotated with `@Nullable` and return `null` explicitly instead of `Optional`.
- `Optional` is used only inside method bodies when it makes nested null-safe access or transformation clearer than chained explicit `null` checks.

## Testing Matrix

Use the matrix below to decide what levels are required. Reviewer validates that the chosen levels are actually sufficient.

| Level | When It Is Required | What It Must Prove | What Does Not Count |
|---|---|---|---|
| Unit | Any non-trivial logic, validation, defaulting, mapping, or error translation | Behavior in isolation with clear inputs and outputs | Relying only on manual smoke checks |
| Web / Controller Slice | New or changed request binding, validation, response mapping, or HTTP error behavior | Request/response contract and boundary behavior | Pure unit tests that never exercise Spring MVC binding |
| Integration | New or changed interaction between Spring-wired components or architectural layers | Wiring and behavior across component boundaries with controlled collaborators | Only Mockito unit tests or only adapter unit tests |
| Live Smoke | Any changed runtime path that affects an API or startup behavior | The built application starts and the main runtime path works from the outside | Using smoke checks as the only evidence for behavior that should be automated |

Additional testing rules:

- A feature that changes controller, orchestration, and integration interaction normally requires all four levels.
- Integration adapter tests cover external API interaction, mapping, and error handling where applicable, but they are not a substitute for broader Spring-wired integration tests when component interaction changed.
- Each test verifies one behavior.
- All test names are descriptive and contain `@DisplayName`.
- Tests cover happy path, edge cases, and failure paths.
- Mocks are used only for true collaborators at the layer boundary.
- Real mappers and value objects are used when simpler than mocks.
- No acceptance criterion may be marked `Verified` from manual `curl` alone if it can and should be covered automatically.

## Evidence Expectations

- The implementation report must point each acceptance criterion to concrete implementation evidence and verification evidence.
- The implementation report must include a code-guidance ledger with evidence or an approved deviation for each applicable gate.
- The reviewer report must explicitly mark each applicable review item `PASS`, `FAIL`, `BLOCKED`, or `N/A`.
- Every failed or blocked item must explain what failed, where it failed, and what remains to be done.
- Verification commands must be recorded exactly enough to be reproducible, together with the observed outcome, but secrets must be redacted.
- Tokens, passwords, `.env.local` contents, and expanded secret-bearing commands must never be copied into implementation reports, review reports, or sign-off artifacts.
- Review and sign-off evidence must identify the verified revision and scope: branch or worktree reference, head commit SHA when available, comparison base or reference when available, and changed files reviewed.
- If a required check could not be executed, the status is `BLOCKED`, not `PASS`.

## Sonar Gate

For `flow-orchestrator`, Sonar analysis is a required static-quality gate before Reviewer Phase 2 can pass.

- Preferred command: `scripts/run-flow-orchestrator-sonar.sh`
- Accepted raw command shape: `mvn verify sonar:sonar -Dsonar.host.url=... -Dsonar.organization=...` with `SONAR_TOKEN` supplied via environment variable
- The Sonar quality gate must pass.
- If Sonar is configured for the workflow but the environment variables or server access are unavailable, the status is `BLOCKED`, not `PASS`.
- Sonar findings must be reviewed before startup and smoke-check sign-off.
- Sonar is not treated as a replacement for Reviewer judgment on project-specific rules that the default Sonar rule set cannot enforce.
- Sonar evidence must include the dashboard URL, CE task URL or ID, and analysis ID when it is available.

See [sonar-flow-orchestrator.md](/Users/alisia/Projects/aiProjects/GitlabFlow/artifacts/reference-docs/sonar-flow-orchestrator.md) for the current setup and limitations.

## Final Verification

- All applicable code standards and collections/streams gates are `PASS` or `Approved deviation`.
- The testing matrix is satisfied for the change.
- All acceptance criteria from the implementation plan are satisfied.
- `mvn test` passes with all tests green.
- `mvn -q -DskipTests compile` passes with no errors.
- For `flow-orchestrator`, Sonar analysis ran and the Sonar quality gate passed.
- The repository-supported startup command starts without errors and logs expected startup messages.
- If an API is added or changed, the corresponding `<api-name>.http` file is created or updated in `/flow-orchestrator/http`.
- API-facing changes are smoke-tested with `curl` by verifying the API returns expected responses for valid and invalid requests.
- If no API contract was changed directly, the existing API endpoints most likely affected by the change were identified and verified with `curl`, with commands and observed responses recorded.
- Team Lead sign-off is blocked while Reviewer reports any applicable `FAIL` or `BLOCKED` item.
