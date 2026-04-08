# Code Guidance

This document defines the required coding and verification expectations for `flow-orchestrator`.
Treat it as a gate, not as optional advice.

Every non-trivial change must be validated against these rules in both:

- the implementation report
- the reviewer report

Each applicable gate must be marked `PASS`, `FAIL`, `BLOCKED`, or `Approved deviation`.
A gate is not passed by assertion alone. It needs code evidence, test evidence, or executed-command evidence.

## Code Standards Gate

- Fields are `final` unless mutation is required.
- Boundary models are immutable.
- Records are used for simple immutable data carriers when they improve clarity.
- Dependencies use constructor injection.
- Methods are small and single-purpose.
- Names express business intent.
- Validation happens early at the correct boundary.
- REST controller parameter validation uses annotations (`@Valid`, `@NotBlank`, `@Positive`, and similar) whenever the rule can be expressed declaratively.
- Manual validation at the controller level is allowed only for project-specific rules that cannot be expressed cleanly with standard validation annotations.
- Logging is structured with SLF4J and includes relevant context for efficient debugging without exposing sensitive information.
- Lombok is used only when it reduces boilerplate without hiding important behavior.
- `@RequiredArgsConstructor` is required for constructor injection when Lombok is on the classpath. Do not write explicit constructors for injection-only classes.
- `@Slf4j` is used for structured logging when Lombok is used.
- Records and classes should use `@Builder` unless there are clear limitations to use it.
- Prefer builder-style object construction over direct `new Class(...)`.

## Naming And Suffix Conventions Gate

- Names must be short, clear, and business-oriented.
- Do not use numeric suffixes without a real semantic reason. Avoid names like `issue1`, `issue2`, `test1`, or similar placeholder naming.
- Prefer `get...` over `list...` for retrieval-oriented names unless `list` is required by an external API, protocol, or established domain term.
- Do not name code after collection types or implementation mechanics. Pick the shortest name that stays unambiguous and understandable.

- Use `*Service` for orchestration/application flow classes that coordinate a use case.
- Use `*Port` for orchestration-facing interfaces that define required downstream capabilities.
- Use `*Adapter` for provider-facing implementations of ports.
- Use `*Mapper` for pure transformation classes between layers or models.
- Use `*Properties` for configuration binding classes.
- Use `*Request` and `*Response` for transport-specific provider or API payload models.
- Use `*Dto` for REST boundary payload records exposed by this application.
- Use `*Input` for orchestration input models that represent a use-case request independent of transport.
- Prefer singular entity names for shared business models such as `Issue`; use wrapper/container names such as `IssuePage` or `SearchIssuesResponse` only when they represent distinct semantics.

## Package Structure And Model Gate

- Root package responsibilities are:
	- `common` for shared cross-cutting error and web concerns.
	- `config` for Spring and application configuration.
	- `orchestration` for application use cases and boundary-facing flow.
	- `integration` for external-system integration code.
- Capability code under `orchestration` is organized by feature and role:
	- `orchestration/<capability>` may contain capability-root orchestration types such as `*Service` and `*Port`.
	- `orchestration/<capability>/model` for orchestration models.
	- `orchestration/<capability>/rest` for REST controller entry points.
	- `orchestration/<capability>/rest/dto` for REST request/response records.
	- `orchestration/<capability>/rest/mapper` for REST-to-model and model-to-REST mapping.
- Provider integration code is organized by provider and capability:
	- `integration/<provider>` may contain provider-root support types such as configuration helpers, project locators, exception mappers, and client setup.
	- `integration/<provider>/<capability>` may contain capability-root provider adapters.
	- `integration/<provider>/<capability>/dto` for provider request/response records.
	- `integration/<provider>/<capability>/mapper` for provider-to-model mapping.
- Use entity-centric output models. Avoid operation-specific output variants when they represent the same business entity.
- For issue flows, prefer `Issue` as the orchestration output model for both search and create operations.
- Use neutral input names like `<Action><Entity>Input` when there is no command bus or CQRS infrastructure.

## DTO Consistency Gate

- Keep one primary response DTO per entity when endpoint responses represent the same entity shape.
- Search/list wrappers may still use operation-specific container DTOs (for example `SearchIssuesResponse`) while item payloads use a single entity DTO (for example `IssueDto`).
- Create endpoints should return the same entity DTO used by retrieval endpoints unless a documented contract requirement demands a different shape.

## Null Handling Gate

- Prefer `Optional.ofNullable(...)` inside method bodies when it is clearer than repeated ternary checks.
- Avoid repeated `value == null ? ... : ...` chains.
- A simple `if (value == null)` is still preferred when it is the clearest form.
- Do not use `Optional` for fields, record components, or public API return types unless there is a strong reason.

## Collections And Streams Gate

- Streams are used for clear transformations.
- Streams are not used when a loop is simpler and clearer.
- Stream pipelines have no hidden side effects.
- Collections are returned empty, not `null`.
- Ordering is explicit when it matters.
- Public methods that can return no value are annotated with `@Nullable` and return `null` explicitly instead of `Optional`.

## Defensive Copy Gate

- Any record with collection fields must defensively copy those collections in a compact constructor.
- REST input DTOs may preserve `null` to represent "not provided" while still sanitizing null elements when required.
- Orchestration models and REST output DTOs should normalize null collections to immutable empty collections unless a contract explicitly requires nullable collections.

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

See `.github/instructions/local-quality-rules.instructions.md` for the shared verification workflow, command order, and required report outputs.

- The local quality gate covers formatting validation, static analysis, and coverage checks.
- If Maven or plugin execution is unavailable in the current environment, the status is `BLOCKED`, not `PASS`.
- The local toolchain is not treated as a replacement for Reviewer judgment on project-specific rules it does not encode.
- The repo-owned toolchain already enforces local-variable finality, identifier naming conventions, and defensive-copy exposure checks.
- Static-analysis evidence must include the executed command and the generated report paths under `flow-orchestrator/target/`.
- PMD rule additions and SpotBugs exclusions must stay narrow, justified, and reviewable. Do not silence findings broadly to force a green build.

## Final Verification

- All applicable code standards and collections/streams gates are `PASS` or `Approved deviation`.
- The testing matrix is satisfied for the change.
- All acceptance criteria from the implementation plan are satisfied.
- The required verification workflow passes and produces current local quality reports.
- The repository-supported startup command starts without errors and logs expected startup messages.
- If an API is added or changed, the corresponding `<api-name>.http` file is created or updated in `/flow-orchestrator/http`.
- API-facing changes are smoke-tested with `curl` by verifying the API returns expected responses for valid and invalid requests.
- If no API contract was changed directly, the existing API endpoints most likely affected by the change were identified and verified with `curl`, with commands and observed responses recorded.
- Team Lead sign-off is blocked while Reviewer reports any applicable `FAIL` or `BLOCKED` item.
