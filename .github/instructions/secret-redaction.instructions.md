---
applyTo: "**"
---

## Secret Redaction Rules

- Never write token values, `.env.local` contents, or expanded secret-bearing commands into any artifact, report, or log
- Record helper-script invocations or redacted raw commands only
- Tokens, passwords, and sensitive payloads must never appear in implementation reports, review reports, sign-off artifacts, or verification logs
- When recording Sonar commands, use the helper script path or redact the raw command
