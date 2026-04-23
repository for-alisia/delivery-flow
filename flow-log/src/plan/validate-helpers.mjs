export function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

export function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

export function requireObject(value, context, issues) {
  if (!isObject(value)) {
    issues.push(`${context} must be an object.`);
  }
}

export function requireArray(value, key, context, issues) {
  if (!Array.isArray(value?.[key])) {
    issues.push(`${context}.${key} must be an array.`);
  }
}

export function requireId(value, context, seen, issues) {
  requireString(value, "id", context, issues, true);

  if (isNonEmptyString(value?.id)) {
    if (seen.has(value.id)) {
      issues.push(`${context}.id '${value.id}' is duplicated.`);
    }
    seen.add(value.id);
  }
}

export function requireString(value, key, context, issues, required) {
  if (!(key in value)) {
    if (required) {
      issues.push(`${context}.${key} is required.`);
    }
    return;
  }

  if (!isNonEmptyString(value[key])) {
    issues.push(`${context}.${key} must be a non-empty string.`);
  }
}

export function requireStringArray(value, key, context, issues, required) {
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
    if (!isNonEmptyString(item)) {
      issues.push(`${context}.${key}[${index}] must be a non-empty string.`);
    }
  });
}

export function requireEnum(value, key, context, allowedValues, issues) {
  requireString(value, key, context, issues, true);

  if (typeof value?.[key] === "string" && !allowedValues.includes(value[key])) {
    issues.push(`${context}.${key} must be one of: ${allowedValues.join(", ")}.`);
  }
}
