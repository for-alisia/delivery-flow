import {
  buildPlanSummary,
  isV3Plan,
  loadPlan,
  resolvePlanPath,
  validatePlanReadiness,
  validatePlanShape,
  validateRiskLinks
} from "../plan/index.mjs";
import {
  buildPlanSummary as buildPlanSummaryV2,
  getPlanSection as getPlanSectionV2,
  validatePlan as validatePlanV2
} from "../plan/legacy-v2.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";

export const PLAN_QUERY_COMMAND_HELP = [
  "validate-plan --feature <name> [--state-path <path>]",
  "plan-summary --feature <name> [--state-path <path>]",
  "plan-get --feature <name> [--section <name>]"
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

  if (!isV3Plan(plan)) {
    const legacy = validatePlanV2(plan);
    return {
      ok: true,
      command: "validate-plan",
      feature,
      planPath,
      schemaVersion: plan.schemaVersion,
      legacy: true,
      ...legacy
    };
  }

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

  if (!isV3Plan(plan)) {
    return {
      ok: true,
      command: "plan-summary",
      feature,
      planPath,
      schemaVersion: plan.schemaVersion,
      legacy: true,
      ...buildPlanSummaryV2(plan)
    };
  }

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
  const planPath = resolvePlanPath(cwd, feature);
  const plan = loadPlan(planPath);

  if (!section) {
    return { ok: true, command: "plan-get", feature, planPath, plan };
  }

  if (!isV3Plan(plan)) {
    const data = getPlanSectionV2(plan, section);
    return {
      ok: true,
      command: "plan-get",
      feature,
      planPath,
      section,
      data,
      schemaVersion: plan.schemaVersion,
      legacy: true
    };
  }

  const sections = {
    scope: plan.scope,
    implementationFlow: plan.implementationFlow,
    contractExamples: plan.contractExamples,
    validationRules: plan.validationRules,
    designDecisions: plan.designDecisions,
    models: plan.models,
    classes: plan.classes,
    slices: plan.slices,
    verification: plan.verification,
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
