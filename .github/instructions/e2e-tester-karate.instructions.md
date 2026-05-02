---
applyTo: "**"
description: "Repeatable Karate and E2E scenario ownership rules for the E2E Tester workflow role."
---

## E2E Tester Rules

- This role is used only when Team Lead records `set-e2e-mode --mode SCENARIOS_REQUIRED` for the feature.
- The approved E2E scenario artifact lives at `artifacts/e2e-scenarios/<feature-name>.e2e.md`.
- Use [artifacts/templates/e2e-scenarios.example.md](../../artifacts/templates/e2e-scenarios.example.md) as the structure reference.
- Scenario IDs must stay stable after approval. Use simple IDs such as `E2E-1`, `E2E-2`, `E2E-3`.
- The scenario artifact is approval-tracked through `flow-log register-artifact e2e` and `approve-artifact e2e`. If you change it after approval, tell Team Lead to re-approve it.
- The scenario-design pass defines coverage and repeatability rules. It does not write Java code, Karate features, or plan content.
- The smoke pass owns Karate `.feature` files under `flow-orchestrator/src/test/karate/resources/<capability>/` and runner updates under `flow-orchestrator/src/test/karate/java/`.
- Put stable scenario IDs in Karate scenario titles or tags so failures map back to the approved scenario artifact.
- Tag smoke scenarios with `@smoke`.
- For write-path scenarios, prefer unique generated values. If the API supports cleanup, clean up explicitly. If cleanup is unavailable, design the assertion so repeated runs remain valid.
- Never rely on fixed mutable resource names for create/update smoke scenarios.
- Use `scripts/flow-log.sh run-check --name karate` to execute smoke coverage. Do not run `scripts/karate-test.sh` directly during the agent workflow.
- If `karate` fails or blocks, inspect `scripts/flow-log.sh check-log --feature <feature-name> --name karate --lines 80` before classifying the issue as implementation, provider-state, or environment related.
- If provider state makes a scenario non-repeatable, report the exact scenario ID, the conflicting resource pattern, and the required uniqueness or cleanup fix.
- Do not change production code. Smoke failures caused by implementation defects go back to Team Lead and then to Java Coder.