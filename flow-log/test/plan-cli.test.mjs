import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { createPlanTempRoot, runCli, runCliRaw } from "./test-helpers.mjs";

test("flow-log plan v4: draft lifecycle create, inspect, and discard", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-lifecycle-");
  const feature = uniqueFeature("lifecycle");

  const init = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);
  assert.equal(init.ok, true);
  assert.equal(init.schemaVersion, "4.0");

  const statusBeforeDiscard = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(statusBeforeDiscard.exists, true);
  assert.equal(statusBeforeDiscard.hasChanges, false);

  const discard = runCli(tempRoot, ["plan-discard-draft", "--feature", feature]);
  assert.equal(discard.discarded, true);

  const firstCreate = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.equal(firstCreate.created, true);

  const secondCreate = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.equal(secondCreate.created, false);
  assert.equal(secondCreate.draftPath, firstCreate.draftPath);
  assert.match(secondCreate.note, /matches canonical/i);

  const statusAfterDiscard = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(statusAfterDiscard.exists, true);
});

test("flow-log plan v4: create-draft refuses dirty reuse", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-dirty-draft-");
  const feature = uniqueFeature("dirty-draft");

  const draft = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);

  const changed = buildValidV4Plan(feature);
  changed.scope.purpose = "Changed locally but not accepted";
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const status = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(status.exists, true);
  assert.equal(status.hasChanges, true);

  const recreate = runCliRaw(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.notEqual(recreate.status, 0);
  assert.match(recreate.stderr, /Draft already exists with unapplied changes/);
});

test("flow-log plan v4: malformed draft JSON fails validation command", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-invalid-json-");
  const feature = uniqueFeature("invalid-json");

  const draft = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);

  fs.writeFileSync(draft.draftPath, "{ malformed json\n");

  const result = runCliRaw(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Invalid draft JSON/);
});

test("flow-log plan v4: validate-draft reports missing top-level keys", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-validate-");
  const feature = uniqueFeature("validate");

  const draft = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);

  const broken = buildValidV4Plan(feature);
  delete broken.slices;
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(broken, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) => issue.includes("Missing top-level key 'slices'")));
});

test("flow-log plan v4: validator catches duplicate ids, bad references, and incomplete units", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-validator-");
  const feature = uniqueFeature("validator");

  const draft = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);

  const broken = buildValidV4Plan(feature);
  broken.sharedRules[0].appliesTo = ["S404"];
  broken.slices[0].units.push({ ...broken.slices[0].units[0] });
  broken.slices[0].units[0].tests.levels = [];
  broken.slices[0].doneWhen = [];

  fs.writeFileSync(draft.draftPath, `${JSON.stringify(broken, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) => issue.includes("sharedRules.appliesTo") && issue.includes("S404")));
  assert.ok(validation.issues.some((issue) => issue.includes("units[2].id 'S1-U1' is duplicated")));
  assert.ok(validation.issues.some((issue) => issue.includes("unit 'S1-U1' must include at least one test level")));
  assert.ok(validation.issues.some((issue) => issue.includes("slice 'S1' must include doneWhen criteria")));
});

test("flow-log plan v4: no-op accept keeps revision and approval metadata", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-accept-noop-");
  const feature = uniqueFeature("noop");

  const init = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);
  const draftForInitialAccept = init;
  fs.writeFileSync(draftForInitialAccept.draftPath, `${JSON.stringify(buildValidV4Plan(feature), null, 2)}\n`);
  const firstAccept = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.equal(firstAccept.changed, true);
  assert.equal(firstAccept.revision, 2);

  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "register-artifact",
    "plan",
    "--feature",
    feature,
    "--path",
    init.planPath
  ]);
  const approve = runCli(tempRoot, [
    "approve-artifact",
    "plan",
    "--feature",
    feature,
    "--by",
    "TL"
  ]);
  assert.equal(approve.artifact.approved, true);
  assert.equal(approve.artifact.approvedRevision, 2);

  runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);

  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, false);
  assert.equal(accepted.revision, 2);

  const summary = runCli(tempRoot, ["summary", "--feature", feature]);
  assert.equal(summary.summary.artifacts.plan.approved, true);
  assert.equal(summary.summary.artifacts.plan.approvedRevision, 2);
  assert.equal(summary.summary.artifacts.plan.approvedHash, accepted.hash);
});

test("flow-log plan v4: changed accept bumps revision and invalidates approval metadata", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-accept-change-");
  const feature = uniqueFeature("accept-change");

  const init = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);

  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "register-artifact",
    "plan",
    "--feature",
    feature,
    "--path",
    init.planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "plan",
    "--feature",
    feature,
    "--by",
    "TL"
  ]);

  fs.writeFileSync(init.draftPath, `${JSON.stringify(buildValidV4Plan(feature), null, 2)}\n`);

  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, true);
  assert.equal(accepted.revision, 2);
  assert.equal(accepted.approvalInvalidation.invalidated, true);

  const state = runCli(tempRoot, ["get", "--feature", feature]);
  assert.equal(state.state.artifacts.plan.approved, false);
  assert.equal(state.state.artifacts.plan.approvedRevision, null);
  assert.equal(state.state.artifacts.plan.approvedHash, null);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.revision, 2);
  assert.equal(summary.sectionCounts.slices, 1);
  assert.equal(summary.sectionCounts.units, 2);
  assert.equal(summary.approval.approved, false);
});

test("flow-log plan v4: accept succeeds when open risk plan refs still exist", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-risk-pass-");
  const feature = uniqueFeature("risk-pass");

  const init = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Slice reference must remain stable",
    "--plan-ref",
    "S1",
    "--by",
    "ArchitectureReviewer"
  ]);

  fs.writeFileSync(init.draftPath, `${JSON.stringify(buildValidV4Plan(feature), null, 2)}\n`);

  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, true);
});

test("flow-log plan v4: accept fails when open risk refs disappear", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-risk-fail-");
  const feature = uniqueFeature("risk-fail");

  const init = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Do not remove slice id S1",
    "--plan-ref",
    "S1",
    "--by",
    "ArchitectureReviewer"
  ]);

  const changed = buildValidV4Plan(feature);
  changed.slices[0].id = "S2";
  changed.sharedRules[0].appliesTo = ["S2"];
  changed.sharedDecisions[0].appliesTo = ["S2"];
  fs.writeFileSync(init.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const accepted = runCliRaw(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.notEqual(accepted.status, 0);
  assert.match(accepted.stderr, /Risk 1 \(OPEN\) references missing plan IDs: S1/);
});

test("flow-log plan v4: connected areas are enforced during validation and accept", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-connected-area-");
  const feature = uniqueFeature("connected-area");

  const init = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Unit changes affect a related area",
    "--plan-ref",
    "S1",
    "--connected-area",
    "S1-U1",
    "--by",
    "ArchitectureReviewer"
  ]);

  const changed = buildValidV4Plan(feature);
  changed.slices[0].units[0].id = "S1-U9";
  fs.writeFileSync(init.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) => issue.includes("missing connected plan IDs: S1-U1")));

  const accepted = runCliRaw(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.notEqual(accepted.status, 0);
  assert.match(accepted.stderr, /missing connected plan IDs: S1-U1/);
});

test("flow-log plan-init-draft defaults new plans to v4", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-init-draft-");
  const feature = uniqueFeature("v4-init-draft");

  const result = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);

  assert.equal(result.ok, true);
  assert.equal(result.command, "plan-init-draft");
  assert.equal(result.schemaVersion, "4.0");
  assert.equal(result.draftCreated, true);
});

test("flow-log plan v4: plan-summary exposes slice metadata and plan-get --slice returns targeted data", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-summary-");
  const feature = uniqueFeature("v4-summary");
  const planPath = path.join(tempRoot, "artifacts", "implementation-plans", `${feature}.plan.json`);

  fs.writeFileSync(planPath, `${JSON.stringify(buildValidV4Plan(feature), null, 2)}\n`);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.schemaVersion, "4.0");
  assert.equal(summary.sectionCounts.slices, 1);
  assert.equal(summary.sectionCounts.units, 2);
  assert.equal(summary.slices[0].id, "S1");
  assert.equal(summary.slices[0].unitCount, 2);
  assert.deepEqual(summary.slices[0].unitIds, ["S1-U1", "S1-U2"]);

  const slice = runCli(tempRoot, ["plan-get", "--feature", feature, "--slice", "S1"]);
  assert.equal(slice.schemaVersion, "4.0");
  assert.equal(slice.sliceId, "S1");
  assert.equal(slice.data.slice.id, "S1");
  assert.equal(slice.data.sharedRules.length, 1);
  assert.equal(slice.data.sharedDecisions.length, 1);
  assert.equal(slice.data.finalVerification.requiredGates[2], "karate");
});

test("flow-log plan v4: plan-get requires a slice or section target", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-plan-get-target-");
  const feature = uniqueFeature("plan-get-target");
  const planPath = path.join(tempRoot, "artifacts", "implementation-plans", `${feature}.plan.json`);

  fs.writeFileSync(planPath, `${JSON.stringify(buildValidV4Plan(feature), null, 2)}\n`);

  const raw = runCliRaw(tempRoot, ["plan-get", "--feature", feature]);
  assert.notEqual(raw.status, 0);
  assert.match(raw.stderr, /requires --section or --slice/i);
});

test("flow-log plan v4: plan-summary marks direct plan edits as stale even if stored hash field is unchanged", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v4-stale-hash-");
  const feature = uniqueFeature("stale-hash");
  const planPath = path.join(tempRoot, "artifacts", "implementation-plans", `${feature}.plan.json`);

  const plan = buildValidV4Plan(feature);
  plan.hash = "persisted-hash";
  fs.writeFileSync(planPath, `${JSON.stringify(plan, null, 2)}\n`);

  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "register-artifact",
    "plan",
    "--feature",
    feature,
    "--path",
    planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "plan",
    "--feature",
    feature,
    "--by",
    "TL"
  ]);

  plan.slices[0].title = "REST boundary changed directly";
  fs.writeFileSync(planPath, `${JSON.stringify(plan, null, 2)}\n`);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.approval.approved, true);
  assert.equal(summary.approval.stale, true);
  assert.notEqual(summary.approval.approvedHash, summary.approval.currentHash);
});

function uniqueFeature(prefix) {
  return `${prefix}-${process.pid}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

function buildValidV4Plan(feature) {
  return {
    schemaVersion: "4.0",
    feature,
    revision: 1,
    status: "draft",
    scope: {
      purpose: "Enable slice-first planning",
      inScope: ["slice-first retrieval"],
      outOfScope: ["legacy mass migration"],
      constraints: ["story owns external contracts"]
    },
    sharedRules: [
      {
        id: "R1",
        rule: "Reuse the existing validation error contract.",
        appliesTo: ["S1"]
      }
    ],
    sharedDecisions: [
      {
        id: "D1",
        title: "Keep payload details in the story",
        decision: "Reference External Contracts instead of duplicating payloads in the plan.",
        rationale: "Reduce plan weight and keep one source of truth.",
        appliesTo: ["S1"]
      }
    ],
    slices: [
      {
        id: "S1",
        title: "REST boundary",
        goal: "Add the REST boundary and orchestration mapping for milestone search.",
        dependsOn: [],
        readsExisting: [
          "flow-orchestrator/src/main/java/com/example/common/web/GlobalExceptionHandler.java"
        ],
        sliceRules: [
          "Keep omitted request bodies equivalent to empty filters."
        ],
        contractDependency: {
          section: "external-contracts",
          notes: ["Use the story contract as the single payload source of truth."]
        },
        compositionNotes: [
          "Single orchestration call; no cross-port composition in this slice."
        ],
        units: [
          {
            id: "S1-U1",
            kind: "java-class",
            locationHint: "flow-orchestrator/src/main/java/com/example/orchestration/milestones/rest/MilestonesController.java",
            status: "new",
            purpose: "Expose the milestone search endpoint.",
            change: "Add the POST endpoint, allow omitted body, and map the request to the service input.",
            contractDetails: ["Preserve the validation error contract fields and names."],
            tests: {
              levels: ["integration"],
              notes: "Cover omitted body, invalid state, and happy path behavior."
            },
            loggingNotes: "Log effective filters and result count without raw payload dumps."
          },
          {
            id: "S1-U2",
            kind: "java-class",
            locationHint: "flow-orchestrator/src/main/java/com/example/orchestration/milestones/SearchMilestonesInput.java",
            status: "new",
            purpose: "Capture orchestration input defaults.",
            change: "Normalize optional collections and preserve immutable inputs.",
            tests: {
              levels: ["unit"],
              notes: "Cover defaulting, immutability, and empty input behavior."
            }
          }
        ],
        doneWhen: [
          "The endpoint exists and omitted bodies default correctly.",
          "The slice passes its planned unit and integration coverage."
        ]
      }
    ],
    finalVerification: {
      requiredGates: ["verifyQuick", "finalCheck", "karate"],
      notes: ["finalCheck and karate must be fresh at signoff."]
    },
    hash: null
  };
}
