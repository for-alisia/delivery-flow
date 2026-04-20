import { spawnSync } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { timestamp } from "./common.mjs";
import { createCheckEntry } from "./schema.mjs";

const DEFAULT_TIMEOUT_MS = 300_000;
const OUTPUT_TAIL_LINES = 80;

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
    sourceFingerprint: options.sourceFingerprint ?? null
  };

  return state.checks[name];
}

export function appendChangedFiles(state, files) {
  const next = new Set(state.changes.files);

  for (const file of files) {
    next.add(file);
  }

  state.changes.files = Array.from(next).sort();
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
    setCheck(state, name, "FAIL", {
      by,
      command: scriptRelPath,
      details: `Execution error: ${error.message}`
    });
    return {
      check: name,
      status: "FAIL",
      exitCode: null,
      signal: null,
      durationMs,
      error: error.message,
      outputTail: [],
      command: scriptRelPath
    };
  }

  const durationMs = Date.now() - startTime;
  const timedOut = result.signal === "SIGTERM";
  const combined = [result.stdout ?? "", result.stderr ?? ""].join("\n");
  const outputTail = tailLines(combined, OUTPUT_TAIL_LINES);

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

  setCheck(state, name, status, {
    by,
    command: scriptRelPath,
    details,
    sourceFingerprint: fingerprint
  });

  const PASS_OUTPUT_TAIL_LINES = 5;
  const returnTail = status === "PASS" ? tailLines(combined, PASS_OUTPUT_TAIL_LINES) : outputTail;

  return {
    check: name,
    status,
    exitCode: result.status,
    signal: result.signal ?? null,
    durationMs,
    timedOut: timedOut || false,
    outputTail: returnTail,
    command: scriptRelPath,
    sourceFingerprint: fingerprint
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
  const lines = text.split("\n");
  const start = Math.max(0, lines.length - maxLines);
  return lines.slice(start).filter(line => line.length > 0);
}
