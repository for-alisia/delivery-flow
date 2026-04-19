# Context Map

Quick-reference index for the `flow-orchestrator` codebase.
Agents use this to locate the right capability and shared infrastructure without scanning the full tree.

All paths relative to `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/` unless marked otherwise.

**Two-tier structure:**
- **This file** — capability index + shared infrastructure. All agents read this.
- **`documentation/capabilities/<capability>.md`** — full file inventory, tests, and design notes per capability. Load only the capability you are working on.

**Maintenance rules:**
- Code Reviewer updates the relevant `capabilities/<capability>.md` and `.http` examples after reviewing the implementation.
- Architect creates a new `capabilities/<capability>.md` when planning a new capability and adds it to the index below.

---

## Application Entry Point

- `FlowOrchestratorApplication.java`

## Configuration Files

- `src/main/resources/application.yml` — default profile (server port, GitLab connection, issues-api defaults)
- `src/main/resources/application-local.yml` — local profile overrides

---

## Capability Index

| Capability | Endpoints | Detail |
|-----------|-----------|--------|
| **issues** — search, create, delete, get-single, label-event history | `POST /api/issues/search`, `POST /api/issues`, `DELETE /api/issues/{issueId}`, `GET /api/issues/{issueId}` | [capabilities/issues.md](capabilities/issues.md) |
| **milestones** — shared model and DTO foundation (no endpoints yet) | N/A | [capabilities/milestones.md](capabilities/milestones.md) |

---

## Shared Infrastructure

### common

| File | Role |
|------|------|
| `common/error/ErrorCode.java` | Error code enum |
| `common/error/IntegrationException.java` | Integration failure exception |
| `common/error/ValidationException.java` | Validation failure exception |
| `common/web/GlobalExceptionHandler.java` | REST exception handler |
| `common/web/ErrorResponse.java` | Error response DTO |

Test: `java/.../common/web/GlobalExceptionHandlerTest.java`

### config

| File | Role |
|------|------|
| `config/GitLabProperties.java` | `app.gitlab.*` — `url`, `token`, `connect-timeout-seconds`, `read-timeout-seconds` |
| `config/AsyncExecutionConfig.java` | Exposes `asyncComposerExecutor` bean (virtual-thread-per-task `ExecutorService`) for `AsyncComposer` |

### orchestration/common

Shared cross-capability orchestration utilities.

| File | Role |
|------|------|
| `orchestration/common/async/AsyncComposer.java` | Fail-fast parallel composition helper — submits tasks via virtual-thread `ExecutorService`, cancels siblings on first failure, unwraps `CompletionException` before rethrow |

Test: `java/.../orchestration/common/async/AsyncComposerTest.java`

### orchestration/common/model

Shared orchestration models reused by multiple capabilities.

| File | Role |
|------|------|
| `orchestration/common/model/User.java` | Shared user/actor model for assignees and change authors |
| `orchestration/common/model/Change.java` | Generic change payload contract |
| `orchestration/common/model/ChangeSet.java` | Generic change-set contract (`ChangeSet<T extends Change>`) |
| `orchestration/common/model/ChangeField.java` | Enum for change fields (currently `label`) |

### orchestration/common/rest/dto

Shared REST DTO contracts reused by capability responses.

| File | Role |
|------|------|
| `orchestration/common/rest/dto/UserDto.java` | Shared user DTO |
| `orchestration/common/rest/dto/ChangeDto.java` | Generic change DTO contract |
| `orchestration/common/rest/dto/ChangeSetDto.java` | Generic change-set DTO contract |
| `orchestration/common/rest/dto/LabelChangeDto.java` | Label change payload DTO |
| `orchestration/common/rest/dto/LabelChangeSetDto.java` | Label change-set DTO |

### orchestration/milestones

Milestone foundation extracted for cross-capability reuse.

| File | Role |
|------|------|
| `orchestration/milestones/model/Milestone.java` | Shared milestone model used by issue detail |
| `orchestration/milestones/rest/dto/MilestoneDto.java` | Shared milestone REST DTO |

### integration/gitlab (shared)

Reusable GitLab client infrastructure for all capability adapters.

| File | Role |
|------|------|
| `integration/gitlab/GitLabRestClientConfig.java` | `RestClient` bean with base URL and auth header |
| `integration/gitlab/GitLabProjectLocator.java` | Resolves GitLab project path to encoded project ID |
| `integration/gitlab/GitLabExceptionMapper.java` | Maps HTTP errors to `IntegrationException` |
| `integration/gitlab/GitLabUriFactory.java` | Builds GitLab API URIs with project path and optional query params |

Tests:
- `java/.../integration/gitlab/GitLabExceptionMapperTest.java`
- `java/.../integration/gitlab/GitLabProjectLocatorTest.java`
- `java/.../integration/gitlab/GitLabUriFactoryTest.java`
- `integration/java/.../integration/gitlab/GitLabRestClientConfigIT.java` (bean-inspection IT: verifies `JdkClientHttpRequestFactory` and bound timeout values; no outbound network calls)

### scripts

| Script | Purpose |
|--------|---------|
| `scripts/verify-quick.sh` | Compile + tests (fast gate) |
| `scripts/final-check.sh` | Formatting + full quality gate |
| `scripts/quality-check.sh` | Maven clean verify |
| `scripts/format-code.sh` | Spotless apply/check |
| `scripts/karate-test.sh` | Karate API smoke tests; reuses a healthy local app or starts one automatically |
| `scripts/coder-handoff-check.sh` | Pre-handoff gate for Java Coder: verifies flow-log state, checks, and final-check |

### orchestration/common/async

| File | Role |
|------|------|
| `orchestration/common/async/AsyncComposer.java` | Fail-fast parallel composition utility — submits suppliers to the virtual-thread executor, cancels siblings on first failure, and unwraps `CompletionException` before rethrowing |

Test: `java/.../orchestration/common/async/AsyncComposerTest.java`

---

### test infrastructure

| File | Role |
|------|------|
| `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | Enables inline mock maker |

---

## Key Patterns

New capabilities follow the package structure defined in `code-guidance.md` → "Package Structure".
Use the `issues` capability (see [capabilities/issues.md](capabilities/issues.md)) as the reference implementation.
Shared GitLab infrastructure listed in "Shared Infrastructure" above is reused by all capability adapters.
