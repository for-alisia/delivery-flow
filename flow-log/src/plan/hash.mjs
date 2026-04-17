import crypto from "node:crypto";

export function computePlanHash(plan) {
  const normalized = normalizeForHash(plan);
  const payload = stableStringify(normalized);
  return crypto.createHash("sha256").update(payload).digest("hex");
}

export function plansEquivalentForAcceptance(leftPlan, rightPlan) {
  const left = stableStringify(normalizeForDiff(leftPlan));
  const right = stableStringify(normalizeForDiff(rightPlan));
  return left === right;
}

export function normalizeForHash(plan) {
  const clone = structuredClone(plan);
  delete clone.hash;
  return clone;
}

export function normalizeForDiff(plan) {
  const clone = normalizeForHash(plan);
  delete clone.revision;
  return clone;
}

export function stableStringify(value) {
  return JSON.stringify(sortValue(value));
}

function sortValue(value) {
  if (Array.isArray(value)) {
    return value.map((entry) => sortValue(entry));
  }

  if (value && typeof value === "object") {
    const sorted = {};
    const keys = Object.keys(value).sort((left, right) => left.localeCompare(right));

    for (const key of keys) {
      sorted[key] = sortValue(value[key]);
    }

    return sorted;
  }

  return value;
}
