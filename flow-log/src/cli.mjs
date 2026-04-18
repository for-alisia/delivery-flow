import { parseArgs } from "./cli-helpers.mjs";
import { dispatchLogCommand, LOG_COMMAND_HELP } from "./log-commands.mjs";
import { dispatchPlanCommand, PLAN_COMMAND_HELP } from "./plan-commands.mjs";

const FLOW_LOG_VERSION = "0.8.0";

export function runCli(argv, io) {
  try {
    const parsed = parseArgs(argv);
    const result = dispatch(parsed, io.cwd);
    io.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } catch (error) {
    io.stderr.write(
      `${JSON.stringify(
        {
          error: error.message
        },
        null,
        2
      )}\n`
    );
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
