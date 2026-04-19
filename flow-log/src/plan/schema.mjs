export const PLAN_V3_SCHEMA_VERSION = "3.0";

export const PLAN_EXAMPLE_TYPES = ["request", "success", "error", "validation-error"];
export const PLAN_MODEL_KINDS = ["record", "enum", "interface", "sealed-interface", "class"];
export const PLAN_MODEL_STATUSES = ["new", "modified", "existing"];
export const PLAN_CLASS_STATUSES = ["new", "modified", "existing"];
export const PLAN_SLICE_TEST_LEVELS = ["unit", "integration", "component"];

export function createInitialPlan(feature) {
  return {
    schemaVersion: PLAN_V3_SCHEMA_VERSION,
    feature,
    revision: 1,
    status: "draft",
    scope: {
      purpose: "",
      inScope: [],
      outOfScope: [],
      constraints: []
    },
    implementationFlow: [],
    contractExamples: [],
    validationRules: [],
    designDecisions: [],
    models: [],
    classes: [],
    slices: [],
    verification: {
      slices: [],
      finalGates: []
    },
    hash: null
  };
}

export function createDraftFromPlan(plan) {
  return structuredClone(plan);
}

export function isV3Plan(plan) {
  return plan?.schemaVersion === PLAN_V3_SCHEMA_VERSION;
}

export function assertV3Plan(plan, contextLabel = "plan") {
  if (!isV3Plan(plan)) {
    const version = typeof plan?.schemaVersion === "string" ? plan.schemaVersion : "unknown";
    throw new Error(`${contextLabel} must use schemaVersion '${PLAN_V3_SCHEMA_VERSION}', found '${version}'.`);
  }
}
