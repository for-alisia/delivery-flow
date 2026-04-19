import { artifactExists } from "./artifacts.mjs";

export function buildSignoffReadiness(state, cwd) {
  const checks = {
    requirementsLocked: state.requirements.locked,
    storyRegistered: Boolean(state.artifacts.story.path),
    storyExists: artifactExists(cwd, state.artifacts.story.path),
    storyApproved: state.artifacts.story.approved,
    planRegistered: Boolean(state.artifacts.plan.path),
    planExists: artifactExists(cwd, state.artifacts.plan.path),
    planApproved: state.artifacts.plan.approved,
    architectureReviewPassed: state.reviews.architectureReview.status === "PASS",
    codeReviewPassed: state.reviews.codeReview.status === "PASS",
    finalCheckPassed: state.checks.finalCheck.status === "PASS",
    karatePassed: state.checks.karate.status === "PASS"
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
  if (!checks.planRegistered) {
    reasons.push("Plan artifact path must be registered.");
  }
  if (checks.planRegistered && !checks.planExists) {
    reasons.push("Plan artifact path does not exist on disk.");
  }
  if (!checks.planApproved) {
    reasons.push("Plan must be approved.");
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
  if (!checks.karatePassed) {
    reasons.push("karate must be PASS.");
  }

  return {
    ready: reasons.length === 0,
    target: "signoff",
    checks,
    reasons
  };
}

export function deriveCurrentPhase(state) {
  if (!state.requirements.locked) {
    return "requirement-lock";
  }
  if (!state.artifacts.story.approved) {
    return "story";
  }
  if (!state.artifacts.plan.approved) {
    return "plan";
  }
  if (state.reviews.architectureReview.status !== "PASS") {
    return "architecture-review";
  }
  if (state.reviews.codeReview.status !== "PASS") {
    return "implementation";
  }
  if (state.checks.finalCheck.status !== "PASS" || state.checks.karate.status !== "PASS") {
    return "verification";
  }

  return "signoff-ready";
}
