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
