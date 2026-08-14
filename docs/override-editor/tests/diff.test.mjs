import { test } from "node:test";
import { strict as assert } from "node:assert";
import { computeSparsePatch } from "../lib/diff.js";

function editable(id, runtimeFacets, baseline, edited) {
  return { id, mod: id.split(":")[0], className: "", displayName: id,
           creativeTabs: [], runtimeFacets, baseline, edited, dirty: false, missingFromDump: false };
}

test("emits only changed fields per item", () => {
  const items = [
    editable("a:x", [],
      { category: "c", subcategory: "s", facets: [], tooltipLines: [] },
      { category: "c", subcategory: "S2", facets: [], tooltipLines: [] }),
    editable("a:y", [],
      { category: "c", subcategory: "s", facets: [], tooltipLines: [] },
      { category: "c", subcategory: "s", facets: [], tooltipLines: [] }),  // unchanged
  ];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.equal(patch.schemaVersion, 1);
  assert.deepEqual(Object.keys(patch.items), ["a:x"]);
  assert.equal(patch.items["a:x"].subcategory, "S2");
  assert.equal(patch.items["a:x"].category, undefined);
});

test("emits tooltipLines when edited", () => {
  const items = [editable("a:x", [],
    { category: "c", subcategory: "s", facets: [], tooltipLines: [] },
    { category: "c", subcategory: "s", facets: [], tooltipLines: ["hi"] })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.deepEqual(patch.items["a:x"].tooltipLines, ["hi"]);
});

test("computes addFacets / removeFacets against runtime baseline, not edited baseline", () => {
  // Runtime emits ["runtime_only"]. Override previously added "kept_add". User edits to drop
  // "runtime_only" and keep "kept_add" + introduces "new_add".
  const items = [editable("a:x",
    ["runtime_only"],
    { category: null, subcategory: null, facets: ["kept_add"],            tooltipLines: [] },
    { category: null, subcategory: null, facets: ["kept_add", "new_add"], tooltipLines: [] })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  // Diff is computed against runtimeFacets, so the saved override layer reconstructs
  // the user's intent from the bare runtime: add both "kept_add" and "new_add", remove "runtime_only".
  assert.deepEqual(patch.items["a:x"].addFacets.sort(), ["kept_add", "new_add"]);
  assert.deepEqual(patch.items["a:x"].removeFacets, ["runtime_only"]);
});

test("no-op edits against runtime emit no facet diff", () => {
  const items = [editable("a:x",
    ["r"],
    { category: null, subcategory: null, facets: ["r"], tooltipLines: [] },
    { category: null, subcategory: null, facets: ["r"], tooltipLines: [] })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.equal(patch.items["a:x"], undefined);
});

test("passes modPatterns through unchanged", () => {
  const patch = computeSparsePatch([], { modPatterns: [{ mod: "m", pathTokens: ["x"] }] });
  assert.equal(patch.modPatterns.length, 1);
  assert.equal(patch.modPatterns[0].mod, "m");
});

test("keeps unchanged visibility fields in item patches", () => {
  const items = [editable("a:x", [],
    { category: null, subcategory: null, facets: [], tooltipLines: [], accessLevel: "dev", visibility: "hidden" },
    { category: null, subcategory: null, facets: [], tooltipLines: [], accessLevel: "dev", visibility: "hidden" })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.equal(patch.items["a:x"].accessLevel, "dev");
  assert.equal(patch.items["a:x"].visibility, "hidden");
});
