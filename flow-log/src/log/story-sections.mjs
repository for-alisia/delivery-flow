import fs from "node:fs";
import { assertFileExists, buildArtifactApprovalState } from "./artifacts.mjs";

export function loadStorySection(state, cwd, sectionName) {
  const storyPath = state.artifacts?.story?.path;

  if (!storyPath) {
    throw new Error("Story artifact path is not registered.");
  }

  if (state.artifacts.story.approved !== true) {
    throw new Error("Story must be approved before story-get can read sections.");
  }
  if (buildArtifactApprovalState(cwd, "story", state.artifacts.story).stale) {
    throw new Error("Story approval is stale. Re-approve the story before story-get.");
  }

  const absoluteStoryPath = assertFileExists(cwd, storyPath, "story artifact");
  const markdown = fs.readFileSync(absoluteStoryPath, "utf8");
  const extracted = extractMarkdownSection(markdown, sectionName);

  if (!extracted) {
    throw new Error(`Story section '${sectionName}' was not found in ${storyPath}.`);
  }

  return {
    storyPath,
    heading: extracted.heading,
    content: extracted.content
  };
}

function extractMarkdownSection(markdown, sectionName) {
  const lines = markdown.split(/\r?\n/);
  const target = normalizeHeading(sectionName);
  let heading = null;
  let startIndex = -1;

  for (let index = 0; index < lines.length; index += 1) {
    const match = /^(##)\s+(.+?)\s*$/.exec(lines[index]);
    if (!match) {
      continue;
    }

    if (startIndex === -1 && normalizeHeading(match[2]) === target) {
      heading = match[2].trim();
      startIndex = index + 1;
      continue;
    }

    if (startIndex !== -1) {
      return {
        heading,
        content: lines.slice(startIndex, index).join("\n").trim()
      };
    }
  }

  if (startIndex === -1) {
    return null;
  }

  return {
    heading,
    content: lines.slice(startIndex).join("\n").trim()
  };
}

function normalizeHeading(value) {
  return String(value)
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}
