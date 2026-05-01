export const PLAN_SCHEMA_VERSION = "4.0";

export const PLAN_UNIT_KINDS = ["java-class", "karate-feature", "archunit-test", "config"];
export const PLAN_UNIT_STATUSES = ["new", "modified", "existing"];
export const PLAN_TEST_LEVELS = ["unit", "integration", "component", "karate", "archunit"];

export function createInitialPlan(feature) {
  return {
    schemaVersion: PLAN_SCHEMA_VERSION,
    feature,
    revision: 1,
    status: "draft",
    scope: {
      purpose: "",
      inScope: [],
      outOfScope: [],
      constraints: []
    },
    sharedRules: [],
    sharedDecisions: [],
    slices: [],
    finalVerification: {
      requiredGates: ["verifyQuick", "finalCheck", "karate"],
      notes: []
    },
    hash: null
  };
}

export function createDraftFromPlan(plan) {
  return structuredClone(plan);
}

export function isPlan(plan) {
  return plan?.schemaVersion === PLAN_SCHEMA_VERSION;
}

export function assertPlan(plan, contextLabel = "plan") {
  if (!isPlan(plan)) {
    const version = typeof plan?.schemaVersion === "string" ? plan.schemaVersion : "unknown";
    throw new Error(`${contextLabel} must use schemaVersion '${PLAN_SCHEMA_VERSION}', found '${version}'.`);
  }
}
