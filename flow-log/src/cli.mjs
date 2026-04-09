import fs from "node:fs";
import {
  ARCH_ESCALATION_DECISIONS,
  ARTIFACT_TYPES,
  CHECK_NAMES,
  CHECK_STATUSES,
  EVENT_TYPES,
  FINDING_SEVERITIES,
  PLAN_SLICE_TEST_LEVELS,
  REVIEW_NAMES,
  REVIEW_STATUSES,
  RISK_SEVERITIES,
  addFinding,
  addPlanClass,
  addPlanExample,
  addPlanModel,
  addPlanModelField,
  addPlanSlice,
  addPlanSliceTest,
  addPlanTest,
  addPlanValidation,
  addRisk,
  appendChangedFiles,
  appendEvent,
  assertFileExists,
  buildArchitectureGate,
  buildCodeReviewGate,
  buildPlanSummary,
  buildSignoffReadiness,
  buildStatus,
  buildSummary,
  bumpPlanRevision,
  completeBatch,
  completeFlow,
  createInitialPlan,
  createInitialState,
  ensureFeatureMatches,
  getPlanSection,
  incrementCodeReviewRound,
  incrementReviewRound,
  loadPlan,
  loadState,
  recordArtifactVerification,
  reopenFinding,
  reopenRisk,
  resetChecksForRedCard,
  resolveFinding,
  resolveRisk,
  resolvePlanPath,
  resolveStatePath,
  respondToFinding,
  respondToRisk,
  savePlan,
  saveState,
  setPlanArchunit,
  setPlanComposition,
  setPlanInfra,
  setPlanKarate,
  setPlanSliceLogging,
  startBatch,
  validatePlan,
  validateValue
} from "./state.mjs";

export function runCli(argv, io) {
  try {
    const parsed = parseArgs(argv);
    const result = dispatch(parsed, io.cwd);
    io.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } catch (error) {
    io.stderr.write(
      `${JSON.stringify(
        {
          error: error.message
        },
        null,
        2
      )}\n`
    );
    process.exitCode = 1;
  }
}

function dispatch(parsed, cwd) {
  const [command, subcommand] = parsed.positionals;

  if (!command || command === "help" || command === "--help") {
    return helpResult();
  }

  switch (command) {
    case "create":
      return handleCreate(parsed, cwd);
    case "lock-requirements":
      return handleLockRequirements(parsed, cwd);
    case "register-artifact":
      return handleRegisterArtifact(parsed, cwd, subcommand);
    case "approve-artifact":
      return handleApproveArtifact(parsed, cwd, subcommand);
    case "set-review":
      return handleSetReview(parsed, cwd);
    case "set-check":
      return handleSetCheck(parsed, cwd);
    case "add-change":
      return handleAddChange(parsed, cwd);
    case "add-event":
      return handleAddEvent(parsed, cwd);
    case "start-batch":
      return handleStartBatch(parsed, cwd);
    case "complete-batch":
      return handleCompleteBatch(parsed, cwd);
    case "reset-checks":
      return handleResetChecks(parsed, cwd);
    case "complete":
      return handleComplete(parsed, cwd);
    case "get":
      return handleGet(parsed, cwd);
    case "history":
      return handleHistory(parsed, cwd);
    case "status":
      return handleStatus(parsed, cwd);
    case "summary":
      return handleSummary(parsed, cwd);
    case "readiness":
      return handleReadiness(parsed, cwd, subcommand);
    case "add-risk":
      return handleAddRisk(parsed, cwd);
    case "respond-risk":
      return handleRespondRisk(parsed, cwd);
    case "resolve-risk":
      return handleResolveRisk(parsed, cwd);
    case "reopen-risk":
      return handleReopenRisk(parsed, cwd);
    case "increment-round":
      return handleIncrementRound(parsed, cwd);
    case "architecture-gate":
      return handleArchitectureGate(parsed, cwd);
    case "add-finding":
      return handleAddFinding(parsed, cwd);
    case "respond-finding":
      return handleRespondFinding(parsed, cwd);
    case "resolve-finding":
      return handleResolveFinding(parsed, cwd);
    case "reopen-finding":
      return handleReopenFinding(parsed, cwd);
    case "increment-code-review-round":
      return handleIncrementCodeReviewRound(parsed, cwd);
    case "code-review-gate":
      return handleCodeReviewGate(parsed, cwd);
    case "init-plan":
      return handleInitPlan(parsed, cwd);
    case "add-plan-example":
      return handleAddPlanExample(parsed, cwd);
    case "add-plan-validation":
      return handleAddPlanValidation(parsed, cwd);
    case "add-plan-model":
      return handleAddPlanModel(parsed, cwd);
    case "add-plan-model-field":
      return handleAddPlanModelField(parsed, cwd);
    case "add-plan-class":
      return handleAddPlanClass(parsed, cwd);
    case "add-plan-slice":
      return handleAddPlanSlice(parsed, cwd);
    case "add-plan-slice-test":
      return handleAddPlanSliceTest(parsed, cwd);
    case "set-plan-slice-logging":
      return handleSetPlanSliceLogging(parsed, cwd);
    case "set-plan-composition":
      return handleSetPlanComposition(parsed, cwd);
    case "set-plan-infra":
      return handleSetPlanInfra(parsed, cwd);
    case "add-plan-test":
      return handleAddPlanTest(parsed, cwd);
    case "set-plan-karate":
      return handleSetPlanKarate(parsed, cwd);
    case "set-plan-archunit":
      return handleSetPlanArchunit(parsed, cwd);
    case "revise-plan":
      return handleRevisePlan(parsed, cwd);
    case "validate-plan":
      return handleValidatePlan(parsed, cwd);
    case "plan-summary":
      return handlePlanSummary(parsed, cwd);
    case "plan-get":
      return handlePlanGet(parsed, cwd);
    default:
      throw new Error(`Unknown command: ${command}`);
  }
}

function handleCreate(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const statePath = resolveStatePath(cwd, feature, optionalFlag(parsed, "state-path"));

  if (optionalFlag(parsed, "force") !== true) {
    assertStateDoesNotExist(statePath);
  }

  const state = createInitialState(feature);
  saveState(statePath, state);

  return {
    ok: true,
    command: "create",
    feature,
    statePath
  };
}

function handleLockRequirements(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  state.requirements.locked = true;
  state.requirements.lockedAt = new Date().toISOString();
  state.requirements.lockedBy = optionalFlag(parsed, "by");
  const requestSource = optionalFlag(parsed, "request-source");
  if (requestSource) {
    state.requirements.requestSource = requestSource;
  }
  saveState(statePath, state);

  return {
    ok: true,
    command: "lock-requirements",
    feature,
    statePath,
    requirements: state.requirements
  };
}

function handleRegisterArtifact(parsed, cwd, artifactType) {
  validateValue(artifactType, ARTIFACT_TYPES, "artifact type");
  const artifactPath = requiredFlag(parsed, "path");
  assertFileExists(cwd, artifactPath, `${artifactType} artifact`);

  const { feature, state, statePath } = openState(parsed, cwd);
  const entry = state.artifacts[artifactType];
  entry.path = artifactPath;
  entry.approved = false;
  entry.approvedAt = null;
  entry.approvedBy = null;
  recordArtifactVerification(entry);
  saveState(statePath, state);

  return {
    ok: true,
    command: "register-artifact",
    feature,
    artifactType,
    statePath,
    artifact: entry
  };
}

function handleApproveArtifact(parsed, cwd, artifactType) {
  validateValue(artifactType, ARTIFACT_TYPES, "artifact type");
  const approver = requiredFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const entry = state.artifacts[artifactType];

  if (!entry.path) {
    throw new Error(`${artifactType} artifact path is not registered.`);
  }

  assertFileExists(cwd, entry.path, `${artifactType} artifact`);
  entry.approved = true;
  entry.approvedAt = new Date().toISOString();
  entry.approvedBy = approver;
  recordArtifactVerification(entry);
  saveState(statePath, state);

  return {
    ok: true,
    command: "approve-artifact",
    feature,
    artifactType,
    statePath,
    artifact: entry
  };
}

function handleSetReview(parsed, cwd) {
  const reviewName = resolveReviewName(parsed);
  const status = requiredFlag(parsed, "status");
  validateValue(status, REVIEW_STATUSES, "review status");

  const { feature, state, statePath } = openState(parsed, cwd);
  state.reviews[reviewName] = {
    status,
    reason: optionalFlag(parsed, "reason"),
    updatedAt: new Date().toISOString(),
    updatedBy: optionalFlag(parsed, "by")
  };
  saveState(statePath, state);

  return {
    ok: true,
    command: "set-review",
    feature,
    review: reviewName,
    statePath,
    reviewState: state.reviews[reviewName]
  };
}

function handleSetCheck(parsed, cwd) {
  const name = requiredFlag(parsed, "name");
  const status = requiredFlag(parsed, "status");
  validateValue(name, CHECK_NAMES, "check name");
  validateValue(status, CHECK_STATUSES, "check status");

  const { feature, state, statePath } = openState(parsed, cwd);
  const reportPaths = normalizeArray(optionalFlag(parsed, "report-path"));

  state.checks[name] = {
    status,
    updatedAt: new Date().toISOString(),
    updatedBy: optionalFlag(parsed, "by"),
    command: optionalFlag(parsed, "command"),
    details: optionalFlag(parsed, "details"),
    reportPaths
  };
  saveState(statePath, state);

  return {
    ok: true,
    command: "set-check",
    feature,
    name,
    statePath,
    check: state.checks[name]
  };
}

function handleAddChange(parsed, cwd) {
  const files = normalizeArray(requiredFlag(parsed, "file"));
  const { feature, state, statePath } = openState(parsed, cwd);
  appendChangedFiles(state, files);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-change",
    feature,
    statePath,
    changedFiles: state.changes.files
  };
}

function handleAddEvent(parsed, cwd) {
  const type = requiredFlag(parsed, "type");
  const reason = requiredFlag(parsed, "reason");
  validateValue(type, EVENT_TYPES, "event type");

  let decision;
  if (type === "archEscalationDecision") {
    decision = requiredFlag(parsed, "decision");
    validateValue(decision, ARCH_ESCALATION_DECISIONS, "escalation decision");
  }

  const { feature, state, statePath } = openState(parsed, cwd);
  appendEvent(state, {
    type,
    by: optionalFlag(parsed, "by"),
    reason,
    decision: decision || undefined,
    target: optionalFlag(parsed, "target"),
    relatedCheck: optionalFlag(parsed, "related-check"),
    relatedReview: optionalFlag(parsed, "related-review")
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-event",
    feature,
    statePath,
    event: state.events[state.events.length - 1]
  };
}

function handleGet(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    statePath,
    state
  };
}

function handleStartBatch(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const slices = normalizeArray(optionalFlag(parsed, "slice"));
  const by = optionalFlag(parsed, "by");
  startBatch(state, slices, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "start-batch",
    feature,
    statePath,
    batch: state.batches.current
  };
}

function handleCompleteBatch(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const status = optionalFlag(parsed, "status") ?? "complete";
  completeBatch(state, status);
  saveState(statePath, state);

  const last = state.batches.history[state.batches.history.length - 1];

  return {
    ok: true,
    command: "complete-batch",
    feature,
    statePath,
    completedBatch: last,
    totalBatches: state.batches.total
  };
}

function handleResetChecks(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const reason = optionalFlag(parsed, "reason") ?? "Red card — checks reset for coder retry";
  resetChecksForRedCard(state);
  appendEvent(state, {
    type: "redCard",
    by: optionalFlag(parsed, "by") ?? "TL",
    reason,
    target: optionalFlag(parsed, "target") ?? "JavaCoder"
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "reset-checks",
    feature,
    statePath,
    checks: {
      verifyQuick: state.checks.verifyQuick.status,
      finalCheck: state.checks.finalCheck.status,
      karate: state.checks.karate.status
    },
    eventRecorded: state.events[state.events.length - 1]
  };
}

function handleComplete(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  completeFlow(state);
  saveState(statePath, state);

  return {
    ok: true,
    command: "complete",
    feature,
    statePath,
    timing: state.timing
  };
}

function handleHistory(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  const limitValue = optionalFlag(parsed, "limit");
  const limit = limitValue === undefined ? undefined : parsePositiveInteger(limitValue, "limit");
  const events = limit ? state.events.slice(-limit) : state.events;

  return {
    ok: true,
    statePath,
    eventCount: state.events.length,
    events
  };
}

function handleStatus(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    status: buildStatus(state, cwd, statePath)
  };
}

function handleSummary(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    summary: buildSummary(state, cwd, statePath)
  };
}

function handleReadiness(parsed, cwd, target) {
  if (target !== "signoff") {
    throw new Error("Only 'signoff' readiness is supported in v1.");
  }

  const { state, statePath } = openState(parsed, cwd);

  return {
    ok: true,
    statePath,
    readiness: buildSignoffReadiness(state, cwd)
  };
}

function handleAddRisk(parsed, cwd) {
  const severity = requiredFlag(parsed, "severity");
  const description = requiredFlag(parsed, "description");
  const suggestedFix = optionalFlag(parsed, "suggested-fix");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = addRisk(state, severity, description, by, suggestedFix);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-risk",
    feature,
    statePath,
    risk
  };
}

function handleRespondRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const status = requiredFlag(parsed, "status");
  const note = requiredFlag(parsed, "note");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = respondToRisk(state, riskId, status, note, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "respond-risk",
    feature,
    statePath,
    risk
  };
}

function handleResolveRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = resolveRisk(state, riskId, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "resolve-risk",
    feature,
    statePath,
    risk
  };
}

function handleReopenRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const reason = optionalFlag(parsed, "reason");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = reopenRisk(state, riskId, reason, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "reopen-risk",
    feature,
    statePath,
    risk
  };
}

function handleIncrementRound(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const round = incrementReviewRound(state);
  saveState(statePath, state);

  return {
    ok: true,
    command: "increment-round",
    feature,
    statePath,
    round
  };
}

function handleArchitectureGate(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);

  return {
    ok: true,
    command: "architecture-gate",
    feature,
    statePath,
    ...buildArchitectureGate(state)
  };
}

// --- Code Findings handlers ---

function handleAddFinding(parsed, cwd) {
  const severity = requiredFlag(parsed, "severity");
  const description = requiredFlag(parsed, "description");
  const file = optionalFlag(parsed, "file");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = addFinding(state, severity, description, file, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-finding",
    feature,
    statePath,
    finding
  };
}

function handleRespondFinding(parsed, cwd) {
  const findingId = parsePositiveInteger(requiredFlag(parsed, "id"), "finding id");
  const status = requiredFlag(parsed, "status");
  const note = requiredFlag(parsed, "note");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = respondToFinding(state, findingId, status, note, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "respond-finding",
    feature,
    statePath,
    finding
  };
}

function handleResolveFinding(parsed, cwd) {
  const findingId = parsePositiveInteger(requiredFlag(parsed, "id"), "finding id");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = resolveFinding(state, findingId, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "resolve-finding",
    feature,
    statePath,
    finding
  };
}

function handleReopenFinding(parsed, cwd) {
  const findingId = parsePositiveInteger(requiredFlag(parsed, "id"), "finding id");
  const reason = optionalFlag(parsed, "reason");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = reopenFinding(state, findingId, reason, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "reopen-finding",
    feature,
    statePath,
    finding
  };
}

function handleIncrementCodeReviewRound(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const round = incrementCodeReviewRound(state);
  saveState(statePath, state);

  return {
    ok: true,
    command: "increment-code-review-round",
    feature,
    statePath,
    round
  };
}

function handleCodeReviewGate(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);

  return {
    ok: true,
    command: "code-review-gate",
    feature,
    statePath,
    ...buildCodeReviewGate(state)
  };
}

// --- Plan Structure Handlers ---

function openPlan(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const planPath = resolvePlanPath(cwd, feature);
  const plan = loadPlan(planPath);
  return { feature, plan, planPath };
}

function handleInitPlan(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const planPath = resolvePlanPath(cwd, feature);

  if (optionalFlag(parsed, "force") !== true) {
    assertPlanDoesNotExist(planPath);
  }

  const plan = createInitialPlan(feature);
  savePlan(planPath, plan);

  return { ok: true, command: "init-plan", feature, planPath, revision: plan.revision };
}

function handleAddPlanExample(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const label = requiredFlag(parsed, "label");
  const type = requiredFlag(parsed, "type");
  const bodyRaw = requiredFlag(parsed, "body");

  let body;
  try {
    body = JSON.parse(bodyRaw);
  } catch {
    throw new Error("--body must be valid JSON");
  }

  const entry = addPlanExample(plan, label, type, body);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-example", feature, entry, total: plan.payloadExamples.length };
}

function handleAddPlanValidation(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const rule = requiredFlag(parsed, "rule");
  const boundary = requiredFlag(parsed, "boundary");
  const reason = requiredFlag(parsed, "reason");

  const entry = addPlanValidation(plan, rule, boundary, reason);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-validation", feature, entry, total: plan.validationBoundary.length };
}

function handleAddPlanModel(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const qualifiedName = requiredFlag(parsed, "qualified-name");
  const type = requiredFlag(parsed, "type");
  const status = requiredFlag(parsed, "status");
  const justification = requiredFlag(parsed, "justification");

  const opts = {};

  const fieldsRaw = optionalFlag(parsed, "fields");
  if (fieldsRaw) {
    try {
      opts.fields = JSON.parse(fieldsRaw);
    } catch {
      throw new Error("--fields must be a valid JSON array");
    }
  }

  const annotationsRaw = optionalFlag(parsed, "annotations");
  if (annotationsRaw) {
    opts.annotations = annotationsRaw.split(",").map((a) => a.trim());
  }

  opts.notes = optionalFlag(parsed, "notes") ?? null;

  const valuesRaw = optionalFlag(parsed, "values");
  if (valuesRaw) {
    opts.values = valuesRaw.split(",").map((v) => v.trim());
  }

  const methodsRaw = optionalFlag(parsed, "methods");
  if (methodsRaw) {
    opts.methods = methodsRaw.split(",").map((m) => m.trim());
  }

  const entry = addPlanModel(plan, qualifiedName, type, status, justification, opts);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-model", feature, entry, totalModels: plan.models.length };
}

function handleAddPlanModelField(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const modelName = requiredFlag(parsed, "model");
  const name = requiredFlag(parsed, "name");
  const type = requiredFlag(parsed, "type");

  const opts = {
    nullable: optionalFlag(parsed, "nullable") === true,
    defensiveCopy: optionalFlag(parsed, "defensive-copy") === true
  };

  const field = addPlanModelField(plan, modelName, name, type, opts);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-model-field", feature, model: modelName, field };
}

function handleAddPlanClass(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const filePath = requiredFlag(parsed, "path");
  const status = requiredFlag(parsed, "status");
  const role = optionalFlag(parsed, "role");

  const entry = addPlanClass(plan, filePath, status, role);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-class", feature, entry, totalClasses: plan.classes.length };
}

function handleAddPlanSlice(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const sliceId = parsePositiveInteger(requiredFlag(parsed, "id"), "slice id");
  const title = requiredFlag(parsed, "title");
  const goal = requiredFlag(parsed, "goal");

  const opts = {};

  const filesRaw = optionalFlag(parsed, "files");
  if (filesRaw) {
    opts.files = filesRaw.split(",").map((f) => f.trim());
  }

  const unitTests = normalizeArray(optionalFlag(parsed, "unit-test"));
  const integrationTests = normalizeArray(optionalFlag(parsed, "integration-test"));
  const componentTests = normalizeArray(optionalFlag(parsed, "component-test"));
  if (unitTests.length > 0 || integrationTests.length > 0 || componentTests.length > 0) {
    opts.tests = { unit: unitTests, integration: integrationTests, component: componentTests };
  }

  const infoLog = optionalFlag(parsed, "info-log");
  const warnLog = optionalFlag(parsed, "warn-log");
  const errorLog = optionalFlag(parsed, "error-log");
  if (infoLog !== undefined || warnLog !== undefined || errorLog !== undefined) {
    opts.logging = {};
    if (infoLog !== undefined) opts.logging.info = infoLog;
    if (warnLog !== undefined) opts.logging.warn = warnLog;
    if (errorLog !== undefined) opts.logging.error = errorLog;
  }

  const entry = addPlanSlice(plan, sliceId, title, goal, opts);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-slice", feature, entry, totalSlices: plan.slices.length };
}

function handleAddPlanSliceTest(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const sliceId = parsePositiveInteger(requiredFlag(parsed, "slice"), "slice id");
  const level = requiredFlag(parsed, "level");
  const test = requiredFlag(parsed, "test");

  addPlanSliceTest(plan, sliceId, level, test);
  savePlan(planPath, plan);
  const slice = plan.slices.find((s) => s.id === sliceId);
  return { ok: true, command: "add-plan-slice-test", feature, sliceId, level, test, totalTests: slice.tests[level].length };
}

function handleSetPlanSliceLogging(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const sliceId = parsePositiveInteger(requiredFlag(parsed, "slice"), "slice id");

  const opts = {};
  const infoLog = optionalFlag(parsed, "info");
  const warnLog = optionalFlag(parsed, "warn");
  const errorLog = optionalFlag(parsed, "error");
  if (infoLog !== undefined) opts.info = infoLog;
  if (warnLog !== undefined) opts.warn = warnLog;
  if (errorLog !== undefined) opts.error = errorLog;

  setPlanSliceLogging(plan, sliceId, opts);
  savePlan(planPath, plan);
  const slice = plan.slices.find((s) => s.id === sliceId);
  return { ok: true, command: "set-plan-slice-logging", feature, sliceId, logging: slice.logging };
}

function handleSetPlanComposition(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const approach = requiredFlag(parsed, "approach");
  const description = requiredFlag(parsed, "description");

  setPlanComposition(plan, approach, description);
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-composition", feature, compositionStrategy: plan.compositionStrategy };
}

function handleSetPlanInfra(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const reusedRaw = optionalFlag(parsed, "reused");
  const newRaw = optionalFlag(parsed, "new");

  const reused = reusedRaw ? reusedRaw.split(",").map((r) => r.trim()) : [];
  const newInfra = newRaw ? newRaw.split(",").map((n) => n.trim()) : [];

  setPlanInfra(plan, reused, newInfra);
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-infra", feature, sharedInfra: plan.sharedInfra };
}

function handleAddPlanTest(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const level = requiredFlag(parsed, "level");
  const required = optionalFlag(parsed, "required") === true;
  const coverage = requiredFlag(parsed, "coverage");

  const entry = addPlanTest(plan, level, required, coverage);
  savePlan(planPath, plan);
  return { ok: true, command: "add-plan-test", feature, entry, totalLevels: plan.testingMatrix.length };
}

function handleSetPlanKarate(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const featureFile = requiredFlag(parsed, "feature-file");
  const scenarios = normalizeArray(optionalFlag(parsed, "scenario"));
  const smokeTagged = optionalFlag(parsed, "smoke-tagged") === true;
  const runnerUpdated = optionalFlag(parsed, "runner-updated") === true;

  setPlanKarate(plan, { featureFile, scenarios, smokeTagged, runnerUpdated });
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-karate", feature, karate: plan.karate };
}

function handleSetPlanArchunit(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const newRules = normalizeArray(optionalFlag(parsed, "new-rule"));
  const existingReviewed = optionalFlag(parsed, "existing-reviewed") === true;

  setPlanArchunit(plan, { newRules, existingReviewed });
  savePlan(planPath, plan);
  return { ok: true, command: "set-plan-archunit", feature, archUnit: plan.archUnit };
}

function handleRevisePlan(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const previousRevision = plan.revision;
  bumpPlanRevision(plan);
  savePlan(planPath, plan);

  return {
    ok: true,
    command: "revise-plan",
    feature,
    previousRevision,
    newRevision: plan.revision,
    note: "All sections cleared. Re-register the revised plan from scratch."
  };
}

function handleValidatePlan(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const result = validatePlan(plan);
  return { ok: true, command: "validate-plan", feature, planPath, ...result };
}

function handlePlanSummary(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  return { ok: true, command: "plan-summary", feature, planPath, ...buildPlanSummary(plan) };
}

function handlePlanGet(parsed, cwd) {
  const { feature, plan, planPath } = openPlan(parsed, cwd);
  const section = optionalFlag(parsed, "section");

  if (section) {
    const data = getPlanSection(plan, section);
    return { ok: true, command: "plan-get", feature, planPath, section, data };
  }

  return { ok: true, command: "plan-get", feature, planPath, plan };
}

function assertPlanDoesNotExist(planPath) {
  if (fs.existsSync(planPath)) {
    throw new Error(`Plan already exists: ${planPath}. Use --force to overwrite.`);
  }
}

function openState(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const statePath = resolveStatePath(cwd, feature, optionalFlag(parsed, "state-path"));
  const state = loadState(statePath);
  ensureFeatureMatches(state, feature, statePath);

  return { feature, state, statePath };
}

function parseArgs(argv) {
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

function requiredFlag(parsed, name) {
  const value = parsed.flags[name];
  if (value === undefined) {
    throw new Error(`Missing required flag: --${name}`);
  }
  return value;
}

function optionalFlag(parsed, name) {
  return parsed.flags[name];
}

function normalizeArray(value) {
  if (value === undefined) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
}

function assertStateDoesNotExist(statePath) {
  try {
    loadState(statePath);
  } catch (error) {
    if (error.message.startsWith("State file does not exist")) {
      return;
    }
    throw error;
  }

  throw new Error(`State file already exists: ${statePath}`);
}

function helpResult() {
  return {
    tool: "flow-log",
    version: "0.7.0",
    commands: [
      "create --feature <name> [--state-path <path>] [--force]",
      "lock-requirements --feature <name> [--by <actor>] [--request-source <path>] [--state-path <path>]",
      "register-artifact <story|plan> --feature <name> --path <file> [--state-path <path>]",
      "approve-artifact <story|plan> --feature <name> --by <actor> [--state-path <path>]",
      "set-review --feature <name> --name <architectureReview|codeReview> --status <PENDING|PASS|FAIL|BLOCKED> [--by <actor>] [--reason <text>] [--state-path <path>]",
      "set-check --feature <name> --name <verifyQuick|finalCheck|karate> --status <NOT_RUN|PASS|FAIL|BLOCKED> [--by <actor>] [--command <cmd>] [--details <text>] [--report-path <path>]... [--state-path <path>]",
      "add-change --feature <name> --file <path> [--file <path>]... [--state-path <path>]",
      "add-event --feature <name> --type <redCard|rejection|reroute|note|batchStart|batchEnd|archEscalationDecision> --reason <text> [--decision <PROCEED_TO_CODING|FINAL_ADJUSTMENT|ESCALATE_TO_USER>] [--by <actor>] [--target <agent>] [--related-check <name>] [--related-review <name>] [--state-path <path>]",
      "start-batch --feature <name> [--slice <name>]... [--by <actor>] [--state-path <path>]",
      "complete-batch --feature <name> [--status <complete|failed|blocked>] [--state-path <path>]",
      "reset-checks --feature <name> [--reason <text>] [--by <actor>] [--target <agent>] [--state-path <path>]",
      "add-risk --feature <name> --severity <CRITICAL|HIGH|MEDIUM|LOW> --description <text> [--suggested-fix <text>] [--by <actor>] [--state-path <path>]",
      "respond-risk --feature <name> --id <number> --status <ADDRESSED|INVALIDATED> --note <text> [--by <actor>] [--state-path <path>]",
      "resolve-risk --feature <name> --id <number> [--by <actor>] [--state-path <path>]",
      "reopen-risk --feature <name> --id <number> [--reason <text>] [--by <actor>] [--state-path <path>]",
      "increment-round --feature <name> [--state-path <path>]",
      "architecture-gate --feature <name> [--state-path <path>]",
      "add-finding --feature <name> --severity <CRITICAL|HIGH|MEDIUM|LOW> --description <text> [--file <path>] [--by <actor>] [--state-path <path>]",
      "respond-finding --feature <name> --id <number> --status <FIXED|DISPUTED> --note <text> [--by <actor>] [--state-path <path>]",
      "resolve-finding --feature <name> --id <number> [--by <actor>] [--state-path <path>]",
      "reopen-finding --feature <name> --id <number> [--reason <text>] [--by <actor>] [--state-path <path>]",
      "increment-code-review-round --feature <name> [--state-path <path>]",
      "code-review-gate --feature <name> [--state-path <path>]",
      "complete --feature <name> [--state-path <path>]",
      "get --feature <name> [--state-path <path>]",
      "history --feature <name> [--limit <n>] [--state-path <path>]",
      "status --feature <name> [--state-path <path>]",
      "summary --feature <name> [--state-path <path>]",
      "readiness signoff --feature <name> [--state-path <path>]",
      "",
      "# Plan Structure (Architect writes, all agents read)",
      "init-plan --feature <name> [--force]",
      "add-plan-example --feature <name> --label <text> --type <request|success|error|validation-error> --body <json>",
      "add-plan-validation --feature <name> --rule <text> --boundary <text> --reason <text>",
      "add-plan-model --feature <name> --qualified-name <fqcn> --type <record|enum|interface|sealed-interface> --status <new|modified> --justification <text> [--fields <json-array>] [--annotations <csv>] [--notes <text>] [--values <csv>] [--methods <csv>]",
      "add-plan-model-field --feature <name> --model <qualified-name> --name <field> --type <java-type> [--nullable] [--defensive-copy]",
      "add-plan-class --feature <name> --path <class-path> --status <new|modified|existing> [--role <text>]",
      "add-plan-slice --feature <name> --id <N> --title <text> --goal <text> [--files <csv>] [--unit-test <text>]... [--integration-test <text>]... [--component-test <text>]... [--info-log <text>] [--warn-log <text>] [--error-log <text>]",
      "add-plan-slice-test --feature <name> --slice <N> --level <unit|integration|component> --test <text>",
      "set-plan-slice-logging --feature <name> --slice <N> [--info <text>] [--warn <text>] [--error <text>]",
      "set-plan-composition --feature <name> --approach <text> --description <text>",
      "set-plan-infra --feature <name> [--reused <csv>] [--new <csv>]",
      "add-plan-test --feature <name> --level <text> --coverage <text> [--required]",
      "set-plan-karate --feature <name> --feature-file <path> [--scenario <text>]... [--smoke-tagged] [--runner-updated]",
      "set-plan-archunit --feature <name> [--new-rule <text>]... [--existing-reviewed]",
      "revise-plan --feature <name>",
      "validate-plan --feature <name>",
      "plan-summary --feature <name>",
      "plan-get --feature <name> [--section <name>]"
    ]
  };
}

function resolveReviewName(parsed) {
  const value = optionalFlag(parsed, "name") ?? optionalFlag(parsed, "phase");

  if (value === undefined) {
    throw new Error("Missing required flag: --name");
  }

  const normalized = REVIEW_ALIASES[value] ?? value;
  validateValue(normalized, REVIEW_NAMES, "review name");
  return normalized;
}

const REVIEW_ALIASES = {
  phase1: "architectureReview",
  phase2: "codeReview"
};

function parsePositiveInteger(value, label) {
  const parsed = Number.parseInt(value, 10);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${label} must be a positive integer.`);
  }

  return parsed;
}
