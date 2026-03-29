# Sonar Setup For `flow-orchestrator`

This document explains how Sonar is used for the `flow-orchestrator` module and how it aligns with `artifacts/code-guidance.md`.

## Scope

- This setup currently applies to `flow-orchestrator` only.
- The repository is configured for SonarQube Cloud at `https://sonarcloud.io`.
- The bound SonarQube Cloud organization is `for-alisia`.
- The bound project key is `com.gitlabflow:flow-orchestrator`.
- Sonar is treated as a required static-quality gate before Reviewer Phase 2 can pass.
- Sonar does not replace Reviewer or Team Lead artifact checks.

## Build Integration

The Maven build now provides:

- JaCoCo coverage reporting in `target/site/jacoco/jacoco.xml`
- Sonar-compatible test report paths from `target/surefire-reports`
- a pinned Sonar Maven scanner plugin version in `flow-orchestrator/pom.xml`

## Local Configuration

The local workspace uses [`.env.local`](/Users/alisia/Projects/aiProjects/GitlabFlow/.env.local), which is gitignored.

Tracked example: [`.env.local.example`](/Users/alisia/Projects/aiProjects/GitlabFlow/.env.local.example)

Expected variables:

- `SONAR_HOST_URL`
- `SONAR_ORGANIZATION`
- `SONAR_TOKEN`

Current repository defaults:

- `SONAR_HOST_URL=https://sonarcloud.io`
- `SONAR_ORGANIZATION=for-alisia`
- `sonar.projectKey=com.gitlabflow:flow-orchestrator` from [`flow-orchestrator/pom.xml`](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-orchestrator/pom.xml)
- `sonar.projectName=flow-orchestrator` from [`flow-orchestrator/pom.xml`](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-orchestrator/pom.xml)

## Preferred Command

Run from the repository root:

```bash
scripts/run-flow-orchestrator-sonar.sh
```

What this does:

- sources `.env.local` when present
- runs tests
- generates the JaCoCo XML report
- sends analysis to Sonar
- waits for the quality gate result by default
- keeps `SONAR_TOKEN` in the environment instead of passing it on the command line

## Raw Maven Command

Use the raw Maven command only when the helper script is unavailable. When documenting it in an artifact, record the redacted form only and never expand or reveal the token value:

```bash
set -a
source .env.local
set +a

cd flow-orchestrator
mvn verify sonar:sonar \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.organization="$SONAR_ORGANIZATION"
```

The scanner reads `SONAR_TOKEN` from the environment. Do not pass `-Dsonar.token=...` on the command line and do not paste resolved secret values into reports.

## Alignment With `artifacts/code-guidance.md`

### Guidance Alignment Matrix

| Code Guidance Area | Sonar Default Rules | Coverage Level | Notes |
|---|---|---|---|
| Bug-prone logic | Yes | Strong | Native Sonar rules are useful here. |
| Maintainability / code smells | Yes | Strong | Best covered through the project Quality Profile. |
| Duplication | Yes | Strong | Native Sonar coverage. |
| Complexity | Yes | Strong | Native Sonar coverage. |
| Security issues / hotspots | Yes | Strong | Native Sonar coverage, but still requires review judgment. |
| Test coverage visibility | Yes, with JaCoCo | Strong | Enabled by the Maven setup in `flow-orchestrator/pom.xml`. |
| Constructor injection preference | Partial | Partial | Sonar may flag field injection or related smells, but not all project-specific preferences. |
| Early validation at boundaries | Partial | Partial | Some smells may be surfaced indirectly, but this remains mostly a review concern. |
| Immutable boundary models | Partial | Partial | Some mutability smells may appear, but this is not fully enforced by default rules. |
| `final` locals and fields by policy | No | Reviewer only | Needs a custom rule or external analyzer. |
| Builder preference for non-trivial construction | No | Reviewer only | Default Sonar does not express this preference. |
| Lombok annotation preferences | No | Reviewer only | Not a good fit for default Sonar rules. |
| Required testing levels from the testing matrix | No | Reviewer only | Sonar sees tests and coverage, not whether the correct test levels exist. |
| Prompt / story / plan alignment | No | Reviewer only | Process validation, not static analysis. |
| Artifact completeness and truthfulness | No | Reviewer only | Process validation, not static analysis. |

### Sonar Default Rules Can Help With

- bug-prone code patterns
- maintainability/code-smell findings
- duplication
- complexity-related findings
- some security issues and security hotspots
- test coverage visibility when JaCoCo is present

### Sonar Default Rules Do Not Reliably Enforce

- `final` locals and fields by project policy
- preference for builder-style construction over direct `new`
- exact Lombok annotation preferences such as `@RequiredArgsConstructor` or `@Slf4j`
- required testing levels from the testing matrix
- prompt/story/plan alignment
- artifact completeness or truthfulness

These remain Reviewer or Team Lead responsibilities unless external analyzers or custom Sonar rules are added later.

## Server-Side Follow-Up

The repository can prepare analysis inputs, but the actual Sonar rule set lives on the SonarQube server.

To align Sonar with this workflow in a real environment:

- create or choose a Quality Profile for Java
- activate the default Sonar Java rules you want as baseline
- define a Quality Gate for release-blocking conditions such as new bugs, new vulnerabilities, and coverage thresholds
- optionally import external issues if you later add Checkstyle, PMD, SpotBugs, or Semgrep

Without that server-side profile and gate configuration, the repo-side setup can run analysis, but it cannot fully control which default rules are active.

## Agent Usage Rules

- `Java Coder` should run [`scripts/run-flow-orchestrator-sonar.sh`](/Users/alisia/Projects/aiProjects/GitlabFlow/scripts/run-flow-orchestrator-sonar.sh) before runtime sign-off and record the helper-script command plus the observed quality-gate result in the implementation report.
- `Reviewer` should rerun the same script during Phase 2 unless the environment is blocked. Reviewer must not accept a stale or coder-reported Sonar result without re-execution.
- `Team Lead` should audit that the review report shows analysis against `for-alisia / com.gitlabflow:flow-orchestrator`, includes Sonar task metadata, and that the reported quality-gate outcome matches the review decision.
- No workflow artifact may include a token value, `.env.local` contents, or an expanded secret-bearing command line.

## Future Hardening Options

If you want to align more of `artifacts/code-guidance.md` with automated Sonar checks, the next step is to add external analyzers and import their findings into Sonar, for example:

- Checkstyle for some style and final-variable rules
- PMD for additional design rules
- SpotBugs for bug detection
- Semgrep for targeted custom patterns
- Sonar custom rules or a plugin if you want server-native rule enforcement

Sonar supports importing external issues in generic or SARIF format, which is the practical path for project-specific rules without immediately writing a Sonar plugin.

Practical recommendation for this repository:

- `final`-by-default is realistic to automate later with a focused external rule set.
- Builder preference should stay primarily in Reviewer checks unless you narrow it to very specific patterns, because a broad "builder instead of `new`" rule tends to be noisy and context-sensitive.
