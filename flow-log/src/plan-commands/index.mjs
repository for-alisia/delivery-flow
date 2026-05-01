import { dispatchDraftPlanCommand, DRAFT_PLAN_COMMAND_HELP } from "./draft.mjs";
import { dispatchPlanQueryCommand, PLAN_QUERY_COMMAND_HELP } from "./query.mjs";

export const PLAN_COMMAND_HELP = [
  ...DRAFT_PLAN_COMMAND_HELP,
  ...PLAN_QUERY_COMMAND_HELP
];

export function dispatchPlanCommand(command, subcommand, parsed, cwd) {
  void subcommand;

  const draftResult = dispatchDraftPlanCommand(command, parsed, cwd);
  if (draftResult !== undefined) {
    return draftResult;
  }

  const queryResult = dispatchPlanQueryCommand(command, parsed, cwd);
  if (queryResult !== undefined) {
    return queryResult;
  }

  return undefined;
}
