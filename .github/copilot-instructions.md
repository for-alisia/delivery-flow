# GitlabFlow Copilot Instructions

## Project Structure

- `flow-orchestrator/`: Java Spring Boot application — root packages: `common`, `config`, `orchestration`, `integration`
- `mcp-server/`: TypeScript MCP server — thin client of `flow-orchestrator`, no business logic

## Build And Verify

Run from `flow-orchestrator/`:
- Compile: `mvn -q -DskipTests compile`
- Test: `mvn test`
- Quick verify: `scripts/verify-quick.sh`
- Final verify: `scripts/final-check.sh`
- Karate smoke: `scripts/karate-test.sh`
- Start: `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`

When recording verification evidence, prefer shared scripts over raw Maven commands.

For local Karate smoke verification, prefer `scripts/karate-test.sh` over a separate manual startup flow. The script reuses a healthy local app when available or starts it automatically.

After modifying code or tests in `flow-orchestrator/`: follow [local-quality-rules.instructions.md](instructions/local-quality-rules.instructions.md) for verification commands, execution order, and evidence recording.

## Key References

Read on demand when relevant; do not load all upfront.

- Capability map and package index: [context-map.md](../documentation/context-map.md)
- Architecture rules and boundaries: [constitution.md](../documentation/constitution.md)
- Coding standards, quality gates, testing matrix: [code-guidance.md](../documentation/code-guidance.md)
- Project context and business goals: [project-overview.md](../documentation/project-overview.md)
- GitLab REST API index: `https://docs.gitlab.com/api/api_resources/`
- GitLab REST API overview: `https://docs.gitlab.com/api/rest/`
- GitLab REST API auth: `https://docs.gitlab.com/api/rest/authentication/`

## GitLab API Verification

- Verify endpoint patterns, parameters, pagination, and authentication against official GitLab docs before adding or changing Feign client methods
- Prefer the resource index first, then open the relevant resource page for exact request and response details
- Do not maintain a repo-owned GitLab endpoint catalog or summary mirror
- Keep integration code, `README.md`, and `.http` examples aligned with the official docs used for the change
