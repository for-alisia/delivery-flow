import { REVIEW_NAMES, validateValue } from "./log-state.mjs";

export function parseArgs(argv) {
  const positionals = [];
  const flags = {};

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];

    if (!token.startsWith("--")) {
      positionals.push(token);
      continue;
    }

    const key = token.slice(2);
    const next = argv[index + 1];

    if (!next || next.startsWith("--")) {
      assignFlag(flags, key, true);
      continue;
    }

    assignFlag(flags, key, next);
    index += 1;
  }

  return { positionals, flags };
}

export function requiredFlag(parsed, name) {
  const value = parsed.flags[name];
  if (value === undefined) {
    throw new Error(`Missing required flag: --${name}`);
  }
  return value;
}

export function optionalFlag(parsed, name) {
  return parsed.flags[name];
}

export function normalizeArray(value) {
  if (value === undefined) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
}

export function resolveReviewName(parsed) {
  const value = optionalFlag(parsed, "name") ?? optionalFlag(parsed, "phase");

  if (value === undefined) {
    throw new Error("Missing required flag: --name");
  }

  const normalized = REVIEW_ALIASES[value] ?? value;
  validateValue(normalized, REVIEW_NAMES, "review name");
  return normalized;
}

export function parsePositiveInteger(value, label) {
  const parsed = Number.parseInt(value, 10);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${label} must be a positive integer.`);
  }

  return parsed;
}

function assignFlag(flags, key, value) {
  if (!(key in flags)) {
    flags[key] = value;
    return;
  }

  if (Array.isArray(flags[key])) {
    flags[key].push(value);
    return;
  }

  flags[key] = [flags[key], value];
}

const REVIEW_ALIASES = {
  phase1: "architectureReview",
  phase2: "codeReview"
};
