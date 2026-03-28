# Code Guidance

This checklist defines the default coding expectations for `flow-orchestrator`.
Validate each change against these guidelines after implementation. Do not pass a change further if any of these criteria are not met. If a criterion cannot be met due to architectural constraints, raise it as a blocker.

## Java Code Checklist

- Local variables and fields are `final` unless mutation is required.
- Boundary models are immutable.
- Records are used for simple immutable data carriers when they improve clarity.
- Dependencies use constructor injection.
- Methods are small and single-purpose.
- Names express business intent.
- Validation happens early at the correct boundary.
- Logging is structured with SLF4J and includes relevant context for efficient debugging without exposing sensitive information.
- Lombok is used only when it reduces boilerplate without hiding important behavior.
- `@RequiredArgsConstructor` is preferred for constructor injection when Lombok is used.
- `@Slf4j` is used for structured logging when Lombok is used.
- `@Builder` is used only when it improves readability for object construction.
- Prefer builder-style object construction over direct `new Class(...)` calls when creating non-trivial objects, especially when there are multiple arguments, optional fields, or a risk of argument-order mistakes.

## Collections And Streams

- Streams are used for clear transformations.
- Streams are not used when a loop is simpler and clearer.
- Stream pipelines have no hidden side effects.
- Collections are returned empty, not `null`.
- Ordering is explicit when it matters.
- Public methods that can return no value are annotated with `@Nullable` and return `null` explicitly instead of `Optional`.
- `Optional` is used only inside method bodies when it makes nested null-safe access or transformation clearer than chained explicit `null` checks.

## Testing Levels

- All added logic is covered by unit tests in isolation unless it belongs more appropriately to web, adapter, or integration-level verification.
- Web/controller tests are added for new or changed request binding, validation, response mapping, or HTTP behavior.
- Integration tests are added for new or changed interactions between components, especially at layer boundaries.
- Integration adapter tests cover external API interaction, mapping, and error handling where applicable.

## Test Checklist

- Each test verifies one behavior.
- All test names are descriptive and contain `@DisplayName`.
- Tests cover happy path, edge cases, and failure paths.
- Mocks are used only for true collaborators at the layer boundary.
- Real mappers and value objects are used when simpler than mocks.

## Final Verification

- All previous criteria are met.
- All acceptance criteria from the implementation plan are satisfied.
- `mvn test` passes with all tests green.
- `mvn -q -DskipTests compile` passes with no errors.
- `mvn spring-boot:run` starts without errors and logs expected startup messages.
- If an API is added or changed, the corresponding `<api-name>.http` file is created or updated in `/flow-orchestrator/http`.
- API-facing changes are smoke-tested with `curl` by verifying the API returns expected responses for valid and invalid requests.
- If no API contract was changed directly, the existing API endpoints most likely affected by the change were identified and verified with `curl`, with commands and observed responses recorded.
