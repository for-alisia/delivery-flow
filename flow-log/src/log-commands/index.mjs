import { handleRegisterArtifact, handleApproveArtifact } from "./artifacts.mjs";
import { handleCompleteBatch, handleResetChecks, handleStartBatch } from "./batches.mjs";
import { handleAddChange, handleRunCheck, handleSetCheck, handleVerifyAll } from "./checks.mjs";
import { handleAddEvent } from "./events.mjs";
import {
  handleAddFinding,
  handleCodeReviewGate,
  handleIncrementCodeReviewRound,
  handleReopenFinding,
  handleResolveFinding,
  handleRespondFinding
} from "./findings.mjs";
import { handleComplete, handleCreate, handleLockRequirements } from "./lifecycle.mjs";
import { handleGet, handleHistory, handleReadiness, handleStatus, handleSummary } from "./queries.mjs";
import {
  handleAddRisk,
  handleArchitectureGate,
  handleIncrementRound,
  handleReclassifyRisk,
  handleReopenRisk,
  handleResolveRisk,
  handleRespondRisk
} from "./risks.mjs";
import { handleSetReview } from "./reviews.mjs";

export const LOG_COMMAND_HELP = [
  "create --feature <name> [--state-path <path>] [--force]",
  "lock-requirements --feature <name> [--by <actor>] [--request-source <path>] [--state-path <path>]",
  "register-artifact <story|plan> --feature <name> --path <file> [--state-path <path>]",
  "approve-artifact <story|plan> --feature <name> --by <actor> [--state-path <path>]",
  "set-review --feature <name> --name <architectureReview|codeReview> --status <PENDING|PASS|FAIL|BLOCKED> [--by <actor>] [--reason <text>] [--state-path <path>]",
  "set-check --feature <name> --name <verifyQuick|finalCheck|karate> --status <NOT_RUN|PASS|FAIL|BLOCKED> [--by <actor>] [--command <cmd>] [--details <text>] [--report-path <path>]... [--state-path <path>]",
  "run-check --feature <name> --name <verifyQuick|finalCheck|karate> [--command <script-path>] [--timeout <ms>] [--by <actor>] [--state-path <path>]",
  "verify-all --feature <name> [--timeout <ms>] [--by <actor>] [--state-path <path>]",
  "add-change --feature <name> --file <path> [--file <path>]... [--state-path <path>]",
  "add-event --feature <name> --type <redCard|rejection|reroute|note|batchStart|batchEnd|archEscalationDecision> --reason <text> [--decision <PROCEED_TO_CODING|FINAL_ADJUSTMENT|ESCALATE_TO_USER>] [--by <actor>] [--target <agent>] [--related-check <name>] [--related-review <name>] [--state-path <path>]",
  "start-batch --feature <name> [--slice <name>]... [--by <actor>] [--state-path <path>]",
  "complete-batch --feature <name> [--status <complete|failed|blocked>] [--state-path <path>]",
  "reset-checks --feature <name> [--reason <text>] [--by <actor>] [--target <agent>] [--state-path <path>]",
  "add-risk --feature <name> [--severity <CRITICAL|HIGH|MEDIUM|LOW|UNCLASSIFIED>] --description <text> [--suggested-fix <text>] [--plan-ref <id>]... [--connected-area <id>]... [--by <actor>] [--state-path <path>]",
  "respond-risk --feature <name> --id <number> --status <ADDRESSED|INVALIDATED> --note <text> [--by <actor>] [--state-path <path>]",
  "resolve-risk --feature <name> --id <number> [--by <actor>] [--state-path <path>]",
  "reopen-risk --feature <name> --id <number> [--reason <text>] [--by <actor>] [--state-path <path>]",
  "reclassify-risk --feature <name> --id <number> --severity <CRITICAL|HIGH|MEDIUM|LOW> --reason <text> [--by <actor>] [--state-path <path>]",
  "increment-round --feature <name> [--state-path <path>]",
  "architecture-gate --feature <name> [--state-path <path>]",
  "add-finding --feature <name> --severity <CRITICAL|HIGH|MEDIUM|LOW> --description <text> [--file <path>] [--by <actor>] [--state-path <path>]",
  "respond-finding --feature <name> --id <number> --status <FIXED|DISPUTED> --note <text> [--by <actor>] [--state-path <path>]",
  "resolve-finding --feature <name> --id <number> [--by <actor>] [--state-path <path>]",
  "reopen-finding --feature <name> --id <number> [--reason <text>] [--by <actor>] [--state-path <path>]",
  "increment-code-review-round --feature <name> [--state-path <path>]",
  "code-review-gate --feature <name> [--state-path <path>]",
  "complete --feature <name> [--state-path <path>]",
  "get --feature <name> [--state-path <path>]",
  "history --feature <name> [--limit <n>] [--state-path <path>]",
  "status --feature <name> [--state-path <path>]",
  "summary --feature <name> [--state-path <path>]",
  "readiness signoff --feature <name> [--state-path <path>]"
];

export function dispatchLogCommand(command, subcommand, parsed, cwd) {
  switch (command) {
    case "create":
      return handleCreate(parsed, cwd);
    case "lock-requirements":
      return handleLockRequirements(parsed, cwd);
    case "register-artifact":
      return handleRegisterArtifact(parsed, cwd, subcommand);
    case "approve-artifact":
      return handleApproveArtifact(parsed, cwd, subcommand);
    case "set-review":
      return handleSetReview(parsed, cwd);
    case "set-check":
      return handleSetCheck(parsed, cwd);
    case "run-check":
      return handleRunCheck(parsed, cwd);
    case "verify-all":
      return handleVerifyAll(parsed, cwd);
    case "add-change":
      return handleAddChange(parsed, cwd);
    case "add-event":
      return handleAddEvent(parsed, cwd);
    case "start-batch":
      return handleStartBatch(parsed, cwd);
    case "complete-batch":
      return handleCompleteBatch(parsed, cwd);
    case "reset-checks":
      return handleResetChecks(parsed, cwd);
    case "complete":
      return handleComplete(parsed, cwd);
    case "get":
      return handleGet(parsed, cwd);
    case "history":
      return handleHistory(parsed, cwd);
    case "status":
      return handleStatus(parsed, cwd);
    case "summary":
      return handleSummary(parsed, cwd);
    case "readiness":
      return handleReadiness(parsed, cwd, subcommand);
    case "add-risk":
      return handleAddRisk(parsed, cwd);
    case "respond-risk":
      return handleRespondRisk(parsed, cwd);
    case "resolve-risk":
      return handleResolveRisk(parsed, cwd);
    case "reopen-risk":
      return handleReopenRisk(parsed, cwd);
    case "reclassify-risk":
      return handleReclassifyRisk(parsed, cwd);
    case "increment-round":
      return handleIncrementRound(parsed, cwd);
    case "architecture-gate":
      return handleArchitectureGate(parsed, cwd);
    case "add-finding":
      return handleAddFinding(parsed, cwd);
    case "respond-finding":
      return handleRespondFinding(parsed, cwd);
    case "resolve-finding":
      return handleResolveFinding(parsed, cwd);
    case "reopen-finding":
      return handleReopenFinding(parsed, cwd);
    case "increment-code-review-round":
      return handleIncrementCodeReviewRound(parsed, cwd);
    case "code-review-gate":
      return handleCodeReviewGate(parsed, cwd);
    default:
      return undefined;
  }
}
