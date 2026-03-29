# GitlabFlow

This repository is split into two parts:

- `flow-orchestrator/` - Spring Boot starter application
- `mcp-server/` - TypeScript MCP server

## Run

Spring Boot:

```bash
cd flow-orchestrator
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
mvn spring-boot:run
```

If you run Spring Boot outside VS Code, activate the local profile explicitly:

```bash
cd flow-orchestrator
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

MCP server:

```bash
cd mcp-server
npm install
npm run build
npm start
```

## Issues API (MVP)

`flow-orchestrator` exposes a read-only issues endpoint via `POST /api/issues`.

- Request body is optional.
- Baseline project comes from server configuration (`app.gitlab.url`), not from the client.
- Optional filters:
	- `labels`: array with at most one value
	- `assignee`: single username string
	- `page`: defaults to `1`
	- `pageSize`: defaults to configured `app.issues-api.default-page-size` (expected `40`)

Request examples:

```json
{}
```

```json
{
	"labels": ["backend"],
	"assignee": "alice",
	"page": 2,
	"pageSize": 20
}
```

Response shape:

```json
{
	"issues": [
		{
			"issueNumber": 99,
			"title": "Mapped issue",
			"state": "opened",
			"labels": ["backend"],
			"assignees": [
				{
					"username": "alice",
					"name": "Alice",
					"webUrl": "https://gitlab.com/alice"
				}
			],
			"webUrl": "https://gitlab.com/group-a/project-a/-/issues/99",
			"createdAt": "2026-03-29T10:00:00Z",
			"updatedAt": "2026-03-29T12:00:00Z"
		}
	],
	"page": 2,
	"pageSize": 20
}
```
