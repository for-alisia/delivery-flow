import fs from "node:fs";
import path from "node:path";

export const PLAN_CLASS_STATUSES = ["new", "modified", "existing"];
export const PLAN_MODEL_TYPES = ["record", "enum", "interface", "sealed-interface"];
export const PLAN_MODEL_STATUSES = ["new", "modified"];
export const PLAN_EXAMPLE_TYPES = ["request", "success", "error", "validation-error"];
export const PLAN_SLICE_TEST_LEVELS = ["unit", "integration", "component"];

export function resolvePlanPath(cwd, feature) {
  return path.resolve(cwd, "artifacts", "implementation-plans", `${feature}.plan.json`);
}

export function createInitialPlan(feature) {
  return {
    schemaVersion: "2.0",
    feature,
    createdAt: timestamp(),
    updatedAt: timestamp(),
    revision: 1,
    status: "draft",
    payloadExamples: [],
    validationBoundary: [],
    models: [],
    classes: [],
    compositionStrategy: null,
    sharedInfra: { reused: [], new: [] },
    slices: [],
    testingMatrix: [],
    karate: null,
    archUnit: null
  };
}

export function loadPlan(planPath) {
  if (!fs.existsSync(planPath)) {
    throw new Error(`Plan file does not exist: ${planPath}`);
  }

  const raw = fs.readFileSync(planPath, "utf8");
  return JSON.parse(raw);
}

export function savePlan(planPath, plan) {
  plan.updatedAt = timestamp();
  fs.mkdirSync(path.dirname(planPath), { recursive: true });
  fs.writeFileSync(planPath, `${JSON.stringify(plan, null, 2)}\n`);
}

export function addPlanExample(plan, label, type, body) {
  validatePlanValue(type, PLAN_EXAMPLE_TYPES, "example type");
  plan.payloadExamples.push({ label, type, body });
  return plan.payloadExamples[plan.payloadExamples.length - 1];
}

export function addPlanValidation(plan, rule, boundary, reason) {
  plan.validationBoundary.push({ rule, boundary, reason });
  return plan.validationBoundary[plan.validationBoundary.length - 1];
}

export function addPlanModel(plan, qualifiedName, type, status, justification, opts = {}) {
  validatePlanValue(type, PLAN_MODEL_TYPES, "model type");
  validatePlanValue(status, PLAN_MODEL_STATUSES, "model status");

  const entry = {
    qualifiedName,
    type,
    status,
    justification,
    fields: opts.fields ?? [],
    annotations: opts.annotations ?? [],
    notes: opts.notes ?? null,
    values: opts.values ?? [],
    methods: opts.methods ?? []
  };

  const index = plan.models.findIndex((model) => model.qualifiedName === qualifiedName);
  if (index >= 0) {
    plan.models[index] = entry;
  } else {
    plan.models.push(entry);
  }
  return entry;
}

export function addPlanModelField(plan, qualifiedName, name, type, opts = {}) {
  const model = plan.models.find((entry) => entry.qualifiedName === qualifiedName);
  if (!model) {
    throw new Error(`Model not found: ${qualifiedName}`);
  }

  const field = {
    name,
    type,
    nullable: opts.nullable ?? false,
    defensiveCopy: opts.defensiveCopy ?? false
  };

  const index = model.fields.findIndex((entry) => entry.name === name);
  if (index >= 0) {
    model.fields[index] = field;
  } else {
    model.fields.push(field);
  }
  return field;
}

export function addPlanClass(plan, filePath, status, role) {
  validatePlanValue(status, PLAN_CLASS_STATUSES, "class status");

  const existing = plan.classes.find((entry) => entry.path === filePath);
  if (existing) {
    existing.status = status;
    existing.role = role ?? existing.role;
    return existing;
  }

  const entry = { path: filePath, status, role: role ?? null };
  plan.classes.push(entry);
  return entry;
}

export function addPlanSlice(plan, sliceId, title, goal, opts = {}) {
  const existing = plan.slices.find((slice) => slice.id === sliceId);
  if (existing) {
    existing.title = title ?? existing.title;
    existing.goal = goal ?? existing.goal;
    existing.files = opts.files ?? existing.files;
    if (opts.tests) {
      for (const level of PLAN_SLICE_TEST_LEVELS) {
        if (opts.tests[level]) {
          existing.tests[level] = opts.tests[level];
        }
      }
    }
    if (opts.logging) {
      if (opts.logging.info !== undefined) existing.logging.info = opts.logging.info;
      if (opts.logging.warn !== undefined) existing.logging.warn = opts.logging.warn;
      if (opts.logging.error !== undefined) existing.logging.error = opts.logging.error;
    }
    return existing;
  }

  const entry = {
    id: sliceId,
    title,
    goal,
    files: opts.files ?? [],
    tests: {
      unit: opts.tests?.unit ?? [],
      integration: opts.tests?.integration ?? [],
      component: opts.tests?.component ?? []
    },
    logging: {
      info: opts.logging?.info ?? "None",
      warn: opts.logging?.warn ?? "None",
      error: opts.logging?.error ?? "None"
    }
  };
  plan.slices.push(entry);
  return entry;
}

export function addPlanSliceTest(plan, sliceId, level, test) {
  validatePlanValue(level, PLAN_SLICE_TEST_LEVELS, "test level");
  const slice = plan.slices.find((entry) => entry.id === sliceId);
  if (!slice) {
    throw new Error(`Slice not found: ${sliceId}`);
  }
  slice.tests[level].push(test);
}

export function setPlanSliceLogging(plan, sliceId, opts) {
  const slice = plan.slices.find((entry) => entry.id === sliceId);
  if (!slice) {
    throw new Error(`Slice not found: ${sliceId}`);
  }
  if (opts.info !== undefined) slice.logging.info = opts.info;
  if (opts.warn !== undefined) slice.logging.warn = opts.warn;
  if (opts.error !== undefined) slice.logging.error = opts.error;
}

export function setPlanComposition(plan, approach, description) {
  plan.compositionStrategy = { approach, description };
}

export function setPlanInfra(plan, reused, newInfra) {
  plan.sharedInfra = {
    reused: reused ?? [],
    new: newInfra ?? []
  };
}

export function addPlanTest(plan, level, required, coverage) {
  const entry = { level, required, coverage };
  const index = plan.testingMatrix.findIndex((testLevel) => testLevel.level === level);
  if (index >= 0) {
    plan.testingMatrix[index] = entry;
  } else {
    plan.testingMatrix.push(entry);
  }
  return entry;
}

export function setPlanKarate(plan, opts) {
  plan.karate = {
    featureFile: opts.featureFile,
    scenarios: opts.scenarios ?? [],
    smokeTagged: opts.smokeTagged ?? false,
    runnerUpdated: opts.runnerUpdated ?? false
  };
}

export function setPlanArchunit(plan, opts) {
  plan.archUnit = {
    newRules: opts.newRules ?? [],
    existingRulesReviewed: opts.existingReviewed ?? false
  };
}

export function bumpPlanRevision(plan) {
  plan.revision += 1;
  plan.status = "draft";
  plan.payloadExamples = [];
  plan.validationBoundary = [];
  plan.models = [];
  plan.classes = [];
  plan.compositionStrategy = null;
  plan.sharedInfra = { reused: [], new: [] };
  plan.slices = [];
  plan.testingMatrix = [];
  plan.karate = null;
  plan.archUnit = null;
}

export function validatePlan(plan) {
  const issues = [];

  if (plan.models.length === 0) {
    issues.push("No models registered.");
  }
  if (plan.classes.length === 0) {
    issues.push("No classes registered.");
  }
  if (plan.slices.length === 0) {
    issues.push("No slices registered.");
  }
  for (const model of plan.models) {
    if (typeof model.justification !== "string" || !model.justification.trim()) {
      issues.push(`Model ${model.qualifiedName} missing justification.`);
    }
    if (model.type === "record" && model.fields.length === 0) {
      issues.push(`Record model ${model.qualifiedName} has no fields.`);
    }
  }
  for (const slice of plan.slices) {
    const totalTests = slice.tests.unit.length + slice.tests.integration.length + slice.tests.component.length;
    if (totalTests === 0 && slice.files.some((file) => file.endsWith(".java"))) {
      issues.push(`Slice ${slice.id} "${slice.title}" has Java files but no tests.`);
    }
  }

  return {
    valid: issues.length === 0,
    issues
  };
}

export function buildPlanSummary(plan) {
  return {
    feature: plan.feature,
    revision: plan.revision,
    status: plan.status,
    updatedAt: plan.updatedAt,
    modelCount: plan.models.length,
    classCount: plan.classes.length,
    sliceCount: plan.slices.length,
    exampleCount: plan.payloadExamples.length,
    validationRuleCount: plan.validationBoundary.length,
    testLevels: plan.testingMatrix.length,
    hasComposition: plan.compositionStrategy !== null,
    hasKarate: plan.karate !== null,
    hasArchUnit: plan.archUnit !== null,
    models: plan.models.map((model) => ({
      qualifiedName: model.qualifiedName,
      type: model.type,
      status: model.status
    })),
    classes: plan.classes.map((entry) => ({ path: entry.path, status: entry.status })),
    slices: plan.slices.map((slice) => ({ id: slice.id, title: slice.title }))
  };
}

export function getPlanSection(plan, section) {
  const sections = {
    payloadExamples: plan.payloadExamples,
    validationBoundary: plan.validationBoundary,
    models: plan.models,
    classes: plan.classes,
    compositionStrategy: plan.compositionStrategy,
    sharedInfra: plan.sharedInfra,
    slices: plan.slices,
    testingMatrix: plan.testingMatrix,
    karate: plan.karate,
    archUnit: plan.archUnit
  };
  if (!(section in sections)) {
    throw new Error(`Unknown plan section: ${section}. Available: ${Object.keys(sections).join(", ")}`);
  }
  return sections[section];
}

function validatePlanValue(value, allowedValues, label) {
  if (!allowedValues.includes(value)) {
    throw new Error(`${label} must be one of: ${allowedValues.join(", ")}`);
  }
}

function timestamp() {
  return new Date().toISOString();
}
