import { spawnSync } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { timestamp } from "./common.mjs";
import { createCheckEntry } from "./schema.mjs";
import { ensureSliceRunsState } from "./slice-runs.mjs";

const DEFAULT_TIMEOUT_MS = 300_000;
const OUTPUT_TAIL_LINES = 80;
const PASS_OUTPUT_TAIL_LINES = 5;
const PERSISTED_OUTPUT_MARKER = "--- output ---";
const DIAGNOSTIC_NOISE_PATTERNS = [
  /warning:\s*setlocale/i,
  /setlocale:\s*LC_/i,
  /cannot change locale/i,
  /^locale:\s*/i
];

const CHECK_SCRIPTS = {
  verifyQuick: "scripts/verify-quick.sh",
  finalCheck: "scripts/final-check.sh",
  karate: "scripts/karate-test.sh"
};

export function setCheck(state, name, status, options = {}) {
  state.checks[name] = {
    status,
    updatedAt: timestamp(),
    updatedBy: options.by ?? null,
    command: options.command ?? null,
    details: options.details ?? null,
    reportPaths: options.reportPaths ?? [],
    logPath: options.logPath ?? null,
    sourceFingerprint: options.sourceFingerprint ?? null
  };

  return state.checks[name];
}

export function appendChangedFiles(state, files) {
  const next = new Set(state.changes.files);
  const sliceRuns = ensureSliceRunsState(state);
  const currentSliceRunFiles = sliceRuns.current
    ? new Set(sliceRuns.current.changedFiles ?? [])
    : null;

  for (const file of files) {
    next.add(file);
    currentSliceRunFiles?.add(file);
  }

  state.changes.files = Array.from(next).sort();

  if (sliceRuns.current) {
    sliceRuns.current.changedFiles = Array.from(currentSliceRunFiles).sort();
  }
}

export function resetChecksForRedCard(state) {
  state.checks.verifyQuick = createCheckEntry();
  state.checks.finalCheck = createCheckEntry();
  state.checks.karate = createCheckEntry();
}

export function summarizeChecks(checks) {
  return {
    verifyQuick: checks.verifyQuick.status,
    finalCheck: checks.finalCheck.status,
    karate: checks.karate.status
  };
}

export function runAndRecordCheck(state, name, cwd, options = {}) {
  const scriptRelPath = options.command ?? CHECK_SCRIPTS[name];
  if (!scriptRelPath) {
    throw new Error(
      `No script mapped for check '${name}'. Provide --command or use one of: ${Object.keys(CHECK_SCRIPTS).join(", ")}`
    );
  }

  const scriptPath = path.resolve(cwd, scriptRelPath);
  const timeoutMs = options.timeout ?? DEFAULT_TIMEOUT_MS;
  const by = options.by ?? "flow-log";
  const startedAt = timestamp();
  const startTime = Date.now();

  let result;
  try {
    result = spawnSync("bash", [scriptPath], {
      cwd,
      timeout: timeoutMs,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
      env: { ...process.env, FORCE_COLOR: "0" }
    });
  } catch (error) {
    const durationMs = Date.now() - startTime;
    const details = `Execution error: ${error.message}`;
    const logPath = persistCheckLog(cwd, state.feature, name, {
      status: "FAIL",
      command: scriptRelPath,
      by,
      startedAt,
      durationMs,
      exitCode: null,
      signal: null,
      details
    }, details);
    setCheck(state, name, "FAIL", {
      by,
      command: scriptRelPath,
      details,
      reportPaths: [logPath],
      logPath
    });
    return {
      check: name,
      status: "FAIL",
      exitCode: null,
      signal: null,
      durationMs,
      error: error.message,
      outputTail: tailLines(redactCheckOutput(details), OUTPUT_TAIL_LINES),
      command: scriptRelPath,
      logPath,
      reportPaths: [logPath]
    };
  }

  const durationMs = Date.now() - startTime;
  const timedOut = result.signal === "SIGTERM";
  const combined = [result.stdout ?? "", result.stderr ?? ""].join("\n");
  const redactedOutput = redactCheckOutput(combined);
  const outputTail = tailLines(redactedOutput, OUTPUT_TAIL_LINES, { omitNoise: true });

  let status;
  let details;
  if (timedOut) {
    status = "FAIL";
    details = `Timed out after ${timeoutMs}ms`;
  } else if (result.status === 0) {
    status = "PASS";
    details = null;
  } else {
    status = "FAIL";
    details = `Exit code ${result.status}`;
  }

  const fingerprint = status === "PASS" ? computeSourceFingerprint(cwd) : null;
  const logPath = persistCheckLog(cwd, state.feature, name, {
    status,
    command: scriptRelPath,
    by,
    startedAt,
    durationMs,
    exitCode: result.status,
    signal: result.signal ?? null,
    timedOut,
    details
  }, redactedOutput);

  setCheck(state, name, status, {
    by,
    command: scriptRelPath,
    details,
    reportPaths: [logPath],
    logPath,
    sourceFingerprint: fingerprint
  });

  const returnTail = status === "PASS"
    ? tailLines(redactedOutput, PASS_OUTPUT_TAIL_LINES, { omitNoise: true })
    : outputTail;

  return {
    check: name,
    status,
    exitCode: result.status,
    signal: result.signal ?? null,
    durationMs,
    timedOut: timedOut || false,
    outputTail: returnTail,
    command: scriptRelPath,
    logPath,
    reportPaths: [logPath],
    sourceFingerprint: fingerprint
  };
}

export function loadPersistedCheckLog(state, name, options = {}) {
  const check = state.checks[name];
  if (!check) {
    throw new Error(`Unknown check: ${name}`);
  }
  if (!check.logPath) {
    throw new Error(`No persisted log found for check '${name}'. Run the check first.`);
  }
  if (!fs.existsSync(check.logPath)) {
    throw new Error(`Persisted check log is missing on disk: ${check.logPath}. Re-run the check.`);
  }

  const content = fs.readFileSync(check.logPath, "utf8");
  const output = extractPersistedOutput(content);
  const allLines = splitNonEmptyLines(output);
  const lines = tailLines(output, options.lines ?? OUTPUT_TAIL_LINES, { omitNoise: true });

  return {
    logPath: check.logPath,
    totalLines: allLines.length,
    truncated: allLines.length > lines.length,
    lines
  };
}

const SOURCE_EXTENSIONS = new Set([
  ".java", ".xml", ".properties", ".yml", ".yaml", ".json", ".feature"
]);

export function computeSourceFingerprint(cwd) {
  const srcDir = path.join(cwd, "flow-orchestrator", "src");
  const pomFile = path.join(cwd, "flow-orchestrator", "pom.xml");
  const entries = [];

  if (fs.existsSync(srcDir)) {
    walkDir(srcDir, entries);
  }
  if (fs.existsSync(pomFile)) {
    const stat = fs.statSync(pomFile);
    entries.push(`${pomFile}:${stat.mtimeMs}`);
  }

  if (entries.length === 0) return null;

  entries.sort();
  return crypto.createHash("sha256").update(entries.join("\n")).digest("hex").slice(0, 16);
}

function walkDir(dir, entries) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkDir(full, entries);
    } else if (SOURCE_EXTENSIONS.has(path.extname(entry.name))) {
      const stat = fs.statSync(full);
      entries.push(`${full}:${stat.mtimeMs}`);
    }
  }
}

export function isCheckStale(check, currentFingerprint) {
  if (check.status !== "PASS") return false;
  if (!check.sourceFingerprint) return false;
  if (!currentFingerprint) return false;
  return check.sourceFingerprint !== currentFingerprint;
}

function tailLines(text, maxLines) {
  if (!text) return [];
  const lines = getTailCandidateLines(text, arguments[2] ?? {});
  const start = Math.max(0, lines.length - maxLines);
  return lines.slice(start);
}

function persistCheckLog(cwd, feature, name, metadata, output) {
  const dir = path.join(cwd, "artifacts", "check-logs", feature);
  fs.mkdirSync(dir, { recursive: true });

  const stamp = (metadata.startedAt ?? timestamp())
    .replace(/:/g, "-")
    .replace(/\./g, "-");
  const filePath = path.join(dir, `${stamp}-${name}-${metadata.status.toLowerCase()}.log`);
  const body = [
    "# flow-log check output",
    `feature: ${feature}`,
    `check: ${name}`,
    `status: ${metadata.status}`,
    `command: ${metadata.command}`,
    `by: ${metadata.by}`,
    `startedAt: ${metadata.startedAt ?? null}`,
    `durationMs: ${metadata.durationMs}`,
    `exitCode: ${metadata.exitCode ?? null}`,
    `signal: ${metadata.signal ?? null}`,
    `timedOut: ${metadata.timedOut === true}`,
    `details: ${metadata.details ?? null}`,
    "",
    PERSISTED_OUTPUT_MARKER,
    output
  ].join("\n");

  fs.writeFileSync(filePath, body, "utf8");
  return filePath;
}

function redactCheckOutput(text) {
  if (!text) return "";

  let sanitized = text;
  const replacements = [
    [/\bBearer\s+[A-Za-z0-9._~+/=-]+\b/gi, "Bearer [REDACTED]"],
    [/\bBasic\s+[A-Za-z0-9+/=]+\b/gi, "Basic [REDACTED]"],
    [/\bglpat-[A-Za-z0-9_-]+\b/g, "[REDACTED_GITLAB_TOKEN]"],
    [/\bgithub_pat_[A-Za-z0-9_]+\b/g, "[REDACTED_GITHUB_TOKEN]"],
    [/\bgh[pousr]_[A-Za-z0-9_]+\b/g, "[REDACTED_GITHUB_TOKEN]"],
    [/("?(?:token|password|passwd|secret|api[_-]?key|access[_-]?key|private[_-]?key|client[_-]?secret|authorization|cookie|session(?:id)?)"?\s*[:=]\s*"?)([^"\n]+)("?)/gi, "$1[REDACTED]$3"],
    [/((?:^|\s)(?:[A-Za-z_][A-Za-z0-9_]*?(?:TOKEN|PASSWORD|PASSWD|SECRET|API_KEY|ACCESS_KEY|PRIVATE_KEY|CLIENT_SECRET))\s*=\s*)([^\s]+)/g, "$1[REDACTED]"]
  ];

  for (const [pattern, replacement] of replacements) {
    sanitized = sanitized.replace(pattern, replacement);
  }

  return sanitized;
}

function extractPersistedOutput(content) {
  const marker = `\n${PERSISTED_OUTPUT_MARKER}\n`;
  const index = content.indexOf(marker);
  return index === -1 ? content : content.slice(index + marker.length);
}

function getTailCandidateLines(text, options = {}) {
  const lines = splitNonEmptyLines(text);
  if (options.omitNoise !== true) {
    return lines;
  }

  const filtered = lines.filter((line) => !isDiagnosticNoiseLine(line));
  return filtered.length > 0 ? filtered : lines;
}

function splitNonEmptyLines(text) {
  return text
    .split("\n")
    .map((line) => line.replace(/\r$/, ""))
    .filter((line) => line.length > 0);
}

function isDiagnosticNoiseLine(line) {
  return DIAGNOSTIC_NOISE_PATTERNS.some((pattern) => pattern.test(line));
}
