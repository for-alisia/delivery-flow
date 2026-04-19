import { dispatchDraftPlanCommand, DRAFT_PLAN_COMMAND_HELP } from "./draft.mjs";
import { dispatchLegacyV2PlanCommand, LEGACY_V2_PLAN_COMMAND_HELP } from "./legacy-v2.mjs";
import { dispatchPlanQueryCommand, PLAN_QUERY_COMMAND_HELP } from "./query.mjs";

export const PLAN_COMMAND_HELP = [
  ...DRAFT_PLAN_COMMAND_HELP,
  ...PLAN_QUERY_COMMAND_HELP,
  "",
  "# Legacy v2 authoring commands (transition only)",
  ...LEGACY_V2_PLAN_COMMAND_HELP
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

  return dispatchLegacyV2PlanCommand(command, parsed, cwd);
}
