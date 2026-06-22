# Classification Migration Pilot (Cnc) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `CncCompat`'s facet-tagging from Java code into bundled override data, proving the loop with a deterministic equivalence test and a facet-aware replay gate, then delete the plugin.

**Architecture:** Extend `modPattern` override rules to carry `addFacets`/`removeFacets`; apply pattern facets early in `resolve()` so a facet-only pattern feeds the evidence classifier and falls through (only a category-bearing pattern terminates). Extend the replay gate to diff facets. Author cnc override data equivalent to the plugin over the real Synesthesia item set, prove it with a durable JUnit test, then delete `CncCompat` and its single classification hook.

**Tech Stack:** Java 21 / JUnit 5 (xplat main, neoforge test), Python 3 stdlib / unittest (`tools/classification-curation`), Gson, NeoForge.

## Global Constraints

- No AI/assistant attribution in commit messages (no `Co-Authored-By`, no generated-by lines).
- Commit from the worktree root using PowerShell `git -C "<worktree>"` (the Bash git-guard hook false-positives on the worktree path containing `.claude`). Worktree root: `C:\WorkDir\AutomatedMaterialsIndex\.claude\worktrees\stupefied-bardeen-2d4ebd`.
- `pathToken` values are single `[_/]`-split components — never contain `_` or `/` (e.g. `spreader`, never `mana_spreader`).
- `ItemFacet` ids are lowercase: `ingredient_organic` = `ItemFacet.INGREDIENT_ORGANIC`, `magic_artifact` = `ItemFacet.MAGIC_ARTIFACT`.
- Python is standard-library only; tests run with `python -W error::ResourceWarning -m unittest discover -s tools/classification-curation/tests -p "test_*.py"`.
- Preserve all `cnc` **guide** integration: `CrittersCrawlersGuideSource` and `CompatIndexRegistry.CrittersCrawlersPlugin` (lines 139–147) are the guidebook, NOT classification — do not touch them.
- The only `cnc` **classification** registration is `ItemProvider.java:475–477`.
- Do not preserve `cncItemKind` meta or `cnc_*` search tokens — no production consumer reads them (accepted delta per spec).

**Spec:** `docs/superpowers/specs/2026-06-22-classification-migration-pilot-cnc-design.md`.

---

### Task 1: ModPatternRule carries facets; parser reads them

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java:122-146` (`parsePatterns`)
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesParseTest.java`

**Interfaces:**
- Produces: `ModPatternRule(String modId, Set<String> pathTokens, EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets, String category, String subcategory)` with a 4-arg convenience constructor `(modId, pathTokens, category, subcategory)` delegating with empty facet sets, and `boolean hasCategory()`.
- Consumes (existing): `ClassificationOverrides.parseFacets(JsonObject, String)` returns `EnumSet<ItemFacet>`; `optString(JsonObject, String)`.

- [ ] **Step 1: Write the failing test**

Add to `ClassificationOverridesParseTest.java` (inside the class):

```java
    @Test
    void parsesModPatternFacets() {
        String json = """
            {
              "items": {},
              "modPatterns": [
                { "mod": "cnc", "pathTokens": ["buckskin", "antler"], "addFacets": ["ingredient_organic"] },
                { "mod": "cnc", "pathTokens": ["potofmouse"], "addFacets": ["magic_artifact"],
                  "removeFacets": ["decorative_block"], "category": "magic", "subcategory": "artifacts" }
              ]
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        ModPatternRule organic = ClassificationOverrides.patternFor("cnc", "buckskin").orElseThrow();
        assertTrue(organic.addFacets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertTrue(organic.removeFacets().isEmpty());
        assertEquals("", organic.category() == null ? "" : organic.category());
        assertEquals(false, organic.hasCategory());

        ModPatternRule artifact = ClassificationOverrides.patternFor("cnc", "potofmouse").orElseThrow();
        assertTrue(artifact.addFacets().contains(ItemFacet.MAGIC_ARTIFACT));
        assertTrue(artifact.removeFacets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertEquals(true, artifact.hasCategory());
        assertEquals("magic", artifact.category());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :neoforge:compileTestJava`
Expected: FAIL — `ModPatternRule` has no `addFacets()`/`hasCategory()` methods (compile error).

- [ ] **Step 3: Rewrite ModPatternRule**

Replace the entire contents of `ModPatternRule.java` with:

```java
package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.Set;

/**
 * Per-mod path-token rule. If an item from {@code modId} has any of {@code pathTokens}:
 * apply {@code addFacets}/{@code removeFacets}, and — when {@link #hasCategory()} — route to
 * {@code category}/{@code subcategory}. A rule with no category applies facets and falls through.
 */
public record ModPatternRule(String modId, Set<String> pathTokens,
                             EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                             String category, String subcategory) {

    public ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
        this(modId, pathTokens, EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                category, subcategory);
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }
}
```

- [ ] **Step 4: Update the parser to read facets**

In `ClassificationOverrides.java`, replace the `out.computeIfAbsent(...)` call at the end of `parsePatterns` (lines 142–144) with:

```java
            out.computeIfAbsent(mod.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                    .add(new ModPatternRule(mod.toLowerCase(Locale.ROOT), tokens,
                            parseFacets(entry, "addFacets"), parseFacets(entry, "removeFacets"),
                            optString(entry, "category"), optString(entry, "subcategory")));
```

(The `tokens` `LinkedHashSet` and `parseFacets` helper already exist in this file.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesParseTest"`
Expected: PASS (both the new test and the existing `parsesItemsAndModPatterns`).

- [ ] **Step 6: Commit**

```
git -C "<worktree>" add xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesParseTest.java
git -C "<worktree>" commit -m "feat: modPattern rules carry add/remove facets"
```

---

### Task 2: Apply modPattern facets in resolve(); facet-only patterns fall through

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java:511-544` (the override block inside `resolve()`)
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideRoutingTest.java`

**Interfaces:**
- Consumes: `ModPatternRule.addFacets()`, `.removeFacets()`, `.hasCategory()`, `.category()`, `.subcategory()` (Task 1); `ClassificationOverrides.patternFor(String modId, String path)`; `FacetCodec.encode(EnumSet<ItemFacet>)`; `route.finish(...)`, `route.skipped(...)`.

Context — the current block (lines 511–544) is:

```java
        java.util.Optional<ClassificationOverride> itemOverride = ClassificationOverrides.forItem(id);
        if (itemOverride.isPresent()) {
            ClassificationOverride o = itemOverride.get();
            facets.addAll(o.addFacets());
            facets.removeAll(o.removeFacets());
            if (!o.addFacets().isEmpty() || !o.removeFacets().isEmpty()) {
                attributes.put(SearchNodeKeys.FACETS, FacetCodec.encode(facets));
            }
        }
        PrimaryCategoryModFamily modFamily = PrimaryCategoryModFamilies.classify(modId);
        AmiConfig.CompatCategoryPolicy categoryPolicy = CompatCategoryPolicyResolver.resolve(attributes);
        if (hasCompatFamily(attributes)) {
            attributes.put(SearchNodeKeys.COMPAT_CATEGORY_POLICY, categoryPolicy.name().toLowerCase(Locale.ROOT));
        }
        FacetProfile routedProfile = new FacetProfile(facets, attributes);
        String candidateSummary = CategoryScorer.candidateSummary(id, routedProfile);
        if (!candidateSummary.isBlank()) {
            attributes.put(SearchNodeKeys.CLASSIFICATION_CANDIDATES, candidateSummary);
        }
        ResolveContext context = new ResolveContext(id, modId, path, facets, attributes, modFamily, categoryPolicy);
        CategoryRouteTrace route = CategoryRouteTrace.start(id, modFamily.name().toLowerCase(Locale.ROOT), facets, attributes);

        if (itemOverride.isPresent() && itemOverride.get().hasForcedCategory()) {
            ClassificationOverride o = itemOverride.get();
            return route.finish("classification_override", "item_override",
                    new CategoryAssignment(o.forceCategory(), o.subcategoryOrEmpty(), attributes));
        }
        Optional<ModPatternRule> patternRule = ClassificationOverrides.patternFor(modId, path);
        if (patternRule.isPresent()) {
            ModPatternRule r = patternRule.get();
            return route.finish("classification_override", "mod_pattern",
                    new CategoryAssignment(r.category(), r.subcategory(), attributes));
        }
        route.skipped("classification_override", "no override matched");
```

- [ ] **Step 1: Write the failing tests**

Add to `ClassificationOverrideRoutingTest.java` (the `EnumSet`, `List`, `Map`, `Set` imports already exist):

```java
    @Test
    void facetOnlyModPatternAddsFacetAndFallsThrough() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("cnc", List.of(new ModPatternRule(
                        "cnc", Set.of("potofmouse"),
                        EnumSet.of(ItemFacet.MAGIC_ARTIFACT), EnumSet.noneOf(ItemFacet.class),
                        null, null))));

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("cnc:potofmouse"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()));

        // facet applied and visible to downstream scoring
        assertTrue(a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        // facet-only pattern must NOT terminate the route as a mod_pattern category
        assertEquals("magic", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
    }

    @Test
    void categoryBearingModPatternStillTerminates() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("examplemod", List.of(new ModPatternRule(
                        "examplemod", Set.of("widget"),
                        EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                        "decoration", "furniture"))));

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:widget"),
                new FacetProfile(EnumSet.of(ItemFacet.MELEE_WEAPON), Map.of()));

        assertEquals("decoration", a.categoryId());
        assertEquals("furniture", a.subcategoryId());
        assertEquals("classification_override", a.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }
```

Add the needed static imports at the top if absent:

```java
import static org.junit.jupiter.api.Assertions.assertTrue;
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideRoutingTest"`
Expected: `facetOnlyModPatternAddsFacetAndFallsThrough` FAILS — current code terminates with `category=null` (NPE or wrong assignment) instead of applying the facet and falling through to `magic/artifacts`.

- [ ] **Step 3: Replace the override block**

Replace lines 511–544 (shown above) with:

```java
        java.util.Optional<ClassificationOverride> itemOverride = ClassificationOverrides.forItem(id);
        Optional<ModPatternRule> patternRule = ClassificationOverrides.patternFor(modId, path);
        if (itemOverride.isPresent()) {
            ClassificationOverride o = itemOverride.get();
            facets.addAll(o.addFacets());
            facets.removeAll(o.removeFacets());
        }
        if (patternRule.isPresent()) {
            ModPatternRule r = patternRule.get();
            facets.addAll(r.addFacets());
            facets.removeAll(r.removeFacets());
        }
        boolean overrideTouchedFacets =
                (itemOverride.isPresent()
                        && (!itemOverride.get().addFacets().isEmpty() || !itemOverride.get().removeFacets().isEmpty()))
                || (patternRule.isPresent()
                        && (!patternRule.get().addFacets().isEmpty() || !patternRule.get().removeFacets().isEmpty()));
        if (overrideTouchedFacets) {
            attributes.put(SearchNodeKeys.FACETS, FacetCodec.encode(facets));
        }
        PrimaryCategoryModFamily modFamily = PrimaryCategoryModFamilies.classify(modId);
        AmiConfig.CompatCategoryPolicy categoryPolicy = CompatCategoryPolicyResolver.resolve(attributes);
        if (hasCompatFamily(attributes)) {
            attributes.put(SearchNodeKeys.COMPAT_CATEGORY_POLICY, categoryPolicy.name().toLowerCase(Locale.ROOT));
        }
        FacetProfile routedProfile = new FacetProfile(facets, attributes);
        String candidateSummary = CategoryScorer.candidateSummary(id, routedProfile);
        if (!candidateSummary.isBlank()) {
            attributes.put(SearchNodeKeys.CLASSIFICATION_CANDIDATES, candidateSummary);
        }
        ResolveContext context = new ResolveContext(id, modId, path, facets, attributes, modFamily, categoryPolicy);
        CategoryRouteTrace route = CategoryRouteTrace.start(id, modFamily.name().toLowerCase(Locale.ROOT), facets, attributes);

        if (itemOverride.isPresent() && itemOverride.get().hasForcedCategory()) {
            ClassificationOverride o = itemOverride.get();
            return route.finish("classification_override", "item_override",
                    new CategoryAssignment(o.forceCategory(), o.subcategoryOrEmpty(), attributes));
        }
        if (patternRule.isPresent() && patternRule.get().hasCategory()) {
            ModPatternRule r = patternRule.get();
            return route.finish("classification_override", "mod_pattern",
                    new CategoryAssignment(r.category(), r.subcategory(), attributes));
        }
        route.skipped("classification_override",
                patternRule.isPresent() ? "mod_pattern facets-only" : "no override matched");
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideRoutingTest"`
Expected: PASS (4 tests: the 2 existing + 2 new).

- [ ] **Step 5: Commit**

```
git -C "<worktree>" add xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideRoutingTest.java
git -C "<worktree>" commit -m "feat: apply modPattern facets early; facet-only patterns fall through"
```

---

### Task 3: Replay gate diffs facets, not just category

**Files:**
- Modify: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideReplayGateTest.java`

**Interfaces:**
- Consumes: `SearchNode.meta(String key, String default)`; `SearchNodeKeys.FACETS`, `ONTOLOGY_CATEGORY`, `ONTOLOGY_SUBCATEGORY`; `SearchNodeMirrorDump.reclassifyItemOntology(List<SearchNode>)`; `ClassificationOverrides.parseAndInstall/clear/loadBundledDefaults`; `explained(SearchNode)` (existing).

Context — the existing helper:

```java
    private static Map<String, String> categoryById(List<SearchNode> nodes) {
        Map<String, String> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            result.put(node.id().toString(),
                    node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "") + "/" + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
        }
        return result;
    }
```

- [ ] **Step 1: Replace `categoryById` with a facet-aware `signatureById`**

Replace the helper with:

```java
    private static Map<String, String> signatureById(List<SearchNode> nodes) {
        Map<String, String> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            String[] facets = node.meta(SearchNodeKeys.FACETS, "").split(",");
            java.util.Arrays.sort(facets);
            result.put(node.id().toString(),
                    node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "") + "/"
                  + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "") + "|"
                  + String.join(",", facets));
        }
        return result;
    }
```

- [ ] **Step 2: Update all `categoryById` call sites**

Replace every `categoryById(` with `signatureById(` — four call sites, at the `baseline`/`afterById` assignments in both `detectsExplainedChangeAndLeavesOthersUntouched` and `writesOverrideReplayGateReport`. (The variable names `baseline`/`afterById` and all diff logic stay unchanged; they now compare the richer signature strings.)

- [ ] **Step 3: Add a facet-only synthetic test**

Add this `@Test` method (it mirrors `detectsExplainedChangeAndLeavesOthersUntouched`, using a facet-only pattern):

```java
    @Test
    void detectsExplainedFacetOnlyChange() throws IOException {
        Path dump = locateDump();
        assumeTrue(Files.exists(dump),
                "no search_nodes dump available locally; the real-data test writes the no-data report");
        List<SearchNode> source = items(dump);
        try {
            ClassificationOverrides.clear();
            Map<String, String> baseline = signatureById(SearchNodeMirrorDump.reclassifyItemOntology(source));

            // facet-only modPattern: minecraft:dirt gains magic_artifact (a facet it never has), no category change
            ClassificationOverrides.parseAndInstall(
                    "{\"items\":{},\"modPatterns\":[{\"mod\":\"minecraft\",\"pathTokens\":[\"dirt\"],"
                  + "\"addFacets\":[\"magic_artifact\"]}]}");
            List<SearchNode> after = SearchNodeMirrorDump.reclassifyItemOntology(source);
            Map<String, String> afterById = signatureById(after);

            assertTrue(afterById.get("minecraft:dirt").contains(ItemFacet.MAGIC_ARTIFACT.id()),
                    "facet-only override must add the facet to the targeted item");
            assertEquals(baseline.get("minecraft:dirt").split("\\|")[0],
                    afterById.get("minecraft:dirt").split("\\|")[0],
                    "facet-only override must not change the targeted item's category");

            List<String> unexplained = new ArrayList<>();
            for (SearchNode node : after) {
                String id = node.id().toString();
                if (!baseline.get(id).equals(afterById.get(id)) && !explained(node)) {
                    unexplained.add(id);
                }
            }
            assertTrue(unexplained.isEmpty(),
                    "a facet-only override changed unexplained items: " + unexplained);
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }
    }
```

Add the import if absent: `import static org.junit.jupiter.api.Assertions.assertEquals;` (already present) and `import com.sanhiruzu.ami.index.ItemFacet;` is unnecessary (same package).

- [ ] **Step 4: Run the gate tests**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideReplayGateTest"`
Expected: PASS. If no dump is present locally, dump-dependent tests report SKIPPED (via `assumeTrue`) and `writesOverrideReplayGateReport` writes the no-data report — still green.

- [ ] **Step 5: Commit**

```
git -C "<worktree>" add neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideReplayGateTest.java
git -C "<worktree>" commit -m "test: replay gate diffs facets alongside category"
```

---

### Task 4: Curation tool — facets on modPattern proposals

**Files:**
- Modify: `tools/classification-curation/validate_proposals.py:39-52` (`modPattern` branch)
- Modify: `tools/classification-curation/apply.py:20-70` (`_pattern_key`, `merge` pattern build)
- Test: `tools/classification-curation/tests/test_validate_proposals.py`, `tools/classification-curation/tests/test_apply.py`

**Interfaces:**
- Produces: `validate_proposal` accepts a `modPattern` with optional `addFacets`/`removeFacets` string lists; a modPattern is valid with `mod` + non-empty single-token `pathTokens` + at least one of (`category` non-empty / `addFacets` / `removeFacets`). `apply.merge` emits `addFacets`/`removeFacets` into the pattern dict when non-empty and includes them in the dedup key.

- [ ] **Step 1: Write failing validator tests**

Add to `tools/classification-curation/tests/test_validate_proposals.py`:

```python
    def test_mod_pattern_facets_only_is_valid(self):
        row = {"id": "cnc:potofmouse", "scope": "modPattern", "decision": "approve",
               "override": {"mod": "cnc", "pathTokens": ["potofmouse"], "addFacets": ["magic_artifact"]},
               "rationale": "x"}
        self.assertEqual(validate_proposals.validate_proposal(row), [])

    def test_mod_pattern_requires_category_or_facets(self):
        row = {"id": "cnc:x", "scope": "modPattern", "decision": "approve",
               "override": {"mod": "cnc", "pathTokens": ["x"]},
               "rationale": "x"}
        self.assertIn("modPattern must set category, addFacets, or removeFacets",
                      validate_proposals.validate_proposal(row))
```

- [ ] **Step 2: Run to verify failure**

Run: `python -m unittest tools.classification_curation.tests.test_validate_proposals` (or via discover). Note the directory has a hyphen, so use discover from the tool dir:
`python -W error::ResourceWarning -m unittest discover -s tools/classification-curation/tests -p "test_validate_proposals.py"`
Expected: FAIL — `test_mod_pattern_facets_only_is_valid` reports "modPattern category must be a non-empty string".

- [ ] **Step 3: Rewrite the `modPattern` validation branch**

Replace the `elif scope == "modPattern":` branch (lines 39–52) with:

```python
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
        add = override.get("addFacets", [])
        rem = override.get("removeFacets", [])
        if not _is_str_list(add) or not _is_str_list(rem):
            errors.append("addFacets/removeFacets must be string arrays")
        has_category = isinstance(category, str) and bool(category.strip())
        if category is not None and not has_category:
            errors.append("modPattern category, when present, must be a non-empty string")
        if not has_category and not add and not rem:
            errors.append("modPattern must set category, addFacets, or removeFacets")
```

- [ ] **Step 4: Run validator tests to verify pass**

Run: `python -W error::ResourceWarning -m unittest discover -s tools/classification-curation/tests -p "test_validate_proposals.py"`
Expected: PASS (existing + 2 new).

- [ ] **Step 5: Write failing apply test**

Add to `tools/classification-curation/tests/test_apply.py`:

```python
    def test_merge_mod_pattern_carries_facets(self):
        ov = {"items": {}, "modPatterns": []}
        updated, _ = apply.merge(ov, [
            {"id": "cnc:potofmouse", "scope": "modPattern", "decision": "approve",
             "override": {"mod": "cnc", "pathTokens": ["potofmouse"], "addFacets": ["magic_artifact"]},
             "rationale": "x"},
        ])
        self.assertEqual(updated["modPatterns"], [
            {"mod": "cnc", "pathTokens": ["potofmouse"], "addFacets": ["magic_artifact"]},
        ])

    def test_merge_facet_pattern_dedups_distinct_facets(self):
        ov = {"items": {}, "modPatterns": []}
        base = {"id": "cnc:a", "scope": "modPattern", "decision": "approve",
                "override": {"mod": "cnc", "pathTokens": ["a"], "addFacets": ["magic_artifact"]},
                "rationale": "x"}
        other = {"id": "cnc:a", "scope": "modPattern", "decision": "approve",
                 "override": {"mod": "cnc", "pathTokens": ["a"], "addFacets": ["ingredient_organic"]},
                 "rationale": "x"}
        updated, _ = apply.merge(ov, [base, dict(base), other])
        self.assertEqual(len(updated["modPatterns"]), 2)  # same tokens, different facets => not duplicate
```

- [ ] **Step 6: Run to verify failure**

Run: `python -W error::ResourceWarning -m unittest discover -s tools/classification-curation/tests -p "test_apply.py"`
Expected: FAIL — pattern dict lacks `addFacets`; dedup collapses the two distinct-facet rows.

- [ ] **Step 7: Update `apply.py`**

Replace `_pattern_key` (lines 35–37) with:

```python
def _pattern_key(pattern):
    return (pattern["mod"], tuple(sorted(pattern["pathTokens"])),
            pattern.get("category", ""), pattern.get("subcategory", ""),
            tuple(sorted(pattern.get("addFacets", []))),
            tuple(sorted(pattern.get("removeFacets", []))))
```

Replace the `else:` pattern-build block inside `merge` (lines 59–69) with:

```python
        else:
            pattern = {"mod": override["mod"], "pathTokens": list(override["pathTokens"])}
            category = override.get("category")
            if category and category.strip():
                pattern["category"] = category
            subcategory = override.get("subcategory")
            if subcategory and subcategory.strip():
                pattern["subcategory"] = subcategory
            if override.get("addFacets"):
                pattern["addFacets"] = list(override["addFacets"])
            if override.get("removeFacets"):
                pattern["removeFacets"] = list(override["removeFacets"])
            key = _pattern_key(pattern)
            if key not in existing_pattern_keys:
                existing_pattern_keys.add(key)
                overrides["modPatterns"].append(pattern)
```

(The `_write_overrides` sort key `(p["mod"], p.get("category", ""), p.get("subcategory", ""))` already tolerates missing category/subcategory — no change needed.)

- [ ] **Step 8: Run the full Python suite**

Run: `python -W error::ResourceWarning -m unittest discover -s tools/classification-curation/tests -p "test_*.py"`
Expected: PASS (all, including existing `test_merge_mod_pattern_dedups` which uses a category-bearing pattern).

- [ ] **Step 9: Commit**

```
git -C "<worktree>" add tools/classification-curation/validate_proposals.py tools/classification-curation/apply.py tools/classification-curation/tests/test_validate_proposals.py tools/classification-curation/tests/test_apply.py
git -C "<worktree>" commit -m "feat: curation tool carries facets on modPattern proposals"
```

(Note: `tools/` is git-ignored; these files are already tracked, so `git add` works without `-f`. If a new file were added it would need `git add -f`.)

---

### Task 5: Author cnc override data + durable equivalence test

**Files:**
- Modify: `xplat/src/main/resources/assets/ami/classification_overrides.json`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/CncOverrideMigrationTest.java`

**Interfaces:**
- Consumes: `ClassificationOverrides.parseAndInstall(String)`, `.clear()`, `.loadBundledDefaults()`; `PrimaryCategoryResolver.resolve(ResourceLocation, EnumSet<ItemFacet>, Map<String,String>)`; `CategoryAssignment.categoryId()/subcategoryId()/attributes()`; `SearchNodeKeys.MOD_ID`, `.ITEM_CLASS`, `.FACETS`; `ItemFacet.INGREDIENT_ORGANIC`, `.MAGIC_ARTIFACT`.

Token equivalence over the real Synesthesia cnc items (verified from the dump):
- organic substrings `buckskin`/`antler`/`tusk`/`wishbone` → tokens match exactly the same items, no over/under-match.
- artifact substrings `potofmouse`/`kill_stick` → tokens `potofmouse` and `kill` match exactly those items.
- `raw_turkey` cannot be a token (would over-match `cooked_turkey`/`raw_venison`) → per-item entry `cnc:raw_turkey`.
- `mandible` → no cnc item contains it → omitted.

- [ ] **Step 1: Write the bundled override data**

Replace the entire contents of `xplat/src/main/resources/assets/ami/classification_overrides.json` with:

```json
{
  "items": {
    "cnc:raw_turkey": { "addFacets": ["ingredient_organic"] }
  },
  "modPatterns": [
    { "mod": "cnc", "pathTokens": ["potofmouse", "kill"], "addFacets": ["magic_artifact"] },
    { "mod": "cnc", "pathTokens": ["buckskin", "antler", "tusk", "wishbone"], "addFacets": ["ingredient_organic"] }
  ]
}
```

(The artifact rule is listed first so `patternFor` prefers it; no cnc item matches both groups, so this is belt-and-suspenders fidelity to the plugin's artifact-priority.)

- [ ] **Step 2: Write the durable equivalence test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/CncOverrideMigrationTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the bundled cnc override data reproduces CncCompat's facet tagging WITHOUT referencing
 * CncCompat — so it survives the plugin's deletion. While CncCompat still exists,
 * SynesthesiaCompatTest pins plugin -> the same literal categories; two green tests sharing the
 * literals = equivalence locked.
 */
class CncOverrideMigrationTest {

    @BeforeEach
    void installBundled() {
        ClassificationOverrides.loadBundledDefaults();
    }

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void organicSingleTokenItemsGainOrganicFacetAndCategory() {
        CategoryAssignment buckskin = resolveBare("cnc:buckskin",
                meta("cnc", "net.imasillylittleguy.cnc.item.DeerLeatherItem"));
        assertTrue(hasFacet(buckskin, ItemFacet.INGREDIENT_ORGANIC));
        assertEquals("ingredients", buckskin.categoryId());
        assertEquals("organic", buckskin.subcategoryId());

        CategoryAssignment wishbone = resolveBare("cnc:wishbone",
                meta("cnc", "net.imasillylittleguy.cnc.item.WishboneItem"));
        assertTrue(hasFacet(wishbone, ItemFacet.INGREDIENT_ORGANIC));
        assertEquals("ingredients", wishbone.categoryId());
        assertEquals("organic", wishbone.subcategoryId());
    }

    @Test
    void organicMultiComponentItemsMatchByToken() {
        // elk_antler -> tokens [elk, antler]; token "antler" must match
        CategoryAssignment antler = resolveBare("cnc:elk_antler",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(antler, ItemFacet.INGREDIENT_ORGANIC));

        // tusk_club -> tokens [tusk, club]; token "tusk" must match
        CategoryAssignment tusk = resolveBare("cnc:tusk_club",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(tusk, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void rawTurkeyGainsOrganicViaPerItemEntry() {
        CategoryAssignment turkey = resolveBare("cnc:raw_turkey",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(turkey, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void artifactItemsGainArtifactFacet() {
        CategoryAssignment pot = resolveBare("cnc:potofmouse",
                meta("cnc", "net.imasillylittleguy.cnc.item.PotofmouseItem"));
        assertTrue(hasFacet(pot, ItemFacet.MAGIC_ARTIFACT));
        assertEquals("magic", pot.categoryId());
        assertEquals("artifacts", pot.subcategoryId());

        // kill_stick -> tokens [kill, stick]; token "kill" must match
        CategoryAssignment killStick = resolveBare("cnc:kill_stick",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(killStick, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void unmatchedCncItemsGainNeitherFacet() {
        CategoryAssignment effigy = resolveBare("cnc:effigy",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(effigy, ItemFacet.INGREDIENT_ORGANIC));
        assertFalse(hasFacet(effigy, ItemFacet.MAGIC_ARTIFACT));

        // cooked_turkey must NOT be caught (raw_turkey was per-item; token "turkey" deliberately unused)
        CategoryAssignment cookedTurkey = resolveBare("cnc:cooked_turkey",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(cookedTurkey, ItemFacet.INGREDIENT_ORGANIC));
    }
}
```

- [ ] **Step 3: Run the migration test (CncCompat still present)**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.CncOverrideMigrationTest"`
Expected: PASS (5 tests).

- [ ] **Step 4: Confirm the existing plugin test still passes (equivalence lock)**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.SynesthesiaCompatTest"`
Expected: PASS — `cncUnknownFamiliesGainOrganicAndArtifactFacts` confirms the plugin produces the same `ingredients/organic` and `magic/artifacts`, matching the override test's literals.

- [ ] **Step 5: Commit**

```
git -C "<worktree>" add xplat/src/main/resources/assets/ami/classification_overrides.json neoforge/src/test/java/com/sanhiruzu/ami/index/CncOverrideMigrationTest.java
git -C "<worktree>" commit -m "feat: bundle cnc classification override data with equivalence test"
```

---

### Task 6: Delete CncCompat and its classification hook

**Files:**
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/CncCompat.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java` (remove lines 475–477 hook + the `CncCompat` import)
- Modify: `neoforge/src/test/java/com/sanhiruzu/ami/index/SynesthesiaCompatTest.java` (remove the `CncCompat` import line 8 + the `cncUnknownFamiliesGainOrganicAndArtifactFacts` method, lines 768–791)

**Interfaces:**
- Consumes: nothing new. Removes the `cnc` classification enrichment; the bundled override (Task 5) now supplies the facets at `resolve()` time.

- [ ] **Step 1: Remove the ItemProvider classification hook**

In `ItemProvider.java`, delete this block (lines 475–477):

```java
        if (namespaceIs(id, "cnc")) {
            ItemProviderCompatHooks.runCompatSafely("CncCompat", () -> CncCompat.enrichItem(id, meta));
        }
```

Then remove the now-unused import line `import com.sanhiruzu.ami.compat.CncCompat;` from `ItemProvider.java` (search for `CncCompat` to find it).

- [ ] **Step 2: Remove the plugin's unit test and import**

In `SynesthesiaCompatTest.java`: delete the import `import com.sanhiruzu.ami.compat.CncCompat;` (line 8) and delete the entire `cncUnknownFamiliesGainOrganicAndArtifactFacts` method (the `@Test` plus its body, lines 768–791).

- [ ] **Step 3: Delete the plugin class**

Delete `xplat/src/main/java/com/sanhiruzu/ami/compat/CncCompat.java`.

- [ ] **Step 4: Verify nothing else references CncCompat**

Run (PowerShell, from worktree): grep for stragglers.
```
git -C "<worktree>" grep -n "CncCompat"
```
Expected: NO matches. (If any remain, they must be removed — but `CrittersCrawlersGuideSource` and `CompatIndexRegistry.CrittersCrawlersPlugin` reference `"cnc"` as a string for the GUIDE, not `CncCompat` — those stay and will not appear in this grep.)

- [ ] **Step 5: Compile and run the full Java test suite**

Run: `./gradlew :neoforge:test`
Expected: BUILD SUCCESSFUL. `CncOverrideMigrationTest` still passes (it never referenced `CncCompat`); `SynesthesiaCompatTest` passes without the cnc method; the replay gate passes (cnc facets in the dump are now supplied by the override — idempotent over the plugin-active dump, 0 unexplained changes).

- [ ] **Step 6: Run the full Python suite (unchanged, sanity)**

Run: `python -W error::ResourceWarning -m unittest discover -s tools/classification-curation/tests -p "test_*.py"`
Expected: PASS.

- [ ] **Step 7: Commit**

```
git -C "<worktree>" add -A xplat/src/main/java/com/sanhiruzu/ami/compat/CncCompat.java xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java neoforge/src/test/java/com/sanhiruzu/ami/index/SynesthesiaCompatTest.java
git -C "<worktree>" commit -m "refactor: delete CncCompat; cnc classification now data-driven"
```

---

## Self-Review

**Spec coverage:**
- Component 1 (modPattern facets in consumer) → Tasks 1 + 2. ✓
- Component 2 (gate diffs facets) → Task 3. ✓
- Component 3 (curation tool facets) → Task 4. ✓
- Component 4 (cnc override data) → Task 5 Step 1. ✓
- Component 5 (equivalence proof) → Task 5 Steps 2–4. ✓
- Sequencing / deletion (preserve guide source) → Task 6 + Global Constraints. ✓
- Accepted deltas (search tokens, `cncItemKind`) → Global Constraints. ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete code and exact gradle/python commands with expected output. ✓

**Type consistency:** `ModPatternRule` 6-arg + 4-arg constructors and `hasCategory()` defined in Task 1 and used consistently in Tasks 2 and 5. `signatureById` defined and all four call sites renamed in Task 3. `resolveBare`/`hasFacet`/`meta` helpers are local to the Task 5 test. `_pattern_key`/`merge` facet handling consistent across Task 4. Facet ids (`ingredient_organic`, `magic_artifact`) consistent with `ItemFacet` enum ids. ✓

**Correctness note:** Over the real Synesthesia dump, the authored tokens reproduce `CncCompat` exactly (verified item-by-item). The `unmatchedCncItemsGainNeitherFacet` test guards against token over-match (the chief risk), including the deliberate `cooked_turkey` negative.
