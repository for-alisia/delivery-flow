import { buildArtifactApprovalState } from "./artifacts.mjs";
import { computeSourceFingerprint, isCheckStale } from "./checks.mjs";

export function buildSignoffReadiness(state, cwd) {
  const story = buildArtifactApprovalState(cwd, "story", state.artifacts.story);
  const plan = buildArtifactApprovalState(cwd, "plan", state.artifacts.plan);
  const currentFingerprint = computeSourceFingerprint(cwd);
  const checks = {
    requirementsLocked: state.requirements.locked,
    storyRegistered: Boolean(story.path),
    storyExists: story.exists,
    storyApproved: story.approved,
    storyApprovalTracked: story.approvalTracked,
    storyStale: story.stale,
    planRegistered: Boolean(plan.path),
    planExists: plan.exists,
    planApproved: plan.approved,
    planApprovalTracked: plan.approvalTracked,
    planStale: plan.stale,
    architectureReviewPassed: state.reviews.architectureReview.status === "PASS",
    codeReviewPassed: state.reviews.codeReview.status === "PASS",
    finalCheckPassed: state.checks.finalCheck.status === "PASS",
    finalCheckStale: isCheckStale(state.checks.finalCheck, currentFingerprint),
    karatePassed: state.checks.karate.status === "PASS",
    karateStale: isCheckStale(state.checks.karate, currentFingerprint)
  };

  const reasons = [];

  if (!checks.requirementsLocked) {
    reasons.push("Requirements must be locked.");
  }
  if (!checks.storyRegistered) {
    reasons.push("Story artifact path must be registered.");
  }
  if (checks.storyRegistered && !checks.storyExists) {
    reasons.push("Story artifact path does not exist on disk.");
  }
  if (!checks.storyApproved) {
    reasons.push("Story must be approved.");
  }
  if (checks.storyApproved && !checks.storyApprovalTracked) {
    reasons.push("Story approval metadata is missing. Re-approve the story.");
  }
  if (checks.storyStale) {
    reasons.push("Story approval is stale. Re-approve the story.");
  }
  if (!checks.planRegistered) {
    reasons.push("Plan artifact path must be registered.");
  }
  if (checks.planRegistered && !checks.planExists) {
    reasons.push("Plan artifact path does not exist on disk.");
  }
  if (!checks.planApproved) {
    reasons.push("Plan must be approved.");
  }
  if (checks.planApproved && !checks.planApprovalTracked) {
    reasons.push("Plan approval metadata is missing. Re-approve the plan.");
  }
  if (checks.planStale) {
    reasons.push("Plan approval is stale. Re-approve the plan.");
  }
  if (!checks.architectureReviewPassed) {
    reasons.push("architectureReview must be PASS.");
  }
  if (!checks.codeReviewPassed) {
    reasons.push("codeReview must be PASS.");
  }
  if (!checks.finalCheckPassed) {
    reasons.push("finalCheck must be PASS.");
  }
  if (checks.finalCheckStale) {
    reasons.push("finalCheck must be re-run because the recorded PASS is stale.");
  }
  if (!checks.karatePassed) {
    reasons.push("karate must be PASS.");
  }
  if (checks.karateStale) {
    reasons.push("karate must be re-run because the recorded PASS is stale.");
  }

  return {
    ready: reasons.length === 0,
    target: "signoff",
    checks,
    reasons
  };
}

export function deriveCurrentPhase(state, cwd) {
  const story = buildArtifactApprovalState(cwd, "story", state.artifacts.story);
  const plan = buildArtifactApprovalState(cwd, "plan", state.artifacts.plan);
  const currentFingerprint = computeSourceFingerprint(cwd);

  if (!state.requirements.locked) {
    return "requirement-lock";
  }
  if (!story.approved || story.stale) {
    return "story";
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
