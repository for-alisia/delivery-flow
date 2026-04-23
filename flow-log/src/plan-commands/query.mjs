import {
  assertPlan,
  buildPlanSummary,
  loadPlan,
  resolvePlanPath,
  validatePlanReadiness,
  validatePlanShape,
  validateRiskLinks
} from "../plan/index.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";

export const PLAN_QUERY_COMMAND_HELP = [
  "validate-plan --feature <name> [--state-path <path>]",
  "plan-summary --feature <name> [--state-path <path>]",
  "plan-get --feature <name> (--section <name> | --slice <id>)"
];

export function dispatchPlanQueryCommand(command, parsed, cwd) {
  switch (command) {
    case "validate-plan":
      return handleValidatePlan(parsed, cwd);
    case "plan-summary":
      return handlePlanSummary(parsed, cwd);
    case "plan-get":
      return handlePlanGet(parsed, cwd);
    default:
      return undefined;
  }
}

function handleValidatePlan(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const explicitStatePath = optionalFlag(parsed, "state-path");
  const planPath = resolvePlanPath(cwd, feature);
  const plan = loadPlan(planPath);
  assertPlan(plan);

  const shape = validatePlanShape(plan);
  const readiness = validatePlanReadiness(plan);
  const riskLinks = validateRiskLinks(plan, cwd, feature, explicitStatePath);

  const issues = [
    ...shape.issues.map((issue) => `[shape] ${issue}`),
    ...readiness.issues.map((issue) => `[readiness] ${issue}`),
    ...riskLinks.issues.map((issue) => `[risk-links] ${issue}`)
  ];

  return {
    ok: true,
    command: "validate-plan",
    feature,
    planPath,
    schemaVersion: plan.schemaVersion,
    valid: issues.length === 0,
    issues,
    shape,
    readiness,
    riskLinks
  };
}

function handlePlanSummary(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const explicitStatePath = optionalFlag(parsed, "state-path");
  const planPath = resolvePlanPath(cwd, feature);
  const plan = loadPlan(planPath);
  assertPlan(plan);

  return {
    ok: true,
    command: "plan-summary",
    feature,
    planPath,
    ...buildPlanSummary(plan, cwd, feature, explicitStatePath, planPath)
  };
}

function handlePlanGet(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const section = optionalFlag(parsed, "section");
  const sliceId = optionalFlag(parsed, "slice");
  const planPath = resolvePlanPath(cwd, feature);
  const plan = loadPlan(planPath);
  assertPlan(plan);

  if (section && sliceId) {
    throw new Error("Use either --section or --slice, not both.");
  }

  if (!section && !sliceId) {
    throw new Error("plan-get requires --section or --slice.");
  }

  if (sliceId) {
    const slice = (plan.slices ?? []).find((entry) => entry.id === sliceId);
    if (!slice) {
      throw new Error(`Unknown slice id: ${sliceId}`);
    }

    return {
      ok: true,
      command: "plan-get",
      feature,
      planPath,
      sliceId,
      data: {
        feature: plan.feature,
        revision: plan.revision,
        scope: plan.scope,
        sharedRules: filterBySlice(plan.sharedRules ?? [], sliceId),
        sharedDecisions: filterBySlice(plan.sharedDecisions ?? [], sliceId),
        slice,
        finalVerification: plan.finalVerification
      },
      schemaVersion: plan.schemaVersion,
    };
  }

  const sections = {
    scope: plan.scope,
    sharedRules: plan.sharedRules,
    sharedDecisions: plan.sharedDecisions,
    slices: plan.slices,
    finalVerification: plan.finalVerification,
    hash: plan.hash
  };

  if (!(section in sections)) {
    throw new Error(`Unknown plan section: ${section}. Available: ${Object.keys(sections).join(", ")}`);
  }

  return {
    ok: true,
    command: "plan-get",
    feature,
    planPath,
    section,
    data: sections[section],
    schemaVersion: plan.schemaVersion
  };
}

function filterBySlice(entries, sliceId) {
  return entries.filter((entry) => (entry.appliesTo ?? []).includes(sliceId));
}
