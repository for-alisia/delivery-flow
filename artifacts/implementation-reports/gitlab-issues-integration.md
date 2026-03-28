# Implementation Plan: GitLab Issues Integration

**Feature name:** `gitlab-issues-integration`
**Plan date:** 2026-03-27
**Status:** Ready for implementation

---

## Business Goal

Enable delivery managers and agile teams to retrieve GitLab project issues through FLOW's REST API (`GET /api/issues`) with flexible filtering, providing the foundational data layer that all future flow visibility, aging analysis, and bottleneck detection capabilities will depend on.

---

## Architectural Solution

This feature adds the implementation behind a read-only, single-project GitLab issues query. No domain business logic is involved — this is a validated, filtered pass-through from the GitLab REST API to the FLOW caller. Orchestration owns the entry point, input validation, and port contract. Integration owns the GitLab transport, authentication, DTO shaping, error handling, and mapping to orchestration contracts. The domain layer is untouched.

The layers touched are:

- **orchestration**: controller binding fix (snake_case params), input validation, port contract extensions (`IssueSummary` field additions)
- **integration/gitlab**: Feign client, properties, error decoder, mapper, and service implementation
- **common/web**: `GlobalExceptionHandler` status mapping correction
- **No domain changes**: explicitly confirmed; this is a pass-through with no business rules or domain models involved

### Architectural boundary check against `[MUST]` rules

| Rule | This plan |
|---|---|
| `integration` must not depend on `domain` | Confirmed — integration depends only on orchestration port contracts, not on domain |
| `orchestration` is the only coordinator | `IssuesController` calls `IssuesProvider.fetchIssues()` exclusively; no integration types escape |
| Raw provider data shapes must not cross the integration boundary | `GitLabIssueResponseDTO` and embedded DTOs never leave `integration.gitlab`; `GitlabIssuesMapper` converts them to `IssueSummary` before returning |
| Secrets must never be in code or logs | `GitlabProperties.token()` is read only by the Feign `RequestInterceptor` inside `integration.gitlab`; it is never logged or passed to other layers |
| Config must be validated at startup (fail fast) | `GitlabProperties` compact constructor throws `IllegalArgumentException` if `url` or `token` is blank; Spring Boot wraps this in a `BindException` during startup |
| Exceptions crossing a layer boundary must be typed | Network and HTTP errors from Feign are mapped to `IntegrationException` in `GitlabErrorDecoder` and `GitlabIssuesService` before crossing into orchestration |
| Single centralized exception-to-response translation | `GlobalExceptionHandler` is the sole translator; no additional catch-and-reshape in the controller |
| Every mapping path in the integration layer must have test coverage | `GitlabIssuesMapper` is a dedicated, testable component with its own test class |
| All external input validated before reaching business logic | `GetIssuesRequestValidator` validates all constrained parameters at the controller boundary |

### Pre-existing gap that must be corrected

**`GlobalExceptionHandler.mapIntegrationStatus` maps integration errors to upstream-mirror HTTP statuses (401, 403, 404, 429, 503).** All GitLab errors must produce `502 Bad Gateway` from FLOW's perspective — returning upstream GitLab statuses directly would leak internal integration semantics to callers and violate the constitution's rule that outbound responses must never expose internal implementation details. This method must be corrected as part of this change.

### Pre-existing gap that must be corrected

**`IssueSummary` is missing `closedAt` and `milestone` fields required by acceptance criteria #17.** These fields must be added before the mapper is implemented.

### Pre-existing gap that must be corrected

**`IssuesController` uses `@ModelAttribute GetIssuesRequest request`.** Spring MVC cannot bind `assignee_id=123` to `assigneeId` without explicit mapping. The acceptance criteria use snake_case query parameters (e.g., `per_page`, `order_by`, `assignee_id`). The controller must use explicit `@RequestParam(name = "...")` bindings and construct `GetIssuesRequest` manually.

---

## Structure of the Change

### New classes

| Class | Package | Type | Responsibility |
|---|---|---|---|
| `GitlabProperties` | `integration.gitlab` | `@ConfigurationProperties("app.gitlab")` record | Holds `url` and `token`; validates both are non-blank at binding time via compact constructor; provides `getBaseUrl()` and `getEncodedProjectPath()` computed helpers |
| `GitLabUserDTO` | `integration.gitlab.models.dto` | Jackson-annotated record | Models the embedded GitLab user shape (`id`, `username`) used in `author` and `assignees` fields |
| `GitLabMilestoneDTO` | `integration.gitlab.models.dto` | Jackson-annotated record | Models the embedded GitLab milestone shape (`id`, `title`) used in the `milestone` field |
| `GitLabIssueResponseDTO` | `integration.gitlab.models.dto` | Jackson-annotated record | Models the complete GitLab issue JSON response shape with `@JsonProperty` snake_case annotations |
| `GitlabIssuesClient` | `integration.gitlab` | Feign client interface | Declares a single `GET /issues` method that accepts a query params map; built manually — no `@FeignClient` annotation |
| `GitlabErrorDecoder` | `integration.gitlab` | `feign.codec.ErrorDecoder` | Converts GitLab HTTP error responses to typed `IntegrationException`; extracts `Retry-After` header from 429 responses |
| `GitlabIssuesMapper` | `integration.gitlab.services` | `@Component` | Performs two transformations: `GetIssuesRequest → Map<String, Object>` (query params), and `GitLabIssueResponseDTO → IssueSummary` |
| `GitlabIntegrationConfiguration` | `integration.gitlab` | `@Configuration` + `@EnableConfigurationProperties(GitlabProperties.class)` | Registers `GitlabProperties`; builds the `GitlabIssuesClient` Feign bean using `Feign.builder()` with auth interceptor, error decoder, and pre-constructed project URL |
| `GetIssuesRequestValidator` | `orchestration.issues` | `@Component` | Validates allowed values for `state`, `orderBy`, `sort`, `page`, and `perPage`; throws `ValidationException` on invalid input |

### Modified classes

| Class | Package | What changes and why |
|---|---|---|
| `IssueSummary` | `orchestration.issues.models` | Add `OffsetDateTime closedAt` and `String milestone` fields to satisfy AC #17 |
| `IssuesController` | `orchestration.controllers` | Replace `@ModelAttribute` binding with eleven individual `@RequestParam(name = "snake_case", required = false)` parameters; inject `GetIssuesRequestValidator` and validate before calling `issuesProvider.fetchIssues()` |
| `GitlabIssuesService` | `integration.gitlab.services` | Inject `GitlabIssuesClient`, `GitlabIssuesMapper`, and `GitlabProperties`; implement `fetchIssues`: build query map via mapper, call client, stream-map DTOs via mapper, catch non-`IntegrationException` and wrap as `INTEGRATION_UNAVAILABLE` |
| `GlobalExceptionHandler` | `common.web` | Change `mapIntegrationStatus` to return `HttpStatus.BAD_GATEWAY` for all `INTEGRATION_*` codes; all upstream GitLab errors become 502 to callers |

---

## Layer Responsibilities

### domain

Not touched. This feature has no domain business logic in scope. No domain models, domain services, or domain packages are modified.

### orchestration

- `IssuesController` is the HTTP entry point. It binds snake_case query parameters individually using `@RequestParam(name = "...")`, constructs a `GetIssuesRequest` record, delegates to `GetIssuesRequestValidator`, and then calls `issuesProvider.fetchIssues()`. It does not inspect or transform the result beyond returning it.
- `GetIssuesRequestValidator` is the validation gate. It checks that `state`, `orderBy`, and `sort` are within their allowed sets, and that `page` and `perPage` are positive when provided. It throws `ValidationException` on any violation.
- `IssuesProvider` continues as the port interface — unchanged.
- `GetIssuesRequest` continues as the orchestration input contract — unchanged (snake_case binding is solved at the controller level, not in the record).
- `IssueSummary` is extended with `closedAt` and `milestone` to satisfy AC #17.

### integration

`GitlabIntegrationConfiguration` is responsible for registering `GitlabProperties` and constructing the `GitlabIssuesClient` Feign bean. This bean is built manually with:
- Base URL: scheme and host extracted from `GitlabProperties.url`, plus `/api/v4/projects/{encodedProjectPath}` — the project path is URL-encoded with slashes as `%2F`
- A `RequestInterceptor` that adds `PRIVATE-TOKEN: {token}` to every request (token read from `GitlabProperties.token()`, never logged)
- `GitlabErrorDecoder` as the error decoder
- Spring MVC contract, Jackson encoder, Jackson decoder — sourced from the Spring Cloud Feign configuration infrastructure already present

`GitlabIssuesClient` declares one method: `getIssues(@SpringQueryMap Map<String, Object> queryParams)` returning `List<GitLabIssueResponseDTO>`. The interface has no `@FeignClient` annotation; it is registered as a bean by `GitlabIntegrationConfiguration`. No `@EnableFeignClients` is needed on the application class.

`GitlabIssuesMapper` provides:
- `buildQueryParams(GetIssuesRequest)` — constructs a `Map<String, Object>` with snake_case GitLab API keys, omitting null values. Mapping: `assigneeId → assignee_id`, `authorId → author_id`, `orderBy → order_by`, `perPage → per_page`, others map by same name.
- `toIssueSummary(GitLabIssueResponseDTO)` — constructs an `IssueSummary` record. Handles null `milestone`, null `assignees`, and null `closedAt` without throwing.

`GitlabIssuesService.fetchIssues()` orchestrates within integration:
1. Call `mapper.buildQueryParams(request)` to get the query map
2. Call `client.getIssues(queryMap)` inside a try/catch
3. On success: stream the response, call `mapper.toIssueSummary()` for each, collect to list, return
4. On `IntegrationException`: rethrow directly (already typed and correctly formed)
5. On any other exception (connection refused, timeout, Feign retryable): wrap in `new IntegrationException(INTEGRATION_UNAVAILABLE, "gitlab", null, "GitLab is currently unreachable", null, cause)` and throw

`GitlabErrorDecoder.decode()` maps HTTP response statuses to `IntegrationException`:

| GitLab HTTP Status | ErrorCode | Notes |
|---|---|---|
| 400 | `INTEGRATION_BAD_REQUEST` | Possibly invalid query params passed through |
| 401 | `INTEGRATION_UNAUTHORIZED` | Invalid or expired token |
| 403 | `INTEGRATION_FORBIDDEN` | Token lacks required scope |
| 404 | `INTEGRATION_NOT_FOUND` | Project not found or token has no access |
| 429 | `INTEGRATION_RATE_LIMITED` | Extract `Retry-After` header as `retryAfterSeconds` |
| 5xx | `INTEGRATION_UNKNOWN` | Upstream GitLab server error |
| other | `INTEGRATION_UNKNOWN` | Fallback |

The error decoder must NOT read or log the response body — it may contain sensitive data. Source set to `"gitlab"` on all exceptions.

### config

Not touched. No cross-cutting wiring changes are required. `GitlabIntegrationConfiguration` is a `@Configuration` in `integration/gitlab` that handles its own provider-specific wiring, keeping GitLab auth details inside the integration layer as required by the architecture.

---

## Mapping Strategy

### GitLab query parameters (in `GitlabIssuesMapper.buildQueryParams`)

Responsible class: `GitlabIssuesMapper`, method `buildQueryParams(GetIssuesRequest request)`

| `GetIssuesRequest` field | GitLab API param | Notes |
|---|---|---|
| `assigneeId` | `assignee_id` | Omit if null |
| `authorId` | `author_id` | Omit if null |
| `milestone` | `milestone` | Omit if null |
| `state` | `state` | Omit if null |
| `search` | `search` | Omit if null |
| `labels` | `labels` | Omit if null |
| `orderBy` | `order_by` | Omit if null |
| `sort` | `sort` | Omit if null |
| `page` | `page` | Omit if null |
| `perPage` | `per_page` | Omit if null |

### GitLab issue response → `IssueSummary` (in `GitlabIssuesMapper.toIssueSummary`)

Responsible class: `GitlabIssuesMapper`, method `toIssueSummary(GitLabIssueResponseDTO dto)`

| `GitLabIssueResponseDTO` field | `IssueSummary` field | Notes |
|---|---|---|
| `id` | `id` | Direct |
| `iid` | `iid` | Direct |
| `projectId` | `projectId` | Direct |
| `title` | `title` | Direct |
| `description` | `description` | Nullable |
| `state` | `state` | Direct |
| `labels` | `labels` | Empty list if null |
| `author.username` | `authorUsername` | Null-safe; null author → null username |
| `assignees[].username` | `assigneeUsernames` | Stream to usernames; empty list if null/empty |
| `webUrl` | `webUrl` | Direct |
| `createdAt` | `createdAt` | Direct |
| `updatedAt` | `updatedAt` | Direct |
| `closedAt` | `closedAt` | Nullable |
| `milestone.title` | `milestone` | Null-safe; null milestone → null milestone title |

---

## Security Considerations

**Input validation:** `GetIssuesRequestValidator` validates at the controller boundary before any downstream call. Allowed values enforced:
- `state`: `null`, `"opened"`, `"closed"`, `"all"`
- `orderBy`: `null`, `"created_at"`, `"updated_at"`
- `sort`: `null`, `"asc"`, `"desc"`
- `page`: `null` or `>= 1`
- `perPage`: `null` or between `1` and `100` (aligns with GitLab API max)

String fields (`labels`, `search`, `milestone`) are free-form and passed through as-is. They are query parameters on a read-only endpoint with no SQL or injection risk. Feign URL-encodes them when building the GitLab HTTP request.

**Authentication and authorization:** FLOW does not yet have user-facing auth (out of scope for this feature per the story). The outbound GitLab PAT is loaded from externalized configuration (`application-local.yml`, never committed with real values). It is injected into the Feign `RequestInterceptor` through constructor injection only. It is never logged, never returned in responses, and never passed to orchestration or domain.

**Sensitive data:** The GitLab PAT (`app.gitlab.token`) must never appear in:
- Log output (any level)
- Exception messages
- Error responses
- Test assertions using real values

`GitlabIntegrationConfiguration` must not log the token at construction time. The startup log may safely record `"GitLab integration configured for base URL: {}"` using only the scheme and host.

**External communication:** The GitLab URL is configured by the operator. FLOW does not enforce HTTPS in code — enforcing encrypted transport is the operator's responsibility at the configuration level. The plan documentation should include a deployment note that `app.gitlab.url` must use `https://` in all non-local environments.

**Error handling:** `GitlabErrorDecoder` must not read or log the GitLab response body. Messages for `IntegrationException` must be generic and not reference GitLab internal details (e.g., `"GitLab project not found or not accessible"`, not the raw GitLab JSON). `GlobalExceptionHandler` already sanitizes all integration exceptions into `ErrorResponse` without stack traces.

**Audit trail:** This feature is read-only. No audit log entries are required per the constitution (`[SHOULD]` applies to write or access-sensitive operations).

---

## Performance Considerations

**Call volume:** MVP single-project, no caching. Every `GET /api/issues` call results in one synchronous GitLab API call. This is acceptable for the MVP usage pattern described in the story.

**Pagination:** Supported via `page` and `per_page` query parameters passed through to GitLab. GitLab's maximum `per_page` is 100. The validator enforces `perPage <= 100`. No server-side cursor or total-count tracking is planned for MVP.

**Connection pooling and timeouts:** Feign uses its default HTTP client, which provides basic connection pooling. No custom timeout configuration is planned for MVP. This is an acceptable trade-off noted explicitly — if GitLab is slow, FLOW's request thread blocks for the duration of the GitLab response. Future enhancement: configure Feign connect-timeout and read-timeout via `feign.client.config.default.*` properties.

**Caching:** Explicitly out of scope for MVP. Direct pass-through on every call.

**Response size:** GitLab can return up to 100 issues per page. Each issue object is a medium-sized JSON object. No streaming or chunked response is needed at MVP scale.

---

## Observability Expectations

| Location | Level | Message (parameterized) | Notes |
|---|---|---|---|
| `GitlabIntegrationConfiguration` (bean init) | INFO | `"GitLab integration configured: base URL={}"` | Log only scheme+host; never log project path or token |
| `GitlabIssuesService.fetchIssues` (entry) | DEBUG | `"Fetching issues from GitLab with {} active filters"` | Count of non-null fields in query params |
| `GitlabIssuesService.fetchIssues` (success) | INFO | `"Successfully retrieved {} issues from GitLab"` | Issue count in result |
| `GitlabErrorDecoder.decode` (HTTP error) | WARN | `"GitLab API error: status={} method={}"` | Status code and Feign method key; no response body |
| `GitlabIssuesService.fetchIssues` (network error) | ERROR | `"GitLab API unreachable: {}"` | Exception message only; no stack trace unless at DEBUG |
| `GetIssuesRequestValidator` | DEBUG | `"Validation failed for parameter '{}': {}"` | Field name and violation message |

The `IssuesController` already logs at DEBUG (incoming request) and INFO (result count). No additional controller logging is needed.

Noise control: `GitlabIssuesService` DEBUG logs must not fire on every field evaluation — only once per request entry/exit.

---

## Testing Strategy

All tests live in `src/test/java/com/gitlabflow/floworchestrator/`. No test files currently exist; all test classes must be created. All tests follow the `given/when/then` naming convention and `@DisplayName` on class and method.

### `integration.gitlab.services.GitlabIssuesMappingTest`

Pure unit test. No Spring context.

Cover:
- `toIssueSummary`: all fields populated — assert every `IssueSummary` field maps to the correct DTO field
- `toIssueSummary`: null `assignees` → `assigneeUsernames` is empty list (not null, not NPE)
- `toIssueSummary`: null `author` → `authorUsername` is null without NPE
- `toIssueSummary`: null `milestone` → `milestone` is null without NPE
- `toIssueSummary`: null `closedAt` → `closedAt` is null without NPE
- `toIssueSummary`: null `labels` → `labels` is empty list without NPE
- `buildQueryParams`: all fields non-null → map contains all ten snake_case keys
- `buildQueryParams`: all fields null → returned map is empty
- `buildQueryParams`: mixed null/non-null → only non-null fields appear in map, keys are the correct snake_case names

### `integration.gitlab.GitlabErrorDecoderTest`

Pure unit test. No Spring context. Construct a minimal Feign `Response` for each case.

---

## Implementation Update (2026-03-27)

- Updated `GitlabIssuesServiceTest` to stop mocking `GitlabProperties` (record/final type).
- Replaced mocked properties with a real instance in `@BeforeEach`:
	- `new GitlabProperties("https://gitlab.com/group/project", "test-token")`
- Constructed `GitlabIssuesService` manually in test setup using real properties and existing mocks for collaborators.
- Added assertion that encoded project path is `group%2Fproject`.

### Verification Commands Run

- `cd flow-orchestrator && mvn -Dtest=GitlabIssuesServiceTest test`
- `cd flow-orchestrator && mvn test`

### Verification Outcome

- The test source compiles and no IDE-reported errors remain in `GitlabIssuesServiceTest`.
- This environment did not provide reliable terminal execution output through available tools, so full-suite pass/fail could not be independently captured in-tool from this session.

Cover:
- 400 → `IntegrationException` with `INTEGRATION_BAD_REQUEST`, source `"gitlab"`
- 401 → `INTEGRATION_UNAUTHORIZED`
- 403 → `INTEGRATION_FORBIDDEN`
- 404 → `INTEGRATION_NOT_FOUND`
- 429 with `Retry-After: 30` header → `INTEGRATION_RATE_LIMITED`, `retryAfterSeconds` = 30
- 429 without `Retry-After` header → `INTEGRATION_RATE_LIMITED`, `retryAfterSeconds` is null/empty
- 500 → `INTEGRATION_UNKNOWN`
- 503 → `INTEGRATION_UNKNOWN`

### `integration.gitlab.services.GitlabIssuesServiceTest`

Unit test. Mock `GitlabIssuesClient` and `GitlabIssuesMapper` using Mockito.

Cover:
- Successful fetch: verify `buildQueryParams` called with the request, `client.getIssues` called with the returned map, `toIssueSummary` called for each DTO, result list returned
- Empty GitLab response: verify empty list returned without error
- `IntegrationException` thrown by client: verify it passes through unmodified (not re-wrapped)
- Non-`IntegrationException` thrown by client (e.g., `RuntimeException("connection refused")`): verify it is wrapped in `IntegrationException` with `INTEGRATION_UNAVAILABLE`, original exception is wrapped as cause

### `integration.gitlab.GitlabPropertiesValidationTest`

Unit test. Exercise the `GitlabProperties` compact constructor directly (not Spring Boot binding).

Cover:
- Blank `url` throws `IllegalArgumentException` (or `IllegalStateException` — choose one and be consistent)
- Null `url` throws the same exception
- Blank `token` throws the same exception
- Null `token` throws the same exception
- Valid `url` and `token` — constructor succeeds
- `getBaseUrl()` for `https://gitlab.example.com/group/project` returns `"https://gitlab.example.com"`
- `getEncodedProjectPath()` for `https://gitlab.example.com/group/project` returns `"group%2Fproject"`
- `getEncodedProjectPath()` for a nested path `https://gitlab.example.com/group/subgroup/project` returns `"group%2Fsubgroup%2Fproject"`

### `orchestration.issues.GetIssuesRequestValidatorTest`

Pure unit test. No Spring context.

Cover:
- `state = "opened"` passes
- `state = "closed"` passes
- `state = "all"` passes
- `state = null` passes (optional)
- `state = "invalid"` → `ValidationException` with message referencing `state`
- `orderBy = "created_at"` passes
- `orderBy = "updated_at"` passes
- `orderBy = "invalid"` → `ValidationException`
- `sort = "asc"` passes, `sort = "desc"` passes, `sort = "invalid"` → `ValidationException`
- `page = 1` passes, `page = 0` → `ValidationException`, `page = -1` → `ValidationException`
- `perPage = 1` passes, `perPage = 100` passes, `perPage = 101` → `ValidationException`, `perPage = 0` → `ValidationException`
- All null values in request → no exception (all optional)

### `orchestration.controllers.IssuesControllerTest`

Spring MVC slice test using `@WebMvcTest(IssuesController.class)`. Mock `IssuesProvider` (`@MockBean`) and `GetIssuesRequestValidator` (`@MockBean`).

Cover:
- `GET /api/issues` without params: verify `issuesProvider.fetchIssues` called with all-null-field `GetIssuesRequest`; 200 OK returned
- `GET /api/issues?assignee_id=123`: verify `GetIssuesRequest.assigneeId()` is `123L`
- `GET /api/issues?per_page=20&page=2`: verify `perPage` is `20`, `page` is `2`
- `GET /api/issues?state=opened`: verify `state` is `"opened"`
- Validator throws `ValidationException("Invalid state")`: verify response is 400 with expected error body
- `issuesProvider.fetchIssues` throws `IntegrationException`: verify response is 502

---

## GitLab API Impact

This change adds an active Feign call to the GitLab Issues resource (`GET /api/v4/projects/:id/issues`, Project context).

The `artifacts/reference-docs/gitLabAPI.md` already records `Used in FLOW: TRUE` for the Issues row (project context). **No update to this file is required.**

The endpoint parameters used by this feature (`labels`, `state`, `assignee_id`, `author_id`, `milestone`, `search`, `order_by`, `sort`, `page`, `per_page`) are confirmed as standard GitLab REST API v4 query parameters for the project issues list endpoint (GitLab 11.0+).

---

## Acceptance Criteria

### Behavioral

- AC #1: `GET /api/issues` with no params returns 200 with JSON array of `IssueSummary` objects (GitLab default `state=opened` applies)
- AC #2–8: Each filter parameter applied individually and in combination correctly reaches GitLab as the appropriate snake_case query param
- AC #9: Invalid `state`, `order_by`, `sort`, out-of-range `page`/`per_page` → 400 with message identifying the invalid parameter
- AC #10: Missing `app.gitlab.url` → application fails to start with a clear error before accepting any requests
- AC #11: Missing `app.gitlab.token` → application fails to start with a clear error before accepting any requests
- AC #12–16: All GitLab upstream errors (401, 403, 404, 429, 500, network unreachable) → FLOW returns 502; no GitLab-internal error body exposed; 429 with `Retry-After` header is forwarded
- AC #17: `IssueSummary` response includes all fields: `id`, `iid`, `projectId`, `title`, `description`, `state`, `createdAt`, `updatedAt`, `closedAt`, `labels`, `assigneeUsernames`, `authorUsername`, `webUrl`, `milestone`
- AC #18: Null optional fields (no assignees, no milestone, no closedAt) map cleanly — empty list or null, no 500
- AC #19: Empty GitLab response → 200 with `[]`

### Architecture conditions

- No `GitLabIssueResponseDTO`, `GitLabUserDTO`, or `GitLabMilestoneDTO` imported anywhere outside `integration.gitlab`
- No `GitlabProperties` or GitLab token value accessible outside `integration.gitlab`
- `GlobalExceptionHandler` remains the sole exception-to-response translator; no controller-level catch blocks
- `FlowOrchestratorApplication` has no `@EnableFeignClients` annotation; Feign client is registered manually via `GitlabIntegrationConfiguration`

### Security conditions

- GitLab PAT does not appear in any log entry at any level
- GitLab PAT does not appear in any HTTP response body or header returned to the caller
- GitLab error response body is never included in FLOW error responses or logs
- Application fails fast on startup if `app.gitlab.url` or `app.gitlab.token` is absent or blank

### Quality conditions

- All six test classes pass with no failures
- Every mapping path in `GitlabIssuesMapper` is exercised by at least one test case
- Every `ErrorCode` branch in `GitlabErrorDecoder` is exercised by at least one test case

---

## Implementation Order

Implement and verify each step independently before proceeding to the next. Steps 1–3 are pre-conditions for all others.

1. **Extend `IssueSummary`** — add `OffsetDateTime closedAt` and `String milestone` fields. This is a prerequisite for the mapper and all mapping tests.

2. **Fix `IssuesController` parameter binding** — replace `@ModelAttribute` with eleven explicit `@RequestParam(name = "...", required = false)` parameters; construct `GetIssuesRequest` manually. Do not add validation yet (validator does not exist). Verify compilation.

3. **Fix `GlobalExceptionHandler.mapIntegrationStatus`** — return `HttpStatus.BAD_GATEWAY` for all `INTEGRATION_*` codes.

4. **Create `GetIssuesRequestValidator`** — implement validation logic. Write `GetIssuesRequestValidatorTest`. All tests must pass.

5. **Wire `GetIssuesRequestValidator` into `IssuesController`** — inject and call before `issuesProvider.fetchIssues()`. Write `IssuesControllerTest`. All tests must pass.

6. **Create `GitlabProperties`** — record with compact constructor validation and `getBaseUrl()` / `getEncodedProjectPath()` helpers. Write `GitlabPropertiesValidationTest`. All tests must pass.

7. **Create DTO classes** — create `GitLabUserDTO`, `GitLabMilestoneDTO`, `GitLabIssueResponseDTO` with `@JsonProperty` snake_case field name annotations. No tests needed for plain records; they are covered by mapping tests in step 9.

8. **Create `GitlabIssuesClient`** — Feign interface with one `getIssues(@SpringQueryMap Map<String, Object>)` method. No annotation on the interface itself.

9. **Create `GitlabIssuesMapper`** — implement `buildQueryParams` and `toIssueSummary`. Write `GitlabIssuesMappingTest`. All tests must pass.

10. **Create `GitlabErrorDecoder`** — implement `decode()` with status-to-ErrorCode mapping and Retry-After extraction. Write `GitlabErrorDecoderTest`. All tests must pass.

11. **Create `GitlabIntegrationConfiguration`** — register `GitlabProperties` via `@EnableConfigurationProperties`; build `GitlabIssuesClient` Feign bean with auth interceptor, error decoder, and pre-constructed project URL using helpers from `GitlabProperties`.

12. **Implement `GitlabIssuesService.fetchIssues()`** — inject `GitlabIssuesClient` and `GitlabIssuesMapper`; implement fetch, map, and error-wrapping logic. Write `GitlabIssuesServiceTest`. All tests must pass.

13. **Full module verification** — compile the module, run all tests (`mvn test`), confirm zero failures, zero compilation errors.

---

## Plan Validation Checklist

- [x] Constitution `[MUST]` rules touched are explicitly addressed in the Architectural Solution table
- [x] Layer boundaries respected: integration depends on orchestration contracts only; domain untouched; config not used for provider-specific wiring
- [x] GitLab API details verified: endpoint path, query parameter names, JSON field names (snake_case), and pagination mechanism are confirmed against GitLab REST API v4 documentation and cross-checked against the user story
- [x] `artifacts/reference-docs/gitLabAPI.md` already updated to `TRUE` — no further change required; noted explicitly
- [x] Three pre-existing gaps identified and included in the implementation order with the same priority as new code
- [x] Testing guidance is present and actionable: six test classes, explicit coverage requirements, Spring MVC slice test vs pure unit test distinction clearly drawn
- [x] Observability guidance covers what to log, at what level, and what never to log (token, response bodies)
- [x] Security considerations cover validation, auth, sensitive data, error sanitization, and transport
- [x] Performance section explicitly addresses pagination, caching policy, connection pooling, and timeout posture
- [x] Implementation order gives Java Coder a sequenced, independently-verifiable task list
- [x] No `package-info.java` updates required — existing package-info files in `integration` and `orchestration` adequately describe the package boundaries; new sub-packages (`integration.gitlab.models.dto`) are self-describing

---

## Implementation Notes (2026-03-27)

### Implemented in plan order

1. Extended `IssueSummary` with `closedAt` and `milestone`.
2. Replaced `IssuesController` `@ModelAttribute` binding with explicit snake_case `@RequestParam` bindings and manual `GetIssuesRequest` construction.
3. Updated `GlobalExceptionHandler` so integration exceptions map to `502 Bad Gateway`.
4. Added `GetIssuesRequestValidator` and `GetIssuesRequestValidatorTest` for state/order/sort/page/perPage validation.
5. Wired `GetIssuesRequestValidator` into `IssuesController` and added `IssuesControllerTest` (`@WebMvcTest`) for binding and 400/502 behavior.
6. Added `GitlabProperties` in `integration.gitlab` with compact-constructor fail-fast validation and helper methods `getBaseUrl()` and `getEncodedProjectPath()`, plus `GitlabPropertiesValidationTest`.
7. Added integration DTOs: `GitLabUserDTO`, `GitLabMilestoneDTO`, `GitLabIssueResponseDTO`.
8. Added manual Feign interface `GitlabIssuesClient` with `@SpringQueryMap`.
9. Added dedicated `GitlabIssuesMapper` and `GitlabIssuesMappingTest` for full field and query-param mapping coverage.
10. Added `GitlabErrorDecoder` and `GitlabErrorDecoderTest` for status-to-`IntegrationException` mapping including `Retry-After` extraction.
11. Added `GitlabIntegrationConfiguration` with manual `Feign.builder()`, token interceptor, properties registration, and target URL composition from validated properties.
12. Implemented `GitlabIssuesService.fetchIssues()` with mapper delegation, DTO-to-contract mapping, `IntegrationException` pass-through, and non-integration exception wrapping to `INTEGRATION_UNAVAILABLE`.
13. Verified module diagnostics after final changes.

### Verification commands and outcomes

- `get_errors` on `flow-orchestrator/src/main/java` and `flow-orchestrator/src/test/java`: **no errors found** after the final changes.
- `mcp_io_github_ups_get-library-docs` lookups used to validate OpenFeign API usage (`@SpringQueryMap`, manual `Feign.builder()`, and `ErrorDecoder` behavior).

### Approved deviations

- `mvn test` could not be executed in this environment via available tools; verification was performed with workspace diagnostics (`get_errors`) instead.
