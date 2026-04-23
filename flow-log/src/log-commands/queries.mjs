import {
  buildSignoffReadiness,
  buildStatus,
  buildSummary
} from "../log/index.mjs";
import { loadStorySection } from "../log/story-sections.mjs";
import {
  optionalFlag,
  parsePositiveInteger
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleGet(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    statePath,
    state
  };
}

export function handleHistory(parsed, cwd) {
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

export function handleStatus(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    status: buildStatus(state, cwd, statePath)
  };
}

export function handleSummary(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    summary: buildSummary(state, cwd, statePath)
  };
}

export function handleStoryGet(parsed, cwd) {
  const section = optionalFlag(parsed, "section");
  if (!section) {
    throw new Error("Missing required flag: --section");
  }

  const { feature, state, statePath } = openState(parsed, cwd);
  const result = loadStorySection(state, cwd, section);

  return {
    ok: true,
    command: "story-get",
    feature,
    statePath,
    storyPath: result.storyPath,
    section,
    heading: result.heading,
    content: result.content
  };
}

export function handleReadiness(parsed, cwd, target) {
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
