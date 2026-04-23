import { collectAllReferences, collectPlanIds } from "./refs.mjs";
import { PLAN_SCHEMA_VERSION, PLAN_TEST_LEVELS, PLAN_UNIT_KINDS, PLAN_UNIT_STATUSES } from "./schema.mjs";
import {
  isNonEmptyString,
  isObject,
  requireArray,
  requireEnum,
  requireId,
  requireObject,
  requireString,
  requireStringArray
} from "./validate-helpers.mjs";

export function validatePlanShape(plan) {
  const issues = [];

  validateTopLevel(plan, issues);
  if (issues.length > 0) {
    return { valid: false, issues };
  }

  validateSharedRules(plan.sharedRules ?? [], issues);
  validateSharedDecisions(plan.sharedDecisions ?? [], issues);
  validateSlices(plan.slices ?? [], issues);
  validateFinalVerification(plan.finalVerification, issues);
  validateCrossReferences(plan, collectPlanIds(plan), issues);

  return {
    valid: issues.length === 0,
    issues
  };
}

function validateTopLevel(plan, issues) {
  if (!isObject(plan)) {
    issues.push("Plan must be a JSON object.");
    return;
  }

  for (const key of ["schemaVersion", "feature", "revision", "status", "scope", "slices", "finalVerification"]) {
    if (!(key in plan)) {
      issues.push(`Missing top-level key '${key}'.`);
    }
  }

  if (plan.schemaVersion !== PLAN_SCHEMA_VERSION) {
    issues.push(`schemaVersion must be '${PLAN_SCHEMA_VERSION}'.`);
  }

  if (!isNonEmptyString(plan.feature)) {
    issues.push("feature must be a non-empty string.");
  }

  if (!Number.isInteger(plan.revision) || plan.revision <= 0) {
    issues.push("revision must be a positive integer.");
  }

  if (!isNonEmptyString(plan.status)) {
    issues.push("status must be a non-empty string.");
  }

  if (!isObject(plan.scope)) {
    issues.push("scope must be an object.");
  } else {
    requireString(plan.scope, "purpose", "scope", issues, true);
    requireStringArray(plan.scope, "inScope", "scope", issues, true);
    requireStringArray(plan.scope, "outOfScope", "scope", issues, true);
    requireStringArray(plan.scope, "constraints", "scope", issues, false);
  }

  requireArray(plan, "sharedRules", "plan", issues);
  requireArray(plan, "sharedDecisions", "plan", issues);
  requireArray(plan, "slices", "plan", issues);

  if (!isObject(plan.finalVerification)) {
    issues.push("finalVerification must be an object.");
  }
}

function validateSharedRules(rules, issues) {
  const seen = new Set();

  rules.forEach((rule, index) => {
    const context = `sharedRules[${index}]`;
    requireObject(rule, context, issues);
    requireId(rule, context, seen, issues);
    requireString(rule, "rule", context, issues, true);
    requireStringArray(rule, "appliesTo", context, issues, true);
  });
}

function validateSharedDecisions(decisions, issues) {
  const seen = new Set();

  decisions.forEach((decision, index) => {
    const context = `sharedDecisions[${index}]`;
    requireObject(decision, context, issues);
    requireId(decision, context, seen, issues);
    requireString(decision, "title", context, issues, true);
    requireString(decision, "decision", context, issues, true);
    requireString(decision, "rationale", context, issues, true);
    requireStringArray(decision, "appliesTo", context, issues, true);
  });
}

function validateSlices(slices, issues) {
  const seenSlices = new Set();
  const seenUnits = new Set();
  const locationOwners = new Map();

  slices.forEach((slice, index) => {
    const context = `slices[${index}]`;
    requireObject(slice, context, issues);
    requireId(slice, context, seenSlices, issues);
    requireString(slice, "title", context, issues, true);
    requireString(slice, "goal", context, issues, true);
    requireStringArray(slice, "dependsOn", context, issues, true);
    requireStringArray(slice, "readsExisting", context, issues, false);
    requireStringArray(slice, "sliceRules", context, issues, false);
    requireStringArray(slice, "compositionNotes", context, issues, false);
    requireStringArray(slice, "doneWhen", context, issues, true);
    requireArray(slice, "units", context, issues);

    if (slice.contractDependency !== undefined) {
      validateContractDependency(slice.contractDependency, `${context}.contractDependency`, issues);
    }

    (slice.units ?? []).forEach((unit, unitIndex) => {
      validateUnit(unit, `${context}.units[${unitIndex}]`, seenUnits, locationOwners, issues);
    });
  });

  for (const [locationHint, owners] of locationOwners.entries()) {
    if (owners.length > 1) {
      issues.push(`Unit location '${locationHint}' is owned by multiple slices (${owners.join(", ")}). Keep one owning slice per artifact.`);
    }
  }
}

function validateContractDependency(contractDependency, context, issues) {
  requireObject(contractDependency, context, issues);
  requireString(contractDependency, "section", context, issues, true);
  requireStringArray(contractDependency, "notes", context, issues, false);
}

function validateUnit(unit, context, seenUnits, locationOwners, issues) {
  requireObject(unit, context, issues);
  requireId(unit, context, seenUnits, issues);
  requireEnum(unit, "kind", context, PLAN_UNIT_KINDS, issues);
  requireString(unit, "locationHint", context, issues, true);
  requireEnum(unit, "status", context, PLAN_UNIT_STATUSES, issues);
  requireString(unit, "purpose", context, issues, true);
  requireString(unit, "change", context, issues, true);
  requireStringArray(unit, "contractDetails", context, issues, false);

  if (!isObject(unit.tests)) {
    issues.push(`${context}.tests must be an object.`);
  } else {
    requireStringArray(unit.tests, "levels", `${context}.tests`, issues, true);
    requireString(unit.tests, "notes", `${context}.tests`, issues, true);

    (unit.tests.levels ?? []).forEach((level, index) => {
      if (!PLAN_TEST_LEVELS.includes(level)) {
        issues.push(`${context}.tests.levels[${index}] must be one of: ${PLAN_TEST_LEVELS.join(", ")}.`);
      }
    });
  }

  if (unit.loggingNotes !== undefined && !isNonEmptyString(unit.loggingNotes)) {
    issues.push(`${context}.loggingNotes must be a non-empty string when provided.`);
  }

  if (isNonEmptyString(unit.locationHint)) {
    const owner = unit.id.split("-")[0] ?? unit.id;
    locationOwners.set(unit.locationHint, [...(locationOwners.get(unit.locationHint) ?? []), owner]);
  }
}

function validateFinalVerification(finalVerification, issues) {
  if (!isObject(finalVerification)) {
    return;
  }

  requireArray(finalVerification, "requiredGates", "finalVerification", issues);
  requireStringArray(finalVerification, "notes", "finalVerification", issues, false);
}

function validateCrossReferences(plan, ids, issues) {
  for (const ref of collectAllReferences(plan)) {
    const targetSet = ids[ref.targetType];
    if (!targetSet || !targetSet.has(ref.targetId)) {
      issues.push(`${ref.field} in '${ref.sourceId}' references missing ${ref.targetType} id '${ref.targetId}'.`);
    }
  }
}
