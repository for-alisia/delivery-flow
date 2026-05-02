import { handleRegisterArtifact, handleApproveArtifact } from "./artifacts.mjs";
import { handleCompleteSliceRun, handleResetChecks, handleStartSliceRun } from "./slice-runs.mjs";
import { handleAddChange, handleCheckLog, handleRunCheck, handleSetCheck, handleVerify } from "./checks.mjs";
import { handleAddEvent } from "./events.mjs";
import { handleSetE2EMode } from "./e2e.mjs";
import {
  handleAddFinding,
  handleCodeReviewGate,
  handleDecideFinding,
  handleIncrementCodeReviewRound,
  handleReopenFinding,
  handleResolveFinding,
  handleRespondFinding
} from "./findings.mjs";
import { handleComplete, handleCreate, handleLockRequirements } from "./lifecycle.mjs";
import { handleGet, handleHistory, handleReadiness, handleStatus, handleStoryGet, handleSummary } from "./queries.mjs";
import {
  handleAddRisk,
  handleArchitectureGate,
  handleDecideRisk,
  handleIncrementRound,
  handleReclassifyRisk,
  handleReopenRisk,
  handleResolveRisk,
  handleRespondRisk
} from "./risks.mjs";
import { handleSetReview } from "./reviews.mjs";

const LIFECYCLE_COMMAND_HELP = [
  "create --feature <name> [--state-path <path>] [--force]",
  "lock-requirements --feature <name> [--by <actor>] [--request-source <path>] [--state-path <path>]",
  "set-e2e-mode --feature <name> --mode <UNDECIDED|REUSE_EXISTING|SCENARIOS_REQUIRED> [--by <actor>] [--reason <text>] [--state-path <path>]",
  "register-artifact <story|e2e|plan> --feature <name> --path <file> [--state-path <path>]",
  "approve-artifact <story|e2e|plan> --feature <name> --by <actor> [--state-path <path>]",
  "set-review --feature <name> --name <architectureReview|codeReview> --status <PENDING|PASS|FAIL|BLOCKED> [--by <actor>] [--reason <text>] [--state-path <path>]",
  "set-check --feature <name> --name <verifyQuick|finalCheck|karate> --status <NOT_RUN|PASS|FAIL|BLOCKED> [--by <actor>] [--command <cmd>] [--details <text>] [--report-path <path>]... [--state-path <path>]",
  "run-check --feature <name> --name <verifyQuick|finalCheck|karate> [--command <script-path>] [--timeout <ms>] [--by <actor>] [--state-path <path>]",
  "check-log --feature <name> --name <verifyQuick|finalCheck|karate> [--lines <n>] [--state-path <path>]",
  "verify --feature <name> [--profile <full|slice>] [--timeout <ms>] [--by <actor>] [--state-path <path>]",
  "add-change --feature <name> --file <path> [--file <path>]... [--state-path <path>]",
  "add-event --feature <name> --type <redCard|rejection|reroute|note|sliceRunStart|sliceRunEnd|archEscalationDecision> --reason <text> [--decision <PROCEED_TO_CODING|FINAL_ADJUSTMENT|ESCALATE_TO_USER>] [--by <actor>] [--target <agent>] [--related-check <name>] [--related-review <name>] [--state-path <path>]",
  "start-slice-run --feature <name> --slice <approved-slice-id> --type <intermediate|final> [--by <actor>] [--state-path <path>]",
  "complete-slice-run --feature <name> [--status <complete|failed|blocked>] [--state-path <path>]",
  "reset-checks --feature <name> [--reason <text>] [--by <actor>] [--target <agent>] [--state-path <path>]"
];

const RISK_COMMAND_HELP = [
  "add-risk --feature <name> [--severity <CRITICAL|HIGH|MEDIUM|LOW|UNCLASSIFIED>] --description <text> --plan-ref <id> [--plan-ref <id>]... [--suggested-fix <text>] [--connected-area <id>]... [--by <actor>] [--state-path <path>]",
  "respond-risk --feature <name> --id <number> --status <ADDRESSED|INVALIDATED> --note <text> [--by <actor>] [--state-path <path>]",
  "decide-risk --feature <name> --id <number> --status <ACCEPTED|DEFERRED> --reason <text> [--follow-up <text>] [--by <actor>] [--state-path <path>]",
  "resolve-risk --feature <name> --id <number> [--by <actor>] [--state-path <path>]",
  "reopen-risk --feature <name> --id <number> [--reason <text>] [--by <actor>] [--state-path <path>]",
  "reclassify-risk --feature <name> --id <number> --severity <CRITICAL|HIGH|MEDIUM|LOW> --reason <text> [--by <actor>] [--state-path <path>]",
  "increment-round --feature <name> [--state-path <path>]",
  "architecture-gate --feature <name> [--state-path <path>]"
];

const FINDING_COMMAND_HELP = [
  "add-finding --feature <name> --severity <CRITICAL|HIGH|MEDIUM|LOW> --description <text> [--file <path>] [--by <actor>] [--state-path <path>]",
  "respond-finding --feature <name> --id <number> --status <FIXED|DISPUTED> --note <text> [--by <actor>] [--state-path <path>]",
  "decide-finding --feature <name> --id <number> --status <ACCEPTED|DEFERRED> --reason <text> [--follow-up <text>] [--by <actor>] [--state-path <path>]",
  "resolve-finding --feature <name> --id <number> [--by <actor>] [--state-path <path>]",
  "reopen-finding --feature <name> --id <number> [--reason <text>] [--by <actor>] [--state-path <path>]",
  "increment-code-review-round --feature <name> [--state-path <path>]",
  "code-review-gate --feature <name> [--state-path <path>]"
];

const QUERY_COMMAND_HELP = [
  "complete --feature <name> [--state-path <path>]",
  "history --feature <name> [--limit <n>] [--state-path <path>]",
  "status --feature <name> [--state-path <path>]",
  "summary --feature <name> [--state-path <path>]",
  "story-get --feature <name> --section <name> [--state-path <path>]",
  "readiness signoff --feature <name> [--state-path <path>]"
];

export const LOG_COMMAND_HELP = [
  ...LIFECYCLE_COMMAND_HELP,
  ...RISK_COMMAND_HELP,
  ...FINDING_COMMAND_HELP,
  ...QUERY_COMMAND_HELP
];

const LIFECYCLE_HANDLERS = {
  create: (parsed, cwd) => handleCreate(parsed, cwd),
  "lock-requirements": (parsed, cwd) => handleLockRequirements(parsed, cwd),
  "set-e2e-mode": (parsed, cwd) => handleSetE2EMode(parsed, cwd),
  "register-artifact": (parsed, cwd, subcommand) => handleRegisterArtifact(parsed, cwd, subcommand),
  "approve-artifact": (parsed, cwd, subcommand) => handleApproveArtifact(parsed, cwd, subcommand),
  "set-review": (parsed, cwd) => handleSetReview(parsed, cwd),
  "set-check": (parsed, cwd) => handleSetCheck(parsed, cwd),
  "run-check": (parsed, cwd) => handleRunCheck(parsed, cwd),
  "check-log": (parsed, cwd) => handleCheckLog(parsed, cwd),
  verify: (parsed, cwd) => handleVerify(parsed, cwd),
  "add-change": (parsed, cwd) => handleAddChange(parsed, cwd),
  "add-event": (parsed, cwd) => handleAddEvent(parsed, cwd),
  "start-slice-run": (parsed, cwd) => handleStartSliceRun(parsed, cwd),
  "complete-slice-run": (parsed, cwd) => handleCompleteSliceRun(parsed, cwd),
  "reset-checks": (parsed, cwd) => handleResetChecks(parsed, cwd),
  complete: (parsed, cwd) => handleComplete(parsed, cwd),
  get: (parsed, cwd) => handleGet(parsed, cwd),
  history: (parsed, cwd) => handleHistory(parsed, cwd),
  status: (parsed, cwd) => handleStatus(parsed, cwd),
  summary: (parsed, cwd) => handleSummary(parsed, cwd),
  "story-get": (parsed, cwd) => handleStoryGet(parsed, cwd),
  readiness: (parsed, cwd, subcommand) => handleReadiness(parsed, cwd, subcommand)
};

const RISK_HANDLERS = {
  "add-risk": (parsed, cwd) => handleAddRisk(parsed, cwd),
  "respond-risk": (parsed, cwd) => handleRespondRisk(parsed, cwd),
  "decide-risk": (parsed, cwd) => handleDecideRisk(parsed, cwd),
  "resolve-risk": (parsed, cwd) => handleResolveRisk(parsed, cwd),
  "reopen-risk": (parsed, cwd) => handleReopenRisk(parsed, cwd),
  "reclassify-risk": (parsed, cwd) => handleReclassifyRisk(parsed, cwd),
  "increment-round": (parsed, cwd) => handleIncrementRound(parsed, cwd),
  "architecture-gate": (parsed, cwd) => handleArchitectureGate(parsed, cwd)
};

const FINDING_HANDLERS = {
  "add-finding": (parsed, cwd) => handleAddFinding(parsed, cwd),
  "respond-finding": (parsed, cwd) => handleRespondFinding(parsed, cwd),
  "decide-finding": (parsed, cwd) => handleDecideFinding(parsed, cwd),
  "resolve-finding": (parsed, cwd) => handleResolveFinding(parsed, cwd),
  "reopen-finding": (parsed, cwd) => handleReopenFinding(parsed, cwd),
  "increment-code-review-round": (parsed, cwd) => handleIncrementCodeReviewRound(parsed, cwd),
  "code-review-gate": (parsed, cwd) => handleCodeReviewGate(parsed, cwd)
};

const COMMAND_HANDLERS = {
  ...LIFECYCLE_HANDLERS,
  ...RISK_HANDLERS,
  ...FINDING_HANDLERS
};

export function dispatchLogCommand(command, subcommand, parsed, cwd) {
  const handler = COMMAND_HANDLERS[command];
  return handler ? handler(parsed, cwd, subcommand) : undefined;
}
