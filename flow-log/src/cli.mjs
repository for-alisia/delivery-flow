import { parseArgs } from "./cli-helpers.mjs";
import { dispatchLogCommand, LOG_COMMAND_HELP } from "./log-commands.mjs";
import { dispatchPlanCommand, PLAN_COMMAND_HELP } from "./plan-commands.mjs";

const FLOW_LOG_VERSION = "0.9.0";

export function runCli(argv, io) {
  try {
    const parsed = parseArgs(argv);
    const result = dispatch(parsed, io.cwd);
    io.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } catch (error) {
    const hint = resolveErrorHint(error.message);
    const payload = { error: error.message };
    if (hint) {
      payload.hint = hint;
    }
    io.stderr.write(`${JSON.stringify(payload, null, 2)}\n`);
    process.exitCode = 1;
  }
}

function dispatch(parsed, cwd) {
  const [command, subcommand] = parsed.positionals;

  if (!command || command === "help" || command === "--help") {
    return helpResult();
  }

  const logResult = dispatchLogCommand(command, subcommand, parsed, cwd);
  if (logResult !== undefined) {
    return logResult;
  }

  const planResult = dispatchPlanCommand(command, subcommand, parsed, cwd);
  if (planResult !== undefined) {
    return planResult;
  }

  throw new Error(`Unknown command: ${command}`);
}

function helpResult() {
  return {
    tool: "flow-log",
    version: FLOW_LOG_VERSION,
    commands: [
      ...LOG_COMMAND_HELP,
      "",
      "# Plan Structure (Architect writes, all agents read)",
      ...PLAN_COMMAND_HELP
    ]
  };
}

function resolveErrorHint(message) {
  if (message.includes("repository root")) {
    return "Change to the repository root before running flow-log.";
  }
  if (message.startsWith("State file does not exist")) {
    return 'Did you run `create --feature <name>` first?';
  }
  if (message.startsWith("Feature mismatch")) {
    return "Check the --feature flag matches the feature name in the state file.";
  }
  if (message.includes("Unexpected token") || message.includes("JSON")) {
    return "State file may be corrupted. Check file contents at the path shown.";
  }
  if (message.startsWith("Unknown command")) {
      return "Run `scripts/flow-log.sh help` from the repository root for available commands.";
  }
  if (message.startsWith("Missing required flag")) {
      return "Run `scripts/flow-log.sh help` from the repository root for command syntax.";
  }
  return undefined;
}
