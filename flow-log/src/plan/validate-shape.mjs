import {
  PLAN_CLASS_STATUSES,
  PLAN_EXAMPLE_TYPES,
  PLAN_MODEL_KINDS,
  PLAN_MODEL_STATUSES,
  PLAN_V3_SCHEMA_VERSION
} from "./schema.mjs";
import { collectAllReferences, collectPlanIds } from "./refs.mjs";

export function validatePlanShape(plan) {
  const issues = [];

  validateTopLevel(plan, issues);
  if (issues.length > 0) {
    return { valid: false, issues };
  }

  validateContractExamples(plan.contractExamples, issues);
  validateValidationRules(plan.validationRules, issues);
  validateDesignDecisions(plan.designDecisions, issues);
  validateImplementationFlow(plan.implementationFlow, issues);
  validateModels(plan.models, issues);
  validateClasses(plan.classes, issues);
  validateSlices(plan.slices, issues);
  validateVerification(plan.verification, issues);

  const ids = collectPlanIds(plan);
  validateCrossReferences(plan, ids, issues);
  validateFlowOrderAndCoverage(plan, ids, issues);

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

  const required = [
    "schemaVersion",
    "feature",
    "revision",
    "status",
    "scope",
    "implementationFlow",
    "contractExamples",
    "validationRules",
    "designDecisions",
    "models",
    "classes",
    "slices",
    "verification"
  ];

  for (const key of required) {
    if (!(key in plan)) {
      issues.push(`Missing top-level key '${key}'.`);
    }
  }

  if (typeof plan.schemaVersion !== "string" || plan.schemaVersion !== PLAN_V3_SCHEMA_VERSION) {
    issues.push(`schemaVersion must be '${PLAN_V3_SCHEMA_VERSION}'.`);
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
    requireStringArray(plan.scope, "inScope", "scope", issues, false);
    requireStringArray(plan.scope, "outOfScope", "scope", issues, false);
    requireStringArray(plan.scope, "constraints", "scope", issues, false);
  }

  requireArray(plan, "implementationFlow", "plan", issues);
  requireArray(plan, "contractExamples", "plan", issues);
  requireArray(plan, "validationRules", "plan", issues);
  requireArray(plan, "designDecisions", "plan", issues);
  requireArray(plan, "models", "plan", issues);
  requireArray(plan, "classes", "plan", issues);
  requireArray(plan, "slices", "plan", issues);

  if (!isObject(plan.verification)) {
    issues.push("verification must be an object.");
  }
}

function validateContractExamples(examples, issues) {
  const seen = new Set();

  examples.forEach((example, index) => {
    const context = `contractExamples[${index}]`;
    requireObject(example, context, issues);
    requireId(example, context, seen, issues);
    requireString(example, "label", context, issues, true);
    requireEnum(example, "type", context, PLAN_EXAMPLE_TYPES, issues);
    if (!("body" in example)) {
      issues.push(`${context}.body is required.`);
    }
  });
}

function validateValidationRules(rules, issues) {
  const seen = new Set();

  rules.forEach((rule, index) => {
    const context = `validationRules[${index}]`;
    requireObject(rule, context, issues);
    requireId(rule, context, seen, issues);
    requireString(rule, "rule", context, issues, true);
    requireString(rule, "owner", context, issues, true);
    requireString(rule, "reason", context, issues, true);
    requireStringArray(rule, "appliesToSlices", context, issues, false);
  });
}

function validateDesignDecisions(decisions, issues) {
  const seen = new Set();

  decisions.forEach((decision, index) => {
    const context = `designDecisions[${index}]`;
    requireObject(decision, context, issues);
    requireId(decision, context, seen, issues);
    requireString(decision, "title", context, issues, true);
    requireString(decision, "decision", context, issues, true);
    requireString(decision, "rationale", context, issues, true);
    requireStringArray(decision, "alternatives", context, issues, false);
  });
}

function validateImplementationFlow(steps, issues) {
  const seen = new Set();

  steps.forEach((step, index) => {
    const context = `implementationFlow[${index}]`;
    requireObject(step, context, issues);
    requireId(step, context, seen, issues);

    if (!Number.isInteger(step.order) || step.order <= 0) {
      issues.push(`${context}.order must be a positive integer.`);
    }

    requireString(step, "slice", context, issues, true);
    requireString(step, "layer", context, issues, true);
    requireString(step, "component", context, issues, true);
    requireString(step, "responsibility", context, issues, true);
    requireString(step, "constitutionReason", context, issues, true);
    requireStringArray(step, "outputs", context, issues, true);
  });
}

function validateModels(models, issues) {
  const seen = new Set();

  models.forEach((model, index) => {
    const context = `models[${index}]`;
    requireObject(model, context, issues);
    requireId(model, context, seen, issues);
    requireString(model, "qualifiedName", context, issues, true);
    requireEnum(model, "kind", context, PLAN_MODEL_KINDS, issues);
    requireEnum(model, "status", context, PLAN_MODEL_STATUSES, issues);
    requireString(model, "purpose", context, issues, true);
    requireString(model, "placementJustification", context, issues, true);
    requireStringArray(model, "ownedBySlices", context, issues, true);
  });
}

function validateClasses(classes, issues) {
  const seen = new Set();

  classes.forEach((classEntry, index) => {
    const context = `classes[${index}]`;
    requireObject(classEntry, context, issues);
    requireId(classEntry, context, seen, issues);
    requireString(classEntry, "path", context, issues, true);
    requireEnum(classEntry, "status", context, PLAN_CLASS_STATUSES, issues);
    requireString(classEntry, "role", context, issues, true);
    requireStringArray(classEntry, "ownedBySlices", context, issues, true);
  });
}

function validateSlices(slices, issues) {
  const seen = new Set();

  slices.forEach((slice, index) => {
    const context = `slices[${index}]`;
    requireObject(slice, context, issues);
    requireId(slice, context, seen, issues);
    requireString(slice, "title", context, issues, true);
    requireString(slice, "goal", context, issues, true);
    requireStringArray(slice, "dependsOn", context, issues, true);
    requireStringArray(slice, "flowSteps", context, issues, true);

    if (!isObject(slice.covers)) {
      issues.push(`${context}.covers must be an object.`);
    } else {
      requireStringArray(slice.covers, "models", `${context}.covers`, issues, true);
      requireStringArray(slice.covers, "classes", `${context}.covers`, issues, true);
      requireStringArray(slice.covers, "validationRules", `${context}.covers`, issues, true);
      requireStringArray(slice.covers, "examples", `${context}.covers`, issues, true);
      requireStringArray(slice.covers, "decisions", `${context}.covers`, issues, true);
    }

    requireStringArray(slice, "implementationTasks", context, issues, true);
    requireStringArray(slice, "doneWhen", context, issues, true);

    if (!isObject(slice.tests)) {
      issues.push(`${context}.tests must be an object.`);
    } else {
      requireStringArray(slice.tests, "unit", `${context}.tests`, issues, true);
      requireStringArray(slice.tests, "integration", `${context}.tests`, issues, true);
      requireStringArray(slice.tests, "component", `${context}.tests`, issues, true);
    }

    if (!isObject(slice.logging)) {
      issues.push(`${context}.logging must be an object.`);
    } else {
      requireStringArray(slice.logging, "info", `${context}.logging`, issues, true);
      requireStringArray(slice.logging, "warn", `${context}.logging`, issues, true);
      requireStringArray(slice.logging, "error", `${context}.logging`, issues, true);
    }

    if (slice.flags !== undefined) {
      if (!isObject(slice.flags)) {
        issues.push(`${context}.flags must be an object when provided.`);
      } else {
        requireBoolean(slice.flags, "contractChanges", `${context}.flags`, issues, false);
        requireBoolean(slice.flags, "validationSensitive", `${context}.flags`, issues, false);
      }
    }
  });
}

function validateVerification(verification, issues) {
  if (!isObject(verification)) {
    return;
  }

  requireArray(verification, "slices", "verification", issues);
  requireArray(verification, "finalGates", "verification", issues);

  (verification.slices ?? []).forEach((entry, index) => {
    const context = `verification.slices[${index}]`;
    requireObject(entry, context, issues);
    requireString(entry, "slice", context, issues, true);
    requireStringArray(entry, "checks", context, issues, true);
    requireString(entry, "evidence", context, issues, true);
  });

  (verification.finalGates ?? []).forEach((entry, index) => {
    const context = `verification.finalGates[${index}]`;
    requireObject(entry, context, issues);
    requireString(entry, "gate", context, issues, true);
    requireString(entry, "owner", context, issues, true);
    requireBoolean(entry, "required", context, issues, true);
    requireString(entry, "evidence", context, issues, true);
  });
}

function validateCrossReferences(plan, ids, issues) {
  const refs = collectAllReferences(plan);

  for (const ref of refs) {
    const targetSet = ids[ref.targetType];

    if (!targetSet) {
      issues.push(`Unknown reference type '${ref.targetType}' from ${ref.field}.`);
      continue;
    }

    if (!isNonEmptyString(ref.targetId) || !targetSet.has(ref.targetId)) {
      issues.push(
        `${ref.field} in '${ref.sourceId}' references missing ${ref.targetType} id '${ref.targetId}'.`
      );
    }
  }
}

function validateFlowOrderAndCoverage(plan, ids, issues) {
  const orders = [];

  for (const step of plan.implementationFlow) {
    if (Number.isInteger(step.order) && step.order > 0) {
      orders.push(step.order);
    }
  }

  if (orders.length > 0) {
    const uniqueOrders = new Set(orders);
    if (uniqueOrders.size !== orders.length) {
      issues.push("implementationFlow.order values must be unique.");
    }

    const sorted = [...uniqueOrders].sort((left, right) => left - right);
    if (sorted[0] !== 1) {
      issues.push("implementationFlow.order must start at 1.");
    }

    for (let index = 0; index < sorted.length; index += 1) {
      const expected = index + 1;
      if (sorted[index] !== expected) {
        issues.push("implementationFlow.order values must be contiguous.");
        break;
      }
    }
  }

  for (const slice of plan.slices) {
    if ((slice.flowSteps ?? []).length === 0) {
      issues.push(`Slice '${slice.id}' must reference at least one flow step.`);
    }
  }

  const flowOwnershipCounts = new Map();
  const flowById = new Map();

  for (const step of plan.implementationFlow) {
    flowById.set(step.id, step);
  }

  for (const slice of plan.slices) {
    for (const flowId of slice.flowSteps ?? []) {
      flowOwnershipCounts.set(flowId, (flowOwnershipCounts.get(flowId) ?? 0) + 1);

      const step = flowById.get(flowId);
      if (step && step.slice !== slice.id) {
        issues.push(`Flow '${flowId}' belongs to slice '${step.slice}' but is referenced by slice '${slice.id}'.`);
      }
    }
  }

  for (const flowId of ids.flow) {
    const count = flowOwnershipCounts.get(flowId) ?? 0;

    if (count !== 1) {
      issues.push(`Flow '${flowId}' must belong to exactly one slice (found ${count}).`);
    }
  }
}

function requireObject(value, context, issues) {
  if (!isObject(value)) {
    issues.push(`${context} must be an object.`);
  }
}

function requireArray(value, key, context, issues) {
  if (!Array.isArray(value?.[key])) {
    issues.push(`${context}.${key} must be an array.`);
  }
}

function requireId(value, context, seen, issues) {
  requireString(value, "id", context, issues, true);

  if (isNonEmptyString(value?.id)) {
    if (seen.has(value.id)) {
      issues.push(`${context}.id '${value.id}' is duplicated in this section.`);
    }
    seen.add(value.id);
  }
}

function requireString(value, key, context, issues, required) {
  if (!(key in value)) {
    if (required) {
      issues.push(`${context}.${key} is required.`);
    }
    return;
  }

  if (typeof value[key] !== "string" || !value[key].trim()) {
    issues.push(`${context}.${key} must be a non-empty string.`);
  }
}

function requireStringArray(value, key, context, issues, required) {
  if (!(key in value)) {
    if (required) {
      issues.push(`${context}.${key} is required.`);
    }
    return;
  }

  if (!Array.isArray(value[key])) {
    issues.push(`${context}.${key} must be an array of strings.`);
    return;
  }

  value[key].forEach((item, index) => {
    if (typeof item !== "string" || !item.trim()) {
      issues.push(`${context}.${key}[${index}] must be a non-empty string.`);
    }
  });
}

function requireEnum(value, key, context, allowedValues, issues) {
  requireString(value, key, context, issues, true);

  if (typeof value?.[key] === "string" && !allowedValues.includes(value[key])) {
    issues.push(`${context}.${key} must be one of: ${allowedValues.join(", ")}.`);
  }
}

function requireBoolean(value, key, context, issues, required) {
  if (!(key in value)) {
    if (required) {
      issues.push(`${context}.${key} is required.`);
    }
    return;
  }

  if (typeof value[key] !== "boolean") {
    issues.push(`${context}.${key} must be boolean.`);
  }
}

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}
