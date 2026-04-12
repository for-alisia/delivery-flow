import {
  ARCH_ESCALATION_DECISIONS,
  EVENT_TYPES,
  appendEvent,
  saveState,
  validateValue
} from "../log/index.mjs";
import {
  optionalFlag,
  requiredFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleAddEvent(parsed, cwd) {
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
