export function timestamp() {
  return new Date().toISOString();
}

export function validateValue(value, allowedValues, label) {
  if (!allowedValues.includes(value)) {
    throw new Error(`${label} must be one of: ${allowedValues.join(", ")}`);
  }
}

export function parsePositiveInteger(value, label) {
  const parsed = Number.parseInt(value, 10);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${label} must be a positive integer.`);
  }

  return parsed;
}

export function nextEventId(events) {
  if (events.length === 0) {
    return 1;
  }

  return events[events.length - 1].id + 1;
}

export function nextRiskId(risks) {
  if (risks.length === 0) {
    return 1;
  }

  return risks[risks.length - 1].id + 1;
}

export function nextFindingId(findings) {
  if (findings.length === 0) {
    return 1;
  }

  return findings[findings.length - 1].id + 1;
}
