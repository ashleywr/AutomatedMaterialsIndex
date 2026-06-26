import { test } from "node:test";
import { strict as assert } from "node:assert";
import { validate } from "../lib/validate.js";
import { KNOWN_FACETS } from "../lib/constants.js";

test("warns on unknown facet ids", () => {
  const sample = { schemaVersion: 1, items: { "m:x": { addFacets: ["totally_made_up"] } }, modPatterns: [] };
  const issues = validate(sample, /* dump */ null);
  assert.ok(issues.some(i => i.severity === "warn" && i.message.includes("totally_made_up")));
});

test("warns on item ids missing from dump", () => {
  const sample = { schemaVersion: 1, items: { "m:ghost": { category: "x" } }, modPatterns: [] };
  const dump = { items: [{ id: "m:alive" }] };
  const issues = validate(sample, dump);
  assert.ok(issues.some(i => i.severity === "warn" && i.itemId === "m:ghost"));
});

test("errors on future schemaVersion", () => {
  const sample = { schemaVersion: 999, items: {}, modPatterns: [] };
  assert.ok(validate(sample, null).some(i => i.severity === "error"));
});

test("accepts a clean fixture without warnings", () => {
  const goodFacet = KNOWN_FACETS[0];
  const sample = { schemaVersion: 1, items: { "m:x": { addFacets: [goodFacet] } }, modPatterns: [] };
  const dump = { items: [{ id: "m:x" }] };
  const issues = validate(sample, dump);
  assert.equal(issues.length, 0);
});
