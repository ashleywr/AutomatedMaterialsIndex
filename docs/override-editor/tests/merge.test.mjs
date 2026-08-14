import { test } from "node:test";
import { strict as assert } from "node:assert";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseRegistryDump, parseOverrides } from "../lib/load.js";
import { mergeForEditing } from "../lib/merge.js";

const here = dirname(fileURLToPath(import.meta.url));
const dump = parseRegistryDump(readFileSync(resolve(here, "fixtures/registry-dump-small.json"), "utf8"));
const ov   = parseOverrides(readFileSync(resolve(here, "fixtures/overrides-baseline.json"), "utf8"));

test("merges dump row with override for same id, baseline reflects override", () => {
  const merged = mergeForEditing(dump, ov);
  const gizmo = merged.find(i => i.id === "modid:gizmo");
  assert.equal(gizmo.baseline.subcategory, "fancy");
  assert.deepEqual(gizmo.baseline.tooltipLines, ["Crafted with love"]);
  assert.equal(gizmo.dirty, false);
  assert.equal(gizmo.missingFromDump, false);
});

test("runtimeFacets reflects the dump verbatim, independent of overrides", () => {
  const dumpWithFacets = {
    schemaVersion: 1,
    items: [{
      id: "m:x", mod: "m", className: "X", displayName: "X",
      creativeTabs: [], currentCategory: null, currentSubcategory: null,
      currentFacets: ["runtime_one"],
    }],
  };
  const ovWithFacetAdd = {
    schemaVersion: 1,
    items: new Map([["m:x", {
      category: null, subcategory: null,
      addFacets: ["override_added"], removeFacets: [], tooltipLines: [],
    }]]),
    modPatterns: [],
  };
  const merged = mergeForEditing(dumpWithFacets, ovWithFacetAdd);
  const x = merged[0];
  assert.deepEqual(x.runtimeFacets, ["runtime_one"]);
  assert.deepEqual(x.baseline.facets.sort(), ["override_added", "runtime_one"]);
});

test("dump-only items appear with no override applied", () => {
  const merged = mergeForEditing(dump, ov);
  const sword = merged.find(i => i.id === "minecraft:diamond_sword");
  assert.equal(sword.baseline.category, "weapons");
  assert.deepEqual(sword.baseline.tooltipLines, []);
});

test("override-only items get missingFromDump=true", () => {
  const ovOnly = { ...ov, items: new Map([
    ...ov.items,
    ["modid:ghost", { category: "x", subcategory: null, addFacets: [], removeFacets: [], tooltipLines: [] }]
  ])};
  const merged = mergeForEditing(dump, ovOnly);
  const ghost = merged.find(i => i.id === "modid:ghost");
  assert.equal(ghost.missingFromDump, true);
});

test("visibility fields round-trip into the editable baseline", () => {
  const ovWithVisibility = {
    schemaVersion: 1,
    items: new Map([["minecraft:diamond_sword", {
      category: null, subcategory: null, addFacets: [], removeFacets: [], tooltipLines: [],
      accessLevel: "dev", visibility: "hidden",
    }]]),
    modPatterns: [],
  };
  const merged = mergeForEditing(dump, ovWithVisibility);
  const sword = merged.find(i => i.id === "minecraft:diamond_sword");
  assert.equal(sword.baseline.accessLevel, "dev");
  assert.equal(sword.baseline.visibility, "hidden");
});
