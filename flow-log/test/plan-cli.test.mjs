import test from "node:test";
import assert from "node:assert/strict";
import { createPlanTempRoot, runCli, runCliRaw } from "./test-helpers.mjs";

test("flow-log plan: full lifecycle — all sections populated, validate, summary, plan-get", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-full-");

  const init = runCli(tempRoot, ["init-plan", "--feature", "demo"]);
  assert.equal(init.ok, true);
  assert.equal(init.revision, 1);

  const example = runCli(tempRoot, [
    "add-plan-example", "--feature", "demo",
    "--label", "Search request",
    "--type", "request",
    "--body", '{"pagination":{"page":1},"filters":{"audit":["label"]}}'
  ]);
  assert.equal(example.ok, true);
  assert.equal(example.total, 1);
  assert.deepEqual(example.entry.body, { pagination: { page: 1 }, filters: { audit: ["label"] } });

  const validation = runCli(tempRoot, [
    "add-plan-validation", "--feature", "demo",
    "--rule", "perPage <= 40",
    "--boundary", "IssuesService",
    "--reason", "Existing runtime guard"
  ]);
  assert.equal(validation.ok, true);
  assert.equal(validation.total, 1);

  const model = runCli(tempRoot, [
    "add-plan-model", "--feature", "demo",
    "--qualified-name", "com.example.Issue",
    "--type", "record",
    "--status", "modified",
    "--justification", "Orchestration entity per constitution Principle 2",
    "--annotations", "@Builder",
    "--fields", '[{"name":"id","type":"long"},{"name":"labels","type":"List<String>","nullable":false,"defensiveCopy":true}]',
    "--notes", "Defensive copy on labels"
  ]);
  assert.equal(model.ok, true);
  assert.equal(model.totalModels, 1);
  assert.equal(model.entry.fields.length, 2);
  assert.equal(model.entry.justification, "Orchestration entity per constitution Principle 2");

  const field = runCli(tempRoot, [
    "add-plan-model-field", "--feature", "demo",
    "--model", "com.example.Issue",
    "--name", "changeSets",
    "--type", "List<ChangeSet>",
    "--nullable",
    "--defensive-copy"
  ]);
  assert.equal(field.ok, true);
  assert.equal(field.field.nullable, true);
  assert.equal(field.field.defensiveCopy, true);

  runCli(tempRoot, [
    "add-plan-model", "--feature", "demo",
    "--qualified-name", "com.example.AuditType",
    "--type", "enum",
    "--status", "new",
    "--justification", "Domain concept in orchestration",
    "--values", "LABEL",
    "--methods", "String value(),static AuditType fromValue(String raw)"
  ]);

  runCli(tempRoot, [
    "add-plan-class", "--feature", "demo",
    "--path", "src/main/java/IssuesService.java",
    "--status", "modified",
    "--role", "Search with audit"
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "demo",
    "--path", "src/main/java/IssuesController.java",
    "--status", "modified",
    "--role", "Delegate search input"
  ]);

  const composition = runCli(tempRoot, [
    "set-plan-composition", "--feature", "demo",
    "--approach", "dependent-then-parallel",
    "--description", "Search first, then parallel enrichment"
  ]);
  assert.equal(composition.compositionStrategy.approach, "dependent-then-parallel");

  const infra = runCli(tempRoot, [
    "set-plan-infra", "--feature", "demo",
    "--reused", "AsyncComposer,GitLabExceptionMapper"
  ]);
  assert.deepEqual(infra.sharedInfra.reused, ["AsyncComposer", "GitLabExceptionMapper"]);

  const slice = runCli(tempRoot, [
    "add-plan-slice", "--feature", "demo",
    "--id", "1",
    "--title", "Orchestration models",
    "--goal", "Add SearchIssuesInput and wire service",
    "--files", "SearchIssuesInput.java,IssuesService.java",
    "--unit-test", "IssuesServiceTest: search with audit",
    "--unit-test", "IssuesServiceTest: search without audit",
    "--component-test", "IssuesApiComponentTest: audit returns changeSets",
    "--info-log", "IssuesService logs audit types",
    "--warn-log", "None",
    "--error-log", "None"
  ]);
  assert.equal(slice.ok, true);
  assert.equal(slice.entry.tests.unit.length, 2);
  assert.equal(slice.entry.tests.component.length, 1);
  assert.equal(slice.entry.logging.info, "IssuesService logs audit types");

  runCli(tempRoot, [
    "add-plan-slice-test", "--feature", "demo",
    "--slice", "1",
    "--level", "unit",
    "--test", "IssuesServiceTest: failure propagation"
  ]);

  runCli(tempRoot, [
    "set-plan-slice-logging", "--feature", "demo",
    "--slice", "1",
    "--error", "IssuesService logs enrichment failure"
  ]);

  runCli(tempRoot, [
    "add-plan-test", "--feature", "demo",
    "--level", "Unit", "--required", "--coverage", "IssuesServiceTest"
  ]);
  runCli(tempRoot, [
    "add-plan-test", "--feature", "demo",
    "--level", "Component", "--required", "--coverage", "IssuesApiComponentTest"
  ]);

  const karate = runCli(tempRoot, [
    "set-plan-karate", "--feature", "demo",
    "--feature-file", "src/test/karate/resources/issues/search-audit.feature",
    "--scenario", "Search with label audit",
    "--scenario", "Search without audit",
    "--smoke-tagged"
  ]);
  assert.equal(karate.karate.smokeTagged, true);
  assert.equal(karate.karate.scenarios.length, 2);

  const archUnit = runCli(tempRoot, [
    "set-plan-archunit", "--feature", "demo",
    "--existing-reviewed"
  ]);
  assert.equal(archUnit.archUnit.existingRulesReviewed, true);
  assert.deepEqual(archUnit.archUnit.newRules, []);

  const valid = runCli(tempRoot, ["validate-plan", "--feature", "demo"]);
  assert.equal(valid.valid, true);
  assert.deepEqual(valid.issues, []);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", "demo"]);
  assert.equal(summary.modelCount, 2);
  assert.equal(summary.classCount, 2);
  assert.equal(summary.sliceCount, 1);
  assert.equal(summary.exampleCount, 1);
  assert.equal(summary.hasComposition, true);
  assert.equal(summary.hasKarate, true);

  const full = runCli(tempRoot, ["plan-get", "--feature", "demo"]);
  assert.ok(full.plan);
  assert.equal(full.plan.schemaVersion, "2.0");
  assert.equal(full.plan.models[0].fields.length, 3);

  const modelsSection = runCli(tempRoot, ["plan-get", "--feature", "demo", "--section", "models"]);
  assert.equal(modelsSection.section, "models");
  assert.equal(modelsSection.data.length, 2);

  const slicesSection = runCli(tempRoot, ["plan-get", "--feature", "demo", "--section", "slices"]);
  assert.equal(slicesSection.data[0].tests.unit.length, 3);
  assert.equal(slicesSection.data[0].logging.error, "IssuesService logs enrichment failure");
});

test("flow-log plan: revise-plan clears all sections and bumps revision", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-revise-");

  runCli(tempRoot, ["init-plan", "--feature", "rev"]);
  runCli(tempRoot, [
    "add-plan-model", "--feature", "rev",
    "--qualified-name", "com.example.Foo", "--type", "record", "--status", "new",
    "--justification", "Test model",
    "--fields", '[{"name":"id","type":"long"}]'
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "rev",
    "--path", "Foo.java", "--status", "new", "--role", "Test"
  ]);
  runCli(tempRoot, [
    "add-plan-slice", "--feature", "rev",
    "--id", "1", "--title", "Slice 1", "--goal", "Do something"
  ]);

  const revised = runCli(tempRoot, ["revise-plan", "--feature", "rev"]);
  assert.equal(revised.previousRevision, 1);
  assert.equal(revised.newRevision, 2);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", "rev"]);
  assert.equal(summary.revision, 2);
  assert.equal(summary.modelCount, 0);
  assert.equal(summary.classCount, 0);
  assert.equal(summary.sliceCount, 0);
  assert.equal(summary.hasComposition, false);

  const invalid = runCli(tempRoot, ["validate-plan", "--feature", "rev"]);
  assert.equal(invalid.valid, false);
  assert.ok(invalid.issues.length >= 3);
});

test("flow-log plan: validate catches missing justification and empty record fields", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-val-");

  runCli(tempRoot, ["init-plan", "--feature", "val"]);
  runCli(tempRoot, [
    "add-plan-model", "--feature", "val",
    "--qualified-name", "com.example.Empty", "--type", "record", "--status", "new",
    "--justification", "   "
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "val",
    "--path", "Empty.java", "--status", "new"
  ]);
  runCli(tempRoot, [
    "add-plan-slice", "--feature", "val",
    "--id", "1", "--title", "S1", "--goal", "Goal"
  ]);

  const result = runCli(tempRoot, ["validate-plan", "--feature", "val"]);
  assert.equal(result.valid, false);
  assert.ok(result.issues.some((issue) => issue.includes("justification")));
  assert.ok(result.issues.some((issue) => issue.includes("no fields")));
});

test("flow-log plan: duplicate class updates in place", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-dup-");

  runCli(tempRoot, ["init-plan", "--feature", "dup-test"]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "dup-test",
    "--path", "MyService.java", "--status", "new", "--role", "Original role"
  ]);

  const updated = runCli(tempRoot, [
    "add-plan-class", "--feature", "dup-test",
    "--path", "MyService.java", "--status", "modified", "--role", "Updated role"
  ]);
  assert.equal(updated.totalClasses, 1);
  assert.equal(updated.entry.status, "modified");
  assert.equal(updated.entry.role, "Updated role");
});

test("flow-log plan: init-plan fails without --force, succeeds with --force", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-force-");

  runCli(tempRoot, ["init-plan", "--feature", "force-test"]);

  const result = runCliRaw(tempRoot, ["init-plan", "--feature", "force-test"]);
  assert.notEqual(result.status, 0);
  assert.ok(result.stderr.includes("Plan already exists"));

  const forced = runCli(tempRoot, ["init-plan", "--feature", "force-test", "--force"]);
  assert.equal(forced.ok, true);
  assert.equal(forced.revision, 1);
});

test("flow-log plan: slice with Java files but no tests triggers validation warning", () => {
  const tempRoot = createPlanTempRoot("flow-log-plan-slicetest-");

  runCli(tempRoot, ["init-plan", "--feature", "st"]);
  runCli(tempRoot, [
    "add-plan-model", "--feature", "st",
    "--qualified-name", "com.example.M", "--type", "enum", "--status", "new",
    "--justification", "Test", "--values", "A"
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "st",
    "--path", "M.java", "--status", "new"
  ]);
  runCli(tempRoot, [
    "add-plan-slice", "--feature", "st",
    "--id", "1", "--title", "S1", "--goal", "Goal",
    "--files", "M.java,Service.java"
  ]);

  const result = runCli(tempRoot, ["validate-plan", "--feature", "st"]);
  assert.equal(result.valid, false);
  assert.ok(result.issues.some((issue) => issue.includes("no tests")));
});
