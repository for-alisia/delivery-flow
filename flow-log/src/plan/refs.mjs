export function collectPlanIds(plan) {
  return {
    slice: collectIds(plan.slices ?? []),
    unit: collectNestedIds(plan.slices ?? [], "units"),
    rule: collectIds(plan.sharedRules ?? []),
    decision: collectIds(plan.sharedDecisions ?? [])
  };
}

export function collectAllReferences(plan) {
  const references = [];

  for (const rule of plan.sharedRules ?? []) {
    for (const sliceId of rule.appliesTo ?? []) {
      references.push(createRef("sharedRules.appliesTo", rule.id, "slice", sliceId));
    }
  }

  for (const decision of plan.sharedDecisions ?? []) {
    for (const sliceId of decision.appliesTo ?? []) {
      references.push(createRef("sharedDecisions.appliesTo", decision.id, "slice", sliceId));
    }
  }

  for (const slice of plan.slices ?? []) {
    for (const dependsOnId of slice.dependsOn ?? []) {
      references.push(createRef("slices.dependsOn", slice.id, "slice", dependsOnId));
    }
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

function collectNestedIds(entries = [], key) {
  const ids = new Set();

  for (const entry of entries) {
    for (const nestedEntry of entry?.[key] ?? []) {
      if (nestedEntry && typeof nestedEntry.id === "string" && nestedEntry.id.trim()) {
        ids.add(nestedEntry.id);
      }
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
