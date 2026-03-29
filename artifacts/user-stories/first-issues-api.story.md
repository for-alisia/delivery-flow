# User Story: First Issues API

**Feature name:** `first-issues-api`
**Story date:** 2026-03-28

## Business Goal

Demonstrate early product value by enabling clients to retrieve work items through the flow-orchestrator API. This establishes the foundation for future flow visibility and bottleneck analysis capabilities while proving the orchestration approach works with real integration data.

## Problem Statement

Enterprise delivery teams currently lack a structured way to programmatically access their work item data for flow analysis. Manual retrieval is slow, inconsistent, and does not support automation or integration with downstream tooling. Teams need a reliable API to retrieve work items so they can begin building visibility and decision support on top of flow-orchestrator.

## User Story

As a delivery manager or platform engineer,
I want to retrieve a list of work items from a project through the flow-orchestrator API,
so that I can begin integrating flow data into my team's delivery visibility and automation tools.

## Business Context and Constraints

- **Primary users or stakeholders:** Delivery managers, engineering managers, platform engineers, Agile team leads
- **Important business rules or terminology:**
  - Work item (MVP): GitLab issue
  - Project: the source project containing work items
  - Pagination: controlling how many items are returned per request
  - Optional request payload: clients may provide filtering or pagination parameters via JSON payload, but sensible defaults apply when omitted
- **Business-facing performance expectations:** Initial retrieval should complete within a reasonable timeout suitable for synchronous API consumption (typically under 10 seconds for standard project sizes)
- **Business-facing security or privacy expectations:** API must validate all inbound parameters, handle authentication securely, and return safe error messages that do not expose sensitive internal details or credentials

## Scope

### In Scope

- API endpoint that accepts a request to retrieve work items from a specified project
- Support for JSON payload with optional parameters for pagination control (items per page, page number)
- Sensible default behavior when payload is omitted or incomplete
- Retrieval of work items from GitLab project issues API for MVP
- Proper validation of inbound requests with clear, safe error responses
- Structured JSON response containing work item data
- Support for pagination of results when large result sets are returned
- Basic error handling for common failure cases (invalid project, authentication failure, integration unavailable)

### Out of Scope

- Domain-specific issue analysis, aging calculations, workflow state detection, or bottleneck identification
- Issue creation, update, or deletion capabilities
- Filtering by workflow state, assignee, labels, or other issue attributes beyond basic pagination
- Cross-project or multi-project retrieval in a single request
- Real-time streaming or webhook-based issue updates
- Issue enrichment with historical or calculated metadata

## Acceptance Criteria

1. **Given** a client sends a valid request for work items from a project **when** the request includes a JSON payload with pagination parameters **then** the API returns a paginated list of work items matching the specified page and page size.

2. **Given** a client sends a valid request for work items from a project **when** the request omits the JSON payload or pagination parameters **then** the API applies default pagination settings and returns the first page of work items.

3. **Given** a client sends a request with an invalid project identifier **when** the orchestrator attempts to retrieve work items **then** the API returns a clear error response indicating the project was not found, without exposing internal system details.

4. **Given** a client sends a request with invalid pagination parameters (e.g., negative page number, zero or negative page size) **when** the request is validated **then** the API returns a clear validation error response before attempting external retrieval.

5. **Given** the external integration (GitLab API) is unavailable or returns an error **when** the orchestrator attempts to retrieve work items **then** the API returns a safe error response indicating the retrieval failed, without exposing credentials or sensitive internal error details.

6. **Given** a successful work item retrieval **when** the response is returned to the client **then** the response includes work item data in a well-structured JSON format suitable for programmatic consumption, and includes pagination metadata if applicable.

## Dependencies and Assumptions

- **External dependencies:**
  - GitLab API project issues endpoint must be available and must support project-scoped issue retrieval with pagination parameters
  - Valid GitLab project access token with read permissions must be configured in the orchestrator
  - Network connectivity between orchestrator and GitLab API must be reliable
- **Assumptions:**
  - The GitLab project issues API supports standard pagination query parameters (page, per_page or equivalent)
  - Clients will consume the API synchronously and can handle paginated responses
  - All work items are publicly readable within the configured GitLab project for the authenticated user
  - Response payload size per page is reasonable and does not exceed typical API gateway or client timeout limits

## Open Questions

- Should the API support custom sorting of work items in the initial release, or is default GitLab sorting sufficient for MVP?
- What is the maximum acceptable page size clients should be allowed to request to prevent performance degradation?
- Should the API return partial results if some work items fail to retrieve, or fail the entire request?
