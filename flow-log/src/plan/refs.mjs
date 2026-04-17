export function collectPlanIds(plan) {
  return {
    flow: collectIds(plan.implementationFlow),
    slice: collectIds(plan.slices),
    model: collectIds(plan.models),
    class: collectIds(plan.classes),
    example: collectIds(plan.contractExamples),
    validationRule: collectIds(plan.validationRules),
    decision: collectIds(plan.designDecisions)
  };
}

export function collectAllReferences(plan) {
  const references = [];

  for (const step of plan.implementationFlow ?? []) {
    references.push(
      createRef("implementationFlow.slice", step.id, "slice", step.slice)
    );
  }

  for (const slice of plan.slices ?? []) {
    for (const flowId of slice.flowSteps ?? []) {
      references.push(createRef("slices.flowSteps", slice.id, "flow", flowId));
    }

    for (const modelId of slice.covers?.models ?? []) {
      references.push(createRef("slices.covers.models", slice.id, "model", modelId));
    }

    for (const classId of slice.covers?.classes ?? []) {
      references.push(createRef("slices.covers.classes", slice.id, "class", classId));
    }

    for (const ruleId of slice.covers?.validationRules ?? []) {
      references.push(createRef("slices.covers.validationRules", slice.id, "validationRule", ruleId));
    }

    for (const exampleId of slice.covers?.examples ?? []) {
      references.push(createRef("slices.covers.examples", slice.id, "example", exampleId));
    }

    for (const decisionId of slice.covers?.decisions ?? []) {
      references.push(createRef("slices.covers.decisions", slice.id, "decision", decisionId));
    }

    for (const dependsOnId of slice.dependsOn ?? []) {
      references.push(createRef("slices.dependsOn", slice.id, "slice", dependsOnId));
    }
  }

  for (const model of plan.models ?? []) {
    for (const sliceId of model.ownedBySlices ?? []) {
      references.push(createRef("models.ownedBySlices", model.id, "slice", sliceId));
    }
  }

  for (const classEntry of plan.classes ?? []) {
    for (const sliceId of classEntry.ownedBySlices ?? []) {
      references.push(createRef("classes.ownedBySlices", classEntry.id, "slice", sliceId));
    }
  }

  for (const rule of plan.validationRules ?? []) {
    for (const sliceId of rule.appliesToSlices ?? []) {
      references.push(createRef("validationRules.appliesToSlices", rule.id, "slice", sliceId));
    }
  }

  for (const verification of plan.verification?.slices ?? []) {
    references.push(createRef("verification.slices.slice", "verification", "slice", verification.slice));
  }

  return references;
}

export function collectAllKnownIds(plan) {
  const byType = collectPlanIds(plan);
  const all = new Set();

  for (const ids of Object.values(byType)) {
    for (const id of ids) {
      all.add(id);
    }
  }

  return all;
}

function collectIds(entries = []) {
  const ids = new Set();

  for (const entry of entries) {
    if (entry && typeof entry.id === "string" && entry.id.trim()) {
      ids.add(entry.id);
    }
  }

  return ids;
}

function createRef(field, sourceId, targetType, targetId) {
  return {
    field,
    sourceId,
    targetType,
    targetId
  };
}
