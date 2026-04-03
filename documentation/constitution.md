# GitlabFlow Project Constitution

This document defines the non-negotiable architectural rules every contributor must follow. These are not suggestions — they are the load-bearing walls of this project. Breaking them compromises correctness, security, maintainability, or the ability to grow the project like lego blocks.

These rules are intentionally technology-agnostic. They survive framework upgrades, language version bumps, and tool replacements. Technology-specific conventions (which libraries to use, which annotations to apply, which test utilities to reach for) belong in the contributor guidelines (`copilot-instructions.md`), not here.

Rules labeled **[MUST]** are never breakable. Rules labeled **[SHOULD]** are strong architectural defaults with rare exceptions that require explicit justification in a code review.

---

## Principle 1: Layered Architecture with Hard Dependency Boundaries

The project is organized into four layers: `orchestration`, `domain`, `integration`, and `config`. The dependency arrow always points inward. No exceptions.

- **[MUST]** `domain` must not depend on `orchestration`, `integration`, or any external system, framework, or transport concern.
- **[MUST]** `integration` must not depend on `domain`.
- **[MUST]** `orchestration` is the only layer permitted to coordinate between `domain` and `integration`.
- **[MUST]** `config` is responsible for wiring components. It must not contain business logic.
- **[MUST]** Entry points in `orchestration` must stay thin: validate input, delegate to a use-case or service, map the result, return the response — nothing more.
- **[MUST]** Any violation of a dependency boundary must be corrected before the change is merged, regardless of how small or how "temporary" the violation appears.

---

## Principle 2: Ports and Adapters — Integration Must Stay Isolated

All provider-specific transport mechanics, protocol details, and external API shapes must stay inside the integration layer. Nothing from that layer leaks outward.

- **[MUST]** Provider-specific client configuration, authentication details, transport headers, URL construction, and pagination mechanics must never appear outside the integration layer.
- **[MUST]** Orchestration defines provider-agnostic ports (interfaces) and request/response contracts. Integration implements those ports.
- **[MUST]** Integration adapters must map provider-specific responses to orchestration contracts before returning data. Raw provider data shapes must not cross the integration boundary.
- **[SHOULD]** When adding a new integration endpoint, define the provider-agnostic request/response model in `orchestration` first, then implement the adapter in the integration layer.

---

## Principle 3: Domain Purity

The domain layer is the most important and most protected layer. It must contain only business meaning.

- **[MUST]** Domain classes must not depend on any framework, dependency injection container, HTTP library, serialization library, or external system concern.
- **[MUST]** Domain services must operate only on domain models and must return domain models.
- **[MUST]** Domain models must not carry transport concerns: no protocol status codes, no provider-specific field names, no serialization annotations, no pagination metadata.
- **[MUST]** If a concept has business meaning, it belongs in `domain`. If it represents transport, presentation, or external API mechanics, it does not.
- **[MUST]** Domain logic must be fully testable without starting any container, server, or runtime environment.

---

## Principle 4: Security Is a First-Class Concern

This project targets large enterprise environments. Security must be considered at every design and implementation decision, not patched in afterward.

- **[MUST]** Secrets, tokens, passwords, or credentials must never be hardcoded in source code or committed to version control.
- **[MUST]** Sensitive configuration values must be loaded from externalized configuration (environment variables or a secrets manager). They must never be embedded in application source.
- **[MUST]** Secrets, tokens, or any sensitive payload must never appear in log output at any log level.
- **[MUST]** All external input received at system boundaries must be validated before it reaches business logic. Never pass raw, unvalidated input to the domain or integration layers.
- **[MUST]** Outbound responses must never expose internal implementation details, stack traces, internal class names, or provider-specific error messages. All exceptions must be mapped to sanitized output at the system boundary.
- **[MUST]** Authentication and authorization must be configured explicitly at the system boundary. Security must never rely on permissive defaults — deny-by-default is the required baseline.
- **[MUST]** Communication with external systems must use encrypted transport in all non-local environments.
- **[SHOULD]** Prefer short-lived, scoped credentials over long-lived tokens wherever the external provider supports it.
- **[SHOULD]** Operations that write data or perform access-sensitive actions must produce an audit log entry that records the actor and the action, without logging the payload.

---

## Principle 5: Externalized and Validated Configuration

- **[MUST]** All environment-specific configuration must be externalized. It must not be hardcoded in application source or committed to version control with real values.
- **[MUST]** Configuration must be centralized in dedicated configuration objects, not scattered through raw property lookups in business code.
- **[MUST]** Configuration objects must be immutable. Configuration must not be mutated at runtime.
- **[MUST]** Required configuration values must be validated at startup. The application must fail fast with a clear error message when mandatory configuration is missing or invalid.
- **[MUST]** Configuration components belong in the `config` layer. They must not leak into `domain` or `integration` package internals.

---

## Principle 6: Prefer Immutability

Mutable shared state is a source of bugs, concurrency issues, and unpredictable behavior.

- **[MUST]** Data models crossing layer boundaries must be immutable. A layer receiving a model must not be able to mutate the object it received.
- **[MUST]** Dependencies must be injected through constructors. Ambient service locators, static lookups, or any mechanism that hides a dependency are not acceptable.
- **[SHOULD]** Prefer immutable data representations at all layers. Mutable representations must be justified by a documented reason.

---

## Principle 7: Error Handling and Exception Design

- **[MUST]** Exceptions crossing a layer boundary must be typed and carry a semantic error category. Never propagate raw runtime exceptions or framework-internal exceptions across architectural boundaries.
- **[MUST]** There must be a single, centralized point where exceptions are translated into outbound responses. Entry points must not independently catch and re-shape exceptions without a specific, documented reason.
- **[MUST]** Semantic error categories must represent business-meaningful failure types, not protocol or transport codes. Protocol-level mapping (e.g., to HTTP status codes) belongs at the system boundary, not in business logic.
- **[MUST]** Failures originating from external system calls must use a distinct exception type from domain validation failures. These are different categories of failure and must not be conflated.

---

## Principle 8: Explicit Mapping at Every Layer Boundary

Data must be explicitly transformed as it crosses each layer boundary. Implicit reuse of a model from the wrong layer is an architectural violation.

- **[MUST]** Integration-layer data shapes must never enter orchestration or domain logic directly. Map them to orchestration contracts at the integration boundary.
- **[MUST]** Orchestration request models must never be passed directly into a domain service. Map them to domain models in the orchestration layer.
- **[MUST]** Domain result models must never be passed directly out of entry points. Map them to response or presentation models in the orchestration layer.
- **[MUST]** Mapping responsibility belongs to the layer that owns the transformation. It must not be scattered or duplicated across layers.
- **[SHOULD]** Mapping logic must be kept separate from business logic. A single method must not simultaneously perform business decisions and data transformation.

---

## Principle 9: Structured and Safe Logging

- **[MUST]** All logging must go through a structured logging framework. Never write diagnostic output directly to standard output in production code.
- **[MUST]** Secrets, tokens, passwords, and sensitive payloads must never appear in any log entry at any log level.
- **[MUST]** Logs from the integration layer must include sufficient context to diagnose communication failures (e.g., which resource, which provider, which status) without exposing sensitive content.
- **[SHOULD]** Use parameterized log messages. Do not construct log strings through string concatenation.
- **[SHOULD]** Orchestration layer logs should record meaningful orchestration decisions and outcomes, not low-level mechanics.

---

## Principle 10: Testing Philosophy

Testing is not optional. All non-trivial behavior must be covered.

- **[MUST]** Tests must follow a consistent structure: establish context, execute the behavior under test, assert the outcome.
- **[MUST]** Each test must verify exactly one behavior. Multiple assertions are acceptable only when they all verify the same single logical outcome.
- **[MUST]** Tests must mock at layer boundaries — the collaborator at the boundary, not the internals of the class under test.
- **[MUST]** Domain logic tests must not require starting a container, server, or external runtime. Domain must be fully testable in isolation.
- **[MUST]** Integration adapter tests must use mocked external calls. Tests that communicate with real external systems are a separate, clearly labeled category.
- **[MUST]** Every mapping path in the integration layer must have test coverage.
- **[SHOULD]** Prefer test-local data over shared fixtures when it makes each test easier to understand in isolation.

---

## Principle 11: Integration Documentation Must Stay in Sync

Every integration with an external API must maintain a documentation artifact that stays current with the implementation.

- **[MUST]** Any change that adds, modifies, or removes an external API integration must update the corresponding documentation artifact in the same change set.
- **[MUST]** The documentation must reflect which external resources are actively used by this project.
- **[MUST]** If a required external resource is missing from the documentation, it must be added as part of the implementing change.
- **[SHOULD]** Before adding a new integration endpoint, confirm the resource and endpoint pattern in the documentation. Do not guess external API paths.

---

## Principle 12: Consumers of the Application Must Stay Thin

Any consumer of the application — an MCP server, a UI client, a CLI tool, or a similar entry point — is not the source of business truth and must not behave like one.

- **[MUST]** Consumer-layer handlers must delegate business behavior, orchestration decisions, and workflow rules to the application. They must not re-implement them.
- **[MUST]** All input arriving at a consumer-layer boundary must be validated before it reaches the application.
- **[MUST]** Provider-specific logic must not appear in consumer-layer code.
- **[SHOULD]** Each consumer-layer entry point must serve a single purpose: validate input, invoke one application capability, return the result.

---

## Principle 13: Modularity and Expansion Readiness

This design must support future integrations (e.g., GitHub, Jira, Azure DevOps) as lego-block extensions without restructuring the core.

- **[MUST]** Orchestration ports must be defined in terms of business intent, not provider mechanics. Port names and method signatures must make sense if the current provider adapter is replaced by a different one tomorrow.
- **[MUST]** The `domain` and `orchestration` layers must have zero direct dependency on any specific integration provider.
- **[MUST]** A new integration provider must be addable by creating a new adapter in the integration layer — with no changes to domain or orchestration code.

---

## Principle 14: Naming and Intent Clarity

- **[MUST]** Class, method, and variable names must express business intent, not technical or framework mechanics.
- **[MUST]** Role-based naming suffixes must reflect the actual responsibility of the component. Use them only when the responsibility is genuine: a port defines a contract, an adapter implements it for a specific provider, a service encapsulates a use-case or domain behavior, a mapper transforms data between representations.
- **[MUST]** Names used for transport, serialization, or external communication must be clearly distinguishable from domain model names.
- **[SHOULD]** Avoid abbreviations in production code except in genuinely narrow scopes.
- **[SHOULD]** Method names should read as business-intent statements, not as descriptions of technical operations.

---

## Principle 15: Code Documentation for Tricky Parts

## Principle 15: Document Why, Not What

Code must explain itself through clear naming and structure. Comments exist for decisions the code cannot communicate on its own.

- **[MUST]** Any non-obvious workaround, external API quirk, or intentional deviation from the standard pattern must have an inline comment explaining the reason.
- **[MUST]** Public contracts on port interfaces must document the business contract — what the caller can expect, not how the implementation works.
- **[SHOULD]** Each top-level layer must declare its responsibility and its permitted dependencies in a short description.
- **[SHOULD]** Any configuration component must document which external system it configures and where to obtain the required values.

