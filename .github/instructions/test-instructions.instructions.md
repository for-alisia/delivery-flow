---
applyTo: "**/flow-orchestrator/**"
description: "Test placement, naming, levels, coverage, and quality rules for writing and updating tests in the flow-orchestrator module."
---

## Test Instructions

- All added or updated tests must follow this instruction. Deviations require explicit approval.
- Mirror the production package path under every test root.
- Keep fast isolated tests in `src/test/java` and name classes `*Test`.
- Use `src/test/java` for unit logic, validation, defaulting, mapping, error translation, adapter tests with mocked collaborators, and lightweight config/context tests.
- Keep architecture tests in `src/test/architecture/java` and name classes `*ArchitectureTest`.
- Use `src/test/architecture/java` for ArchUnit and similar structural rules that validate package boundaries, dependency rules, layering, and cycle constraints without starting Spring or a running application.
- The Architect defines new ArchUnit rules when a plan introduces new layer interactions or package boundaries not covered by existing rules. The Coder implements those rules alongside production code. Same ownership model as Karate tests.
- Do not remove or weaken existing ArchUnit rules without explicit approval. They enforce `constitution.md` boundaries.
- Keep Spring-backed integration tests in `src/test/integration/java` and name classes `*IT`.
- Use `src/test/integration/java` for `@WebMvcTest` and other Spring-wired slice or integration tests that prove request binding, validation, response mapping, HTTP errors, or cross-bean behavior.
- Keep running-application tests in `src/test/component/java` and name classes `*ComponentTest`.
- Use `src/test/component/java` when the application must run on a real port or when outbound HTTP must be verified through stubs such as WireMock.
- Keep component fixtures in `src/test/component/resources/stubs/...`.
- Do not configure reusable stub servers or shared stub scenarios inline in test classes; keep them in dedicated support classes.
- Keep Karate API smoke tests in `src/test/karate/resources/<capability>/` as `.feature` files and name runners `*KarateTest` in `src/test/karate/java/`.
- Use Karate tests for end-to-end API verification against a running application. Tag all smoke scenarios with `@smoke`.
- Karate tests are isolated from unit and quality-gate runs — they only execute via `scripts/karate-test.sh` or the `-Pkarate` Maven profile. Do not add Karate runner classes to surefire or default failsafe includes.
- For local smoke verification, prefer `scripts/karate-test.sh` over manual startup plus raw Maven. The script reuses a healthy local app when available or starts it automatically.
- The Architect writes Karate `.feature` files directly as part of the implementation plan. The Team Lead executes them. The Coder does NOT write or modify Karate tests.
- Prefer the smallest test level that proves the change.
- Do not duplicate the same scenario across unit, integration, and component tests unless each level proves something different.
- Every non-trivial logic, validation, defaulting, mapping, or error translation change requires unit coverage.
- Any changed request contract or HTTP validation/error behavior requires integration coverage.
- Any changed real HTTP flow to GitLab or another downstream requires component coverage.
- API or startup changes still require smoke verification, but smoke checks do not replace automated tests.
- Each test verifies one behavior.
- Every test uses a descriptive name and `@DisplayName`.
- Cover happy path, edge cases, and failure paths relevant to the change.
- Mock only true layer-boundary collaborators; prefer real value objects and mappers when simpler.
- When using Mockito argument matchers (`any()`, `eq()`, `argThat()`), ALL arguments in the same stubbing or verification call must use matchers. Do not mix raw values with matchers — wrap raw values with `eq()`.
- Do not duplicate string literals across test files. Extract repeated test constants (URLs, paths, fixture values) into `private static final` fields or a shared test-support class.
- Each plan slice must include at least one edge-case or boundary-condition test. If the slice has no edge cases, document why in the plan.
- For mappers that guard N fields with `requiredField()` or equivalent null-checks, tests must cover null input for each guarded field independently, not just one representative field.
- Required module line coverage is `85%+`; `mvn verify` must stay green.
