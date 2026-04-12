# Implementation Plan: First Issues API

**Artifact path:** `artifacts/implementation-plans/first-issues-api.plan.md`
**Task name:** `first-issues-api`
**Plan date:** `2026-03-30`
**Status:** `Ready for Coder`

## Ownership

- Architect owns this plan only.
- Coder writes `artifacts/implementation-reports/first-issues-api.report.json`.
- Reviewer writes `artifacts/review-reports/first-issues-api.review.json`.
- Team Lead writes `artifacts/implementation-signoffs/first-issues-api.signoff.json`.

## Business Goal

Deliver the first customer-facing issue retrieval capability through `POST /api/issues`, using the configured GitLab project as the fixed baseline, while preserving the project constitution: thin orchestration boundary, provider-agnostic port contract, provider-specific integration isolation, centralized sanitized error handling, and test coverage at unit, integration, and component levels.

## Requirement Lock / Source Of Truth

- Original request source: `artifacts/user-prompts/feature-1-first-issues-api.md`
- Story source: `artifacts/user-stories/first-issues-api.story.md`
- Non-negotiable input or source-of-truth constraints:
  - Expose `POST /api/issues`.
  - Request body is optional; missing body means list all issues using defaults.
  - Request JSON shape uses nested `pagination` and `filters` objects.
  - Default pagination is `page=1`, `perPage=40` when omitted.
  - Supported filters are `state`, `labels`, `assignee`, and `milestone`.
  - `labels`, `assignee`, and `milestone` each accept at most one value and must fail with a validated client error when more than one is provided.
- Non-negotiable contract or payload constraints:
  - Response contains `items`, `count`, and `page`.
  - Each item exposes `id`, `title`, `description`, `state`, `labels`, `assignee`, `milestone`, and `parent`.
  - `parent` is the parent epic id when present, otherwise `null`.
- Non-negotiable configuration or external-system constraints:
  - Clients do not provide project selection.
  - Baseline project comes from externalized configuration and must not be hardcoded.
  - Integration must use GitLab project issues endpoint `GET /projects/:id/issues`.
  - Integration must use token-based authentication and keep transport/auth/URL mechanics inside integration.
- Explicit assumptions or unresolved items:
  - `count` will mean the number of items returned in the current API page, not the total number of matching items across all pages. This keeps the contract stable even when GitLab omits `x-total` headers for large result sets.
  - Response `id` uses GitLab issue `id` and `parent` uses GitLab `epic.id`, because the locked contract asks for `id` and `parent epic id`, not `iid`.
  - Existing user-managed keys `app.gitlab.url` and `app.gitlab.token` remain the configuration source of truth; implementation must not require renaming them.
  - `perPage` will be rejected when it exceeds configured `app.issues-api.max-page-size`, because GitLab offset pagination documents a max `per_page` of `100` and the repository already defines this limit in configuration.

## Payload Examples

### Request Example

```json
{
  "pagination": {
    "page": 2,
    "perPage": 20
  },
  "filters": {
    "state": "opened",
    "labels": ["bug"],
    "assignee": ["john.doe"],
    "milestone": ["Oran"]
  }
}
```

### Success Response Example

```json
{
  "items": [
    {
      "id": 123,
      "title": "Issue title",
      "description": "Issue description",
      "state": "opened",
      "labels": ["bug"],
      "assignee": "john.doe",
      "milestone": "Oran",
      "parent": 42
    }
  ],
  "count": 1,
  "page": 2
}
```

### Error Response Example

```json
{
  "code": "INTEGRATION_FAILURE",
  "message": "Unable to retrieve issues from GitLab",
  "details": []
}
```

### Validation Error Response Example

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "filters.labels must contain at most 1 value"
  ]
}
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `pagination.page` must be `>= 1` when provided | `DTO binding` | Static scalar rule on the inbound HTTP contract; reject before orchestration logic runs. |
| `pagination.perPage` must be `>= 1` when provided | `DTO binding` | Static scalar rule on the inbound HTTP contract; reject before orchestration logic runs. |
| `filters.state` must be one of `opened`, `closed`, or `all` when provided | `DTO binding` | Enum-based request contract validation keeps unsupported values from leaking to integration. |
| `filters.labels` must contain at most one value | `DTO binding` | Cardinality is a request-shape rule, not business logic. |
| `filters.assignee` must contain at most one value | `DTO binding` | Cardinality is a request-shape rule, not business logic. |
| `filters.milestone` must contain at most one value | `DTO binding` | Cardinality is a request-shape rule, not business logic. |
| `pagination.perPage` must not exceed configured max page size | `use case` | The limit depends on runtime configuration (`app.issues-api.max-page-size`), so it cannot live in a static annotation alone. |
| Missing body or missing nested request sections must resolve to defaults | `use case` | This is defaulting/orchestration behavior that needs config access and should happen once in a single place before the port call. |

Startup configuration validation is outside the request boundary table and must be enforced through immutable configuration properties so the application fails fast when the configured GitLab URL or token is missing or malformed.

## Scope

### In Scope

- `POST /api/issues` endpoint in `flow-orchestrator`.
- Optional request body handling.
- Pagination defaulting and validation.
- Filtering by `state`, `labels`, `assignee`, and `milestone`.
- Mapping GitLab issue data into the locked response contract.
- Centralized sanitized mapping of validation, malformed JSON, and downstream integration failures.
- Immutable configuration objects for baseline GitLab project settings and issues API defaults.
- Unit, integration, and component tests with distinct responsibilities.
- Documentation updates for the endpoint contract and local HTTP examples.

### Out of Scope

- Multiple labels, assignees, or milestones per request.
- Client-selected project switching.
- Create, update, delete, or workflow-changing issue operations.
- Sorting, searching, or any filter not explicitly locked by the story.
- Returning GitLab provider-specific fields beyond the locked response contract.
- Reworking unrelated shared error contracts beyond what is needed for this feature.

## Class Structure

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/domain/package-info.java` | New | Add the missing top-level domain layer description to keep all four constitutional layers documented consistently. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/domain/issues/Issue.java` | New | Immutable domain model for a single issue projection used between orchestration and integration. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/domain/issues/IssuePage.java` | New | Immutable domain result carrying `items`, current-page `count`, and `page`. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/domain/issues/IssueQuery.java` | New | Immutable domain query containing resolved page, per-page, and single-value filters. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/domain/issues/IssueState.java` | New | Domain enum for provider-agnostic state filter values accepted by this API. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/GitLabProperties.java` | New | Immutable `@ConfigurationProperties` for `app.gitlab`, validating presence and parseability of configured GitLab URL/token without exposing secrets. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/IssuesApiProperties.java` | New | Immutable `@ConfigurationProperties` for `app.issues-api`, exposing default page size and max page size. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/GetIssuesPort.java` | New | Provider-agnostic orchestration port for retrieving issues from the configured baseline project. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/GetIssuesService.java` | New | Thin orchestration use case that resolves defaults, enforces config-backed `perPage` limits, logs sanitized request/result context, and delegates to the port. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesController.java` | New | REST controller exposing `POST /api/issues`, accepting optional body, delegating to the use case, and returning the mapped response contract. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesRequest.java` | New | Immutable request DTO for the endpoint, containing optional nested pagination and filters sections. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/PaginationRequest.java` | New | Immutable nested request DTO with Bean Validation for positive page/per-page values. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssueFiltersRequest.java` | New | Immutable nested request DTO with state plus single-value list fields for labels, assignee, and milestone. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesResponse.java` | New | Immutable outbound response DTO containing `items`, `count`, and `page`. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssueResponseItem.java` | New | Immutable outbound issue item DTO matching the locked API contract exactly. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesRequestMapper.java` | New | Maps nullable inbound DTO structures into a fully resolved `IssueQuery` using configured defaults and single-value extraction. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesResponseMapper.java` | New | Maps `IssuePage` domain results to the public API response DTO. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/GitLabProjectLocator.java` | New | Parses configured GitLab project URL into API base URL and URL-encoded project path for downstream requests. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/GitLabRestClientConfig.java` | New | Registers the integration-scoped `RestClient` (or `RestClient.Builder` customization) that adds the `PRIVATE-TOKEN` header without leaking token handling outside integration. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/GitLabExceptionMapper.java` | New | Converts downstream HTTP/auth/rate-limit/network failures into typed `IntegrationException` values with sanitized messages and semantic error codes. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapter.java` | New | Implements `GetIssuesPort`, constructs the GitLab project issues request, applies query parameters, calls GitLab, and maps provider DTOs into domain models. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssueResponse.java` | New | Provider-specific DTOs for the GitLab issues endpoint response, including nested assignee, milestone, and epic data. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssueMapper.java` | New | Maps GitLab response DTOs into domain `Issue` models, using the first assignee username when present and `epic.id` for `parent`. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/common/web/GlobalExceptionHandler.java` | Modified | Keep centralized exception translation, add explicit malformed JSON handling as `VALIDATION_ERROR`, and add structured sanitized logging for validation/integration/unexpected failures. |
| `flow-orchestrator/http/issues.http` | Modified | Provide runnable local examples for default request, filtered request, and validation-error request. |
| `README.md` | Modified | Align or replace the stale issues API contract section so repository documentation matches the locked story and `.http` examples. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesRequestMapperTest.java` | New | Unit coverage for null-body defaulting, nested defaulting, list flattening, and max-page validation behavior. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesResponseMapperTest.java` | New | Unit coverage for outbound mapping and null handling of assignee, milestone, and parent values. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/integration/gitlab/GitLabProjectLocatorTest.java` | New | Unit coverage for parsing the configured project URL into base API URL and encoded project path. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssueMapperTest.java` | New | Unit coverage for mapping labels, deprecated/single assignee data, milestone titles, and epic parent ids. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapterTest.java` | New | Unit coverage for query construction and sanitized error mapping from downstream failures. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/common/web/GlobalExceptionHandlerTest.java` | New | Unit coverage for malformed JSON, validation errors, integration errors, and generic unexpected failures. |
| `flow-orchestrator/src/test/integration/java/com/gitlabflow/floworchestrator/orchestration/issues/api/IssuesControllerIT.java` | New | `@WebMvcTest` coverage for request binding, optional body handling, validation responses, and HTTP-level response contract behavior. |
| `flow-orchestrator/src/test/component/java/com/gitlabflow/floworchestrator/issues/IssuesApiComponentTest.java` | New | Running-application coverage that proves the full HTTP flow from `POST /api/issues` to stubbed GitLab issue retrieval and back. |
| `flow-orchestrator/src/test/component/java/com/gitlabflow/floworchestrator/issues/support/GitLabIssuesStubSupport.java` | New | Dedicated support for reusable WireMock stubs so component tests stay small and do not embed large stub definitions inline. |

## Implementation Slices

### Slice 1 - Lock the public API contract and orchestration flow

- Goal: Create the endpoint, request/response DTOs, domain models, port contract, and defaulting/mapping flow for `POST /api/issues` without adding provider-specific behavior to orchestration.
- Affected scope:
  - `domain/issues/*`
  - `orchestration/issues/*`
  - `config/IssuesApiProperties.java`
  - `domain/package-info.java`
- Payload / contract impact:
  - Introduces the locked request body shape with optional nested objects.
  - Introduces the locked success response shape with `items`, `count`, and `page`.
  - Keeps the existing shared error envelope shape (`code`, `message`, `details`).
- Validation boundary decisions:
  - Use Bean Validation on request DTOs for positive integers, enum-backed state, and single-value list limits.
  - Use the orchestration service/request mapper to resolve null body and missing nested sections into default page/per-page values.
  - Use the orchestration service to reject `perPage` values above configured max.
- Unit tests:
  - `IssuesRequestMapperTest`
  - `IssuesResponseMapperTest`
  - A focused unit test on `GetIssuesService` if max-page validation/defaulting logic becomes non-trivial enough to deserve its own class-level test.
- Integration / Web tests:
  - `IssuesControllerIT` covers:
    - missing body -> `200 OK`
    - valid filter body -> `200 OK`
    - multiple labels -> `400 BAD_REQUEST`
    - multiple assignees -> `400 BAD_REQUEST`
    - multiple milestones -> `400 BAD_REQUEST`
    - invalid page/perPage values -> `400 BAD_REQUEST`
- Edge / failure coverage:
  - `{}` request body applies defaults.
  - missing `filters` object applies no filters.
  - missing `pagination` object applies defaults.
  - null assignee/milestone/parent values map to `null`, not empty strings.
- INFO logging:
  - `GetIssuesService` logs a sanitized request summary: page, per-page, and which filter categories are present.
  - `GetIssuesService` logs the sanitized result summary: count and page.
- WARN logging:
  - None in this slice.
- ERROR logging:
  - None in this slice.
- Documentation updates:
  - None in this slice.

### Slice 2 - Add GitLab baseline configuration and integration adapter

- Goal: Resolve the configured GitLab project baseline, call the documented project issues endpoint with token auth, and map GitLab payloads to domain models without leaking provider details outside integration.
- Affected scope:
  - `config/GitLabProperties.java`
  - `integration/gitlab/*`
  - `integration/gitlab/issues/*`
- Payload / contract impact:
  - No outward API contract changes.
  - Outbound GitLab request must use:
    - path: `/api/v4/projects/{encodedProjectPath}/issues`
    - query params: `page`, `per_page`, `state`, `labels`, `assignee_username`, `milestone`
    - auth header: `PRIVATE-TOKEN`
- Validation boundary decisions:
  - Fail fast at startup when `app.gitlab.url` or `app.gitlab.token` is missing/blank or when the configured project URL cannot be parsed into host + project path.
  - Do not accept project selection from clients.
- Unit tests:
  - `GitLabProjectLocatorTest`
  - `GitLabIssueMapperTest`
  - `GitLabIssuesAdapterTest`
  - Add a focused config binding/context test only if the configuration property validation cannot be covered adequately by existing startup verification.
- Integration / Web tests:
  - None in this slice; HTTP boundary tests remain in Slice 1 and component flow proof remains in Slice 3.
- Edge / failure coverage:
  - Encoded project path uses `%2F` for namespaced project paths.
  - Missing upstream assignee, milestone, or epic maps to `null` output fields.
  - Downstream `401` -> `INTEGRATION_AUTHENTICATION_FAILED`.
  - Downstream `404` -> `INTEGRATION_NOT_FOUND`.
  - Downstream `429` -> `INTEGRATION_RATE_LIMITED`.
  - Other downstream `4xx/5xx` or network I/O failures -> `INTEGRATION_FAILURE`.
- INFO logging:
  - Adapter logs sanitized downstream request context: resource `project issues`, page, per-page, and filter presence.
  - Adapter logs success summary: returned issue count for the page.
- WARN logging:
  - Adapter logs sanitized downstream known failures (`401`, `404`, `429`, other handled HTTP errors`) with upstream status and resource name only.
- ERROR logging:
  - Adapter logs network/time-out/unexpected downstream failures with sanitized resource context and exception category, never headers or token values.
- Documentation updates:
  - None in this slice.

### Slice 3 - Finish centralized error behavior and prove the full HTTP flow

- Goal: Ensure malformed input and downstream failures are translated centrally and prove the application-to-GitLab path with component tests against stubs.
- Affected scope:
  - `common/web/GlobalExceptionHandler.java`
  - integration component tests and stub support
- Payload / contract impact:
  - Malformed JSON request bodies become `400 BAD_REQUEST` with `VALIDATION_ERROR` instead of falling through to a generic `500`.
  - Integration failures continue to use sanitized error envelopes and do not leak upstream provider messages or stack traces.
- Validation boundary decisions:
  - Malformed JSON is treated as boundary validation failure and mapped centrally at the web layer.
- Unit tests:
  - `GlobalExceptionHandlerTest`
- Integration / Web tests:
  - Extend `IssuesControllerIT` to cover malformed JSON and integration exception to HTTP status/body mapping.
- Edge / failure coverage:
  - Invalid JSON body -> `400 BAD_REQUEST` with `VALIDATION_ERROR`.
  - Downstream auth/rate-limit/failure responses do not leak token, URL, or raw GitLab response body.
  - Unexpected exception still returns `INTERNAL_ERROR` with generic message.
- INFO logging:
  - None in this slice.
- WARN logging:
  - `GlobalExceptionHandler` logs validation failures and handled integration failures with sanitized summaries only.
- ERROR logging:
  - `GlobalExceptionHandler` logs unexpected unhandled exceptions.
- Documentation updates:
  - None in this slice.

### Slice 4 - Sync documentation and execute repository verification flow

- Goal: Update the repo-facing contract artifacts and run the required local verification workflow so coder handoff and later review do not depend on tribal knowledge.
- Affected scope:
  - `flow-orchestrator/http/issues.http`
  - `README.md`
  - verification evidence artifacts written later by coder/reviewer
- Payload / contract impact:
  - `.http` examples must reflect the final locked request/response contract and show both valid and invalid requests.
  - README issues API section must no longer describe the stale contract currently shown in the repository.
- Validation boundary decisions:
  - None.
- Unit tests:
  - None.
- Integration / Web tests:
  - None beyond the automated tests already added in earlier slices.
- Edge / failure coverage:
  - `.http` examples must include:
    - no-body request
    - filtered request
    - invalid multi-value filter request
- INFO logging:
  - None.
- WARN logging:
  - None.
- ERROR logging:
  - None.
- Documentation updates:
  - Update `flow-orchestrator/http/issues.http`.
  - Update `README.md` issues API section to the locked contract and current terminology.

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | Request defaulting, DTO-to-domain mapping, domain-to-response mapping, GitLab URL parsing/encoding, GitLab payload mapping, downstream error mapping, exception handler behavior | `flow-orchestrator/src/test/java/...` and `mvn test` via `scripts/verify-quick.sh` |
| Integration / Web | Yes | `@WebMvcTest` for optional body acceptance, Bean Validation failures, malformed JSON, response contract, and centralized integration-error mapping | `flow-orchestrator/src/test/integration/java/...` and `mvn test`/failsafe through `scripts/verify-quick.sh` |
| Component | Yes | Running application + WireMock proving outbound request path, query params, auth header presence, response mapping, and sanitized downstream failure behavior | `flow-orchestrator/src/test/component/java/...`, `src/test/component/resources/stubs/...`, and `scripts/quality-check.sh` |
| Manual smoke | Yes | Local startup plus `curl` checks for no-body success, filtered success, and invalid multi-value failure | `artifacts/implementation-reports/first-issues-api-verification.log` |
| Static analysis | Yes | Checkstyle, PMD, CPD, SpotBugs, JaCoCo coverage gate | `flow-orchestrator/target/checkstyle-result.xml`, `flow-orchestrator/target/pmd.xml`, `flow-orchestrator/target/cpd.xml`, `flow-orchestrator/target/spotbugsXml.xml`, `flow-orchestrator/target/site/jacoco/jacoco.xml` |

## Acceptance Criteria

- Calling `POST /api/issues` without a request body returns issues from the configured GitLab project using default pagination `page=1` and `perPage=40`.
- Providing `filters.state = "opened"` returns only opened issues.
- Providing a single label returns only issues matching that label.
- Providing multiple labels returns a clear validation error.
- Providing a single assignee returns only issues assigned to that username.
- Providing multiple assignees returns a clear validation error.
- Providing a single milestone returns only issues in that milestone.
- Providing multiple milestones returns a clear validation error.
- Successful responses include `id`, `title`, `description`, `state`, `labels`, `assignee`, `milestone`, and `parent` for every item.
- Successful responses include `items`, `count`, and `page`.
- Custom pagination values are honored when valid.
- Missing pagination values fall back to the defaults.

## Final Verification

- Run `scripts/verify-quick.sh` from the repository root and record the exact command and result.
- Run `scripts/quality-check.sh` from the repository root and record the exact command, result, and generated report paths under `flow-orchestrator/target/`.
- Start the application with the repository-supported local startup flow (`SPRING_PROFILES_ACTIVE=local mvn spring-boot:run` from `flow-orchestrator/`, or the equivalent VS Code task) and record the observed startup result.
- Execute and record `curl` smoke checks for:
  - `POST /api/issues` with no body
  - `POST /api/issues` with a valid filtered request body
  - `POST /api/issues` with an invalid multi-value filter request body
- Record evidence in the implementation artifacts with exact commands, outcomes, and report paths; if any command cannot run, mark it `BLOCKED` with the observed error.

## Risks / Notes

- The repository currently contains a stale issues API description in `README.md`; the implementation must update it in the same change set so consumer documentation stays aligned.
- GitLab returns both deprecated `assignee` and array-based `assignees` fields; mapping must prefer a stable singular outbound `assignee` without exposing provider quirks outside integration.
- GitLab epic data can be absent even when other issue fields are present; `parent` must map to `null` in that case rather than failing the whole request.
- Offset pagination headers such as `x-total` can be omitted for very large result sets; this is why `count` is intentionally defined here as the current page item count.
- No secrets, tokens, or expanded secret-bearing commands may appear in code, logs, reports, or verification artifacts.

## Linked Handoff Artifacts

- Implementation report target: `artifacts/implementation-reports/first-issues-api.report.json`
- Review report target: `artifacts/review-reports/first-issues-api.review.json`
- Sign-off target: `artifacts/implementation-signoffs/first-issues-api.signoff.json`