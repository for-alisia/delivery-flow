# User Story: GitLab Issues Retrieval API

**Feature name:** `first-issues-api`
**Story date:** `2026-03-29`

## Business Goal

Demonstrate early product direction by delivering a working API that retrieves issues from the configured GitLab project. Enterprise client teams need a simple, reliable way to fetch their issue data through GitlabFlow so they can validate the platform's capability and build confidence in the product direction before more complex orchestration features are introduced.

## Problem Statement

Enterprise client teams currently lack a consistent API to retrieve their GitLab issues through GitlabFlow. Without this capability, clients cannot begin using the platform or evaluating whether GitlabFlow meets their delivery visibility needs. This is blocking early adoption and product validation.

## User Story

As an enterprise client team member,
I want to call a GitlabFlow API to retrieve issues from our configured GitLab project with optional filtering and pagination controls,
so that I can immediately start using GitlabFlow for basic delivery visibility and confirm the platform is moving in the right direction.

## Locked Request Constraints

These are the non-negotiable constraints from the original request that must be preserved exactly.

- **Source-of-truth integration constraint**: The GitLab project is fixed by application configuration (`application-local.yml`) and treated as the baseline project for this API and all future APIs. Clients must not supply project id or path on each API request. The configured project is the single source of truth.
- **HTTP method contract**: The API must use HTTP POST to receive filter and pagination input. This preserves payload-based client usage and avoids GET-with-body interoperability issues across HTTP libraries, proxies, and servers. The operation remains logically read-only and idempotent even though POST is used for transport.
- **Input contract constraint**: Users prefer JSON payload input over query parameters. Request input must be optional — when no payload is provided, the API must apply sensible defaults. Clients provide input in a client-friendly JSON structure and GitlabFlow maps the input internally to provider-specific formats.
- **Filtering constraint**: The API must support optional filtering by labels and assignee. Both filters are optional and may be used independently or combined.
  - **Labels input**: Clients provide a `labels` array. For the MVP, only a single label value is allowed. If more than one label is provided, the API must reject the request with a validation error.
  - **Assignee input**: Clients provide a single `assignee` string value.
- **Pagination constraint**: Users must be able to control the number of items returned per request and select which page to retrieve via optional `page` and page size fields in the JSON payload. Both pagination fields are optional and the API must work with defaults when omitted.
  - **Default pagination values**: When pagination fields are omitted, `page` defaults to `1` and page size defaults to `40`.
  - **Configuration constraint**: The default page size value must be loaded from application configuration, not hardcoded in a class.
  - GitLab project issues list endpoint supports standard pagination via `page` and `per_page`. This has been verified in `artifacts/reference-docs/gitLabAPI.md` and confirmed via live access check (HTTP 200 response received from configured project issues endpoint).
- **Client-friendly contract principle**: GitlabFlow must not mirror GitLab blindly. The API accepts a client-friendly payload structure. GitLab-specific parameter formats, naming conventions, and data shapes are integration concerns. The integration layer maps between GitlabFlow's client contract and GitLab's provider contract internally.
- **GitLab endpoint assumption confirmed**: The GitLab project-scoped `Issues` resource at endpoint pattern `/projects/:id/issues` is the intended MVP integration starting point. This resource is documented in `artifacts/reference-docs/gitLabAPI.md` and marked as `Used in FLOW: TRUE`.
- **Architecture constraint**: The implementation must preserve the constitution boundaries described in `artifacts/constitution.md`. Even though this MVP does not require domain-specific business logic, the structure must remain maintainable, with explicit error handling, input validation, externalized configuration, safe logging, boundary mapping, and strong test coverage.
- **Quality constraint**: The implementation must include unit tests and higher-level integration tests as part of implementation readiness.
- **Documentation constraint**: The implementation must include API usage documentation and must keep `artifacts/reference-docs/gitLabAPI.md` synchronized with any GitLab integration endpoints that are added or activated.
- **Security constraint**: Secrets, tokens, or any sensitive configuration must remain externalized and must never appear in logs, reports, or any artifact.

### Unresolved Items Requiring Clarification

The following details were not specified in the original request and must be resolved during architecture or implementation planning:

- **Exact response field set**: It is unclear whether the MVP should return a full mapped issue summary with all fields received from GitLab, or a narrower product-specific shape that includes only the fields relevant to initial delivery visibility.
- **Error response format**: It is unclear whether the client expects a specific error response format (for example, RFC 7807 Problem Details, custom JSON structure, or standard HTTP status codes only).

## Business Context and Constraints

- **Primary users or stakeholders**: Enterprise delivery managers, engineering managers, and Agile teams who need reliable delivery-flow visibility.
- **Important business rules or terminology**: The configured GitLab project serves as the baseline source of truth. GitlabFlow is a SaaS platform that must work immediately without per-request project configuration. "Issue" means a GitLab issue in the MVP scope.
- **Business-facing performance expectations**: The API must respond within a reasonable time for typical issue-list requests (dozens to hundreds of issues per page). Exact SLAs are not defined for the MVP but responses must not block or hang indefinitely.
- **Business-facing security or privacy expectations**: Token and credential values must remain externalized and must never appear in logs or error messages. All errors returned to clients must be sanitized and must not expose internal implementation details.

## Scope

### In Scope

- Expose a single API endpoint that retrieves issues from the configured GitLab project.
- Accept an optional JSON payload for filtering (labels, assignee) and pagination control (page size, page number).
- Apply sensible defaults when no payload is provided.
- Support optional filtering by labels and assignee, either independently or combined.
- Map and return issue data retrieved from GitLab to the API consumer.
- Handle GitLab integration errors gracefully and return sanitized error responses.
- Validate input at the system boundary before passing it to internal layers.
- Externalize GitLab project configuration and token values.
- Provide unit tests and integration tests covering success, error, validation, and boundary cases.
- Update `artifacts/reference-docs/gitLabAPI.md` with the GitLab project issues endpoint details if not already complete.
- Provide API usage documentation so consumers understand how to call the endpoint and what response to expect.

### Out of Scope

- Support for multiple GitLab projects or project-switching per request. The configured project is the only supported source.
- Support for issue creation, update, deletion, or any write operation. This story is read-only retrieval.
- Support for filtering by milestone, state, or any other attribute beyond labels and assignee.
- Support for advanced search, sorting, or ordering beyond what GitLab returns by default.
- Domain-specific business logic such as aging calculations, bottleneck detection, or flow metrics. This story establishes the integration foundation only.
- Support for GitLab merge requests, epics, milestones, or any other resource type. Issues are the only resource in scope.
- Authentication or authorization for GitlabFlow consumers. This is assumed to be handled separately.
- Support for keyset-based pagination. Only offset-based pagination (page/per_page) is in scope.

## Acceptance Criteria

1. **Given** the configured GitLab project contains issues, **when** a client sends an HTTP POST request to the issues retrieval API without a request payload, **then** the API returns a successful response containing a list of issues retrieved from the configured project with default pagination applied (page 1, 40 items per page) and no filtering applied.

2. **Given** the configured GitLab project contains issues with various labels, **when** a client sends an HTTP POST request with a JSON payload specifying a single label in the `labels` array, **then** the API returns a successful response containing only issues that match the specified label.

3. **Given** a client provides a JSON payload with more than one label in the `labels` array, **when** the client sends an HTTP POST request to the issues retrieval API, **then** the API returns a validation error response indicating that only a single label is allowed in the MVP.

4. **Given** the configured GitLab project contains issues with various assignees, **when** a client sends an HTTP POST request with a JSON payload specifying an `assignee` string value, **then** the API returns a successful response containing only issues assigned to the specified assignee.

5. **Given** the configured GitLab project contains issues, **when** a client sends an HTTP POST request with a JSON payload specifying both a single label in the `labels` array and an `assignee` string, **then** the API returns a successful response containing only issues that match both filter criteria.

6. **Given** the configured GitLab project contains issues, **when** a client sends an HTTP POST request with a valid JSON payload specifying pagination controls (page size and page number) with or without filters, **then** the API returns a successful response containing the requested page of issues with the specified page size.

7. **Given** a client provides an invalid input value (for example, negative page number, excessively large page size, multiple labels, or malformed filter value), **when** the client sends an HTTP POST request to the issues retrieval API, **then** the API returns a validation error response that clearly indicates the problem without exposing internal implementation details.

8. **Given** the GitLab integration fails (for example, due to network issues, authentication errors, or unavailable service), **when** a client sends an HTTP POST request to the issues retrieval API, **then** the API returns an error response that indicates the operation failed without exposing token values, internal stack traces, or GitLab-specific implementation details.

9. **Given** the GitLab integration succeeds but returns an empty list of issues (either because no issues exist or because the applied filters match no issues), **when** a client sends an HTTP POST request to the issues retrieval API, **then** the API returns a successful response with an empty or clearly empty result set.

10. **Given** implementation is complete, **when** reviewing the codebase, **then** the integration layer contains a GitLab-specific adapter that maps between the client-friendly GitlabFlow contract and GitLab's provider-specific parameter formats and data shapes, the orchestration layer coordinates the use case flow and applies the default page size value loaded from configuration when pagination fields are omitted, explicit mapping exists between GitLab response data and the API response model, and no GitLab-specific transport details appear outside the integration layer.

11. **Given** implementation is complete, **when** reviewing test coverage, **then** unit tests exist for orchestration logic, mapping logic, validation rules including label-array-max-1 validation, and default pagination behavior, and integration tests exist that verify the full POST request-to-response flow using mocked GitLab responses including filtering and pagination scenarios.

12. **Given** implementation is complete, **when** reviewing documentation, **then** `artifacts/reference-docs/gitLabAPI.md` accurately reflects the GitLab project issues endpoint as used by FLOW, and API usage documentation exists that describes the POST endpoint, the expected payload format including `labels` array (max 1 value), `assignee` string, and optional pagination fields, the response format, and example calls.

## Dependencies and Assumptions

- **External dependencies**:
  - GitLab project URL and access token are correctly configured in `application-local.yml`.
  - The configured GitLab project is accessible and the token has read permissions for project issues.
  - GitLab API availability and standard list pagination behavior remain stable.
- **Assumptions**:
  - The GitLab project issues endpoint pattern `/projects/:id/issues` is the correct resource for MVP integration (verified assumption).
  - GitLab list endpoints support `page` and `per_page` query parameters for offset pagination (verified assumption).
  - The Spring Boot application can successfully authenticate to GitLab using the configured token (confirmed via live access check).
  - API consumers will use HTTP and JSON to call the GitlabFlow API.
  - The application will run in a single-tenant mode with one configured GitLab project per deployment for the MVP. Multi-tenancy is not an MVP requirement.

## Open Questions

- Should the MVP response include all fields received from GitLab, or only a subset of fields relevant to delivery visibility?
- Are there specific error response format requirements for the client (for example, RFC 7807 Problem Details, custom JSON structure, or standard HTTP status codes only)?
