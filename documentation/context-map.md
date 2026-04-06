# Context Map

Quick-reference map of the `flow-orchestrator` codebase.
Agents and humans use this to locate code by capability without scanning the full tree.

**Maintenance rule:** Team Lead updates this file after every feature delivery that adds or changes capabilities, endpoints, models, or configuration.

---

## Capabilities

### issues

Search and create GitLab issues through a provider-agnostic orchestration layer.

- **Endpoints:** `POST /api/issues/search`, `POST /api/issues`
- **Orchestration**
  - Service: `orchestration/issues/IssuesService`
  - Port: `orchestration/issues/IssuesPort`
  - Models: `orchestration/issues/model/` — `Issue`, `IssuePage`, `IssueQuery`, `IssueState`, `CreateIssueInput`
- **REST layer**
  - Controller: `orchestration/issues/rest/IssuesController`
  - DTOs: `orchestration/issues/rest/dto/` — `SearchIssuesRequest`, `SearchIssuesResponse`, `CreateIssueRequest`, `IssueDto`, `IssueFiltersRequest`, `PaginationRequest`
  - Mappers: `orchestration/issues/rest/mapper/` — `IssuesRequestMapper`, `IssuesResponseMapper`
- **Integration (GitLab)**
  - Adapter: `integration/gitlab/issues/GitLabIssuesAdapter` (implements `IssuesPort`)
  - DTOs: `integration/gitlab/issues/dto/` — `GitLabIssueResponse`, `GitLabCreateIssueRequest`
  - Mapper: `integration/gitlab/issues/mapper/GitLabIssuesMapper`
- **Config:** `config/IssuesApiProperties` (`app.issues-api.*` — default-page-size, max-page-size)
- **Tests:** 12 unit, 1 integration (`IssuesControllerIT`), 1 component (`IssuesApiComponentTest`)
- **Design notes:**
  - `Issue` is the unified output model for both search and create
  - `IssueDto` is the single API response shape for all issue endpoints; fields: `id` (GitLab global id), `issueId` (project-scoped number, maps from GitLab `iid`), `title`, `description`, `state`, `labels`, `assignee`, `milestone`, `parent`
  - `GitLabIssuesAdapter` handles both search (GET) and create (POST) via `RestClient`
  - Pagination is GitLab header-based, mapped in adapter

---

## Shared Infrastructure

### common

Cross-cutting error handling and web concerns. Shared by all capabilities.

- **Error types:** `common/error/` — `ErrorCode`, `IntegrationException`, `ValidationException`
- **Web:** `common/web/` — `GlobalExceptionHandler`, `ErrorResponse`

### config

Application and provider configuration. Shared by all capabilities.

- **GitLab connection:** `config/GitLabProperties` (`app.gitlab.*` — url, token)
- **REST client setup:** `integration/gitlab/GitLabRestClientConfig`
- **Project resolution:** `integration/gitlab/GitLabProjectLocator`
- **Exception mapping:** `integration/gitlab/GitLabExceptionMapper`

### scripts

Verification and quality tooling.

- `scripts/verify-quick.sh` — compile + tests (fast gate)
- `scripts/final-check.sh` — formatting + full quality gate
- `scripts/quality-check.sh` — Maven clean verify
- `scripts/format-code.sh` — Spotless apply/check
- `scripts/smoke-test.sh` — HTTP endpoint verification (requires running app)

---

## Key Patterns

When adding a new capability (e.g., `merge-requests`, `pipelines`), follow the `issues` structure:

1. `orchestration/<capability>/` — `<Capability>Service`, `<Capability>Port`
2. `orchestration/<capability>/model/` — orchestration models
3. `orchestration/<capability>/rest/` — controller
4. `orchestration/<capability>/rest/dto/` — request/response records
5. `orchestration/<capability>/rest/mapper/` — API mappers
6. `integration/gitlab/<capability>/` — adapter implementing the port
7. `integration/gitlab/<capability>/dto/` — GitLab-specific records
8. `integration/gitlab/<capability>/mapper/` — GitLab-to-orchestration mapper
9. `config/<Capability>ApiProperties` — capability-specific configuration

Shared GitLab infrastructure (`GitLabRestClientConfig`, `GitLabProjectLocator`, `GitLabExceptionMapper`) lives at `integration/gitlab/` and is reused by all capability adapters.
