export function validatePlanReadiness(plan) {
  const issues = [];

  if ((plan.slices ?? []).length === 0) {
    issues.push("Readiness check failed: at least one slice is required.");
  }

  for (const slice of plan.slices ?? []) {
    if ((slice.units ?? []).length === 0) {
      issues.push(`Readiness check failed: slice '${slice.id}' must include at least one unit.`);
    }

    if ((slice.doneWhen ?? []).length === 0) {
      issues.push(`Readiness check failed: slice '${slice.id}' must include doneWhen criteria.`);
    }

    for (const unit of slice.units ?? []) {
      if ((unit.tests?.levels ?? []).length === 0) {
        issues.push(`Readiness check failed: unit '${unit.id}' must include at least one test level.`);
      }

      if (typeof unit.tests?.notes !== "string" || unit.tests.notes.trim().length === 0) {
        issues.push(`Readiness check failed: unit '${unit.id}' must include test notes.`);
      }
    }
  }

  if ((plan.finalVerification?.requiredGates ?? []).length === 0) {
    issues.push("Readiness check failed: finalVerification.requiredGates must include at least one gate.");
  }

  return {
    valid: issues.length === 0,
    issues
  };
}
