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
- Pagination defaults to `page=1` and `perPage=40` when omitted.
- Supported filters are `state`, `labels`, `assignee`, and `milestone`.
- `labels`, `assignee`, and `milestone` must each contain at most one value.

Request examples:

```json
{}
```

```json
{
	"pagination": {
		"page": 2,
		"perPage": 20
	},
	"filters": {
		"state": "opened",
		"labels": ["backend"],
		"assignee": ["alice"],
		"milestone": ["M1"]
	}
}
```

Response shape:

```json
{
	"items": [
		{
			"id": 99,
			"title": "Mapped issue",
			"description": "Issue description",
			"state": "opened",
			"labels": ["backend"],
			"assignee": "alice",
			"milestone": "M1",
			"parent": 42
		}
	],
	"count": 1,
	"page": 2
}
```

Validation error shape:

```json
{
	"code": "VALIDATION_ERROR",
	"message": "Request validation failed",
	"details": [
		"filters.labels must contain at most 1 value"
	]
}
```
