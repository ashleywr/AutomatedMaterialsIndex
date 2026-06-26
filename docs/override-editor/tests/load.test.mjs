import { test } from "node:test";
import { strict as assert } from "node:assert";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseRegistryDump, parseOverrides } from "../lib/load.js";

const here = dirname(fileURLToPath(import.meta.url));
const dumpText = readFileSync(resolve(here, "fixtures/registry-dump-small.json"), "utf8");
const ovText   = readFileSync(resolve(here, "fixtures/overrides-baseline.json"), "utf8");

test("parseRegistryDump parses fixture", () => {
  const r = parseRegistryDump(dumpText);
  assert.equal(r.schemaVersion, 1);
  assert.equal(r.items.length, 2);
  assert.equal(r.items[0].id, "minecraft:diamond_sword");
});

test("parseRegistryDump rejects future schema version", () => {
  const bad = JSON.stringify({ schemaVersion: 2, items: [] });
  assert.throws(() => parseRegistryDump(bad), /schema/i);
});

test("parseOverrides parses fixture", () => {
  const r = parseOverrides(ovText);
  assert.equal(r.schemaVersion, 1);
  assert.equal(r.items.size, 1);
  assert.equal(r.items.get("modid:gizmo").subcategory, "fancy");
});

test("parseOverrides treats missing schemaVersion as 1", () => {
  const r = parseOverrides(JSON.stringify({ items: {} }));
  assert.equal(r.schemaVersion, 1);
});
