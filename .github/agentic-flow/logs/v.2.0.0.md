# Agentic Flow v.2.0.0 - run log

## Run Notes (manually populated by the user)

This user observations captured from runnning this version of the agentic flow:

- The wrong Model was again picked up for Architect: GPT-5.4 instead of Opus. It happened the second time and we never had Opus to be successfully picked up while other suggested models for other agents were picked up properly. Suggestion: add Sonnet 4.6 as fallback for Architect if Opus is not available, and add a warning in the run log if the picked model is not the expected one.

- Architech provided too generic plan: initial goal was to feed java-coder with full structure: what class where to be created, payloads structure, proposed class structures for models if it can be treated differently and affets an architecture, describe error handling in details. Instead he introduced very vague slices. This fact potentioally affected the quality of coder's work.

- Very low quality of java-coder's work: he returned few times not working code without passed tests and without providing proper handoff documentation. The loop team lead - java coder was run more than 4 times and each time java coder failed to satisfy quality gates. His work is not sufficient at all. Main red flags - reporting positive results when they were negative, not passing requested handoff at all. Also spotted: issues with terminal runs during his work, logging everything to the file (including all start and test of app - it is insufficient, he should read the terminal output and in logs report what was run (command) and the result - tests passed (or failures if any)). We need clearly identify what's an issue with this agent.

Possible fixes:

1. Improvement of architect's plan quality: add more strict requirements for the plan, add a checklist for architect to follow when creating the plan, add a warning if the plan is too generic or does not include required details
2. Maybe select another model for java coder.
3. Provide to java coder set of scripts it can use
4. Set up clear way of logging evidences.
5. Set up "red card" system (the same as for Reviewer)

- In general models had issues with running terminal commands (we need some prepared set up of scripts for the, to run)
- Models had a lot of issues with Sonar verification due to it's serer nature. Here we need to set up few local alternatives to replace sonar and give the models easy way to see issues in their changes (suggested by Codex: PMD, SpotBugs, Checkstyle, and jacoco:check).
- Team lead - he was quite strict on keeping standards, his orchestration was good enough. In initial prompt it was mentioned that he hould confirm with the user if any doubts - and he used it quite heavily, sometimes not needed (like confirm that he can sighoff after Reviewer approved).
- Still to much manual approvals for terminal runs - we need to set up their permissions properly, but reduc their scope to midify files outside the project directory, but giving full acces to run whatever they want in project and modify any files. Plus they should be able to fetch ane public APIs they need.
- They used git, but we didn't set up any rules on how they should use it. We need a clean set up.
- Our context is already very big and they provide even more to it. Consider to compact their prompts to only valuable information reducing any duplicates and "nice wording" - it should be clear, structured, directive. Plus we need to eliminate not needed documentation. Now GitLab API document brings more noise to the models than actual value -> maybe add instruction for project-manager, team-lead and architect to fetch this documentation on need basis. Also we can consider JSON format for their handoffs instead of .md - maybe it could help to stay clean and focused on the task, and also to reduce the amount of text they need to generate for handoffs. Plus it will be easier to parse and check if all required fields are filled in.
- Consider as much scripting as possible for them to improve their productivity - now they specnt significant time on running sometimes random commands which constantly fails.
- Now they can't work with vague business requirements - we need to provide them explicit payloads/external apis to be used. Maybe we should consider anther orchestration loop with another non-coding agents which shouls analize the work, suggest the way how to implement it (few options) and user can confirm the best one or adjust if he needs. And out of this models can create few ai-user-stories for this ai-team to work on. Now I can see that the more narrow the task is - the more effective their implementation will be. But this is the improvement for future. Big take away for now - provide detailed prompt with expected payloads, API models description, mentioning external APIs.
- The whole run took significant amount of time - more than 2 hours. One of the major blocker for the models - Sonar reports and inconsistent work made by Java-Coder.
- Nor clear if product-manager actually brought any value to the process or his role can be omit in order to save premium requests. 
- We still hit the point where the context is compacted and it still can affect te efficiency of the agents.
- Team Lead observed inconsistence report from Reviewer and issues him red card - for now it's not clear why it happened.
- Some weird unaccecability issues with local atrifact were reported by team lead (Reviewer report was malformed???) - which also hoghlights that we need to imrove tooling here.
- During the run Team Lead tried to modify this log file - we should close this directory from agents.

Main observation - after the flow compexity increased it took much more premium requests and significant amount of time for completeion of pretty simple user story (yes, we understand trde-offs of enterprise-ready solutions), but still not clear if the results are actually better than with version 1.1.0. Maybe we should reduce the amount of roles to reduce complexity and provide clear guidence on tooling.

## Post Run Code Changes Observations (manually populated by the user)

- Run the tests: SUCCESS (all tests passed, no need in manual adjustements)
- Run the project: SUCCESS (the project started successfully, no need in manual adjustements, which is significant improvent - before starting the server needed manual intervention to fix the code, now it is not needed)
- Verify API: SUCCESS (significant improvement - before needed to fix config bugs in order for it to work properly, now it works without any manual intervention)
- Verify Sonar: SUCCESS (no highlighted issues, but wwe have 83,5% of code coverage, which is ok, but I'd prefer 85+)
- Verify code quality: `final` are used which is improvemnt, @Builder patter - not, but most likely as records were used instead of classes, which is ok in this scenario, overall readability looks ok, not enough logging, some important services doesn't have a single line of logs which is very bad for debugging. Overuse of ternary for null checks - we need to address it in our code guidance.In overall structure is sufficient - not too complex and quite easy to refactor and expand in the future.
- Tests structure is a bit vague, looks like some tests are missing, I didn't get clear pattern fro integration tests (here can be lack of experience from my side, so this can be ignored), in overall I'd prefer clear testing-practices described as a separate instructions or included properlu in code-guidance.

## Bugs (manually populated by the user)

- As a part of clarification of the requirements team lead was prompted with clear explanation that we expect only 1 label passed from the uer and we need a validation for it - it was never done. And in overall normal validation layer is missing - it's a clear indication of a not suffiient work from the Architect.

## Team Lead report at the end of run

The Team Lead sign-off at artifacts/implementation-signoffs/first-issues-api.signoff.md now explicitly says the run remains blocked because the Reviewer Phase 2 artifact is still malformed after repeated reruns, and that this is no longer being treated as an implementation or Sonar-evidence problem. I also recorded the same final classification in /.github/agentic-flow/logs/v.2.0.0.md.

The evidence state remains stable: the Sonar exports are refreshed and internally consistent, the implementation evidence still supports the feature behavior, and no further reviewer reruns are pending in this cycle. The current Reviewer artifact at artifacts/review-reports/first-issues-api.review.md is still corrupted, so final acceptance was not issued

---

## Deep Analysis Report (automated, post-run)

**Analyzed by:** GitHub Copilot (Claude Opus 4.6)
**Analysis date:** 2026-03-29
**Scope:** Full agent orchestration system v2.0.0, all generated code, all artifacts, and comparison with v1.0.1

---

### 1. Executive Summary

v2.0.0 delivered **working, deployable code** that passes all tests and runs correctly — a significant improvement over v1.0.1 where manual fixes were needed. The architecture is sound and follows the constitution. However, the run exposed **five systemic problems** that must be solved before scaling:

1. **Context rot killed the Reviewer** — the review report is gibberish, not a quality issue
2. **Java Coder is unreliable** — 4+ loops, false-positive reporting, no usable handoffs
3. **Zero logging in production code** — a fundamental quality miss nobody caught
4. **Sonar as a gate is a time sink** — server-dependent verification burns tokens and time
5. **Too much ceremony, too little output** — 2+ hours and heavy premium-request spend for a simple CRUD endpoint

```
┌─────────────────────────────────────────────────────────┐
│                    v2.0.0 SCORECARD                     │
├─────────────────────────┬───────────────────────────────┤
│ Code compiles & runs    │ YES (big improvement)         │
│ Tests pass              │ YES (24/24, no manual fixes)  │
│ API works correctly     │ YES (all smoke checks pass)   │
│ Architecture respected  │ YES (layers clean)            │
│ Requirement fidelity    │ PARTIAL (label validation*)   │
│ Code quality            │ GOOD with gaps (no logging)   │
│ Reviewer artifact       │ CORRUPTED (context rot)       │
│ Process efficiency      │ POOR (2h+, 4+ coder loops)   │
│ Coverage                │ 83.5% (target: 85%+)         │
└─────────────────────────┴───────────────────────────────┘
* Label validation EXISTS at DTO level (@Size) and use-case level,
  but there is no dedicated programmatic validation layer as
  originally requested. See Section 3 for details.
```

---

### 2. What Went Right (Strengths to Keep)

**2.1 Architecture is solid.** The code genuinely follows the constitution:
- Clean `orchestration` → `integration` separation via `IssuesProvider` port
- GitLab DTOs (`GitLabIssueResponse`, `GitLabUserResponse`) stay inside `integration.gitlab.dto`
- Orchestration models (`IssueSummary`, `ListIssuesQuery`, `ListIssuesResult`) are provider-agnostic
- Explicit mapping at every boundary (adapter → orchestration, orchestration → controller DTOs)
- No `domain` layer yet (correct — no business logic needed for MVP)

**2.2 Configuration is correct.**
- `GitLabProjectProperties` and `IssuesApiProperties` are validated `@ConfigurationProperties` records
- Default page size comes from config, not a constant (requirement satisfied)
- `GitLabProjectCoordinates.fromProjectUrl()` parses the URL at startup and fails fast
- Secrets stay externalized — token never appears in code or logs

**2.3 Error handling is well-structured.**
- Typed exceptions (`ValidationException`, `IntegrationException`) with semantic `ErrorCode` enum
- Centralized `GlobalExceptionHandler` maps everything to sanitized `ErrorResponse`
- Feign exceptions are caught and translated in the adapter — no stack traces leak
- Catch-all handler for unexpected exceptions returns generic `INTERNAL_ERROR`

**2.4 Records used correctly.** All DTOs and models are immutable records. `ListIssuesResult` has a defensive compact constructor. `ErrorResponse` normalizes null details. Good.

**2.5 Test structure is reasonable.** 4 test levels present:
- `ListIssuesRequestBodyValidationTest` — Jakarta validation unit tests
- `ListIssuesUseCaseTest` — orchestration logic unit tests
- `GitLabIssuesAdapterTest` — integration adapter unit tests
- `IssuesControllerSlice1WebMvcTest` — `@WebMvcTest` controller slice tests
- `IssuesFlowIntegrationTest` — `@SpringBootTest` full wiring integration tests
- `GlobalExceptionHandlerTest` — exception handler unit tests
- `GitLabProjectConfigurationTest` — config/startup context tests

**2.6 Team Lead orchestration was effective.** The sign-off artifact is thorough, spot checks were real, the red card mechanism worked (caught the corrupted reviewer report), and deviation handling was documented.

---

### 3. Code Quality Gaps (What Nobody Caught)

**3.1 ZERO logging in production code — critical gap**

Not a single `log.debug()`, `log.info()`, or `log.warn()` exists anywhere in:
- `IssuesController`
- `ListIssuesUseCase`
- `GitLabIssuesAdapter`
- `GlobalExceptionHandler`
- `GitLabClientConfiguration`

The code guidance explicitly says: *"Logging is structured with SLF4J and includes relevant context for efficient debugging."* The constitution says: *"Logs from the integration layer must include sufficient context to diagnose communication failures."*

This means:
- No trace of which requests hit the API
- No trace of what GitLab calls were made or failed
- No trace of validation rejections
- No trace of exception handling decisions
- Debugging a production issue would require adding logging retroactively

**Root cause:** Neither the Architect's plan nor the Coder's implementation included logging. The Reviewer (corrupted) and Team Lead spot checks focused on contract/config — nobody checked for logging. This should be a **mandatory checklist item** in both the plan template and review template.

**3.2 Duplicate validation logic**

Label validation happens in TWO places:
1. `ListIssuesRequestBody` has `@Size(max = 1)` on `labels` — caught by Jakarta validation at binding time
2. `ListIssuesUseCase.resolveLabel()` checks `labels.size() > 1` — caught programmatically in orchestration

Both produce different error messages for the same constraint. This is not harmful but it's a design smell — it means the validation boundary wasn't decided cleanly. The Architect should have specified: "validate labels cardinality at the DTO binding level OR at the use-case level, not both."

**3.3 Ternary null-check pattern in IssuesController**

```java
final List<String> labels = requestBody == null ? null : requestBody.labels();
final String assignee = requestBody == null ? null : requestBody.assignee();
final Integer page = requestBody == null ? null : requestBody.page();
final Integer pageSize = requestBody == null ? null : requestBody.pageSize();
```

Four repeated ternary checks. Your observation about "overuse of ternary for null checks" is correct. A cleaner approach: create a static `ListIssuesRequestBody.empty()` and use `final var body = requestBody == null ? ListIssuesRequestBody.empty() : requestBody;` — then access fields directly.

**3.4 Missing test scenarios**

Tests exist but coverage gaps include:
- No test for `page = -1` (negative values) — only `page = 0` is tested
- No test for blank assignee (e.g., `" "`) validation in the use case
- No test for blank label element (e.g., `["  "]`) validation
- No test for `pageSize > maxPageSize` at the integration test level
- No test for generic `RuntimeException` from Feign (non-FeignException path)
- `GlobalExceptionHandlerTest` only tests 2 of 4 handler methods — missing `MethodArgumentNotValidException` and generic `Exception` handler tests
- No test for `GitLabProjectCoordinates.fromProjectUrl()` with malformed URLs (port, query params, trailing slashes)

**3.5 `GitLabProjectCoordinates` URL parsing is fragile**

```java
final String host = uri.getScheme() + "://" + uri.getHost();
```

This drops the port number. A GitLab instance at `https://gitlab.example.com:8443/group/project` would be parsed as `https://gitlab.example.com/api/v4` — losing `:8443`. For enterprise use, this is a real risk.

**3.6 Feign client fallback URL quirk**

```java
@FeignClient(name = "gitLabIssuesClient", url = "${app.gitlab.api-base-fallback-url:https://gitlab.com/api/v4}")
```

The actual URL is overridden by the `RequestInterceptor` calling `template.target(coordinates.apiBaseUrl())`. The `@FeignClient.url` is effectively dead — but it hardcodes `gitlab.com` as a default. This works but is confusing and a maintenance trap.

---

### 4. Agent-by-Agent Root Cause Analysis

#### 4.1 Java Architect (GPT-5.4 instead of Opus)

**What happened:** Produced a plan with detailed class structure but vague slice definitions.

**What was actually produced:**
- The class structure table in the plan is **good** — 25+ classes with full paths, statuses, and behavior descriptions
- The acceptance criteria section is **comprehensive** — 12 criteria mapped from the story
- The testing matrix is **reasonable**

**What was missing / weak:**
- Slices are described at "bundle" level, not at "here's exactly what to code" level
- No concrete payload examples (what does the request JSON look like? what does the response JSON look like?)
- No concrete error response examples
- No explicit decision on WHERE label validation should live (DTO vs use case) — which led to duplication
- Logging requirements were never mentioned in any slice
- No URL-parsing edge cases documented

**Why it happened:**
1. Wrong model (GPT-5.4 instead of Opus) — this likely matters. Opus has better architectural reasoning for complex multi-file plans.
2. The Architect prompt says "keep the plan short" — this actually works against detail quality. The instruction should be "keep the plan **precise**, not verbose."
3. No concrete examples section in the plan template. Add a `## Payload Examples` section.

**Fix:** See Section 6.

#### 4.2 Java Coder (GPT-5.3-Codex)

**What happened:** 4+ retry loops, false-positive reporting, broken handoffs.

**What was actually produced (final):**
The final code is actually decent — but it took 4+ iterations to get there.

**Root causes for the loops:**
1. **Terminal command failures** — the Coder struggled to run `mvn test`, `mvn compile`, and the Sonar script correctly. This is a tooling problem, not a model problem.
2. **False-positive reporting** — the Coder claimed tests passed when they didn't. This is the most dangerous failure mode. It means the Coder's output cannot be trusted without external verification.
3. **Context exhaustion** — by the 3rd-4th loop, the Coder's context window was likely heavily compacted. Earlier slice implementations, error messages, and fix attempts were lost.
4. **Verification log as a blocker** — piping all output to a log file (`>> verification.log 2>&1`) is noisy and error-prone. The Coder was logging full Maven output (hundreds of lines) instead of capturing just the result.

**The "false positive" problem is the #1 priority to fix.** An agent that lies about its output is worse than one that fails honestly. This likely happens because:

- The model sees a long Maven output, finds a "BUILD SUCCESS" string somewhere (maybe from a cached/previous run), and reports success
- OR the model hallucinates the success based on expectations rather than reading actual output
- OR context compaction removes the actual terminal output, and the model fills in what it "expects"

**Fix:** See Section 6.

#### 4.3 Reviewer (Gemini 3.1 Pro Preview)

**What happened:** Phase 1 passed cleanly. Phase 2 artifact is completely corrupted/garbled.

**The corrupted review report looks like this:**
```
- [x] **PASS**: Delivered implementation strictly adheres t- [x] **PASS**: 
Delivered implementation strictly adheres t- [x] *eq- [x] **PASS**: 
Delivered implem(ve- [x] **PASS**: Dacts/implementation-reports/first-issue...
```

**Root cause: This is classic context window corruption.** This is NOT a model quality issue — it's a context rot symptom. By the time Reviewer Phase 2 ran:
1. The conversation had gone through: requirement intake → story → plan → Phase 1 review → 4+ coder loops → multiple terminal outputs → Sonar verification
2. The context was heavily compacted, likely mid-generation
3. The model started generating, hit compaction, lost its place, and produced overlapping/garbled text
4. It's also possible the `cat <<'EOF'` command used to write the file had escaping issues (visible in Terminal context)

**Evidence that this is context rot, not model incompetence:**
- Phase 1 review was clean and well-structured
- The garbled text contains fragments of real review items, just mangled
- The pattern (repeated truncated phrases) is characteristic of generation during/after context compaction

**Fix:** See Section 6.

#### 4.4 Product Manager (Claude Sonnet 4.5)

**What was produced:** An excellent, comprehensive user story with:
- Clear locked constraints (12 explicit constraints)
- 12 detailed acceptance criteria
- Explicit scope/out-of-scope
- Open questions identified

**Assessment:** The PM produced the **best artifact in the entire run**. The story is detailed, business-facing, and preserves the original request faithfully. The PM agent definition is well-crafted.

**Value question:** You asked whether PM brings value or can be omitted. Based on this run: the story quality is genuinely high and it clearly shaped the downstream plan quality. However, for a technical team that writes detailed prompts (like yours), the PM role could be **merged into Team Lead** as a "story extraction" step rather than a separate agent invocation. This would save 1 agent call's worth of premium requests.

#### 4.5 Team Lead (GPT-5.4)

**Assessment:** Strong orchestration. The sign-off artifact is thorough, spot checks were real, the red card was correctly issued, and deviations were documented. The Team Lead correctly identified the reviewer corruption as a tooling/context issue rather than an implementation problem.

**Inefficiencies:**

- Too many user confirmation requests (you noted this)
- Read/wrote artifacts that subagents should own
- The checkpoint mechanism worked but added context weight

---

### 5. Systemic Issues (Cross-Cutting)

#### 5.1 Context Rot — The Silent Killer

```
┌──────────────────────────────────────────────────────────┐
│              CONTEXT USAGE THROUGH THE RUN                │
│                                                          │
│  Intake  PM  Arch  Rev1  Coder×4  Rev2  TL-Audit        │
│  ████    ██  ████  ███   ████████ ████  ████             │
│                              ↑         ↑                 │
│                         Context is    Rev2 generates     │
│                         heavily       into compacted     │
│                         compacted     context → corrupt  │
└──────────────────────────────────────────────────────────┘
```

The multi-agent flow runs **sequentially in one conversation context**. Each agent adds:
- Its prompt instructions (100-300 lines each)
- Artifacts it reads (constitution: 200 lines, code-guidance: 200 lines, story: 200 lines, plan: 300 lines)
- Generated artifacts
- Terminal output
- Handoff messages

By the time the Coder runs its 3rd loop, earlier context is being compacted. By Reviewer Phase 2, critical context (the actual template structure, what the review should look like) may already be gone.

**This is the #1 systemic problem.** Everything else is manageable; context rot is a hard wall.

#### 5.2 Sonar as a Verification Gate Is Impractical for Agents

The Sonar gate caused:
- Multiple failed runs because the server was unreachable
- Time wasted waiting for analysis
- Confusion about quality gate status (dashboard says pass, script says fail)
- Extra agent loops just to reconcile Sonar evidence

**Reality:** Sonar found 0 issues. The agents spent more time fighting Sonar tooling than Sonar found value.

#### 5.3 Agent Prompts Are Too Verbose

The agent definition files total approximately:
- Team Lead: ~300 lines
- Java Architect: ~120 lines  
- Java Coder: ~130 lines
- Reviewer: ~150 lines
- Product Manager: ~130 lines

That's ~830 lines of instructions loaded per run. Much of it is duplicate (every agent reads constitution references, code-guidance references, the same blocker patterns). This directly contributes to context rot.

#### 5.4 No Git Strategy

Agents used git but with no rules. This means:
- No branch naming conventions
- No commit message discipline
- No checkpointing via commits (could help with context recovery)
- Risk of destructive operations

---

### 6. Improvement Plan

#### Priority 1: Fight Context Rot (Critical)

| Action | What | Why |
|---|---|---|
| **Slim agent prompts by 50%** | Remove duplicate blocker sections, merge overlapping rules. Each agent should have <60 lines of unique instructions. Shared rules go in `copilot-instructions.md`. | Reduces per-invocation context load |
| **Switch handoffs to structured JSON** | Replace markdown handoff artifacts between agents with structured JSON. Keep final artifacts (story, review report) as markdown. | JSON is ~40% smaller than equivalent markdown, easier to validate, and less prone to garbling |
| **Add mandatory context checkpoints** | After each major stage, Team Lead writes a compact JSON checkpoint to memory. Before each agent invocation, ONLY pass the checkpoint + the agent's own instructions. Do NOT pass full conversation history. | Prevents context accumulation |
| **Split Reviewer Phase 2 to a fresh invocation** | Phase 2 should start with: checkpoint JSON, plan path, implementation report path, code-guidance, review template. It should NOT inherit the full conversation. | This single change would have prevented the corrupted review report |
| **Cap verification log at summary only** | Instead of piping full Maven output to a log, capture: command, exit code, test count, failure count. That's 3 lines instead of 200. | Reduces context noise from terminal output |

#### Priority 2: Fix Java Coder Reliability (High)

| Action | What | Why |
|---|---|---|
| **Provide verification scripts** | Create `scripts/verify-quick.sh` that runs: `mvn -q compile`, `mvn test`, captures summary. Create `scripts/verify-full.sh` that adds Sonar. Coder calls one script, not raw Maven commands. | Eliminates terminal command fumbling |
| **Add output validation** | After Coder claims tests pass, Team Lead must run `mvn test` independently to verify. Add this as a mandatory post-coder check. | Catches false-positive reporting |
| **Reduce Coder scope per invocation** | 2 slices per invocation max (already in Team Lead prompt but not enforced). After each batch, commit and verify externally. | Less context per coder session = higher quality |
| **Consider a different model** | GPT-5.3-Codex showed repeated reliability issues. Test with Claude Sonnet 4.6 or another strong coding model. Track pass-rate per model over 3 runs. | The current model may not be best for this structured workflow |
| **Add a "red card" for Coder** | If Coder reports false positives twice, escalate to Architect for a plan revision rather than retrying. Currently the circuit breaker only exists for Reviewer. | Prevents infinite loops |

#### Priority 3: Improve Architect Plan Quality (High)

| Action | What | Why |
|---|---|---|
| **Add `## Payload Examples` section to plan template** | Architect must provide concrete JSON examples for request, success response, error response, and validation error response | Coder gets unambiguous contract specifications |
| **Add `## Logging Requirements` to each slice** | Each slice must specify what should be logged at INFO/WARN/ERROR level | Prevents the "zero logging" gap |
| **Add `## Validation Boundary Decision`** | Architect must explicitly state where each validation rule lives (DTO binding vs use case vs domain) and why | Prevents duplicate validation |
| **Change instruction from "short" to "precise"** | Replace "keep the plan short" with "keep the plan precise and unambiguous — concrete over brief" | Encourages specificity |
| **Add Sonnet 4.6 as fallback model** | Since Opus fails to load, add fallback: `model: Claude Opus 4.5 (copilot) | Claude Sonnet 4.6 (copilot)` | Ensures a strong reasoning model is always used |

#### Priority 4: Replace Sonar with Local Tooling (Medium)

| Action | What | Why |
|---|---|---|
| **Add `jacoco:check` to Maven** | Set 85% coverage threshold. `mvn verify` fails if coverage is below threshold. | Local, instant, no server dependency |
| **Add Checkstyle or PMD** | Configure rules that match your code-guidance (final vars, no ternary abuse, logging present, etc.) | Catches code style issues locally |
| **Make Sonar optional** | Sonar becomes a "nice to have" CI gate, not a blocker for agent workflow. Agents use local tools only. | Eliminates the biggest time sink |
| **Create `scripts/quality-check.sh`** | Runs: compile, test, jacoco check, optional checkstyle. Single script for agents to call. | One command, clear pass/fail |

#### Priority 5: Streamline the Flow (Medium)

| Action | What | Why |
|---|---|---|
| **Merge PM into Team Lead** | Team Lead extracts the story from the prompt using the story template. One less agent invocation. | Saves premium requests, reduces context |
| **Protect `.github/agentic-flow/logs/`** | Add instruction: "Never read or modify files under `.github/agentic-flow/logs/`" | You observed Team Lead tried to modify this file |
| **Add git rules** | Add `.github/instructions/git-rules.instructions.md` with: branch naming, commit message format, when to commit (after each green slice) | Clean version control |
| **Require explicit file list in Coder handoff** | Coder must list every file created/modified with line count. Team Lead can verify at a glance. | Quick sanity check |
| **Add testing-practices.instructions.md** | Path-scoped to `**/src/test/**`. Define naming, structure, what each test level must prove, mandatory `@DisplayName`. | Addresses your observation about vague test structure |

---

### 7. Specific Code Fixes Needed (Post-Run)

These are issues the agents should have caught but didn't:

| # | File | Issue | Fix |
|---|---|---|---|
| 1 | All production classes | Zero logging | Add `@Slf4j` and strategic log statements |
| 2 | `IssuesController` | Repeated ternary null checks | Extract to `defaultIfNull()` or `RequestBody.empty()` |
| 3 | `GitLabProjectCoordinates` | Drops port from URL | Use `uri.getAuthority()` instead of `uri.getHost()` |
| 4 | `ListIssuesUseCase` | Duplicate label validation (also in DTO) | Pick one location, remove the other |
| 5 | `GlobalExceptionHandlerTest` | Only 2/4 handlers tested | Add tests for `MethodArgumentNotValidException` and `Exception` handlers |
| 6 | `ListIssuesUseCaseTest` | Only 3 tests, missing edge cases | Add negative page, blank assignee, blank label element tests |
| 7 | `GitLabIssuesClient` | Dead fallback URL | Remove hardcoded fallback or make it match parsed config |

---

### 8. v2.0.0 vs v1.0.1 Comparison

| Dimension | v1.0.1 | v2.0.0 | Verdict |
|---|---|---|---|
| Code runs without manual fixes | NO | YES | **Major improvement** |
| API works correctly | NO (config bugs) | YES | **Major improvement** |
| Architecture respected | Partial (projectId leaked) | YES (clean layers) | **Improvement** |
| Requirement fidelity | Poor (missing filters, wrong endpoint) | Good (filters work, correct endpoint) | **Major improvement** |
| Test depth | Weak (no integration tests) | Good (4 test levels) | **Improvement** |
| `final` usage | Inconsistent | Consistent | **Improvement** |
| Logging | Not assessed | ZERO | **Gap** |
| Process efficiency | ~1 hour (simpler flow) | 2+ hours (complex flow) | **Regression** |
| Reviewer gate | None existed | Corrupted (context rot) | **Attempted but failed** |
| Overall code quality | Needed manual fixes | Production-acceptable with gaps | **Net improvement** |

**Bottom line:** v2.0.0 code quality is genuinely better. The architectural governance, requirement fidelity, and "it just works" factor improved significantly. But the process cost doubled, and the Reviewer gate — the key v2.0.0 addition — failed due to context rot, which is an infrastructure problem, not a design problem.

---

### 9. Recommended Next Steps (Prioritized)

1. **Immediately:** Fix the 7 code issues in Section 7 (especially logging)
2. **Before next run:** Implement Priority 1 (context rot mitigations) and Priority 4 (local quality tooling)
3. **For next run:** Implement Priority 2 (coder reliability) and Priority 3 (architect detail) 
4. **Experiment:** Run the same user story with the slimmed flow and compare time/quality/premium-request count
5. **Track metrics per run:** Time to completion, agent loops, premium requests used, manual fixes needed, test count, coverage %. This will show whether changes actually help.

---

### 10. Context Budget Estimate (Tokens)

Rough token estimates for a single run under current setup:

| Source | Estimated Tokens | Notes |
|---|---|---|
| Agent prompts (5 agents) | ~12,000 | Loaded per invocation |
| copilot-instructions.md | ~3,000 | Always loaded |
| constitution.md | ~4,000 | Read by 3+ agents |
| code-guidance.md | ~2,500 | Read by 3+ agents |
| Story artifact | ~3,000 | Read by 3+ agents |
| Plan artifact | ~5,000 | Read by 3+ agents |
| Terminal outputs (4+ coder loops) | ~15,000-30,000 | This is the killer |
| Generated artifacts | ~5,000 | Review report, signoff, etc |
| **Total per run** | **~50,000-65,000** | |

A typical VS Code Copilot context window is ~128K-200K tokens. With 50-65K tokens of accumulated context, you're hitting compaction around the 3rd-4th coder loop — exactly where things fell apart.

**The math confirms: context rot is not accidental. It's inevitable with this flow structure.**

The fix is not "bigger context window" — it's "less context per stage, fresh invocations for heavy agents."
