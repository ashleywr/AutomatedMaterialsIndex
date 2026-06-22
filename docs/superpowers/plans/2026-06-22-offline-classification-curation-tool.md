# Offline Classification-Curation Tool (Plan 3a) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the dev-time curation tool (`tools/classification-curation/`) that turns an `ami_dumps` capture into reviewed entries in the bundled `classification_overrides.json`, plus the Java replay-diff gate that proves the data changes only what it should.

**Architecture:** Six file-connected stages. Python does the deterministic plumbing — `detect.py` flags weak/contradictory items into `candidates.jsonl`, `evidence.py` joins the recipe graph + tabs + tags into `evidence_batch.jsonl`; a Claude Code agent reads that batch and writes `proposals.jsonl`; a human flips each proposal's `decision`; `apply.py` folds approvals into `classification_overrides.json` (rejects → a persistent ledger); and a Java test (`ClassificationOverrideReplayGateTest`) re-runs the real `PrimaryCategoryResolver` with vs. without the overrides and fails if any item changes that no override targets. All handoffs are diffable JSONL/JSON files; the runtime never reads the dump.

**Tech Stack:** Python 3 (standard library only — `json`, `unittest`; no third-party deps). Java 21 / JUnit 5 in the existing `neoforge:test` module, reusing `SearchNodeMirrorDump.reclassifyItemOntology` and the `ClassificationOverrides` registry.

## Global Constraints

- Python tool uses the **standard library only** — no pip dependencies; tests run with `python -m unittest`.
- The tool is **dev-time only**; its sole runtime product is committed override data. The runtime never reads `ami_dumps`.
- Output `classification_overrides.json` MUST match the shape the existing `ClassificationOverrides` consumer parses: a top-level object with `items` (id → `{category, subcategory, addFacets, removeFacets}`) and `modPatterns` (array of `{mod, pathTokens, category, subcategory}`). Item/pattern category and subcategory are plain strings; `addFacets`/`removeFacets`/`pathTokens` are string arrays.
- `modPatterns[].pathTokens` MUST be single tokens as produced by splitting an item path on `[_/]` and lowercasing (e.g. `"spreader"`, never `"mana_spreader"`); the Java consumer matches token-by-token.
- All ids are lowercased namespaced resource locations (`namespace:path`).
- Java classifier code stays pure-xplat; only **test** code lands in `neoforge/src/test`.
- No AI/assistant attribution in any commit message.
- The canonical local dump for running the gate is `C:/Users/ashle/AppData/Roaming/PrismLauncher/instances/Synesthesia/minecraft/ami_dumps/search/search_nodes.jsonl` (overridable via `-Dami.searchNodesDump=` or `AMI_SEARCH_NODES_DUMP`).

---

## File Structure

```
tools/classification-curation/
  .gitignore                 # ignore generated batch files; keep reject ledger + fixtures
  README.md                  # end-to-end run instructions + reject-ledger semantics
  schema.py                  # shared record IO + parsing helpers + detector constants
  detect.py                  # stage 1: search_nodes.jsonl -> candidates.jsonl
  evidence.py                # stage 2: candidates + recipes/tabs/tags -> evidence_batch.jsonl
  validate_proposals.py      # guard: proposals.jsonl conforms to the proposal schema
  apply.py                   # stage 5: approved proposals -> classification_overrides.json (+ reject ledger)
  prompts/
    propose.md               # stage 3: the Claude Code proposal-agent contract
  tests/
    fixtures/
      search_nodes.jsonl     # ~8 synthetic item nodes covering every detector
      recipes_runtime.jsonl  # ~4 synthetic recipes wiring fixture edges
    test_schema.py
    test_detect.py
    test_evidence.py
    test_validate_proposals.py
    test_apply.py

neoforge/src/test/java/com/sanhiruzu/ami/index/
  ClassificationOverrideReplayGateTest.java   # stage 6: replay-diff gate
```

Each Python module is one stage with one responsibility; `schema.py` holds the shared record/IO helpers so the stages stay DRY. Tests live beside the tool and import the modules by inserting the tool dir onto `sys.path` (the dir name contains a hyphen, so it is not importable as a package).

---

### Task 1: Scaffold + shared schema/IO helpers

**Files:**
- Create: `tools/classification-curation/.gitignore`
- Create: `tools/classification-curation/schema.py`
- Create: `tools/classification-curation/tests/test_schema.py`

**Interfaces:**
- Produces (all consumed by later tasks):
  - `read_jsonl(path: str) -> list[dict]` — parse a JSONL file, skipping blank lines.
  - `write_jsonl(path: str, rows: list[dict]) -> None` — write one compact JSON object per line, trailing newline.
  - `split_id(rid: str) -> tuple[str, str]` — `"botania:mana_spreader"` → `("botania", "mana_spreader")`; an id with no colon returns `("minecraft", rid)`.
  - `csv_list(value) -> list[str]` — split a comma-joined metadata string into a list; `None`, `""`, and the literal `"None"` all yield `[]`.
  - `WEAK_PHASES: set[str]` — `{"fallback", "evidence_fallback", "compat_fallback", "unknown"}`.
  - `TAB_EXPECTED_CATEGORIES: dict[str, set[str]]` — curator-extensible tab→allowed-category map, seeded `{"Combat": {"armor", "tools"}}`.

- [ ] **Step 1: Create `.gitignore` for generated artifacts**

Generated batch files are scratch; the reject ledger and fixtures are tracked.

```gitignore
# Generated pipeline artifacts (root-anchored so fixtures + ledger stay tracked)
/candidates.jsonl
/evidence_batch.jsonl
/proposals.jsonl
```

- [ ] **Step 2: Write the failing test**

`tools/classification-curation/tests/test_schema.py`:

```python
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import schema


class SchemaTest(unittest.TestCase):
    def test_jsonl_round_trip(self):
        rows = [{"id": "a:b", "n": 1}, {"id": "c:d", "n": 2}]
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "x.jsonl")
            schema.write_jsonl(p, rows)
            self.assertEqual(schema.read_jsonl(p), rows)

    def test_read_jsonl_skips_blank_lines(self):
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "x.jsonl")
            with open(p, "w", encoding="utf-8") as f:
                f.write('{"id":"a:b"}\n\n{"id":"c:d"}\n')
            self.assertEqual(schema.read_jsonl(p), [{"id": "a:b"}, {"id": "c:d"}])

    def test_split_id(self):
        self.assertEqual(schema.split_id("botania:mana_spreader"), ("botania", "mana_spreader"))
        self.assertEqual(schema.split_id("bareword"), ("minecraft", "bareword"))

    def test_csv_list_handles_empty_forms(self):
        self.assertEqual(schema.csv_list("placeable,stone_block"), ["placeable", "stone_block"])
        self.assertEqual(schema.csv_list(""), [])
        self.assertEqual(schema.csv_list(None), [])
        self.assertEqual(schema.csv_list("None"), [])

    def test_constants(self):
        self.assertIn("evidence_fallback", schema.WEAK_PHASES)
        self.assertEqual(schema.TAB_EXPECTED_CATEGORIES["Combat"], {"armor", "tools"})


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run test to verify it fails**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_schema.py" -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'schema'`.

- [ ] **Step 4: Write minimal implementation**

`tools/classification-curation/schema.py`:

```python
"""Shared record IO and parsing helpers for the classification-curation pipeline."""
import json

WEAK_PHASES = {"fallback", "evidence_fallback", "compat_fallback", "unknown"}

# Curator-extensible: when an item sits in a creative tab listed here but its
# classified category is not in the allowed set, detect.py flags a contradiction.
# Seeded only with the relationship verified from the dump (Combat -> armor/tools).
TAB_EXPECTED_CATEGORIES = {
    "Combat": {"armor", "tools"},
}


def read_jsonl(path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))
    return rows


def write_jsonl(path, rows):
    with open(path, "w", encoding="utf-8") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False))
            f.write("\n")


def split_id(rid):
    if ":" in rid:
        namespace, path = rid.split(":", 1)
        return namespace, path
    return "minecraft", rid


def csv_list(value):
    if not value:
        return []
    if value == "None":
        return []
    return [part for part in value.split(",") if part]
```

- [ ] **Step 5: Run test to verify it passes**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_schema.py" -v`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add tools/classification-curation/.gitignore tools/classification-curation/schema.py tools/classification-curation/tests/test_schema.py
git commit -m "feat: scaffold classification-curation tool with shared schema helpers"
```

---

### Task 2: `detect.py` — flag review candidates

**Files:**
- Create: `tools/classification-curation/detect.py`
- Create: `tools/classification-curation/tests/fixtures/search_nodes.jsonl`
- Create: `tools/classification-curation/tests/test_detect.py`

**Interfaces:**
- Consumes: `schema.read_jsonl`, `schema.write_jsonl`, `schema.split_id`, `schema.csv_list`, `schema.WEAK_PHASES`, `schema.TAB_EXPECTED_CATEGORIES`.
- Produces:
  - `detectors_for(node: dict, reject_ids: set[str]) -> list[str]` — returns the sorted detector names that fire for one search node (`[]` if none / if rejected). Detector names: `weak_route`, `empty_facets`, `mod_pseudo_category`, `tab_contradiction`.
  - `candidate_record(node: dict, detectors: list[str]) -> dict` — the `candidates.jsonl` row.
  - `load_reject_ids(path: str) -> set[str]` — reads a reject ledger (JSONL of `{"id": ...}`), returns the id set; missing file → empty set.
  - `run(nodes_path: str, out_path: str, reject_path: str | None) -> int` — writes `candidates.jsonl`, returns the candidate count.
  - CLI: `python detect.py <search_nodes.jsonl> <candidates.jsonl> [reject_ledger.jsonl]`.

Candidate row shape:
```json
{"id":"minecraft:fire_charge","mod":"minecraft","path":"fire_charge",
 "category":"utility","subcategory":"","routePhase":"evidence_fallback","routeRule":"",
 "facets":["projectile"],"tab":"Combat","tabId":"minecraft:combat","detectors":["tab_contradiction","weak_route"]}
```

Detector rules (each reads `node["metadata"]`):
- `weak_route`: `classificationRoutePhase` ∈ `WEAK_PHASES`, OR `ontologyCategory` is missing/empty.
- `empty_facets`: `csv_list(metadata.get("facets"))` is empty.
- `mod_pseudo_category`: `ontologyCategory` equals the id's namespace (e.g. `swem:saddle` classified as category `swem`).
- `tab_contradiction`: `creativeTabLabel` ∈ `TAB_EXPECTED_CATEGORIES`, `ontologyCategory` is non-empty, and `ontologyCategory` ∉ the allowed set.

Only `type == "ITEM"` nodes are considered; ids in the reject ledger are skipped entirely.

- [ ] **Step 1: Write the fixture nodes**

`tools/classification-curation/tests/fixtures/search_nodes.jsonl` — one JSON object per line (write each as a single line; shown indented here only for readability):

```json
{"id":"minecraft:stone","type":"ITEM","displayName":"Stone","metadata":{"facets":"placeable,stone_block","ontologyCategory":"geology","ontologySubcategory":"stone","classificationRoutePhase":"primary_rule","classificationRouteRule":"geology blocks","creativeTabLabel":"Building Blocks","creativeTabId":"minecraft:building_blocks","modId":"minecraft"},"unresolvedEdges":{"OUTPUT_OF":["minecraft:stone_from_smelting"],"USED_IN":["minecraft:stone_bricks"]}}
{"id":"minecraft:fire_charge","type":"ITEM","displayName":"Fire Charge","metadata":{"facets":"projectile","ontologyCategory":"utility","ontologySubcategory":"","classificationRoutePhase":"evidence_fallback","classificationRouteRule":"","creativeTabLabel":"Combat","creativeTabId":"minecraft:combat","modId":"minecraft"},"unresolvedEdges":{"USED_IN":["minecraft:fire_charge"]}}
{"id":"minecraft:goat_horn","type":"ITEM","displayName":"Goat Horn","metadata":{"facets":"","ontologyCategory":"utility","ontologySubcategory":"","classificationRoutePhase":"evidence_fallback","classificationRouteRule":"","creativeTabLabel":"Tools & Utilities","creativeTabId":"minecraft:tools_and_utilities","modId":"minecraft"},"unresolvedEdges":{}}
{"id":"swem:saddle","type":"ITEM","displayName":"English Saddle","metadata":{"facets":"wearable","ontologyCategory":"swem","ontologySubcategory":"","classificationRoutePhase":"compat_fallback","classificationRouteRule":"","creativeTabLabel":"SWEM","creativeTabId":"swem:tab","modId":"swem"},"unresolvedEdges":{"OUTPUT_OF":["swem:english_saddle"]}}
{"id":"botania:mana_spreader","type":"ITEM","displayName":"Mana Spreader","metadata":{"facets":"placeable","ontologyCategory":"tech","ontologySubcategory":"","classificationRoutePhase":"primary_rule","classificationRouteRule":"tech blocks","creativeTabLabel":"Botania","creativeTabId":"botania:tab","modId":"botania"},"unresolvedEdges":{"OUTPUT_OF":["botania:mana_spreader"]}}
{"id":"create:cogwheel","type":"ITEM","displayName":"Cogwheel","metadata":{"facets":"placeable,kinetic","ontologyCategory":"create","ontologySubcategory":"","classificationRoutePhase":"primary_rule","classificationRouteRule":"create kinetics","creativeTabLabel":"Create's Building Blocks","creativeTabId":"create:base","modId":"create"},"unresolvedEdges":{"OUTPUT_OF":["create:crafting/kinetics/cogwheel"]}}
{"id":"alexscaves:ferromental","type":"ITEM","displayName":"Ferromental","metadata":{"facets":"","ontologyCategory":"","ontologySubcategory":"","classificationRoutePhase":"unknown","classificationRouteRule":"","creativeTabLabel":"Alex's Caves","creativeTabId":"alexscaves:tab","modId":"alexscaves"},"unresolvedEdges":{}}
{"id":"farmersdelight:cabbage","type":"ITEM","displayName":"Cabbage","metadata":{"facets":"edible","ontologyCategory":"nature","ontologySubcategory":"crop","classificationRoutePhase":"hard_identity","classificationRouteRule":"food","creativeTabLabel":"Food & Drinks","creativeTabId":"minecraft:food_and_drinks","modId":"farmersdelight"},"unresolvedEdges":{"OUTPUT_OF":["farmersdelight:cabbage_from_seeds"],"USED_IN":["farmersdelight:cabbage_rolls"]}}
```

Expected detector outcomes: `minecraft:stone` → none; `minecraft:fire_charge` → `weak_route` (`tab_contradiction` does NOT fire — `utility` is not in Combat's allowed set, so it *does* fire... see note); `minecraft:goat_horn` → `empty_facets`+`weak_route`; `swem:saddle` → `mod_pseudo_category`+`weak_route`; `botania:mana_spreader` → none; `create:cogwheel` → `mod_pseudo_category`; `alexscaves:ferromental` → `empty_facets`+`weak_route`; `farmersdelight:cabbage` → none.

Note for the implementer: `minecraft:fire_charge` has tab `Combat` and category `utility` ∉ `{armor, tools}`, so `tab_contradiction` fires too — its detectors are `["tab_contradiction", "weak_route"]`. The test below encodes the exact expected sets.

- [ ] **Step 2: Write the failing test**

`tools/classification-curation/tests/test_detect.py`:

```python
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import detect
import schema

FIXTURE = os.path.join(os.path.dirname(__file__), "fixtures", "search_nodes.jsonl")


class DetectTest(unittest.TestCase):
    def setUp(self):
        self.by_id = {n["id"]: n for n in schema.read_jsonl(FIXTURE)}

    def detectors(self, rid, reject=None):
        return detect.detectors_for(self.by_id[rid], reject or set())

    def test_clean_item_has_no_detectors(self):
        self.assertEqual(self.detectors("minecraft:stone"), [])
        self.assertEqual(self.detectors("botania:mana_spreader"), [])
        self.assertEqual(self.detectors("farmersdelight:cabbage"), [])

    def test_weak_route_and_tab_contradiction(self):
        self.assertEqual(self.detectors("minecraft:fire_charge"), ["tab_contradiction", "weak_route"])

    def test_empty_facets(self):
        self.assertEqual(self.detectors("minecraft:goat_horn"), ["empty_facets", "weak_route"])
        self.assertEqual(self.detectors("alexscaves:ferromental"), ["empty_facets", "weak_route"])

    def test_mod_pseudo_category(self):
        self.assertEqual(self.detectors("swem:saddle"), ["mod_pseudo_category", "weak_route"])
        self.assertEqual(self.detectors("create:cogwheel"), ["mod_pseudo_category"])

    def test_reject_ledger_suppresses(self):
        self.assertEqual(self.detectors("swem:saddle", {"swem:saddle"}), [])

    def test_run_writes_candidates(self):
        with tempfile.TemporaryDirectory() as d:
            out = os.path.join(d, "candidates.jsonl")
            count = detect.run(FIXTURE, out, None)
            rows = schema.read_jsonl(out)
            ids = {r["id"] for r in rows}
            self.assertEqual(count, len(rows))
            self.assertIn("swem:saddle", ids)
            self.assertNotIn("minecraft:stone", ids)
            row = next(r for r in rows if r["id"] == "minecraft:fire_charge")
            self.assertEqual(row["facets"], ["projectile"])
            self.assertEqual(row["mod"], "minecraft")
            self.assertEqual(row["path"], "fire_charge")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run test to verify it fails**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_detect.py" -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'detect'`.

- [ ] **Step 4: Write minimal implementation**

`tools/classification-curation/detect.py`:

```python
"""Stage 1: flag weak / contradictory items from a search_nodes.jsonl dump."""
import sys

import schema


def detectors_for(node, reject_ids):
    if node.get("type") != "ITEM":
        return []
    rid = node["id"]
    if rid in reject_ids:
        return []
    meta = node.get("metadata", {})
    namespace, _ = schema.split_id(rid)
    category = (meta.get("ontologyCategory") or "").strip()
    phase = (meta.get("classificationRoutePhase") or "").strip()
    tab = (meta.get("creativeTabLabel") or "").strip()

    found = set()
    if phase in schema.WEAK_PHASES or not category:
        found.add("weak_route")
    if not schema.csv_list(meta.get("facets")):
        found.add("empty_facets")
    if category and category == namespace:
        found.add("mod_pseudo_category")
    expected = schema.TAB_EXPECTED_CATEGORIES.get(tab)
    if expected is not None and category and category not in expected:
        found.add("tab_contradiction")
    return sorted(found)


def candidate_record(node, detectors):
    meta = node.get("metadata", {})
    mod, path = schema.split_id(node["id"])
    return {
        "id": node["id"],
        "mod": mod,
        "path": path,
        "category": (meta.get("ontologyCategory") or "").strip(),
        "subcategory": (meta.get("ontologySubcategory") or "").strip(),
        "routePhase": (meta.get("classificationRoutePhase") or "").strip(),
        "routeRule": (meta.get("classificationRouteRule") or "").strip(),
        "facets": schema.csv_list(meta.get("facets")),
        "tab": (meta.get("creativeTabLabel") or "").strip(),
        "tabId": (meta.get("creativeTabId") or "").strip(),
        "detectors": detectors,
    }


def load_reject_ids(path):
    if not path:
        return set()
    try:
        return {row["id"] for row in schema.read_jsonl(path) if "id" in row}
    except FileNotFoundError:
        return set()


def run(nodes_path, out_path, reject_path):
    reject_ids = load_reject_ids(reject_path)
    rows = []
    for node in schema.read_jsonl(nodes_path):
        detectors = detectors_for(node, reject_ids)
        if detectors:
            rows.append(candidate_record(node, detectors))
    schema.write_jsonl(out_path, rows)
    return len(rows)


if __name__ == "__main__":
    nodes = sys.argv[1]
    out = sys.argv[2]
    reject = sys.argv[3] if len(sys.argv) > 3 else None
    n = run(nodes, out, reject)
    print(f"wrote {n} candidates to {out}")
```

- [ ] **Step 5: Run test to verify it passes**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_detect.py" -v`
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add tools/classification-curation/detect.py tools/classification-curation/tests/test_detect.py tools/classification-curation/tests/fixtures/search_nodes.jsonl
git commit -m "feat: add detect stage for classification-curation candidates"
```

---

### Task 3: `evidence.py` — assemble recipe/tab/tag context

**Files:**
- Create: `tools/classification-curation/evidence.py`
- Create: `tools/classification-curation/tests/fixtures/recipes_runtime.jsonl`
- Create: `tools/classification-curation/tests/test_evidence.py`

**Interfaces:**
- Consumes: `schema.read_jsonl`, `schema.write_jsonl`, `schema.csv_list`; `candidates.jsonl` rows from Task 2; the same `search_nodes.jsonl`; `recipes_runtime.jsonl`.
- Produces:
  - `build_recipe_index(recipes_path: str) -> dict[str, dict]` — recipe id → `{"inputs": [item_id, ...], "output": item_id_or_None}`.
  - `build_node_index(nodes_path: str) -> tuple[dict, dict]` — returns `(info_by_id, edges_by_id)` where `info_by_id[id] = {"name", "category", "subcategory"}` for every ITEM node, and `edges_by_id[id] = {"OUTPUT_OF": [...], "USED_IN": [...]}`.
  - `evidence_record(candidate, info_by_id, edges_by_id, recipe_index) -> dict` — the `evidence_batch.jsonl` row.
  - `run(candidates_path, nodes_path, recipes_path, out_path, limit=12) -> int`.
  - CLI: `python evidence.py <candidates.jsonl> <search_nodes.jsonl> <recipes_runtime.jsonl> <evidence_batch.jsonl>`.

Join logic — `unresolvedEdges` point to **recipe ids**, so reach neighbor items through the recipe index:
- `craftedFrom`: union of `recipe_index[r]["inputs"]` for each `r` in the item's `OUTPUT_OF` edges (recipes that output this item), excluding the item itself, deduped, capped at `limit`, each resolved to `{"id", "name", "category"}` via `info_by_id`.
- `usedToMake`: the set of `recipe_index[r]["output"]` for each `r` in the item's `USED_IN` edges (recipes that consume this item), excluding the item itself and `None`, deduped, capped at `limit`, resolved the same way.
- Neighbor ids absent from `info_by_id` resolve to `{"id": id, "name": "", "category": ""}` (do not drop — the bare id is still signal).

Evidence row shape:
```json
{"id":"swem:saddle","displayName":"English Saddle","mod":"swem","path":"saddle",
 "current":{"category":"swem","subcategory":"","routePhase":"compat_fallback","routeRule":""},
 "facets":["wearable"],"tab":{"label":"SWEM","id":"swem:tab"},"tags":[],
 "detectors":["mod_pseudo_category","weak_route"],"runnerUp":"",
 "craftedFrom":[{"id":"minecraft:leather","name":"Leather","category":"ingredients"}],
 "usedToMake":[]}
```
`runnerUp` is `csv_list(metadata.get("classificationCandidates"))` joined back with `"; "` for the proposer to read — but the candidate row from Task 2 does not carry it, so re-read it from the node index step. To keep `evidence.py` self-contained, capture `classificationCandidates` and `tags` into `info_by_id` is wrong (only ITEM display info lives there); instead capture the full metadata for candidate ids during `build_node_index`. Implement `build_node_index` to also return `meta_by_id` for candidate lookup. Final signature: `build_node_index(nodes_path) -> (info_by_id, edges_by_id, meta_by_id)`.

- [ ] **Step 1: Write the recipe fixture**

`tools/classification-curation/tests/fixtures/recipes_runtime.jsonl` (one object per line):

```json
{"id":"swem:english_saddle","modId":"swem","inputs":[[{"itemId":"minecraft:leather","displayName":"Leather","count":3}]],"output":{"itemId":"swem:saddle","displayName":"English Saddle","count":1}}
{"id":"minecraft:stone_bricks","modId":"minecraft","inputs":[[{"itemId":"minecraft:stone","displayName":"Stone","count":4}]],"output":{"itemId":"minecraft:stone_bricks","displayName":"Stone Bricks","count":4}}
{"id":"minecraft:stone_from_smelting","modId":"minecraft","inputs":[[{"itemId":"minecraft:cobblestone","displayName":"Cobblestone","count":1}]],"output":{"itemId":"minecraft:stone","displayName":"Stone","count":1}}
{"id":"farmersdelight:cabbage_rolls","modId":"farmersdelight","inputs":[[{"itemId":"farmersdelight:cabbage","displayName":"Cabbage","count":1}]],"output":{"itemId":"farmersdelight:cabbage_rolls","displayName":"Cabbage Rolls","count":1}}
```

Note: the fixture `search_nodes.jsonl` from Task 2 must also carry `minecraft:leather` and `minecraft:cobblestone` info for neighbor resolution to be non-empty. Add these two ITEM nodes to `tools/classification-curation/tests/fixtures/search_nodes.jsonl` (append as new lines):

```json
{"id":"minecraft:leather","type":"ITEM","displayName":"Leather","metadata":{"facets":"material","ontologyCategory":"ingredients","ontologySubcategory":"","classificationRoutePhase":"primary_rule","classificationRouteRule":"ingredients","creativeTabLabel":"Ingredients","creativeTabId":"minecraft:ingredients","modId":"minecraft"},"unresolvedEdges":{}}
{"id":"minecraft:cobblestone","type":"ITEM","displayName":"Cobblestone","metadata":{"facets":"placeable,stone_block","ontologyCategory":"geology","ontologySubcategory":"stone","classificationRoutePhase":"primary_rule","classificationRouteRule":"geology blocks","creativeTabLabel":"Building Blocks","creativeTabId":"minecraft:building_blocks","modId":"minecraft"},"unresolvedEdges":{}}
```

Both are cleanly classified, so they add zero new candidates (Task 2's test asserts membership, not exact count, so it stays green).

- [ ] **Step 2: Write the failing test**

`tools/classification-curation/tests/test_evidence.py`:

```python
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import detect
import evidence
import schema

FIX = os.path.join(os.path.dirname(__file__), "fixtures")
NODES = os.path.join(FIX, "search_nodes.jsonl")
RECIPES = os.path.join(FIX, "recipes_runtime.jsonl")


class EvidenceTest(unittest.TestCase):
    def test_recipe_index(self):
        idx = evidence.build_recipe_index(RECIPES)
        self.assertEqual(idx["swem:english_saddle"]["inputs"], ["minecraft:leather"])
        self.assertEqual(idx["swem:english_saddle"]["output"], "swem:saddle")

    def test_node_index(self):
        info, edges, meta = evidence.build_node_index(NODES)
        self.assertEqual(info["minecraft:leather"]["category"], "ingredients")
        self.assertEqual(edges["swem:saddle"]["OUTPUT_OF"], ["swem:english_saddle"])

    def test_evidence_for_saddle(self):
        info, edges, meta = evidence.build_node_index(NODES)
        ridx = evidence.build_recipe_index(RECIPES)
        cand = detect.candidate_record(
            {"id": "swem:saddle", "type": "ITEM",
             "metadata": {"facets": "wearable", "ontologyCategory": "swem",
                          "classificationRoutePhase": "compat_fallback",
                          "creativeTabLabel": "SWEM", "creativeTabId": "swem:tab"}},
            ["mod_pseudo_category", "weak_route"])
        rec = evidence.evidence_record(cand, info, edges, ridx)
        self.assertEqual(rec["craftedFrom"], [{"id": "minecraft:leather", "name": "Leather", "category": "ingredients"}])
        self.assertEqual(rec["usedToMake"], [])
        self.assertEqual(rec["tab"], {"label": "SWEM", "id": "swem:tab"})

    def test_run_round_trip(self):
        with tempfile.TemporaryDirectory() as d:
            cands = os.path.join(d, "candidates.jsonl")
            detect.run(NODES, cands, None)
            out = os.path.join(d, "evidence_batch.jsonl")
            n = evidence.run(cands, NODES, RECIPES, out)
            rows = schema.read_jsonl(out)
            self.assertEqual(n, len(rows))
            saddle = next(r for r in rows if r["id"] == "swem:saddle")
            self.assertEqual(saddle["craftedFrom"][0]["id"], "minecraft:leather")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run test to verify it fails**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_evidence.py" -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'evidence'`.

- [ ] **Step 4: Write minimal implementation**

`tools/classification-curation/evidence.py`:

```python
"""Stage 2: assemble recipe-graph + tab + tag evidence for each candidate."""
import sys

import schema


def build_recipe_index(recipes_path):
    index = {}
    for recipe in schema.read_jsonl(recipes_path):
        inputs = []
        seen = set()
        for slot in recipe.get("inputs", []) or []:
            for alt in slot or []:
                item = alt.get("itemId")
                if item and item not in seen:
                    seen.add(item)
                    inputs.append(item)
        output = (recipe.get("output") or {}).get("itemId") or None
        index[recipe["id"]] = {"inputs": inputs, "output": output}
    return index


def build_node_index(nodes_path):
    info_by_id = {}
    edges_by_id = {}
    meta_by_id = {}
    for node in schema.read_jsonl(nodes_path):
        if node.get("type") != "ITEM":
            continue
        rid = node["id"]
        meta = node.get("metadata", {})
        info_by_id[rid] = {
            "name": node.get("displayName", ""),
            "category": (meta.get("ontologyCategory") or "").strip(),
            "subcategory": (meta.get("ontologySubcategory") or "").strip(),
        }
        edges = node.get("unresolvedEdges", {}) or {}
        edges_by_id[rid] = {
            "OUTPUT_OF": list(edges.get("OUTPUT_OF", []) or []),
            "USED_IN": list(edges.get("USED_IN", []) or []),
        }
        meta_by_id[rid] = meta
    return info_by_id, edges_by_id, meta_by_id


def _resolve(neighbor_id, info_by_id):
    info = info_by_id.get(neighbor_id)
    if info is None:
        return {"id": neighbor_id, "name": "", "category": ""}
    return {"id": neighbor_id, "name": info["name"], "category": info["category"]}


def _neighbors(ids, info_by_id, limit):
    out = []
    seen = set()
    for nid in ids:
        if nid in seen:
            continue
        seen.add(nid)
        out.append(_resolve(nid, info_by_id))
        if len(out) >= limit:
            break
    return out


def evidence_record(candidate, info_by_id, edges_by_id, recipe_index, limit=12):
    rid = candidate["id"]
    edges = edges_by_id.get(rid, {"OUTPUT_OF": [], "USED_IN": []})

    crafted_ids = []
    for recipe_id in edges["OUTPUT_OF"]:
        for item in recipe_index.get(recipe_id, {}).get("inputs", []):
            if item != rid:
                crafted_ids.append(item)

    used_ids = []
    for recipe_id in edges["USED_IN"]:
        output = recipe_index.get(recipe_id, {}).get("output")
        if output and output != rid:
            used_ids.append(output)

    info = info_by_id.get(rid, {})
    return {
        "id": rid,
        "displayName": info.get("name", ""),
        "mod": candidate["mod"],
        "path": candidate["path"],
        "current": {
            "category": candidate["category"],
            "subcategory": candidate["subcategory"],
            "routePhase": candidate["routePhase"],
            "routeRule": candidate["routeRule"],
        },
        "facets": candidate["facets"],
        "tab": {"label": candidate["tab"], "id": candidate["tabId"]},
        "tags": schema.csv_list(candidate.get("tagsCsv")),
        "detectors": candidate["detectors"],
        "runnerUp": candidate.get("runnerUp", ""),
        "craftedFrom": _neighbors(crafted_ids, info_by_id, limit),
        "usedToMake": _neighbors(used_ids, info_by_id, limit),
    }


def run(candidates_path, nodes_path, recipes_path, out_path, limit=12):
    info_by_id, edges_by_id, meta_by_id = build_node_index(nodes_path)
    recipe_index = build_recipe_index(recipes_path)
    rows = []
    for candidate in schema.read_jsonl(candidates_path):
        meta = meta_by_id.get(candidate["id"], {})
        candidate = dict(candidate)
        candidate["tagsCsv"] = meta.get("tags")
        candidate["runnerUp"] = "; ".join(schema.csv_list(meta.get("classificationCandidates")))
        rows.append(evidence_record(candidate, info_by_id, edges_by_id, recipe_index, limit))
    schema.write_jsonl(out_path, rows)
    return len(rows)


if __name__ == "__main__":
    n = run(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
    print(f"wrote {n} evidence rows to {sys.argv[4]}")
```

- [ ] **Step 5: Run test to verify it passes**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_evidence.py" -v`
Expected: PASS (4 tests). Also re-run `test_detect.py` to confirm the two appended fixture nodes did not break it:
Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_detect.py" -v`
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add tools/classification-curation/evidence.py tools/classification-curation/tests/test_evidence.py tools/classification-curation/tests/fixtures/recipes_runtime.jsonl tools/classification-curation/tests/fixtures/search_nodes.jsonl
git commit -m "feat: add evidence-assembly stage joining recipe graph to candidates"
```

---

### Task 4: Proposal contract + `validate_proposals.py`

**Files:**
- Create: `tools/classification-curation/prompts/propose.md`
- Create: `tools/classification-curation/validate_proposals.py`
- Create: `tools/classification-curation/tests/test_validate_proposals.py`

**Interfaces:**
- Consumes: `schema.read_jsonl`.
- Produces:
  - `validate_proposal(row: dict) -> list[str]` — returns a list of human-readable error strings for one proposal (`[]` if valid).
  - `validate_file(path: str) -> list[str]` — returns all errors prefixed with line numbers.
  - CLI: `python validate_proposals.py <proposals.jsonl>` — prints errors, exits `1` if any, `0` if clean.

Proposal schema (the contract the Claude proposal agent must emit; one JSON object per line):
- `id` (string, namespaced), `scope` (`"item"` | `"modPattern"`), `decision` (`"pending"` | `"approve"` | `"reject"`), `override` (object), `rationale` (string).
- `scope == "item"`: `override` may contain `category` (non-empty string if present), `subcategory` (string), `addFacets` (string array), `removeFacets` (string array). At least one of `category`/`addFacets`/`removeFacets` must be present and non-empty.
- `scope == "modPattern"`: `override` must contain `mod` (non-empty), `pathTokens` (non-empty array of single tokens with no `_` or `/`), `category` (non-empty), `subcategory` (string, may be empty).

- [ ] **Step 1: Write the proposal-agent prompt**

`tools/classification-curation/prompts/propose.md`:

```markdown
# Classification Proposal Agent

You are proposing classification overrides for Minecraft mod items that the
runtime classifier got wrong or left weak. You are given one JSON object per
line from `evidence_batch.jsonl`. For each, decide whether an override would
improve it and emit at most one proposal object.

## Input (per line)
`id`, `displayName`, `mod`, `path`, `current` (category/subcategory/route),
`facets`, `tab`, `tags`, `detectors`, `runnerUp`, `craftedFrom[]`, `usedToMake[]`.

## Output (one JSON object per line, to `proposals.jsonl`)
- `id`: the item id from the input.
- `scope`: `"item"` for a single item; `"modPattern"` when a whole family of a
  mod's items shares a path token and should route together.
- `override`:
  - item: `{ "category": "...", "subcategory": "...", "addFacets": [], "removeFacets": [] }`
  - modPattern: `{ "mod": "...", "pathTokens": ["spreader"], "category": "...", "subcategory": "..." }`
- `rationale`: one sentence grounded in the evidence (recipe neighbors, tab, tags).
- `decision`: always `"pending"` — the human sets approve/reject.

## Rules
- Use existing category ids seen in `current`/`craftedFrom` (e.g. `armor`,
  `tools`, `masonry`, `nature`, `decoration`, `magic`, `ingredients`,
  `geology`, `utility`); do not invent new top-level categories in this tool.
- `pathTokens` must be single tokens (no `_` or `/`). `"mana_spreader"` →
  token `"spreader"`.
- If the current classification already looks correct, emit no line for that id.
- Ground every proposal in the evidence; never guess from the name alone.
```

- [ ] **Step 2: Write the failing test**

`tools/classification-curation/tests/test_validate_proposals.py`:

```python
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import schema
import validate_proposals as vp


class ValidateTest(unittest.TestCase):
    def test_valid_item(self):
        row = {"id": "minecraft:fire_charge", "scope": "item", "decision": "pending",
               "override": {"category": "tools", "subcategory": "throwable"}, "rationale": "x"}
        self.assertEqual(vp.validate_proposal(row), [])

    def test_valid_mod_pattern(self):
        row = {"id": "botania:mana_spreader", "scope": "modPattern", "decision": "approve",
               "override": {"mod": "botania", "pathTokens": ["spreader"], "category": "tech", "subcategory": "mana"},
               "rationale": "x"}
        self.assertEqual(vp.validate_proposal(row), [])

    def test_bad_scope(self):
        self.assertTrue(vp.validate_proposal({"id": "a:b", "scope": "nope", "decision": "pending", "override": {}}))

    def test_item_requires_some_change(self):
        row = {"id": "a:b", "scope": "item", "decision": "pending", "override": {"subcategory": "x"}}
        self.assertTrue(vp.validate_proposal(row))

    def test_pattern_token_must_be_single(self):
        row = {"id": "a:b", "scope": "modPattern", "decision": "pending",
               "override": {"mod": "a", "pathTokens": ["mana_spreader"], "category": "tech"}}
        self.assertTrue(vp.validate_proposal(row))

    def test_bad_decision(self):
        row = {"id": "a:b", "scope": "item", "decision": "maybe",
               "override": {"category": "tools"}}
        self.assertTrue(vp.validate_proposal(row))

    def test_validate_file(self):
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "proposals.jsonl")
            schema.write_jsonl(p, [
                {"id": "a:b", "scope": "item", "decision": "pending", "override": {"category": "tools"}, "rationale": "x"},
                {"id": "c:d", "scope": "nope", "decision": "pending", "override": {}},
            ])
            errors = vp.validate_file(p)
            self.assertEqual(len(errors), 1)
            self.assertIn("line 2", errors[0])


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run test to verify it fails**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_validate_proposals.py" -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'validate_proposals'`.

- [ ] **Step 4: Write minimal implementation**

`tools/classification-curation/validate_proposals.py`:

```python
"""Guard: verify proposals.jsonl conforms to the proposal schema before apply."""
import sys

import schema

SCOPES = {"item", "modPattern"}
DECISIONS = {"pending", "approve", "reject"}


def _is_str_list(value):
    return isinstance(value, list) and all(isinstance(v, str) for v in value)


def validate_proposal(row):
    errors = []
    rid = row.get("id")
    if not isinstance(rid, str) or ":" not in rid:
        errors.append("id must be a namespaced string")
    scope = row.get("scope")
    if scope not in SCOPES:
        errors.append(f"scope must be one of {sorted(SCOPES)}")
    if row.get("decision") not in DECISIONS:
        errors.append(f"decision must be one of {sorted(DECISIONS)}")
    override = row.get("override")
    if not isinstance(override, dict):
        errors.append("override must be an object")
        return errors

    if scope == "item":
        category = override.get("category")
        if category is not None and (not isinstance(category, str) or not category.strip()):
            errors.append("item category, when present, must be a non-empty string")
        add = override.get("addFacets", [])
        rem = override.get("removeFacets", [])
        if not _is_str_list(add) or not _is_str_list(rem):
            errors.append("addFacets/removeFacets must be string arrays")
        if not (category and category.strip()) and not add and not rem:
            errors.append("item override must change category, addFacets, or removeFacets")
    elif scope == "modPattern":
        mod = override.get("mod")
        if not isinstance(mod, str) or not mod.strip():
            errors.append("modPattern mod must be a non-empty string")
        tokens = override.get("pathTokens")
        if not _is_str_list(tokens) or not tokens:
            errors.append("pathTokens must be a non-empty string array")
        else:
            for token in tokens:
                if "_" in token or "/" in token:
                    errors.append(f"pathToken '{token}' must be a single token (no '_' or '/')")
        category = override.get("category")
        if not isinstance(category, str) or not category.strip():
            errors.append("modPattern category must be a non-empty string")
    return errors


def validate_file(path):
    all_errors = []
    for index, row in enumerate(schema.read_jsonl(path), start=1):
        for error in validate_proposal(row):
            all_errors.append(f"line {index} ({row.get('id', '?')}): {error}")
    return all_errors


if __name__ == "__main__":
    errors = validate_file(sys.argv[1])
    for error in errors:
        print(error)
    if errors:
        print(f"{len(errors)} error(s)")
        sys.exit(1)
    print("proposals valid")
```

- [ ] **Step 5: Run test to verify it passes**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_validate_proposals.py" -v`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**

```bash
git add tools/classification-curation/prompts/propose.md tools/classification-curation/validate_proposals.py tools/classification-curation/tests/test_validate_proposals.py
git commit -m "feat: add proposal-agent contract and proposal validator"
```

---

### Task 5: `apply.py` — merge approvals into the bundled override JSON

**Files:**
- Create: `tools/classification-curation/apply.py`
- Create: `tools/classification-curation/tests/test_apply.py`

**Interfaces:**
- Consumes: `schema.read_jsonl`; `validate_proposals.validate_proposal`; `proposals.jsonl`; the existing `classification_overrides.json`; the reject ledger.
- Produces:
  - `load_overrides(path: str) -> dict` — returns the parsed override object, or `{"items": {}, "modPatterns": []}` if the file is missing.
  - `merge(overrides: dict, proposals: list[dict]) -> tuple[dict, list[dict]]` — returns `(updated_overrides, reject_rows)`. Raises `ValueError` if any approved proposal fails `validate_proposal`.
  - `run(proposals_path, overrides_path, reject_path) -> tuple[int, int]` — returns `(approved_count, rejected_count)`; writes the override file (pretty, item keys sorted) and appends reject rows.
  - CLI: `python apply.py <proposals.jsonl> <classification_overrides.json> <reject_ledger.jsonl>`.

Merge rules:
- Only `decision == "approve"` and `decision == "reject"` are acted on; `pending` is ignored (and reported).
- Approved `item`: set `overrides["items"][id]` to a dict carrying only the present, non-empty fields among `category`, `subcategory`, `addFacets`, `removeFacets`.
- Approved `modPattern`: append `{mod, pathTokens, category, subcategory}` to `overrides["modPatterns"]`, deduped by the tuple `(mod, tuple(sorted(pathTokens)), category, subcategory)`.
- Rejected: emit a reject row `{"id": id, "scope": scope, "key": <stable key>}` where the key is `id` (coarse, by-id suppression — documented in the README).
- Output JSON: `items` keys sorted alphabetically; `modPatterns` sorted by `(mod, category, subcategory)` for deterministic diffs; 2-space indent; trailing newline.

- [ ] **Step 1: Write the failing test**

`tools/classification-curation/tests/test_apply.py`:

```python
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import apply
import schema


class ApplyTest(unittest.TestCase):
    def test_merge_item_keeps_only_present_fields(self):
        ov = {"items": {}, "modPatterns": []}
        updated, rejects = apply.merge(ov, [
            {"id": "minecraft:fire_charge", "scope": "item", "decision": "approve",
             "override": {"category": "tools", "subcategory": "throwable", "addFacets": [], "removeFacets": []},
             "rationale": "x"},
        ])
        self.assertEqual(updated["items"]["minecraft:fire_charge"],
                         {"category": "tools", "subcategory": "throwable"})
        self.assertEqual(rejects, [])

    def test_merge_mod_pattern_dedups(self):
        ov = {"items": {}, "modPatterns": []}
        prop = {"id": "botania:mana_spreader", "scope": "modPattern", "decision": "approve",
                "override": {"mod": "botania", "pathTokens": ["spreader"], "category": "tech", "subcategory": "mana"},
                "rationale": "x"}
        updated, _ = apply.merge(ov, [prop, dict(prop)])
        self.assertEqual(len(updated["modPatterns"]), 1)

    def test_reject_recorded(self):
        ov = {"items": {}, "modPatterns": []}
        updated, rejects = apply.merge(ov, [
            {"id": "a:b", "scope": "item", "decision": "reject", "override": {"category": "tools"}, "rationale": "x"},
        ])
        self.assertEqual(updated["items"], {})
        self.assertEqual(rejects, [{"id": "a:b", "scope": "item", "key": "a:b"}])

    def test_merge_rejects_invalid_approved(self):
        ov = {"items": {}, "modPatterns": []}
        with self.assertRaises(ValueError):
            apply.merge(ov, [{"id": "a:b", "scope": "item", "decision": "approve",
                              "override": {"subcategory": "x"}, "rationale": "x"}])

    def test_run_writes_files(self):
        with tempfile.TemporaryDirectory() as d:
            proposals = os.path.join(d, "proposals.jsonl")
            overrides = os.path.join(d, "classification_overrides.json")
            reject = os.path.join(d, "reject_ledger.jsonl")
            schema.write_jsonl(proposals, [
                {"id": "minecraft:fire_charge", "scope": "item", "decision": "approve",
                 "override": {"category": "tools"}, "rationale": "x"},
                {"id": "a:b", "scope": "item", "decision": "reject", "override": {"category": "tools"}, "rationale": "x"},
            ])
            approved, rejected = apply.run(proposals, overrides, reject)
            self.assertEqual((approved, rejected), (1, 1))
            data = json.load(open(overrides, encoding="utf-8"))
            self.assertEqual(data["items"]["minecraft:fire_charge"], {"category": "tools"})
            self.assertEqual(schema.read_jsonl(reject), [{"id": "a:b", "scope": "item", "key": "a:b"}])


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_apply.py" -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'apply'`.

- [ ] **Step 3: Write minimal implementation**

`tools/classification-curation/apply.py`:

```python
"""Stage 5: fold approved proposals into the bundled classification_overrides.json."""
import json
import os
import sys

import schema
import validate_proposals


def load_overrides(path):
    if not os.path.exists(path):
        return {"items": {}, "modPatterns": []}
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    data.setdefault("items", {})
    data.setdefault("modPatterns", [])
    return data


def _item_entry(override):
    entry = {}
    category = override.get("category")
    if category and category.strip():
        entry["category"] = category
    subcategory = override.get("subcategory")
    if subcategory and subcategory.strip():
        entry["subcategory"] = subcategory
    if override.get("addFacets"):
        entry["addFacets"] = list(override["addFacets"])
    if override.get("removeFacets"):
        entry["removeFacets"] = list(override["removeFacets"])
    return entry


def _pattern_key(pattern):
    return (pattern["mod"], tuple(sorted(pattern["pathTokens"])),
            pattern.get("category", ""), pattern.get("subcategory", ""))


def merge(overrides, proposals):
    overrides = {"items": dict(overrides.get("items", {})),
                 "modPatterns": list(overrides.get("modPatterns", []))}
    reject_rows = []
    existing_pattern_keys = {_pattern_key(p) for p in overrides["modPatterns"]}

    for row in proposals:
        decision = row.get("decision")
        if decision not in ("approve", "reject"):
            continue
        if decision == "reject":
            reject_rows.append({"id": row["id"], "scope": row.get("scope", ""), "key": row["id"]})
            continue
        errors = validate_proposals.validate_proposal(row)
        if errors:
            raise ValueError(f"approved proposal {row.get('id')} invalid: {errors}")
        override = row["override"]
        if row["scope"] == "item":
            overrides["items"][row["id"]] = _item_entry(override)
        else:
            pattern = {
                "mod": override["mod"],
                "pathTokens": list(override["pathTokens"]),
                "category": override.get("category", ""),
                "subcategory": override.get("subcategory", ""),
            }
            key = _pattern_key(pattern)
            if key not in existing_pattern_keys:
                existing_pattern_keys.add(key)
                overrides["modPatterns"].append(pattern)
    return overrides, reject_rows


def _write_overrides(path, overrides):
    items = dict(sorted(overrides["items"].items()))
    patterns = sorted(overrides["modPatterns"],
                      key=lambda p: (p["mod"], p.get("category", ""), p.get("subcategory", "")))
    payload = {"items": items, "modPatterns": patterns}
    with open(path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _append_rejects(path, reject_rows):
    if not reject_rows:
        return
    with open(path, "a", encoding="utf-8") as f:
        for row in reject_rows:
            f.write(json.dumps(row, ensure_ascii=False))
            f.write("\n")


def run(proposals_path, overrides_path, reject_path):
    proposals = schema.read_jsonl(proposals_path)
    overrides = load_overrides(overrides_path)
    updated, reject_rows = merge(overrides, proposals)
    _write_overrides(overrides_path, updated)
    _append_rejects(reject_path, reject_rows)
    approved = sum(1 for r in proposals if r.get("decision") == "approve")
    return approved, len(reject_rows)


if __name__ == "__main__":
    approved, rejected = run(sys.argv[1], sys.argv[2], sys.argv[3])
    print(f"applied {approved} approved, recorded {rejected} rejected")
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_apply.py" -v`
Expected: PASS (5 tests).

- [ ] **Step 5: Run the whole Python suite**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_*.py" -v`
Expected: PASS (all tests across the five modules).

- [ ] **Step 6: Commit**

```bash
git add tools/classification-curation/apply.py tools/classification-curation/tests/test_apply.py
git commit -m "feat: add apply stage merging approved proposals into override data"
```

---

### Task 6: Java replay-diff gate (`ClassificationOverrideReplayGateTest`)

**Files:**
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideReplayGateTest.java`

**Interfaces:**
- Consumes: `SearchNodeMirrorDump.readJsonl(Path)`, `SearchNodeMirrorDump.reclassifyItemOntology(List<SearchNode>)`, `ClassificationOverrides.{clear, parseAndInstall, loadBundledDefaults, forItem, patternFor}`, `SearchNode.{id, type, meta, displayName}`, `SearchNodeKeys.{ONTOLOGY_CATEGORY, ONTOLOGY_SUBCATEGORY}`, `NodeType.ITEM`.
- Produces: a markdown report at `neoforge/build/reports/ami-classification/override-replay-gate.md`; behavioral assertions.

Design — diff two replays of the **same** dump under current code:
1. `ClassificationOverrides.clear()` → `baseline = reclassifyItemOntology(items)`.
2. `ClassificationOverrides.parseAndInstall(<bundled json>)` → `withOverrides = reclassifyItemOntology(items)`.
3. Restore with `ClassificationOverrides.loadBundledDefaults()` in `finally`.
4. An item **changed** when its `category/subcategory` differs between the two replays. A changed item is **explained** when `forItem(id).isPresent()` or `patternFor(namespace, path).isPresent()` after install; otherwise it is an **unexplained regression**.

Two tests: a behavioral test using a synthetic in-string override (deterministic, dump-guarded), and the real-data gate over the bundled file.

- [ ] **Step 1: Write the test**

`neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideReplayGateTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverrideReplayGateTest {

    private static Path locateDump() {
        String configured = System.getProperty("ami.searchNodesDump");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_SEARCH_NODES_DUMP");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path synesthesia = Path.of(System.getProperty("user.home"),
                "AppData", "Roaming", "PrismLauncher", "instances", "Synesthesia",
                "minecraft", "ami_dumps", "search", "search_nodes.jsonl");
        if (Files.exists(synesthesia)) {
            return synesthesia;
        }
        return repoRoot().resolve(Path.of("run", "neoforge-emi", "ami_dumps", "search", "search_nodes.jsonl"));
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    private static Path bundledOverrides() {
        return repoRoot().resolve(Path.of(
                "xplat", "src", "main", "resources", "assets", "ami", "classification_overrides.json"));
    }

    private static List<SearchNode> items(Path dump) throws IOException {
        return SearchNodeMirrorDump.readJsonl(dump).stream()
                .filter(node -> node.type() == NodeType.ITEM)
                .toList();
    }

    private static Map<String, String> categoryByid(List<SearchNode> nodes) {
        Map<String, String> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            result.put(node.id().toString(),
                    node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "") + "/" + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
        }
        return result;
    }

    private static boolean explained(SearchNode node) {
        ResourceLocation id = node.id();
        return ClassificationOverrides.forItem(id).isPresent()
                || ClassificationOverrides.patternFor(id.getNamespace(), id.getPath()).isPresent();
    }

    @Test
    void detectsExplainedChangeAndLeavesOthersUntouched() throws IOException {
        Path dump = locateDump();
        if (!Files.exists(dump)) {
            return; // no dump locally; the real-data test writes the no-data report
        }
        List<SearchNode> source = items(dump);
        try {
            ClassificationOverrides.clear();
            Map<String, String> baseline = categoryByid(SearchNodeMirrorDump.reclassifyItemOntology(source));

            ClassificationOverrides.parseAndInstall(
                    "{\"items\":{\"minecraft:dirt\":{\"category\":\"weapon\",\"subcategory\":\"throwable\"}},\"modPatterns\":[]}");
            List<SearchNode> after = SearchNodeMirrorDump.reclassifyItemOntology(source);
            Map<String, String> afterById = categoryByid(after);

            assertEquals("weapon/throwable", afterById.get("minecraft:dirt"),
                    "forced override must win for the targeted item");

            List<String> unexplained = new ArrayList<>();
            for (SearchNode node : after) {
                String id = node.id().toString();
                if (!baseline.get(id).equals(afterById.get(id)) && !explained(node)) {
                    unexplained.add(id);
                }
            }
            assertTrue(unexplained.isEmpty(),
                    "a single-item override changed untargeted items: " + unexplained);
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }
    }

    @Test
    void writesOverrideReplayGateReport() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of(
                "neoforge", "build", "reports", "ami-classification", "override-replay-gate.md"));
        Files.createDirectories(reportPath.getParent());

        Path dump = locateDump();
        if (!Files.exists(dump)) {
            Files.writeString(reportPath, "# AMI Override Replay Gate\n\nNo dump found at `" + dump + "`.\n");
            assertTrue(Files.exists(reportPath));
            return;
        }

        String json = Files.readString(bundledOverrides(), StandardCharsets.UTF_8);
        List<SearchNode> source = items(dump);
        List<String> unexplained = new ArrayList<>();
        long changed;
        try {
            ClassificationOverrides.clear();
            Map<String, String> baseline = categoryByid(SearchNodeMirrorDump.reclassifyItemOntology(source));

            ClassificationOverrides.parseAndInstall(json);
            List<SearchNode> after = SearchNodeMirrorDump.reclassifyItemOntology(source);
            Map<String, String> afterById = categoryByid(after);

            changed = 0;
            for (SearchNode node : after) {
                String id = node.id().toString();
                if (!baseline.get(id).equals(afterById.get(id))) {
                    changed++;
                    if (!explained(node)) {
                        unexplained.add(id + ": " + baseline.get(id) + " -> " + afterById.get(id));
                    }
                }
            }
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }

        StringBuilder report = new StringBuilder();
        report.append("# AMI Override Replay Gate\n\n");
        report.append("Source dump: `").append(dump).append("`\n\n");
        report.append("- Items compared: ").append(source.size()).append("\n");
        report.append("- Changed by overrides: ").append(changed).append("\n");
        report.append("- Unexplained changes: ").append(unexplained.size()).append("\n\n");
        if (!unexplained.isEmpty()) {
            report.append("## Unexplained changes\n\n");
            unexplained.stream().limit(200).forEach(line -> report.append("- `").append(line).append("`\n"));
        }
        Files.writeString(reportPath, report.toString());

        assertTrue(Files.exists(reportPath));
        if (Boolean.getBoolean("ami.overrideGateStrict")) {
            assertFalse(!unexplained.isEmpty(),
                    () -> "Overrides changed untargeted items. See " + reportPath.toAbsolutePath());
        }
    }
}
```

- [ ] **Step 2: Run the gate tests**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideReplayGateTest"`
Expected: PASS. With the bundled override file empty, `writesOverrideReplayGateReport` reports `Changed by overrides: 0`; `detectsExplainedChangeAndLeavesOthersUntouched` confirms a forced `minecraft:dirt` override wins and changes nothing else.

- [ ] **Step 3: Verify the report**

Run: `python -c "print(open(r'neoforge/build/reports/ami-classification/override-replay-gate.md',encoding='utf-8').read())"`
Expected: a markdown report showing items compared (~21k for the Synesthesia dump), `Changed by overrides: 0`, `Unexplained changes: 0`.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideReplayGateTest.java
git commit -m "test: add override replay-diff gate over the search-node dump"
```

---

### Task 7: README — end-to-end run + reject-ledger semantics

**Files:**
- Create: `tools/classification-curation/README.md`

**Interfaces:** None (documentation). Documents the exact commands wiring Tasks 1–6 into one loop.

- [ ] **Step 1: Write the README**

`tools/classification-curation/README.md`:

````markdown
# Classification Curation Tool

Dev-time pipeline that turns an `ami_dumps` capture into reviewed entries in
`xplat/src/main/resources/assets/ami/classification_overrides.json`. The runtime
never reads the dump — only the committed override JSON.

## Stages

| # | Stage | Command | Output |
|---|-------|---------|--------|
| 1 | detect | `python detect.py <dump>/search/search_nodes.jsonl candidates.jsonl reject_ledger.jsonl` | `candidates.jsonl` |
| 2 | evidence | `python evidence.py candidates.jsonl <dump>/search/search_nodes.jsonl <dump>/recipes/recipes_runtime.jsonl evidence_batch.jsonl` | `evidence_batch.jsonl` |
| 3 | propose | Claude Code agent reading `evidence_batch.jsonl` per `prompts/propose.md` | `proposals.jsonl` |
| 4 | review | Human edits each line's `decision` to `approve`/`reject` | `proposals.jsonl` |
| 5 | apply | `python validate_proposals.py proposals.jsonl && python apply.py proposals.jsonl ../../xplat/src/main/resources/assets/ami/classification_overrides.json reject_ledger.jsonl` | override JSON + `reject_ledger.jsonl` |
| 6 | verify | `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideReplayGateTest"` (add `-Dami.overrideGateStrict=true` to fail on unexplained changes) | `neoforge/build/reports/ami-classification/override-replay-gate.md` |

Run all commands from `tools/classification-curation/`. Set `<dump>` to your
`ami_dumps` directory (e.g. the Synesthesia instance).

## Reject ledger

`reject_ledger.jsonl` is persistent curation memory and **is committed**.
`detect.py` reads it and skips any id already rejected, so a rejected item is
not re-surfaced on later rounds. Rejection is by id (coarse): re-considering a
rejected item means removing its line from the ledger.

## Loop until dry

Re-run stages 1–6 each round. The reject ledger plus the growing override file
shrink the candidate pool until `detect.py` reports zero (or only items you have
deliberately deferred).

## Tests

`python -m unittest discover -s tests -p "test_*.py" -v`
````

- [ ] **Step 2: Verify links and commands**

Run: `python -m unittest discover -s tools/classification-curation/tests -p "test_*.py"`
Expected: PASS (full suite, confirming the documented test command works).

- [ ] **Step 3: Commit**

```bash
git add tools/classification-curation/README.md
git commit -m "docs: document classification-curation pipeline run and reject ledger"
```

---

## Self-Review

**Spec coverage (Component 3 of the design):**
- Six-stage pipeline → Tasks 1–7. detect → Task 2; evidence assembly → Task 3; proposal contract → Task 4; review (human, `decision` field) → documented Task 4/7; apply → Task 5; Java replay-diff gate → Task 6.
- Detectors (weak route / empty facets / cross-signal contradiction / mod-name pseudo-category) → Task 2 (`weak_route`, `empty_facets`, `tab_contradiction`, `mod_pseudo_category`).
- Recipe-graph neighbors via `OUTPUT_OF`/`USED_IN` → Task 3 (joined through the recipe index, since edges are recipe ids).
- Reject ledger → Tasks 2 (read) + 5 (write) + 7 (semantics).
- Override JSON shape matches the `ClassificationOverrides` consumer → Task 5 (`items` map + `modPatterns` array; `pathTokens` single tokens).
- Verification = Java replay-diff reusing existing infra → Task 6 (`SearchNodeMirrorDump.reclassifyItemOntology`, `ClassificationOverrides`).
- Seed batch (string-match plugins) → out of scope for 3a; it is Plan 3b, which consumes this tool. Noted in the spec.

**Placeholder scan:** No TBD/TODO; every code step shows complete code; commands have expected output.

**Type consistency:** `detect.candidate_record` produces the row `evidence.run` reads (keys `id/mod/path/category/subcategory/routePhase/routeRule/facets/tab/tabId/detectors`); `evidence.run` augments with `tagsCsv`/`runnerUp` before `evidence_record`. `validate_proposals.validate_proposal` is reused by `apply.merge`. The override JSON keys (`category`, `subcategory`, `addFacets`, `removeFacets`, `mod`, `pathTokens`) match `ClassificationOverrides.parseItems`/`parsePatterns`. The Java test uses only confirmed `ClassificationOverrides` methods (`clear`, `parseAndInstall`, `loadBundledDefaults`, `forItem`, `patternFor`) and `SearchNodeMirrorDump` entry points.
