import {
  addPlanClass,
  addPlanExample,
  addPlanModel,
  addPlanModelField,
  addPlanSlice,
  addPlanSliceTest,
  addPlanTest,
  addPlanValidation,
  bumpPlanRevision,
  loadPlan,
  resolvePlanPath,
  savePlan,
  setPlanArchunit,
  setPlanComposition,
  setPlanInfra,
  setPlanKarate,
  setPlanSliceLogging
} from "../plan/legacy-v2.mjs";
import {
  normalizeArray,
  optionalFlag,
  parsePositiveInteger,
  requiredFlag
} from "../cli-helpers.mjs";

export const LEGACY_V2_PLAN_COMMAND_HELP = [
  "add-plan-example --feature <name> --label <text> --type <request|success|error|validation-error> --body <json>    # legacy v2",
  "add-plan-validation --feature <name> --rule <text> --boundary <text> --reason <text>                                # legacy v2",
  "add-plan-model --feature <name> --qualified-name <fqcn> --type <record|enum|interface|sealed-interface> --status <new|modified> --justification <text> [--fields <json-array>] [--annotations <csv>] [--notes <text>] [--values <csv>] [--methods <csv>]    # legacy v2",
  "add-plan-model-field --feature <name> --model <qualified-name> --name <field> --type <java-type> [--nullable] [--defensive-copy]    # legacy v2",
  "add-plan-class --feature <name> --path <class-path> --status <new|modified|existing> [--role <text>]               # legacy v2",
  "add-plan-slice --feature <name> --id <N> --title <text> --goal <text> [--files <csv>] [--unit-test <text>]... [--integration-test <text>]... [--component-test <text>]... [--info-log <text>] [--warn-log <text>] [--error-log <text>]    # legacy v2",
  "add-plan-slice-test --feature <name> --slice <N> --level <unit|integration|component> --test <text>                # legacy v2",
  "set-plan-slice-logging --feature <name> --slice <N> [--info <text>] [--warn <text>] [--error <text>]                # legacy v2",
  "set-plan-composition --feature <name> --approach <text> --description <text>                                         # legacy v2",
  "set-plan-infra --feature <name> [--reused <csv>] [--new <csv>]                                                       # legacy v2",
  "add-plan-test --feature <name> --level <text> --coverage <text> [--required]                                         # legacy v2",
  "set-plan-karate --feature <name> --feature-file <path> [--scenario <text>]... [--smoke-tagged] [--runner-updated]  # legacy v2",
  "set-plan-archunit --feature <name> [--new-rule <text>]... [--existing-reviewed]                                      # legacy v2",
  "revise-plan --feature <name>                                                                                          # legacy v2"
];

export function dispatchLegacyV2PlanCommand(command, parsed, cwd) {
  switch (command) {
    case "add-plan-example":
      return handleAddPlanExample(parsed, cwd);
    case "add-plan-validation":
      return handleAddPlanValidation(parsed, cwd);
    case "add-plan-model":
      return handleAddPlanModel(parsed, cwd);
    case "add-plan-model-field":
      return handleAddPlanModelField(parsed, cwd);
    case "add-plan-class":
      return handleAddPlanClass(parsed, cwd);
    case "add-plan-slice":
      return handleAddPlanSlice(parsed, cwd);
    case "add-plan-slice-test":
      return handleAddPlanSliceTest(parsed, cwd);
    case "set-plan-slice-logging":
      return handleSetPlanSliceLogging(parsed, cwd);
    case "set-plan-composition":
      return handleSetPlanComposition(parsed, cwd);
    case "set-plan-infra":
      return handleSetPlanInfra(parsed, cwd);
    case "add-plan-test":
      return handleAddPlanTest(parsed, cwd);
    case "set-plan-karate":
      return handleSetPlanKarate(parsed, cwd);
    case "set-plan-archunit":
      return handleSetPlanArchunit(parsed, cwd);
    case "revise-plan":
      return handleRevisePlan(parsed, cwd);
    default:
      return undefined;
  }
}

function handleAddPlanExample(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const label = requiredFlag(parsed, "label");
  const type = requiredFlag(parsed, "type");
  const body = parseJsonFlag(requiredFlag(parsed, "body"), "--body must be valid JSON");

  const entry = addPlanExample(plan, label, type, body);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-example", feature, entry, total: plan.payloadExamples.length, legacy: true };
}

function handleAddPlanValidation(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const rule = requiredFlag(parsed, "rule");
  const boundary = requiredFlag(parsed, "boundary");
  const reason = requiredFlag(parsed, "reason");

  const entry = addPlanValidation(plan, rule, boundary, reason);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-validation", feature, entry, total: plan.validationBoundary.length, legacy: true };
}

function handleAddPlanModel(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const qualifiedName = requiredFlag(parsed, "qualified-name");
  const type = requiredFlag(parsed, "type");
  const status = requiredFlag(parsed, "status");
  const justification = requiredFlag(parsed, "justification");

  const opts = {};
  const fieldsRaw = optionalFlag(parsed, "fields");
  if (fieldsRaw) {
    opts.fields = parseJsonFlag(fieldsRaw, "--fields must be a valid JSON array");
  }

  const annotationsRaw = optionalFlag(parsed, "annotations");
  if (annotationsRaw) {
    opts.annotations = splitCsv(annotationsRaw);
  }

  opts.notes = optionalFlag(parsed, "notes") ?? null;

  const valuesRaw = optionalFlag(parsed, "values");
  if (valuesRaw) {
    opts.values = splitCsv(valuesRaw);
  }

  const methodsRaw = optionalFlag(parsed, "methods");
  if (methodsRaw) {
    opts.methods = splitCsv(methodsRaw);
  }

  const entry = addPlanModel(plan, qualifiedName, type, status, justification, opts);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-model", feature, entry, totalModels: plan.models.length, legacy: true };
}

function handleAddPlanModelField(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const modelName = requiredFlag(parsed, "model");
  const name = requiredFlag(parsed, "name");
  const type = requiredFlag(parsed, "type");

  const opts = {
    nullable: optionalFlag(parsed, "nullable") === true,
    defensiveCopy: optionalFlag(parsed, "defensive-copy") === true
  };

  const field = addPlanModelField(plan, modelName, name, type, opts);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-model-field", feature, model: modelName, field, legacy: true };
}

function handleAddPlanClass(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const filePath = requiredFlag(parsed, "path");
  const status = requiredFlag(parsed, "status");
  const role = optionalFlag(parsed, "role");

  const entry = addPlanClass(plan, filePath, status, role);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-class", feature, entry, totalClasses: plan.classes.length, legacy: true };
}

function handleAddPlanSlice(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const sliceId = parsePositiveInteger(requiredFlag(parsed, "id"), "slice id");
  const title = requiredFlag(parsed, "title");
  const goal = requiredFlag(parsed, "goal");

  const opts = {};
  const filesRaw = optionalFlag(parsed, "files");
  if (filesRaw) {
    opts.files = splitCsv(filesRaw);
  }

  const unitTests = normalizeArray(optionalFlag(parsed, "unit-test"));
  const integrationTests = normalizeArray(optionalFlag(parsed, "integration-test"));
  const componentTests = normalizeArray(optionalFlag(parsed, "component-test"));
  if (unitTests.length > 0 || integrationTests.length > 0 || componentTests.length > 0) {
    opts.tests = { unit: unitTests, integration: integrationTests, component: componentTests };
  }

  const infoLog = optionalFlag(parsed, "info-log");
  const warnLog = optionalFlag(parsed, "warn-log");
  const errorLog = optionalFlag(parsed, "error-log");
  if (infoLog !== undefined || warnLog !== undefined || errorLog !== undefined) {
    opts.logging = {};
    if (infoLog !== undefined) opts.logging.info = infoLog;
    if (warnLog !== undefined) opts.logging.warn = warnLog;
    if (errorLog !== undefined) opts.logging.error = errorLog;
  }

  const entry = addPlanSlice(plan, sliceId, title, goal, opts);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-slice", feature, entry, totalSlices: plan.slices.length, legacy: true };
}

function handleAddPlanSliceTest(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const sliceId = parsePositiveInteger(requiredFlag(parsed, "slice"), "slice id");
  const level = requiredFlag(parsed, "level");
  const test = requiredFlag(parsed, "test");

  addPlanSliceTest(plan, sliceId, level, test);
  savePlan(planPath, plan);
  const slice = plan.slices.find((entry) => entry.id === sliceId);
  return { ok: true, command: "add-plan-slice-test", feature, sliceId, level, test, totalTests: slice.tests[level].length, legacy: true };
}

function handleSetPlanSliceLogging(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const sliceId = parsePositiveInteger(requiredFlag(parsed, "slice"), "slice id");

  const opts = {};
  const infoLog = optionalFlag(parsed, "info");
  const warnLog = optionalFlag(parsed, "warn");
  const errorLog = optionalFlag(parsed, "error");
  if (infoLog !== undefined) opts.info = infoLog;
  if (warnLog !== undefined) opts.warn = warnLog;
  if (errorLog !== undefined) opts.error = errorLog;

  setPlanSliceLogging(plan, sliceId, opts);
  savePlan(planPath, plan);
  const slice = plan.slices.find((entry) => entry.id === sliceId);
  return { ok: true, command: "set-plan-slice-logging", feature, sliceId, logging: slice.logging, legacy: true };
}

function handleSetPlanComposition(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const approach = requiredFlag(parsed, "approach");
  const description = requiredFlag(parsed, "description");

  setPlanComposition(plan, approach, description);
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-composition", feature, compositionStrategy: plan.compositionStrategy, legacy: true };
}

function handleSetPlanInfra(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const reusedRaw = optionalFlag(parsed, "reused");
  const newRaw = optionalFlag(parsed, "new");

  const reused = reusedRaw ? splitCsv(reusedRaw) : [];
  const newInfra = newRaw ? splitCsv(newRaw) : [];

  setPlanInfra(plan, reused, newInfra);
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-infra", feature, sharedInfra: plan.sharedInfra, legacy: true };
}

function handleAddPlanTest(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const level = requiredFlag(parsed, "level");
  const required = optionalFlag(parsed, "required") === true;
  const coverage = requiredFlag(parsed, "coverage");

  const entry = addPlanTest(plan, level, required, coverage);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-test", feature, entry, totalLevels: plan.testingMatrix.length, legacy: true };
}

function handleSetPlanKarate(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const featureFile = requiredFlag(parsed, "feature-file");
  const scenarios = normalizeArray(optionalFlag(parsed, "scenario"));
  const smokeTagged = optionalFlag(parsed, "smoke-tagged") === true;
  const runnerUpdated = optionalFlag(parsed, "runner-updated") === true;

  setPlanKarate(plan, { featureFile, scenarios, smokeTagged, runnerUpdated });
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-karate", feature, karate: plan.karate, legacy: true };
}

function handleSetPlanArchunit(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const newRules = normalizeArray(optionalFlag(parsed, "new-rule"));
  const existingReviewed = optionalFlag(parsed, "existing-reviewed") === true;

  setPlanArchunit(plan, { newRules, existingReviewed });
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-archunit", feature, archUnit: plan.archUnit, legacy: true };
}

function handleRevisePlan(parsed, cwd) {
  const { feature, plan, planPath } = openLegacyPlan(parsed, cwd);
  const previousRevision = plan.revision;
  bumpPlanRevision(plan);
  savePlan(planPath, plan);

  return {
    ok: true,
    command: "revise-plan",
    feature,
    previousRevision,
    newRevision: plan.revision,
    note: "All v2 sections cleared. Re-register the revised plan from scratch.",
    legacy: true
  };
}

function openLegacyPlan(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const planPath = resolvePlanPath(cwd, feature);
  const plan = loadPlan(planPath);

  if (plan.schemaVersion !== "2.0") {
    throw new Error(
      `Legacy v2 command requires schemaVersion '2.0'. Found '${plan.schemaVersion}'. Use the v3 draft lifecycle commands instead.`
    );
  }

  return { feature, plan, planPath };
}

function parseJsonFlag(rawValue, errorMessage) {
  try {
    return JSON.parse(rawValue);
  } catch {
    throw new Error(errorMessage);
  }
}

function splitCsv(rawValue) {
  return rawValue.split(",").map((value) => value.trim());
}
