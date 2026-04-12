import {
  REVIEW_STATUSES,
  saveState,
  setReview,
  validateValue
} from "../log/index.mjs";
import { requiredFlag, resolveReviewName, optionalFlag } from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleSetReview(parsed, cwd) {
  const reviewName = resolveReviewName(parsed);
  const status = requiredFlag(parsed, "status");
  validateValue(status, REVIEW_STATUSES, "review status");

  const { feature, state, statePath } = openState(parsed, cwd);
  const reviewState = setReview(
    state,
    reviewName,
    status,
    optionalFlag(parsed, "reason"),
    optionalFlag(parsed, "by")
  );
  saveState(statePath, state);

  return {
    ok: true,
    command: "set-review",
    feature,
    review: reviewName,
    statePath,
    reviewState
  };
}
