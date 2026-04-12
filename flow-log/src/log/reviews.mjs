import { timestamp } from "./common.mjs";

export function setReview(state, reviewName, status, reason, by) {
  state.reviews[reviewName] = {
    status,
    reason: reason ?? null,
    updatedAt: timestamp(),
    updatedBy: by ?? null
  };

  return state.reviews[reviewName];
}

export function summarizeReviews(reviews) {
  return {
    architectureReview: reviews.architectureReview.status,
    codeReview: reviews.codeReview.status
  };
}
