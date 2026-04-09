# Architecture Guidance

Use this document when making structural decisions for `flow-orchestrator`.
Coding rules live in `code-guidance.md`. Non-negotiable walls live in `constitution.md`.
This document covers **where things go, how things compose, and when to extract shared mechanisms**.

---

## Model Hierarchy

### Orchestration model is the source of truth

Orchestration models define business meaning: field names, types, constraints, and interface contracts.
Every model that crosses a layer boundary starts as an orchestration model.

### DTO is transport adaptation

REST DTOs adapt orchestration models for HTTP transport. Specific DTO rules are in `code-guidance.md` → "DTO Consistency Gate".

Integration DTOs (`GitLab*Response`, `GitLab*Request`) mirror the external provider's API shape. They have no obligation to match orchestration models — the adapter mapper bridges the gap.

---

## Shared Infrastructure

### Extraction principle

When a mechanism appears in one capability, it is capability-local.
When a second capability needs it, **extract to shared infrastructure before duplicating**.

Do not wait for a third instance.

### Future capability awareness

Read `documentation/project-overview.md` before planning. Upcoming capabilities will need their own adapters, services, and model extensions — but they reuse shared infrastructure (see `documentation/context-map.md` → "Shared Infrastructure" for current inventory). Per-capability file inventories live in `documentation/capabilities/<capability>.md`.

**When designing for capability N, ask: will capability N+1 need the same mechanism?** If yes, design it as shared from the start or ensure easy extraction.

### Architect responsibility

1. Pattern already exists in shared infra → **reuse it**.
2. Pattern exists capability-local and plan introduces a second instance → **extract to shared infra in this plan**.
3. Pattern is new but will obviously repeat → **place in shared infra from the start**.

### `executeGitLabOperation` pattern

Currently lives inside `GitLabIssuesAdapter`. When a second adapter is added, extract to `integration/gitlab/` as a shared component.

---

## Composition

### Parallel execution

Independent port calls in an orchestration service must execute in parallel. Sequential execution requires a documented data dependency between calls.

When a second service needs the same parallel-composition pattern, extract a shared utility rather than duplicating the boilerplate.

### Composition lives in orchestration

Orchestration services own data composition from multiple sources (Constitution Principle 2).
Adapters must not compose multiple external calls into one return value.

### Enrichment pattern

When an endpoint returns a base entity enriched with data from other sources:

1. Define a composition model (e.g., `EnrichedIssueDetail`) for base + supplements.
2. Fetch in parallel via separate port calls.
3. Compose in the service. Map to DTO in the response mapper.

---

## Integration

Every adapter method follows the same shape:

1. Build URI via `GitLabUriFactory`.
2. Execute one `RestClient` call wrapped in `executeGitLabOperation`.
3. Map response to orchestration model via a dedicated mapper.
4. Return the orchestration model.

See `constitution.md` Principle 2 for the non-negotiable boundary rules.

---

## Performance

- Independent external calls execute in parallel — see [Composition](#parallel-execution).
- No unnecessary object allocation when a direct mapping suffices.
- Composition methods log total duration.

---

## Model Structure Ownership

The **Architect defines all model structures** in the implementation plan:

- Record names, fields, types, and nullability.
- Interface contracts with all accessor methods.
- Sealed hierarchy with permitted implementations.
- Enum discriminators with initial values.

The **Coder implements exactly what the plan specifies**. Model structure decisions are not made during coding.
