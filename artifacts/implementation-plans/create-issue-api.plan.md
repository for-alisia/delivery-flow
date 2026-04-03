# Implementation Plan: Create Issue API

**Artifact path:** `artifacts/implementation-plans/create-issue-api.plan.md`
**Task name:** `create-issue-api`
**Plan date:** `2026-04-02`
**Status:** `Draft`

## Business Goal

Enable clients to create GitLab issues in the single configured project via `POST /api/issues`, returning `id`, `title`, `description`, and `labels`. Concurrently, rename the existing fetch endpoint to `POST /api/issues/search` to free the base path.

## Requirement Lock / Source Of Truth

- **Original request:** `artifacts/user-prompts/feature-2-create-issue-api.md`
- **Config constraint:** Configured project is the only source — clients must not supply project id, path, or URL.
- **Endpoint constraint:** `POST /api/issues` creates; `POST /api/issues/search` fetches. Old `POST /api/issues` fetch route is permanently replaced — not kept alive.
- **Request constraint:** `title` required non-null non-blank; `description` optional; `labels` optional array of non-blank strings.
- **Response constraint:** HTTP 201; body contains `id` (GitLab issue `id`, not `iid`), `title`, `description` (present, may be `null`), `labels`.
- **Error constraint:** Malformed JSON, validation failures, integration failures reuse existing shared error shapes without changes.
- **GitLab API (confirmed against `https://docs.gitlab.com/api/issues/`):** `POST /projects/:id/issues`; `title` required string; `description` optional string; `labels` optional **comma-separated string** (not a JSON array); response includes `id` (global), `title`, `description`, `labels` (array), `state`; success returns HTTP 201.

## Payload Examples

```json
// POST /api/issues — full request
{ "title": "Deploy failure", "description": "Step 3 failed", "labels": ["bug", "deploy"] }

// POST /api/issues — title only
{ "title": "Reporting bug" }

// 201 Created — full
{ "id": 84, "title": "Deploy failure", "description": "Step 3 failed", "labels": ["bug", "deploy"] }

// 201 Created — no description
{ "id": 85, "title": "Reporting bug", "description": null, "labels": [] }

// 400 Validation Error — blank title
{ "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["title must not be blank"] }

// 400 Validation Error — blank label element
{ "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["labels[1].<list element> must not be blank"] }

// 400 Validation Error — malformed JSON
{ "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["Malformed JSON request body"] }

// 502 Integration Error
{ "code": "INTEGRATION_AUTHENTICATION_FAILED", "message": "Unable to create issue in GitLab", "details": [] }
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `title` non-null, non-blank | DTO binding (`@NotBlank`) | All external input validated at system boundary before service layer |
| Each `labels` element non-blank | DTO binding (`@NotBlank` on list elements via Hibernate Validator cascade) | Same boundary; element-level via `List<@NotBlank String>` |
| Malformed JSON | System boundary (`GlobalExceptionHandler`) | `HttpMessageNotReadableException` already handled centrally — no change needed |

## Scope

### In Scope
- Rename fetch endpoint `POST /api/issues` → `POST /api/issues/search` in controller, tests, smoke, and `.http` file.
- New `POST /api/issues` handler on `IssuesController` returning HTTP 201.
- Orchestration layer: `CreateIssueCommand`, `CreatedIssue`, `CreateIssuePort`, `CreateIssueService`.
- API layer: `CreateIssueRequest`, `CreateIssueResponse`, `CreateIssueRequestMapper`, `CreateIssueResponseMapper`.
- Integration layer: `GitLabCreateIssueRequest`, `GitLabCreateIssueMapper`, `GitLabCreateIssueAdapter`.
- Unit, `@WebMvcTest`, and WireMock component tests; `issues.http` and `smoke-test.sh` updates.

### Out of Scope
Project selection per request; old `/api/issues` fetch route kept alive; issue update/delete/state transitions; fields beyond title/description/labels; multi-project.

## Class Structure

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `orchestration.issues.CreateIssueCommand` | New (record) | `String title, @Nullable String description, List<String> labels` — immutable command |
| `orchestration.issues.CreatedIssue` | New (record) | `long id, String title, @Nullable String description, List<String> labels` — immutable result; compact constructor: `labels = labels == null ? List.of() : List.copyOf(labels)` |
| `orchestration.issues.CreateIssuePort` | New (interface) | `CreatedIssue createIssue(CreateIssueCommand command)` |
| `orchestration.issues.CreateIssueService` | New (`@Service`, `@Slf4j`, `@RequiredArgsConstructor`) | Delegates to `CreateIssuePort`; INFO logs on entry and exit |
| `orchestration.issues.api.CreateIssueRequest` | New (record) | `@NotBlank String title`, `@Nullable String description`, `@Nullable List<@NotBlank String> labels`; compact constructor filters null elements from labels list |
| `orchestration.issues.api.CreateIssueResponse` | New (record) | `long id, String title, @Nullable String description, List<String> labels`; compact constructor: `labels = labels == null ? List.of() : List.copyOf(labels)` |
| `orchestration.issues.api.CreateIssueRequestMapper` | New (`@Component`) | `CreateIssueCommand toCommand(CreateIssueRequest)`: null labels → `List.of()` |
| `orchestration.issues.api.CreateIssueResponseMapper` | New (`@Component`) | `CreateIssueResponse toResponse(CreatedIssue)` |
| `orchestration.issues.api.IssuesController` | Modified | (a) `@PostMapping` on `getIssues` → `@PostMapping("/search")`; (b) add `@PostMapping` `createIssue` handler returning `ResponseEntity<CreateIssueResponse>` status 201; inject `CreateIssueService`, `CreateIssueRequestMapper`, `CreateIssueResponseMapper` |
| `integration.gitlab.issues.GitLabCreateIssueRequest` | New (record, `@JsonInclude(NON_NULL)`) | `String title, @Nullable String description, @Nullable String labels` — labels is comma-joined string |
| `integration.gitlab.issues.GitLabCreateIssueMapper` | New (`@Component`, `@Slf4j`) | `GitLabCreateIssueRequest toRequest(CreateIssueCommand)`: joins labels with `,`; `CreatedIssue toCreatedIssue(GitLabIssueResponse)`: maps `id`, `title`, `description`, `labels` |
| `integration.gitlab.issues.GitLabCreateIssueAdapter` | New (`@Component`, `@Slf4j`, `@RequiredArgsConstructor`, implements `CreateIssuePort`) | POST to `/projects/{projectPath}/issues` via `RestClient`; uses `GitLabProjectLocator`, `GitLabExceptionMapper`, `GitLabCreateIssueMapper`; same exception-handling pattern as `GitLabIssuesAdapter` |

## Implementation Slices

### Slice 1 — Rename fetch endpoint

- **Goal:** Rename `POST /api/issues` fetch to `POST /api/issues/search`.
- **Affected scope:** `IssuesController` (mapping annotation), `IssuesControllerIT` (`ENDPOINT` constant), `IssuesApiComponentTest` (all path references), `smoke-test.sh` (two check calls), `issues.http` (headers and request paths).
- **Payload / contract impact:** Path change only — no body or response structure changes.
- **INFO logging:** None.
- **WARN logging:** None.
- **ERROR logging:** None.
- **Unit tests:** None new.
- **Integration / Web tests:** All existing `IssuesControllerIT` cases continue to pass at the new path. No new cases.
- **Edge / failure coverage:** N/A — pure path rename.
- **Documentation updates:** `issues.http` section headers and endpoint paths updated to `/api/issues/search`. `smoke-test.sh` both issue checks changed to `/api/issues/search`.

### Slice 2 — Orchestration domain, port, and service

- **Goal:** Introduce `CreateIssueCommand`, `CreatedIssue`, `CreateIssuePort`, `CreateIssueService`.
- **Affected scope:** `orchestration.issues.*` (new files only).
- **Payload / contract impact:** None externally visible in this slice.
- **INFO logging:** `CreateIssueService` — on entry: `"Create issue request received descriptionPresent={} labelCount={}"` (no title value in log); on exit: `"Issue created id={}"`.
- **WARN logging:** None.
- **ERROR logging:** None.
- **Unit tests:** `CreateIssueServiceTest` — mock `CreateIssuePort`; verify command passed through; verify result returned unchanged; verify `IntegrationException` propagates without re-wrapping.
- **Integration / Web tests:** None.
- **Edge / failure coverage:** Port throws `IntegrationException` — propagates directly; port returns `CreatedIssue` with null description — returned as-is.
- **Documentation updates:** None.

### Slice 3 — API DTO, mappers, and controller handler

- **Goal:** Add `CreateIssueRequest` (with Bean Validation), `CreateIssueResponse`, mappers, and `POST /api/issues` handler on `IssuesController`.
- **Affected scope:** `orchestration.issues.api.*` (new files + `IssuesController` injection and new method).
- **Payload / contract impact:** New endpoint exposed: `POST /api/issues` → HTTP 201 with `CreateIssueResponse`.
- **INFO logging:** `IssuesController.createIssue` — on entry: `"Create issue request received"`; on exit: `"Create issue response returned id={}"`.
- **WARN logging:** None.
- **ERROR logging:** None.
- **Unit tests:** `CreateIssueRequestMapperTest` — null labels → `List.of()`; non-null labels passed through. `CreateIssueResponseMapperTest` — null description → null; `CreatedIssue` fields map correctly. `CreateIssueRequestTest` — compact constructor: null labels list stays null; list with null elements has nulls filtered.
- **Integration / Web tests:** `IssuesControllerIT` — add `@MockBean CreateIssueService`, `@MockBean CreateIssueRequestMapper`, `@MockBean CreateIssueResponseMapper`; add cases: (1) valid full body returns 201 with all fields; (2) title-only body returns 201; (3) blank title returns 400 `VALIDATION_ERROR`; (4) labels list with blank element returns 400; (5) malformed JSON returns 400; (6) `IntegrationException` from service → 502 with expected error code.
- **Edge / failure coverage:** Missing body (truly absent) → 400. Empty object `{}` → 400 (title required).
- **Documentation updates:** None (covered in Slice 1 and Slice 4).

### Slice 4 — Integration adapter and component coverage

- **Goal:** Implement `GitLabCreateIssueAdapter`, `GitLabCreateIssueMapper`, `GitLabCreateIssueRequest`; add component test coverage; finalize `.http` file.
- **Affected scope:** `integration.gitlab.issues.*` (new files); `IssuesApiComponentTest` (new test cases); component test fixtures.
- **GitLab call:** `restClient.post().uri(b -> b.path("/projects/{p}/issues").build(projectLocator.projectReference().projectPath())).contentType(APPLICATION_JSON).body(gitLabCreateIssueMapper.toRequest(command)).retrieve().body(GitLabIssueResponse.class)`.
- **Labels mapping:** `toRequest` — if `command.labels()` is non-empty, `String.join(",", command.labels())`; otherwise, field is `null` (excluded by `@JsonInclude(NON_NULL)`).
- **INFO logging:** `"Creating GitLab issue descriptionPresent={} labelCount={}"` on entry; `"GitLab issue created id={}"` on success.
- **WARN logging:** `"GitLab request failed resource=create-issue status={} category={}"`.
- **ERROR logging:** `"GitLab transport failure resource=create-issue category={}"`.
- **Unit tests:** `GitLabCreateIssueMapperTest` — labels joined; empty list → null labels; non-empty description preserved; `toCreatedIssue` maps `id` not `iid`, description nullable, labels list defensive copy. `GitLabCreateIssueAdapterTest` — mock `RestClient`; success path returns correct `CreatedIssue`; 401 → `INTEGRATION_AUTHENTICATION_FAILED`; 404 → `INTEGRATION_NOT_FOUND`; 429 → `INTEGRATION_RATE_LIMITED`; `RestClientException` → `INTEGRATION_FAILURE`.
- **Integration / Web tests:** None.
- **Component tests:** Add `GitLabCreateIssueStubSupport` class. Add fixture `stubs/issues/gitlab-create-issue-response.json` with `id`, `iid`, `title`, `description`, `labels`, `state`. Add two `IssuesApiComponentTest` cases: (1) GitLab returns 201 with fixture → API returns 201 with `id`, `title`, `description`, `labels`; (2) GitLab returns 401 → API returns 502 with `INTEGRATION_AUTHENTICATION_FAILED`.
- **Documentation updates:** Add create-issue examples to `issues.http` using `POST /api/issues`.

## Testing Matrix

| Slice | Unit | Web MVC IT | Component | Notes |
|---|---|---|---|---|
| 1 (rename) | — | Update path constant in `IssuesControllerIT` | Update paths in `IssuesApiComponentTest` | No new test cases |
| 2 (domain/service) | `CreateIssueServiceTest` | — | — | Mock port; pass-through + exception propagation |
| 3 (API layer) | `CreateIssueRequestMapperTest`, `CreateIssueResponseMapperTest`, `CreateIssueRequestTest` | 6 new cases in `IssuesControllerIT` | — | Bean validation on list elements; mock 3 new beans in `@WebMvcTest` |
| 4 (integration) | `GitLabCreateIssueMapperTest`, `GitLabCreateIssueAdapterTest` | — | 2 new cases in `IssuesApiComponentTest` + `GitLabCreateIssueStubSupport` + fixture | 401/404/429/5xx error category mapping |

## Final Verification

- All fields `final`; all new data carriers are records; constructor injection via `@RequiredArgsConstructor`.
- `scripts/verify-quick.sh` passes: compile + all unit, IT, and component tests green.
- `scripts/quality-check.sh` passes: Checkstyle, PMD, CPD, SpotBugs, JaCoCo ≥ 85%.
- `scripts/smoke-test.sh` passes: `/api/issues/search` default and filtered checks return 200; new `/api/issues` create check returns 201.
- `issues.http` reflects both renamed search endpoint and new create endpoint.
- No title, description, or label values appear in any log output — log structural metadata only.
- No `GitLabIssueResponse`, `GitLabCreateIssueRequest`, or other integration types cross the integration boundary.
- Acceptance criteria 1–6 from `artifacts/user-stories/create-issue-api.story.md` are covered by concrete test evidence recorded in the implementation report.
