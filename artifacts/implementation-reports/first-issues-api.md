# Implementation Plan: First Issues API

**Task name:** `first-issues-api`
**Plan date:** `2026-03-29`
**Status:** `Draft`

## Ownership

- Architect owns the planning sections through `Final Verification` and `Risks / Notes` before implementation handoff.
- Reviewer owns `artifacts/review-reports/first-issues-api.review.md`.
- Coder updates `Implementation Update`, `Code Guidance Ledger`, `Acceptance Criteria -> Evidence`, `Blocked Verification`, and `Implementation Details` during or after delivery.
- Team Lead records approvals and final acceptance in a separate sign-off artifact under `artifacts/implementation-signoffs/`.

## Business Goal

Deliver the first working GitlabFlow API that returns issues from the configured baseline GitLab project so enterprise clients can begin using the product immediately. The implementation must establish a maintainable enterprise baseline with validated configuration, thin HTTP entrypoints, explicit boundary mapping, sanitized error handling, and test coverage that can support future provider growth.

## Requirement Lock / Source Of Truth

- Original request source: `artifacts/user-prompts/feature-1-first-issues-api.md`
- Active story source: `artifacts/user-stories/first-issues-api.story.md`
- Architecture and quality sources: `artifacts/constitution.md`, `artifacts/code-guidance.md`, `artifacts/project-overview.md`, repository copilot instructions, and `artifacts/templates/implementation-plan-template.md`
- Non-negotiable input or source-of-truth constraints:
  - The configured GitLab project is the single source of truth; clients never provide project id or path.
  - The HTTP contract is `POST` with optional JSON request body for a logically read-only operation.
  - Filters in scope are optional `labels` and optional `assignee`, independently or combined.
  - `labels` is an array in the client contract but may contain at most one value; more than one must fail validation.
  - `assignee` is a single string in the client contract.
  - Pagination is optional; defaults are page `1` and page size `40`.
- Non-negotiable contract or payload constraints:
  - Default page size must come from configuration, not a class constant.
  - GitLab-specific parameter names and shapes stay in integration only.
  - Provider facts already locked: use `GET /projects/:id/issues`, map label filter to GitLab `labels` string, map assignee filter to GitLab `assignee_username` single-value array, and use GitLab `assignees` response array.
- Non-negotiable configuration or external-system constraints:
  - Keep secrets externalized and never log or report token values.
  - Fail fast on missing or malformed GitLab/project configuration.
  - Keep `artifacts/reference-docs/gitLabAPI.md` synchronized if the integration is activated or clarified.
- Explicit assumptions or resolved items:
  - Response shape decision: return a client-focused wrapper `{ issues, page, pageSize }`; each issue item is a narrow summary with `issueNumber`, `title`, `state`, `labels`, `assignees`, `webUrl`, `createdAt`, and `updatedAt`. Do not expose raw GitLab DTO fields, deprecated `assignee`, or provider pagination headers in the first API contract.
  - Error response decision: use a small custom sanitized error envelope in `common.web` with a stable semantic error code, user-safe message, and optional validation details list instead of introducing RFC 7807 as an unrequested standard.
  - Configuration decision: preserve the configured baseline project as a single externalized source, parse it once at startup into GitLab API coordinates, and never require per-request project input.

## Scope

### In Scope

- Add one POST endpoint that returns issues from the configured baseline GitLab project.
- Accept an optional JSON body with `labels`, `assignee`, `page`, and `pageSize`.
- Apply defaults when the request body or pagination fields are omitted.
- Validate request payloads, especially the single-label rule and pagination bounds.
- Introduce validated immutable configuration for baseline project connection and issues pagination defaults.
- Implement a provider-agnostic orchestration port and a GitLab adapter that maps request and response data explicitly.
- Introduce sanitized exception handling and client-safe error responses.
- Add automated tests at unit, web, and Spring integration levels plus live smoke verification instructions.
- Update API usage documentation, `flow-orchestrator/http/issues.http`, and `artifacts/reference-docs/gitLabAPI.md` as needed.

### Out of Scope

- Multi-project or per-request project selection.
- Any write operation on issues.
- Additional filters beyond one label and one assignee.
- Sorting, provider-specific advanced search, or keyset pagination.
- Domain analytics or flow metrics.
- MCP server feature work.

## Class Structure

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `com.gitlabflow.floworchestrator.FlowOrchestratorApplication` | modified | Enable configuration properties scanning if needed so immutable validated config records are registered cleanly. |
| `com.gitlabflow.floworchestrator.config.GitLabProjectProperties` | new | Immutable validated configuration for the baseline GitLab project URL and access token; no hardcoded values. |
| `com.gitlabflow.floworchestrator.config.IssuesApiProperties` | new | Immutable validated configuration for default page size and maximum allowed page size. |
| `com.gitlabflow.floworchestrator.config.GitLabProjectCoordinates` | new | Parsed startup value object holding GitLab API base URL and encoded project path/id derived from the configured baseline project. |
| `com.gitlabflow.floworchestrator.config.GitLabClientConfiguration` | new | Creates the GitLab Feign client/interceptor setup, validates startup configuration, and keeps token handling and URL parsing out of orchestration. |
| `com.gitlabflow.floworchestrator.orchestration.controllers.IssuesController` | new | Exposes `POST /api/issues`, accepts an optional request body, delegates to orchestration, and maps orchestration results to HTTP response DTOs. |
| `com.gitlabflow.floworchestrator.orchestration.controllers.dto.ListIssuesRequestBody` | new | Client request transport model with optional `labels`, `assignee`, `page`, and `pageSize`; carries boundary-level structural validation only. |
| `com.gitlabflow.floworchestrator.orchestration.controllers.dto.ListIssuesResponseBody` | new | Client response wrapper containing `issues`, `page`, and `pageSize`. |
| `com.gitlabflow.floworchestrator.orchestration.controllers.dto.IssueResponseBody` | new | Client-facing issue summary DTO using business-first names rather than GitLab field names. |
| `com.gitlabflow.floworchestrator.orchestration.controllers.dto.AssigneeResponseBody` | new | Client-facing assignee summary DTO with only the fields needed for the first API. |
| `com.gitlabflow.floworchestrator.orchestration.issues.ListIssuesUseCase` | new | Applies config-driven defaults and validation, constructs a provider-agnostic query, invokes the provider port, and returns orchestration results. |
| `com.gitlabflow.floworchestrator.orchestration.issues.IssuesProvider` | new | Provider-agnostic orchestration port for listing issues from the configured source system. |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesQuery` | new | Immutable orchestration request model with resolved single-label, assignee, page, and pageSize values. |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesResult` | new | Immutable orchestration result model holding issue summaries plus resolved pagination values. |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary` | new | Provider-agnostic issue summary returned from integration to orchestration. |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.AssigneeSummary` | new | Provider-agnostic assignee summary mapped from GitLab `assignees`. |
| `com.gitlabflow.floworchestrator.integration.gitlab.GitLabIssuesClient` | new | Feign client for `GET /projects/:id/issues` using provider-specific query params only inside integration. |
| `com.gitlabflow.floworchestrator.integration.gitlab.GitLabIssuesAdapter` | new | Implements `IssuesProvider`, maps orchestration query to GitLab request DTO/query params, maps GitLab responses to orchestration summaries, and translates provider failures into typed integration exceptions. |
| `com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabListIssuesRequest` | new | Provider-specific request DTO for `labels`, `assignee_username`, `page`, and `per_page`. |
| `com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabIssueResponse` | new | Provider DTO for the GitLab issue response body, including `assignees` and only the fields required for mapped output. |
| `com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabUserResponse` | new | Provider DTO for GitLab assignee/user items. |
| `com.gitlabflow.floworchestrator.common.errors.ErrorCode` | new | Semantic error categories for validation and external integration failures. |
| `com.gitlabflow.floworchestrator.common.errors.ValidationException` | new | Typed boundary-validation exception carrying safe details for client response mapping. |
| `com.gitlabflow.floworchestrator.common.errors.IntegrationException` | new | Typed external-system failure exception carrying provider/source context without leaking tokens or raw provider payloads. |
| `com.gitlabflow.floworchestrator.common.web.ErrorResponse` | new | Sanitized HTTP error response contract with code, message, and optional details. |
| `com.gitlabflow.floworchestrator.common.web.GlobalExceptionHandler` | new | Centralized translation from typed exceptions to HTTP statuses and sanitized error bodies. |
| `flow-orchestrator/http/issues.http` | modified | Executable API examples for default request, each filter scenario, pagination, and validation failure. |
| `README.md` | modified | Short API usage section covering the new POST endpoint, payload, and response shape. |
| `artifacts/reference-docs/gitLabAPI.md` | modified | Keep the project issues resource notes aligned with the actual active GitLab filters and endpoint usage. |

## Implementation Slices

### Slice 1 - Startup Configuration, HTTP Contract, and Failure Envelope

- Short description: Establish the validated configuration model, the POST boundary contract, and the centralized sanitized error contract before adding integration behavior.
- Affected scope: `config`, `common.errors`, `common.web`, `orchestration.controllers`, `orchestration.controllers.dto`, `FlowOrchestratorApplication`.
- Unit tests:
  - Failing-first tests for request structural validation (`labels` max 1, positive page/pageSize, optional body semantics).
  - Failing-first tests for global exception mapping to sanitized validation/integration responses.
  - Failing-first tests for configuration parsing/validation helpers.
- Integration / Web tests:
  - `@WebMvcTest` for `POST /api/issues` with no body, invalid multi-label payload, invalid pagination payload, and sanitized error response shape.
  - Startup/config context test proving invalid required config fails fast.
- Edge / failure coverage:
  - Null request body must be accepted.
  - Blank/empty optional fields must not silently produce provider-specific garbage values.
  - Missing/invalid baseline project configuration must fail startup, not first request.
- Documentation updates:
  - None in this slice beyond comments needed for non-obvious provider/config quirks.

### Slice 2 - Orchestration Use Case and Client Response Shaping

- Short description: Add the use case and orchestration models that resolve defaults, enforce config-driven page-size limits, and shape the client response independently from GitLab transport.
- Affected scope: `orchestration.issues`, `orchestration.issues.model`, `orchestration.controllers` mapping to response DTOs.
- Unit tests:
  - Failing-first tests for default page/pageSize behavior.
  - Failing-first tests for page-size upper bound validation sourced from configuration.
  - Failing-first tests for empty result handling and response pagination echo.
  - Failing-first tests for mapping `IssueSummary` to client response DTOs.
- Integration / Web tests:
  - `@WebMvcTest` with mocked use case/provider for success cases: no body, label only, assignee only, combined filters, explicit pagination.
  - Spring-wired integration test for controller + use case + exception handler interaction.
- Edge / failure coverage:
  - Empty result set returns HTTP success with `issues: []`.
  - Validation error returned when configured max page size is exceeded.
  - No GitLab field names appear in the outward JSON contract.
- Documentation updates:
  - Capture the final outward response shape in README and `issues.http` once the contract is fixed here.

### Slice 3 - GitLab Adapter, Provider Mapping, and Error Translation

- Short description: Implement the GitLab integration behind the orchestration port with explicit request/response mapping and typed failure translation.
- Affected scope: `integration.gitlab`, `integration.gitlab.dto`, `config.GitLabClientConfiguration`, orchestration port wiring.
- Unit tests:
  - Failing-first tests for mapping one client label to GitLab `labels` string.
  - Failing-first tests for mapping one assignee to single-value `assignee_username` array.
  - Failing-first tests for mapping GitLab `assignees` array to provider-agnostic assignee summaries.
  - Failing-first tests for translation of authentication, not-found, rate-limit, and generic provider failures into `IntegrationException` categories.
- Integration / Web tests:
  - Spring integration test covering controller -> use case -> adapter wiring with `GitLabIssuesClient` mocked at the external boundary.
  - Optional focused Spring context test proving the configured Feign client bean uses the derived GitLab API base coordinates.
- Edge / failure coverage:
  - Keep the GitLab CE single-assignee rule explicit in code and tests.
  - Do not propagate raw Feign exceptions, stack traces, or provider messages to clients.
  - Preserve empty-list success behavior.
- Documentation updates:
  - Update `artifacts/reference-docs/gitLabAPI.md` with any missing notes about the active issue filters/params used by FLOW.

### Slice 4 - API Usage Docs, Executable Examples, and Final Verification

- Short description: Finish consumer-facing documentation and collect the verification evidence expected by Reviewer and Team Lead.
- Affected scope: `README.md`, `flow-orchestrator/http/issues.http`, implementation-report evidence sections after coding.
- Unit tests:
  - None beyond keeping the full suite green.
- Integration / Web tests:
  - None new unless documentation examples reveal a missing automated scenario.
- Edge / failure coverage:
  - Include a `.http` example for multi-label validation failure and at least one provider-failure smoke path description.
- Documentation updates:
  - `README.md` API usage section.
  - `flow-orchestrator/http/issues.http` examples for: default request, label only, assignee only, combined filters, explicit pagination, invalid multi-label request.
  - `artifacts/reference-docs/gitLabAPI.md` sync confirmation.

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | Validation rules, config parsing, defaulting, response mapping, GitLab request/response mapping, error translation | JUnit 5 / Mockito test classes green in `mvn test` |
| Web / Controller Slice | Yes | Request binding, optional body handling, response JSON shape, HTTP statuses, sanitized validation/integration errors | `@WebMvcTest` results in `mvn test` |
| Integration | Yes | Spring wiring across controller, use case, adapter, config properties, and global exception handling with mocked external boundary | `@SpringBootTest` or focused Spring context tests green in `mvn test` |
| Live Smoke | Yes | Application startup and external API behavior for valid default request, filtered request, and invalid multi-label request | Startup command plus redacted `curl` evidence recorded in implementation report |

## Acceptance Criteria

- Given the configured GitLab project contains issues, when a client sends `POST /api/issues` without a request payload, then the API returns a successful response with issues from the configured project using default pagination (page 1, page size 40) and no filters.
- Given the configured project contains issues with various labels, when a client sends a payload with exactly one value in `labels`, then the API returns only matching issues.
- Given a client sends more than one label in `labels`, when the request reaches the API, then the API returns a validation error stating that only one label is allowed in the MVP.
- Given the configured project contains issues with various assignees, when a client sends a payload with `assignee`, then the API returns only issues assigned to that assignee.
- Given the configured project contains issues, when a client sends both a single label and an assignee, then the API returns only issues matching both filters.
- Given the configured project contains issues, when a client sends valid pagination controls with or without filters, then the API returns the requested page using the requested page size.
- Given a client sends invalid input such as a negative page number, an excessive page size, multiple labels, or malformed filter values, when the request reaches the API, then the API returns a clear sanitized validation error.
- Given the GitLab integration fails because of network, authentication, or service availability problems, when a client calls the endpoint, then the API returns a sanitized failure response without token values, stack traces, or raw GitLab implementation details.
- Given the GitLab integration succeeds but returns no issues, when a client calls the endpoint, then the API returns a successful response with an empty issue list.
- Given implementation is complete, when reviewing the codebase, then GitLab-specific request/response details remain in integration, orchestration coordinates the use case, default page size comes from configuration, and explicit mapping exists at layer boundaries.
- Given implementation is complete, when reviewing tests, then unit tests cover orchestration logic, mapping logic, validation rules including the one-label rule, and default pagination behavior, and integration/web tests cover full POST request-to-response flow with mocked GitLab responses.
- Given implementation is complete, when reviewing documentation, then `artifacts/reference-docs/gitLabAPI.md` is synchronized with the active GitLab issues integration and API usage documentation exists for the POST endpoint, payload, response shape, and examples.

## Final Verification

- Implement each slice test-first: add the failing automated test for the slice before production code.
- `mvn test`
- `mvn -q -DskipTests compile`
- Sonar analysis for `flow-orchestrator` via `scripts/run-flow-orchestrator-sonar.sh` and passing quality gate, or `BLOCKED` with explicit reason if environment access is unavailable.
- Repository-supported startup verification using the local Spring Boot run task/profile.
- Required redacted `curl` smoke checks for: no-body default request, valid filtered request, and invalid multi-label request.
- Confirm `flow-orchestrator/http/issues.http`, README API usage notes, and `artifacts/reference-docs/gitLabAPI.md` were updated in the same change set.

## Risks / Notes

- Keep the outward contract intentionally narrow; do not add raw GitLab IDs, deprecated `assignee`, or provider headers unless the requirement expands.
- The baseline project configuration likely exists today as a single project URL; startup parsing must be explicit and fail fast rather than guessing malformed values.
- Document the GitLab CE single-assignee-array quirk inline where the integration request is built because it is a non-obvious external API constraint.
- Logging must include safe context such as operation name and provider status but never tokens, request payload secrets, or raw sensitive bodies.
- If a required external config/server dependency blocks Sonar or live smoke execution, the implementation report must mark it `BLOCKED`, not `PASS`.

## Implementation Update

- Summary: Verification-evidence refresh rerun completed for Sonar consistency. Runtime behavior was not redesigned in this refresh. Required verification commands were rerun and logged; Sonar quality gate still fails, while refreshed local Sonar export artifacts now reflect current unresolved-issues and metrics snapshots.
- Branch / worktree reference: `feature-1-v3`
- Head commit SHA at verification: `514b306` (working tree contains additional uncommitted changes)
- Compared against base/reference: Existing branch working tree; correction scope limited to verification rerun and minimal Sonar-targeted cleanup.
- Changed files:
  - `artifacts/implementation-reports/first-issues-api.md`
  - `artifacts/implementation-reports/first-issues-api-verification.log`
  - `artifacts/implementation-reports/sonar-issues.json`
  - `artifacts/implementation-reports/sonar-metrics.json`
- Approved deviations (must be approved by Team Lead): none
- `artifacts/code-guidance.md` quality gate passed: No
- Verification commands and outcomes (record secrets in redacted form only):
  - `mvn test` -> PASS (`BUILD SUCCESS`, `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`)
  - `mvn -q -DskipTests compile` -> PASS
  - `scripts/run-flow-orchestrator-sonar.sh` -> FAIL (`QUALITY GATE STATUS: FAILED`)
  - `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` -> PASS (startup reached `Tomcat started on port 8081`)
  - `curl -X POST http://localhost:8081/api/issues` (no body) -> `HTTP 200`
  - `curl -X POST http://localhost:8081/api/issues` (valid filter payload with one label and assignee) -> `HTTP 200`
  - `curl -X POST http://localhost:8081/api/issues` (invalid multi-label payload) -> `HTTP 400`, `VALIDATION_ERROR`
  - Evidence log: `artifacts/implementation-reports/first-issues-api-verification.log`
- Sonar command and quality gate result: Executed via helper script; quality gate failed.
- Sonar dashboard URL: `https://sonarcloud.io/dashboard?id=com.gitlabflow%3Aflow-orchestrator`
- Sonar CE task URL / ID: `https://sonarcloud.io/api/ce/task?id=AZ0525aoRM9lBm1Kn4nJ` / `AZ0525aoRM9lBm1Kn4nJ` (from `target/sonar/report-task.txt`).
- Sonar analysis ID: Not available from this environment (`/api/ce/task` response returns `Project doesn't exist` without additional access context).
- Refreshed Sonar exports:
  - `artifacts/implementation-reports/sonar-issues.json` refreshed from SonarCloud unresolved-issues API (current snapshot: `total: 0`).
  - `artifacts/implementation-reports/sonar-metrics.json` refreshed from SonarCloud measures API (current snapshot includes `coverage: 83.5`, `bugs: 0`, `code_smells: 0`, `vulnerabilities: 0`).
- Documentation updates completed: No additional doc content change required in this correction; existing slice-4 docs remain in place and runtime smoke evidence was added to the verification log and this report.

## Code Guidance Ledger

| Gate | Status | Evidence / Notes |
|---|---|---|
| Code Standards Gate | PASS | Minimal correction only: removed unused `domain/package-info.java`; no API/orchestration/integration contract redesign. |
| Collections And Streams Gate | PASS | No stream or collection behavior changes in this correction rerun. |
| Testing Matrix | PASS | Unit/web/integration tests pass (`mvn test`), and live smoke checks passed for default request, valid filtered request, and invalid multi-label request. |
| Sonar Gate | FAIL | `scripts/run-flow-orchestrator-sonar.sh` executed; quality gate failed (see verification log and dashboard URL). |
| Final Verification | BLOCKED | `mvn test`, compile, startup, and required smoke checks passed, but final sign-off remains blocked by failed Sonar gate. |

## Acceptance Criteria -> Evidence

| Acceptance Criterion | Implementation Evidence | Verification Evidence | Status |
|---|---|---|---|
| AC1 | Defaulting logic and endpoint behavior implemented in slices 1-2. | `mvn test` passes; live no-body `POST /api/issues` returned `HTTP 200` with issues and defaults (`page=1`, `pageSize=40`). | VERIFIED |
| AC2 | Label filter mapping implemented in adapter and orchestration. | `GitLabIssuesAdapterTest.mapsSingleLabelToGitLabLabelsQueryParameter` passes; live valid filtered request returned `HTTP 200`. | VERIFIED |
| AC3 | `labels` max-one boundary validation implemented. | Live smoke multi-label request returned `HTTP 400` with `VALIDATION_ERROR`; web/unit tests pass. | VERIFIED |
| AC4 | Assignee filter mapping implemented in adapter and orchestration. | Adapter/integration tests pass; live valid filtered request (with assignee) returned `HTTP 200`. | VERIFIED |
| AC5 | Combined label+assignee path implemented in orchestration/integration flow. | `IssuesFlowIntegrationTest.wiresControllerToUseCaseToAdapterAndReturnsMappedResponse` passes; live combined-filter request returned `HTTP 200`. | VERIFIED |
| AC6 | Pagination controls and echo behavior implemented. | Unit/integration tests pass; live filtered request echoed `page=2`, `pageSize=20` with `HTTP 200`. | VERIFIED |
| AC7 | Invalid payload validation and sanitized error mapping implemented. | Web/unit tests and live multi-label smoke confirm sanitized validation behavior. | VERIFIED |
| AC8 | Integration failures translated to sanitized error envelope. | Adapter exception-mapping tests pass (`401/403/404/429/500` categories). | VERIFIED |
| AC9 | Empty-list success handling implemented. | `IssuesFlowIntegrationTest.returnsEmptyListWhenGitLabReturnsNoIssues` passes. | VERIFIED |
| AC10 | Layer boundaries and explicit mapping maintained; startup project URL parsing retained. | Code inspection plus passing adapter/integration tests; runtime now targets parsed API base URL, not project URL as Feign base path. | VERIFIED |
| AC11 | Unit/web/integration tests cover validation, mapping, defaulting, and flow wiring. | `mvn test` passes and includes the required slice test classes. | VERIFIED |
| AC12 | Docs already aligned in prior slice-4 delivery artifacts. | Existing README, `issues.http`, and `gitLabAPI.md` updates present; no regressions from this correction. | VERIFIED |

## Blocked Verification

- Sonar gate is failed: helper script executed successfully but quality gate result is `FAILED`; final sign-off remains blocked until Sonar quality gate passes.
- Sonar evidence refresh itself is complete: local `sonar-issues.json` and `sonar-metrics.json` were regenerated successfully from SonarCloud and now match current snapshot values.

## Implementation Details

Coder updates implementation notes here during or after delivery.

- Added shared defaults for `app.issues-api.default-page-size` and `app.issues-api.max-page-size` in `application.yml` so local profile startup does not fail binding `IssuesApiProperties` to `0` when local override omits issues-api values.
- Corrected GitLab runtime targeting by changing Feign mapping to `/projects/{projectId}/issues` and moving effective base URL control to parsed `GitLabProjectCoordinates.apiBaseUrl()` inside the request interceptor.
- Removed unused `domain/package-info.java` to address the remaining Sonar code-smell candidate reported in prior evidence snapshots.
- Verified startup now succeeds with local profile and runtime requests return expected outcomes for required smoke checks (`200`, `200`, `400`).
