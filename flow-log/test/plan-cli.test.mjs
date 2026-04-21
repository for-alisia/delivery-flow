import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { createPlanTempRoot, runCli, runCliRaw } from "./test-helpers.mjs";

test("flow-log plan v3: draft lifecycle create/status/discard", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-lifecycle-");
  const feature = uniqueFeature("lifecycle");

  const init = runCli(tempRoot, ["init-plan", "--feature", feature]);
  assert.equal(init.ok, true);
  assert.equal(init.schemaVersion, "3.0");

  const firstCreate = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.equal(firstCreate.created, true);

  const secondCreate = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.equal(secondCreate.created, false);
  assert.equal(secondCreate.draftPath, firstCreate.draftPath);
  assert.match(secondCreate.note, /matches canonical/i);

  const statusBeforeDiscard = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(statusBeforeDiscard.exists, true);
  assert.equal(statusBeforeDiscard.hasChanges, false);

  const discard = runCli(tempRoot, ["plan-discard-draft", "--feature", feature]);
  assert.equal(discard.discarded, true);

  const statusAfterDiscard = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(statusAfterDiscard.exists, false);

  const discardAgain = runCli(tempRoot, ["plan-discard-draft", "--feature", feature]);
  assert.equal(discardAgain.discarded, false);
});

test("flow-log plan v3: create-draft refuses to silently reuse dirty draft", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-dirty-draft-");
  const feature = uniqueFeature("dirty-draft");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);

  const changed = buildValidV3Draft(feature);
  changed.scope.purpose = "Changed locally but not accepted";
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const status = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(status.exists, true);
  assert.equal(status.hasChanges, true);

  const recreate = runCliRaw(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.notEqual(recreate.status, 0);
  assert.match(recreate.stderr, /Draft already exists with unapplied changes/);
});

test("flow-log plan v3: malformed draft JSON fails validation command", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-invalid-json-");
  const feature = uniqueFeature("invalid-json");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);

  fs.writeFileSync(draft.draftPath, "{ malformed json\n");

  const result = runCliRaw(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Invalid draft JSON/);
});

test("flow-log plan v3: validate-draft reports shape and readiness issues", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-validate-");
  const feature = uniqueFeature("validate");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);

  const broken = buildValidV3Draft(feature);
  delete broken.slices;
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(broken, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) => issue.includes("Missing top-level key 'slices'")));
});

test("flow-log plan v3: validator catches duplicate IDs, bad references, order, and incomplete slices", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-validator-");
  const feature = uniqueFeature("validator");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);

  const broken = buildValidV3Draft(feature);
  broken.models.push({ ...broken.models[0] });
  broken.implementationFlow[0].order = 2;
  broken.slices[0].flowSteps = [];
  broken.slices[0].covers.models = ["M404"];
  broken.slices[0].tests = { unit: [], integration: [], component: [] };
  broken.slices[0].doneWhen = [];

  fs.writeFileSync(draft.draftPath, `${JSON.stringify(broken, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) => issue.includes("models[1].id 'M1' is duplicated")));
  assert.ok(validation.issues.some((issue) => issue.includes("implementationFlow.order must start at 1")));
  assert.ok(validation.issues.some((issue) => issue.includes("references missing model id 'M404'")));
  assert.ok(validation.issues.some((issue) => issue.includes("slice 'S1' must include at least one test entry")));
  assert.ok(validation.issues.some((issue) => issue.includes("slice 'S1' must include doneWhen criteria")));
  assert.ok(validation.issues.some((issue) => issue.includes("Slice 'S1' must reference at least one flow step")));
});

test("flow-log plan v3: no-op accept keeps revision/hash and does not invalidate approval", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-accept-noop-");
  const feature = uniqueFeature("noop");

  const init = runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draftForInitialAccept = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  fs.writeFileSync(draftForInitialAccept.draftPath, `${JSON.stringify(buildValidV3Draft(feature), null, 2)}\n`);
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
  assert.equal(typeof approve.artifact.approvedHash, "string");

  runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);

  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, false);
  assert.equal(accepted.revision, 2);
  assert.equal(typeof accepted.hash, "string");

  const summary = runCli(tempRoot, ["summary", "--feature", feature]);
  assert.equal(summary.summary.artifacts.plan.approved, true);
  assert.equal(summary.summary.artifacts.plan.approvedRevision, 2);
  assert.equal(summary.summary.artifacts.plan.approvedHash, accepted.hash);
});

test("flow-log plan v3: changed accept bumps revision and invalidates approval metadata", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-accept-change-");
  const feature = uniqueFeature("accept-change");

  const init = runCli(tempRoot, ["init-plan", "--feature", feature]);

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

  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(buildValidV3Draft(feature), null, 2)}\n`);

  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, true);
  assert.equal(accepted.revision, 2);
  assert.equal(typeof accepted.hash, "string");
  assert.equal(accepted.approvalInvalidation.invalidated, true);

  const state = runCli(tempRoot, ["get", "--feature", feature]);
  assert.equal(state.state.artifacts.plan.approved, false);
  assert.equal(state.state.artifacts.plan.approvedRevision, null);
  assert.equal(state.state.artifacts.plan.approvedHash, null);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.revision, 2);
  assert.equal(summary.sectionCounts.slices, 1);
  assert.equal(summary.approval.approved, false);
  assert.equal(summary.approval.currentRevision, 2);
});

test("flow-log plan v3: accept succeeds when open risk plan refs still exist", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-risk-pass-");
  const feature = uniqueFeature("risk-pass");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Model reference must remain stable",
    "--plan-ref",
    "M1",
    "--by",
    "ArchitectureReviewer"
  ]);

  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(buildValidV3Draft(feature), null, 2)}\n`);

  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, true);
});

test("flow-log plan v3: accept fails with non-zero exit when open risk plan refs disappear", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-risk-fail-");
  const feature = uniqueFeature("risk-fail");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Do not remove model id M1",
    "--plan-ref",
    "M1",
    "--by",
    "ArchitectureReviewer"
  ]);

  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  const changed = buildValidV3Draft(feature);
  changed.models[0].id = "M2";
  changed.slices[0].covers.models = ["M2"];
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const accepted = runCliRaw(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.notEqual(accepted.status, 0);
  assert.match(accepted.stderr, /Risk 1 \(OPEN\) references missing plan IDs: M1/);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.revision, 1);
});

test("flow-log plan v3: addressed risks still enforce plan ref stability until reviewer resolves them", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-addressed-risk-");
  const feature = uniqueFeature("addressed-risk");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Keep model id M1 stable while review is open",
    "--plan-ref",
    "M1",
    "--by",
    "ArchitectureReviewer"
  ]);
  runCli(tempRoot, [
    "respond-risk",
    "--feature",
    feature,
    "--id",
    "1",
    "--status",
    "ADDRESSED",
    "--note",
    "Handled in draft",
    "--by",
    "JavaArchitect"
  ]);

  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  const changed = buildValidV3Draft(feature);
  changed.models[0].id = "M2";
  changed.slices[0].covers.models = ["M2"];
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const accepted = runCliRaw(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.notEqual(accepted.status, 0);
  assert.match(accepted.stderr, /Risk 1 \(ADDRESSED\) references missing plan IDs: M1/);
});

test("flow-log plan v3: connected areas are enforced during validation and accept", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-connected-area-");
  const feature = uniqueFeature("connected-area");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Changes in the model also affect a related area",
    "--connected-area",
    "M1",
    "--by",
    "ArchitectureReviewer"
  ]);

  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  const changed = buildValidV3Draft(feature);
  changed.models[0].id = "M2";
  changed.slices[0].covers.models = ["M2"];
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) => issue.includes("Risk 1 (OPEN) references missing connected plan IDs: M1")));

  const accepted = runCliRaw(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.notEqual(accepted.status, 0);
  assert.match(accepted.stderr, /Risk 1 \(OPEN\) references missing connected plan IDs: M1/);
});

test("flow-log plan v3: resolved risks do not block missing plan refs on accept", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-risk-resolved-");
  const feature = uniqueFeature("risk-resolved");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  runCli(tempRoot, ["create", "--feature", feature]);
  runCli(tempRoot, [
    "add-risk",
    "--feature",
    feature,
    "--severity",
    "HIGH",
    "--description",
    "Reference M1 while open",
    "--plan-ref",
    "M1",
    "--by",
    "ArchitectureReviewer"
  ]);
  runCli(tempRoot, [
    "respond-risk",
    "--feature",
    feature,
    "--id",
    "1",
    "--status",
    "ADDRESSED",
    "--note",
    "Handled",
    "--by",
    "JavaArchitect"
  ]);
  runCli(tempRoot, [
    "resolve-risk",
    "--feature",
    feature,
    "--id",
    "1",
    "--by",
    "ArchitectureReviewer"
  ]);

  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);
  const changed = buildValidV3Draft(feature);
  changed.models[0].id = "M2";
  changed.slices[0].covers.models = ["M2"];
  fs.writeFileSync(draft.draftPath, `${JSON.stringify(changed, null, 2)}\n`);

  const accepted = runCli(tempRoot, ["plan-accept-draft", "--feature", feature]);
  assert.equal(accepted.accepted, true);
  assert.equal(accepted.changed, true);
});

test("flow-log plan v3: plan-summary exposes approval metadata vs current revision", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-summary-");
  const feature = uniqueFeature("summary");

  const init = runCli(tempRoot, ["init-plan", "--feature", feature]);

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

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.approval.approved, true);
  assert.equal(summary.approval.approvedRevision, 1);
  assert.equal(summary.approval.currentRevision, 1);
  assert.equal(summary.approval.stale, false);
});

test("flow-log legacy v2 plan commands remain available", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v2-legacy-");
  const feature = uniqueFeature("legacy");
  const planPath = path.join(tempRoot, "artifacts", "implementation-plans", `${feature}.plan.json`);

  fs.writeFileSync(planPath, `${JSON.stringify(buildLegacyV2Plan(feature), null, 2)}\n`);

  const addClass = runCli(tempRoot, [
    "add-plan-class",
    "--feature",
    feature,
    "--path",
    "src/main/java/com/example/LegacyService.java",
    "--status",
    "new",
    "--role",
    "legacy test"
  ]);
  assert.equal(addClass.legacy, true);
  assert.equal(addClass.totalClasses, 1);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", feature]);
  assert.equal(summary.legacy, true);
  assert.equal(summary.schemaVersion, "2.0");

  const draftWorkflow = runCliRaw(tempRoot, ["plan-create-draft", "--feature", feature]);
  assert.notEqual(draftWorkflow.status, 0);
  assert.match(draftWorkflow.stderr, /v3 draft commands require schemaVersion '3.0'/);
});

test("flow-log plan-init-draft creates canonical plan and draft in one step", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-init-draft-");
  const feature = uniqueFeature("init-draft");

  const result = runCli(tempRoot, ["plan-init-draft", "--feature", feature]);
  assert.equal(result.ok, true);
  assert.equal(result.command, "plan-init-draft");
  assert.equal(result.schemaVersion, "3.0");
  assert.equal(result.draftCreated, true);
  assert.ok(result.planPath);
  assert.ok(result.draftPath);

  // Verify both plan and draft exist by running status
  const draftStatus = runCli(tempRoot, ["plan-draft-status", "--feature", feature]);
  assert.equal(draftStatus.ok, true);
  assert.equal(draftStatus.exists, true);
});

test("flow-log plan-init-draft rejects if plan exists without --force", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-init-draft-exists-");
  const feature = uniqueFeature("init-draft-exists");

  runCli(tempRoot, ["init-plan", "--feature", feature]);

  const raw = runCliRaw(tempRoot, ["plan-init-draft", "--feature", feature]);
  assert.notEqual(raw.status, 0);
  assert.match(raw.stderr, /Plan already exists/);
});

test("flow-log plan v3: validator warns on existing models with too many fields", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-existing-model-");
  const feature = uniqueFeature("existing-bloat");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);

  const plan = buildValidV3Draft(feature);
  // Add an existing model with 5 fields (exceeds limit of 3)
  plan.models.push({
    id: "M2",
    qualifiedName: "com.gitlabflow.existing.BigModel",
    kind: "record",
    status: "existing",
    purpose: "reused unchanged",
    placementJustification: "already placed correctly",
    ownedBySlices: ["S1"],
    fields: [
      { name: "a", type: "String", nullable: false },
      { name: "b", type: "String", nullable: false },
      { name: "c", type: "String", nullable: false },
      { name: "d", type: "String", nullable: false },
      { name: "e", type: "String", nullable: false }
    ]
  });
  plan.slices[0].covers.models.push("M2");

  fs.writeFileSync(draft.draftPath, `${JSON.stringify(plan, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  assert.equal(validation.valid, false);
  assert.ok(validation.issues.some((issue) =>
    issue.includes("has status 'existing' but lists 5 fields")
  ));
});

test("flow-log plan v3: validator accepts plan without implementationFlow and contractExamples", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-v3-optional-sections-");
  const feature = uniqueFeature("optional-sections");

  runCli(tempRoot, ["init-plan", "--feature", feature]);
  const draft = runCli(tempRoot, ["plan-create-draft", "--feature", feature]);

  const plan = buildValidV3Draft(feature);
  // Remove optional sections
  delete plan.implementationFlow;
  delete plan.contractExamples;
  // Clear flowSteps and example refs in slice since sections are absent
  plan.slices[0].flowSteps = ["F1"];  // Will fail cross-ref but that's separate
  plan.slices[0].covers.examples = [];
  // Remove the flowSteps ref since implementationFlow is gone
  plan.slices[0].flowSteps = [];

  fs.writeFileSync(draft.draftPath, `${JSON.stringify(plan, null, 2)}\n`);

  const validation = runCli(tempRoot, ["plan-validate-draft", "--feature", feature]);
  // Should fail only on "Slice must reference at least one flow step" — not on missing top-level keys
  assert.ok(!validation.issues.some((issue) => issue.includes("Missing top-level key 'implementationFlow'")));
  assert.ok(!validation.issues.some((issue) => issue.includes("Missing top-level key 'contractExamples'")));
});

function uniqueFeature(prefix) {
  return `${prefix}-${process.pid}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

function buildValidV3Draft(feature) {
  return {
    schemaVersion: "3.0",
    feature,
    revision: 1,
    status: "draft",
    scope: {
      purpose: "Enable flow-log v3 planning workflow",
      inScope: ["plan workflow"],
      outOfScope: ["migration tooling"],
      constraints: ["single canonical artifact"]
    },
    implementationFlow: [
      {
        id: "F1",
        order: 1,
        slice: "S1",
        layer: "orchestration",
        component: "PlanService",
        responsibility: "Validate and accept draft",
        constitutionReason: "Keep plan lifecycle deterministic",
        outputs: ["canonical plan"]
      }
    ],
    contractExamples: [
      {
        id: "EX1",
        label: "accept draft request",
        type: "request",
        body: {
          feature,
          command: "plan-accept-draft"
        }
      }
    ],
    validationRules: [
      {
        id: "VR1",
        rule: "plan refs must remain stable for open risks",
        owner: "flow-log",
        reason: "preserve review traceability",
        appliesToSlices: ["S1"]
      }
    ],
    designDecisions: [
      {
        id: "D1",
        title: "Use full-draft editing",
        decision: "Architect edits one full draft and tool accepts atomically",
        rationale: "Global architecture reasoning works better on whole plans",
        alternatives: ["section-only editing"]
      }
    ],
    models: [
      {
        id: "M1",
        qualifiedName: "com.gitlabflow.plan.model.PlanDraft",
        kind: "record",
        status: "new",
        purpose: "draft lifecycle input",
        placementJustification: "plan workflow domain model",
        ownedBySlices: ["S1"]
      }
    ],
    classes: [
      {
        id: "C1",
        path: "flow-log/src/plan/accept.mjs",
        status: "modified",
        role: "accept and invalidate stale approval",
        ownedBySlices: ["S1"]
      }
    ],
    slices: [
      {
        id: "S1",
        title: "Draft acceptance",
        goal: "validate and accept plan draft",
        dependsOn: [],
        flowSteps: ["F1"],
        covers: {
          models: ["M1"],
          classes: ["C1"],
          validationRules: ["VR1"],
          examples: ["EX1"],
          decisions: ["D1"]
        },
        implementationTasks: ["add validators", "wire accept command"],
        tests: {
          unit: ["plan-cli.test.mjs"],
          integration: [],
          component: []
        },
        logging: {
          info: ["accept command summary"],
          warn: [],
          error: []
        },
        doneWhen: ["accept command updates canonical revision"],
        flags: {
          contractChanges: true,
          validationSensitive: true
        }
      }
    ],
    verification: {
      slices: [
        {
          slice: "S1",
          checks: ["unit"],
          evidence: "plan-cli tests"
        }
      ],
      finalGates: [
        {
          gate: "finalCheck",
          owner: "TL",
          required: true,
          evidence: "flow-log npm test"
        }
      ]
    },
    hash: null
  };
}

function buildLegacyV2Plan(feature) {
  return {
    schemaVersion: "2.0",
    feature,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    revision: 1,
    status: "draft",
    payloadExamples: [],
    validationBoundary: [],
    models: [],
    classes: [],
    compositionStrategy: null,
    sharedInfra: { reused: [], new: [] },
    slices: [],
    testingMatrix: [],
    karate: null,
    archUnit: null
  };
}
