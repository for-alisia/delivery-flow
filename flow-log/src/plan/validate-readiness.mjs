export function validatePlanReadiness(plan) {
  const issues = [];

  if ((plan.slices ?? []).length === 0) {
    issues.push("Readiness check failed: at least one slice is required.");
  }

  if ((plan.implementationFlow ?? []).length === 0) {
    issues.push("Readiness check failed: at least one implementation flow step is required.");
  }

  for (const model of plan.models ?? []) {
    if ((model.status === "new" || model.status === "modified") && (model.ownedBySlices ?? []).length === 0) {
      issues.push(`Readiness check failed: model '${model.id}' must be owned by at least one slice.`);
    }
  }

  for (const classEntry of plan.classes ?? []) {
    if ((classEntry.status === "new" || classEntry.status === "modified") && (classEntry.ownedBySlices ?? []).length === 0) {
      issues.push(`Readiness check failed: class '${classEntry.id}' must be owned by at least one slice.`);
    }
  }

  for (const slice of plan.slices ?? []) {
    if ((slice.implementationTasks ?? []).length === 0) {
      issues.push(`Readiness check failed: slice '${slice.id}' must include implementationTasks.`);
    }

    if ((slice.doneWhen ?? []).length === 0) {
      issues.push(`Readiness check failed: slice '${slice.id}' must include doneWhen criteria.`);
    }

    const tests = slice.tests ?? { unit: [], integration: [], component: [] };
    const totalTests = (tests.unit?.length ?? 0) + (tests.integration?.length ?? 0) + (tests.component?.length ?? 0);
    if (totalTests === 0) {
      issues.push(`Readiness check failed: slice '${slice.id}' must include at least one test entry.`);
    }

    if (slice.flags?.contractChanges === true && (slice.covers?.examples?.length ?? 0) === 0) {
      issues.push(`Readiness check failed: contract-changing slice '${slice.id}' must reference at least one contract example.`);
    }

    if (slice.flags?.validationSensitive === true && (slice.covers?.validationRules?.length ?? 0) === 0) {
      issues.push(`Readiness check failed: validation-sensitive slice '${slice.id}' must reference at least one validation rule.`);
    }
  }

  const coveredSlices = new Set((plan.verification?.slices ?? []).map((entry) => entry.slice));
  for (const slice of plan.slices ?? []) {
    if (!coveredSlices.has(slice.id)) {
      issues.push(`Readiness check failed: verification does not cover slice '${slice.id}'.`);
    }
  }

  return {
    valid: issues.length === 0,
    issues
  };
}
