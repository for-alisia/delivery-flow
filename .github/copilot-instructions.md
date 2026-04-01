# GitlabFlow Copilot Instructions

## Project Structure

- `flow-orchestrator/`: Java Spring Boot application — layers: `orchestration`, `domain`, `integration`, `config`
- `mcp-server/`: TypeScript MCP server — thin client of `flow-orchestrator`, no business logic here

## Build And Verify Commands

- Compile: `mvn -q -DskipTests compile` (run from `flow-orchestrator/`)
- Test: `mvn test` (run from `flow-orchestrator/`)
- Quick verify: `scripts/verify-quick.sh`
- Full local quality gate: `scripts/quality-check.sh`
- Start: `mvn spring-boot:run` with `SPRING_PROFILES_ACTIVE=local` (run from `flow-orchestrator/`)
- Shared verification reference and report-path guide: [local-quality-flow-orchestrator.md](../artifacts/reference-docs/local-quality-flow-orchestrator.md)
- When recording verification evidence, prefer the shared scripts over raw Maven commands.

## Key References

Read these on demand when relevant — do not load all upfront:

- Architecture rules and non-negotiable boundaries: [constitution.md](../artifacts/constitution.md)
- Coding standards, quality gates, and testing matrix: [code-guidance.md](../artifacts/code-guidance.md)
- Shared local verification workflow and report paths: [local-quality-flow-orchestrator.md](../artifacts/reference-docs/local-quality-flow-orchestrator.md)
- Project context and business goals: [project-overview.md](../artifacts/project-overview.md)
- Official GitLab REST API resource index: `https://docs.gitlab.com/api/api_resources/`
- Official GitLab REST API overview: `https://docs.gitlab.com/api/rest/`
- Official GitLab REST API authentication: `https://docs.gitlab.com/api/rest/authentication/`

## GitLab API Verification Workflow

- Verify endpoint patterns, parameters, pagination, and authentication against the official GitLab docs before adding or changing GitLab Feign client methods
- Prefer the resource index first, then open the relevant resource page for exact request and response details
- Do not create or maintain a repo-owned GitLab endpoint catalog or summary mirror
- Keep integration code, `README.md`, and `.http` examples aligned with the official GitLab docs actually used for the change

## Shared Agent Workflow Rules

- Never read or modify files under `.github/agentic-flow/logs/`
- Subagents return structured summaries — not file contents or lengthy explanations
- All handoffs are artifact-backed on disk — do not advance on chat-only claims
- The artifact on disk is the source of truth, not the agent's chat summary
- Escalation flag: `<<ESCALATE_TO_ARCHITECT>>` when a fundamental plan flaw blocks progress
- If the same feature receives 2 `Java Coder` false-positive red cards, route the work back to `Java Architect` for plan revision instead of retrying the coder on the same plan
