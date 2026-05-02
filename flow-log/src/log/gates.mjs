import { buildArtifactApprovalState } from "./artifacts.mjs";
import { computeSourceFingerprint, isCheckStale } from "./checks.mjs";
import { getUndecidedNonBlockingFindings } from "./findings.mjs";
import { getUndecidedNonBlockingRisks } from "./risks.mjs";

export function buildSignoffReadiness(state, cwd) {
  const story = buildArtifactApprovalState(cwd, "story", state.artifacts.story);
  const e2e = buildArtifactApprovalState(cwd, "e2e", state.artifacts.e2e);
  const plan = buildArtifactApprovalState(cwd, "plan", state.artifacts.plan);
  const currentFingerprint = computeSourceFingerprint(cwd);
  const e2eMode = state.e2e?.mode ?? "UNDECIDED";
  const e2eScenariosRequired = e2eMode === "SCENARIOS_REQUIRED";
  const undecidedNonBlockingRisks = getUndecidedNonBlockingRisks(state).length;
  const undecidedNonBlockingFindings = getUndecidedNonBlockingFindings(state).length;
  const checks = {
    requirementsLocked: state.requirements.locked,
    e2eMode,
    storyRegistered: Boolean(story.path),
    storyExists: story.exists,
    storyApproved: story.approved,
    storyApprovalTracked: story.approvalTracked,
    storyStale: story.stale,
    e2eRegistered: Boolean(e2e.path),
    e2eExists: e2e.exists,
    e2eApproved: e2e.approved,
    e2eApprovalTracked: e2e.approvalTracked,
    e2eStale: e2e.stale,
    planRegistered: Boolean(plan.path),
    planExists: plan.exists,
    planApproved: plan.approved,
    planApprovalTracked: plan.approvalTracked,
    planStale: plan.stale,
    architectureReviewPassed: state.reviews.architectureReview.status === "PASS",
    undecidedNonBlockingRisks,
    codeReviewPassed: state.reviews.codeReview.status === "PASS",
    undecidedNonBlockingFindings,
    finalCheckPassed: state.checks.finalCheck.status === "PASS",
    finalCheckStale: isCheckStale(state.checks.finalCheck, currentFingerprint),
    karatePassed: state.checks.karate.status === "PASS",
    karateStale: isCheckStale(state.checks.karate, currentFingerprint)
  };
  const reasons = collectSignoffReasons(checks, e2eScenariosRequired);

  return {
    ready: reasons.length === 0,
    target: "signoff",
    checks,
    reasons
  };
}

export function deriveCurrentPhase(state, cwd) {
  const story = buildArtifactApprovalState(cwd, "story", state.artifacts.story);
  const e2e = buildArtifactApprovalState(cwd, "e2e", state.artifacts.e2e);
  const plan = buildArtifactApprovalState(cwd, "plan", state.artifacts.plan);
  const currentFingerprint = computeSourceFingerprint(cwd);
  const e2eMode = state.e2e?.mode ?? "UNDECIDED";
  const undecidedNonBlockingRisks = getUndecidedNonBlockingRisks(state).length;
  const undecidedNonBlockingFindings = getUndecidedNonBlockingFindings(state).length;

  if (!state.requirements.locked) {
    return "requirement-lock";
  }
  if (!story.approved || story.stale) {
    return "story";
  }
  if (e2eMode === "UNDECIDED") {
    return "e2e-decision";
  }
  if (e2eMode === "SCENARIOS_REQUIRED" && (!e2e.approved || e2e.stale)) {
    return "e2e-scenarios";
  }
  if (!plan.approved || plan.stale) {
    return "plan";
  }
  if (state.reviews.architectureReview.status !== "PASS") {
    return "architecture-review";
  }
  if (state.reviews.codeReview.status !== "PASS") {
    return "implementation";
  }
  if (undecidedNonBlockingRisks > 0 || undecidedNonBlockingFindings > 0) {
    return "review-debt";
  }
  if (
    state.checks.finalCheck.status !== "PASS" ||
    state.checks.karate.status !== "PASS" ||
    isCheckStale(state.checks.finalCheck, currentFingerprint) ||
    isCheckStale(state.checks.karate, currentFingerprint)
  ) {
    return "verification";
  }

  return "signoff-ready";
}

function collectSignoffReasons(checks, e2eScenariosRequired) {
  const reasons = [];

  pushReasons(reasons, [
    [!checks.requirementsLocked, "Requirements must be locked."],
    [checks.e2eMode === "UNDECIDED", "E2E mode must be decided: REUSE_EXISTING or SCENARIOS_REQUIRED."],
    [!checks.architectureReviewPassed, "architectureReview must be PASS."],
    [checks.undecidedNonBlockingRisks > 0, `Non-blocking architecture risks still require TL decision: ${checks.undecidedNonBlockingRisks} item(s) must be marked ACCEPTED or DEFERRED.`],
    [!checks.codeReviewPassed, "codeReview must be PASS."],
    [checks.undecidedNonBlockingFindings > 0, `Non-blocking code findings still require TL decision: ${checks.undecidedNonBlockingFindings} item(s) must be marked ACCEPTED or DEFERRED.`],
    [!checks.finalCheckPassed, "finalCheck must be PASS."],
    [checks.finalCheckStale, "finalCheck must be re-run because the recorded PASS is stale."],
    [!checks.karatePassed, "karate must be PASS."],
    [checks.karateStale, "karate must be re-run because the recorded PASS is stale."]
  ]);

  appendArtifactReasons(reasons, {
    registered: checks.storyRegistered,
    exists: checks.storyExists,
    approved: checks.storyApproved,
    approvalTracked: checks.storyApprovalTracked,
    stale: checks.storyStale,
    missingRegisteredMessage: "Story artifact path must be registered.",
    missingExistsMessage: "Story artifact path does not exist on disk.",
    missingApprovedMessage: "Story must be approved.",
    missingTrackedMessage: "Story approval metadata is missing. Re-approve the story.",
    staleMessage: "Story approval is stale. Re-approve the story."
  });

  appendArtifactReasons(reasons, {
    registered: checks.planRegistered,
    exists: checks.planExists,
    approved: checks.planApproved,
    approvalTracked: checks.planApprovalTracked,
    stale: checks.planStale,
    missingRegisteredMessage: "Plan artifact path must be registered.",
    missingExistsMessage: "Plan artifact path does not exist on disk.",
    missingApprovedMessage: "Plan must be approved.",
    missingTrackedMessage: "Plan approval metadata is missing. Re-approve the plan.",
    staleMessage: "Plan approval is stale. Re-approve the plan."
  });

  if (e2eScenariosRequired) {
    appendArtifactReasons(reasons, {
      registered: checks.e2eRegistered,
      exists: checks.e2eExists,
      approved: checks.e2eApproved,
      approvalTracked: checks.e2eApprovalTracked,
      stale: checks.e2eStale,
      missingRegisteredMessage: "E2E scenario artifact path must be registered when scenarios are required.",
      missingExistsMessage: "E2E scenario artifact path does not exist on disk.",
      missingApprovedMessage: "E2E scenario artifact must be approved when scenarios are required.",
      missingTrackedMessage: "E2E scenario approval metadata is missing. Re-approve the E2E scenario artifact.",
      staleMessage: "E2E scenario approval is stale. Re-approve the E2E scenario artifact."
    });
  }

  return reasons;
}

function appendArtifactReasons(reasons, artifact) {
  pushReasons(reasons, [
    [!artifact.registered, artifact.missingRegisteredMessage],
    [artifact.registered && !artifact.exists, artifact.missingExistsMessage],
    [!artifact.approved, artifact.missingApprovedMessage],
    [artifact.approved && !artifact.approvalTracked, artifact.missingTrackedMessage],
    [artifact.stale, artifact.staleMessage]
  ]);
}

function pushReasons(reasons, conditionMessages) {
  for (const [condition, message] of conditionMessages) {
    if (condition) {
      reasons.push(message);
    }
  }
}
