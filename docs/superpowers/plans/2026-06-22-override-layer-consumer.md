# Override Layer Consumer (Plan 2a) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a data-driven classification override layer that, at the top of `PrimaryCategoryResolver.resolve()`, applies per-item facet add/remove + forced category and per-mod path-token pattern rules — routing items into **existing** categories from bundled JSON, so per-mod string rules become editable data instead of code.

**Architecture:** A static `ClassificationOverrides` registry holds two lookups (per-item overrides keyed by `mod:path`, per-mod pattern rules keyed by mod id), loaded from a bundled `assets/ami/classification_overrides.json` via the classpath (the `AmiTaxonomyCatalog` pattern). `resolve()` consults it before any existing gate: it mutates the (mutable) facet set for add/remove, then short-circuits via `route.finish("classification_override", …)` for a forced category or a matching pattern rule. Pure xplat; unit-tested through `resolve()`.

**Tech Stack:** Java 21, Minecraft 1.21.1 / NeoForge, Gson, JUnit 5. Code in `xplat`; tests in the `neoforge` module exercising pure-`xplat` classes.

## Global Constraints

- Classifier code is pure xplat — no client/server/platform imports in `ClassificationOverrides`, `ClassificationOverride`, `ModPatternRule`, or the resolver edit.
- Tests go under `neoforge/src/test/java/com/sanhiruzu/ami/index/`; construct ids with `new ResourceLocation("namespace:path")` and profiles with `new FacetProfile(EnumSet, Map)`.
- Run tests with `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.<ClassName>"`.
- Commit messages: plain imperative, no AI/assistant attribution. When running git via Bash, never put a path containing `.claude` in the command string (a commit-guard hook rejects the literal substring `claude`); run `git` from the repo root with relative paths, no `cd`.
- `CategoryAssignment` is `record CategoryAssignment(String categoryId, String subcategoryId, Map<String,String> attributes)` — it copies attributes defensively.
- The override gate must run at the **top** of `resolve()` (before `shouldUseEarlyCompatRouteMetadata`) and finish through `route.finish("classification_override", <ruleId>, …)` so the route trace is correct. Do not reorder the existing gates.
- Precedence within the override: per-item forced category beats per-mod pattern rule.

## Out of Scope (other plans)

- `promoteMod` and dynamic **new** top-level categories + category-tree UI registration → Plan 2b (pairs with the WORKFLOW ontology work).
- Datapack-overridable per-mod files via a `ResourceManager` reload listener → later; Plan 2a ships defaults as one bundled classpath JSON.
- Migrating the ~30 compat plugins and the in-resolver per-mod blocks into the data file → Plan 3 (this plan builds the consumer they target).

---

### Task 1: Override data model + static registry

**Files:**
- Create: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java`
- Create: `xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java`
- Create: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java`
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesTest.java`

**Interfaces:**
- Produces:
  - `record ClassificationOverride(EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets, String forceCategory, String forceSubcategory)` — `forceCategory`/`forceSubcategory` may be null; `subcategoryOrEmpty()` returns `forceSubcategory` or `""`.
  - `record ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory)`.
  - `ClassificationOverrides` static API: `void install(Map<String,ClassificationOverride> items, Map<String,List<ModPatternRule>> patterns)`, `void clear()`, `Optional<ClassificationOverride> forItem(ResourceLocation id)`, `Optional<ModPatternRule> patternFor(String modId, String path)`.

- [ ] **Step 1: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void forItemReturnsInstalledOverride() {
        ClassificationOverrides.install(
                Map.of("examplemod:widget", new ClassificationOverride(
                        EnumSet.of(ItemFacet.MAGIC_REAGENT), EnumSet.noneOf(ItemFacet.class), "magic", "reagents")),
                Map.of());

        Optional<ClassificationOverride> found = ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget"));
        assertTrue(found.isPresent());
        assertEquals("magic", found.get().forceCategory());
        assertTrue(found.get().addFacets().contains(ItemFacet.MAGIC_REAGENT));
        assertTrue(ClassificationOverrides.forItem(new ResourceLocation("examplemod:other")).isEmpty());
    }

    @Test
    void patternForMatchesAnyPathToken() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("botania", List.of(new ModPatternRule(
                        "botania", Set.of("mana", "spreader"), "magic", "reagents"))));

        Optional<ModPatternRule> hit = ClassificationOverrides.patternFor("botania", "mana_spreader");
        assertTrue(hit.isPresent());
        assertEquals("magic", hit.get().category());
        assertFalse(ClassificationOverrides.patternFor("botania", "petal_apothecary").isPresent());
        assertFalse(ClassificationOverrides.patternFor("create", "mana_spreader").isPresent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesTest"`
Expected: FAIL — `ClassificationOverride`, `ModPatternRule`, `ClassificationOverrides` don't exist (compile error).

- [ ] **Step 3: Create the two records**

`xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java`:

```java
package com.sanhiruzu.ami.index;

import java.util.EnumSet;

/**
 * Per-item classification override loaded from data. {@code forceCategory}/{@code forceSubcategory}
 * may be null when the override only adjusts facets.
 */
public record ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                     EnumSet<ItemFacet> removeFacets,
                                     String forceCategory,
                                     String forceSubcategory) {
    public boolean hasForcedCategory() {
        return forceCategory != null && !forceCategory.isBlank();
    }

    public String subcategoryOrEmpty() {
        return forceSubcategory == null ? "" : forceSubcategory;
    }
}
```

`xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java`:

```java
package com.sanhiruzu.ami.index;

import java.util.Set;

/** Per-mod path-token rule: if an item from {@code modId} has any of {@code pathTokens}, route to category/subcategory. */
public record ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
}
```

- [ ] **Step 4: Create the registry**

`xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ClassificationOverrides {
    private static volatile Map<String, ClassificationOverride> itemOverrides = Map.of();
    private static volatile Map<String, List<ModPatternRule>> modPatternRules = Map.of();

    private ClassificationOverrides() {
    }

    public static void install(Map<String, ClassificationOverride> items,
                               Map<String, List<ModPatternRule>> patterns) {
        itemOverrides = Map.copyOf(items);
        modPatternRules = Map.copyOf(patterns);
    }

    public static void clear() {
        itemOverrides = Map.of();
        modPatternRules = Map.of();
    }

    public static Optional<ClassificationOverride> forItem(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemOverrides.get(id.toString().toLowerCase(Locale.ROOT)));
    }

    public static Optional<ModPatternRule> patternFor(String modId, String path) {
        if (modId == null || path == null) {
            return Optional.empty();
        }
        List<ModPatternRule> rules = modPatternRules.get(modId.toLowerCase(Locale.ROOT));
        if (rules == null) {
            return Optional.empty();
        }
        String[] tokens = path.toLowerCase(Locale.ROOT).split("[_/]");
        for (ModPatternRule rule : rules) {
            for (String token : tokens) {
                if (rule.pathTokens().contains(token)) {
                    return Optional.of(rule);
                }
            }
        }
        return Optional.empty();
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesTest"`
Expected: PASS (both tests).

- [ ] **Step 6: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesTest.java
git commit -m "feat: add classification override data model and registry"
```

---

### Task 2: Parse override JSON into the registry

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java`
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesParseTest.java`

**Interfaces:**
- Produces: `ClassificationOverrides.parseAndInstall(String json)` — parses the schema below and installs it. Schema:

```json
{
  "items": {
    "examplemod:widget": { "category": "magic", "subcategory": "reagents",
                           "addFacets": ["magic_reagent"], "removeFacets": ["decorative_block"] }
  },
  "modPatterns": [
    { "mod": "botania", "pathTokens": ["mana", "spreader"], "category": "magic", "subcategory": "reagents" }
  ]
}
```

- Consumes: `ItemFacet.byId(String)` (`xplat/.../index/ItemFacet.java`) for facet name → enum; Gson (`com.google.gson`), already a dependency (used in `AmiDataFixes`/`AmiTaxonomyCatalog`).

- [ ] **Step 1: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesParseTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesParseTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void parsesItemsAndModPatterns() {
        String json = """
            {
              "items": {
                "examplemod:widget": { "category": "magic", "subcategory": "reagents",
                                       "addFacets": ["magic_reagent"], "removeFacets": ["decorative_block"] }
              },
              "modPatterns": [
                { "mod": "botania", "pathTokens": ["mana", "spreader"], "category": "magic", "subcategory": "reagents" }
              ]
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        ClassificationOverride item = ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget")).orElseThrow();
        assertEquals("magic", item.forceCategory());
        assertEquals("reagents", item.forceSubcategory());
        assertTrue(item.addFacets().contains(ItemFacet.MAGIC_REAGENT));
        assertTrue(item.removeFacets().contains(ItemFacet.DECORATIVE_BLOCK));

        ModPatternRule rule = ClassificationOverrides.patternFor("botania", "mana_spreader").orElseThrow();
        assertEquals("magic", rule.category());
    }

    @Test
    void blankOrMalformedJsonInstallsEmpty() {
        ClassificationOverrides.parseAndInstall("not json");
        assertTrue(ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget")).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesParseTest"`
Expected: FAIL — `parseAndInstall` is undefined (compile error).

- [ ] **Step 3: Add the parser**

In `ClassificationOverrides.java`, add these imports under the existing ones:

```java
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
```

Add these methods to the class:

```java
    public static void parseAndInstall(String json) {
        Map<String, ClassificationOverride> items = new LinkedHashMap<>();
        Map<String, List<ModPatternRule>> patterns = new LinkedHashMap<>();
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                parseItems(root, items);
                parsePatterns(root, patterns);
            }
        } catch (RuntimeException ignored) {
            // Malformed override data must never break indexing; fall back to empty.
        }
        install(items, patterns);
    }

    private static void parseItems(JsonObject root, Map<String, ClassificationOverride> out) {
        if (!root.has("items") || !root.get("items").isJsonObject()) {
            return;
        }
        JsonObject items = root.getAsJsonObject("items");
        for (String id : items.keySet()) {
            if (!items.get(id).isJsonObject()) {
                continue;
            }
            JsonObject entry = items.getAsJsonObject(id);
            String category = optString(entry, "category");
            String subcategory = optString(entry, "subcategory");
            out.put(id.toLowerCase(Locale.ROOT), new ClassificationOverride(
                    parseFacets(entry, "addFacets"),
                    parseFacets(entry, "removeFacets"),
                    category,
                    subcategory));
        }
    }

    private static void parsePatterns(JsonObject root, Map<String, List<ModPatternRule>> out) {
        if (!root.has("modPatterns") || !root.get("modPatterns").isJsonArray()) {
            return;
        }
        JsonArray rules = root.getAsJsonArray("modPatterns");
        for (JsonElement element : rules) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String mod = optString(entry, "mod");
            if (mod == null || mod.isBlank()) {
                continue;
            }
            Set<String> tokens = new LinkedHashSet<>();
            if (entry.has("pathTokens") && entry.get("pathTokens").isJsonArray()) {
                for (JsonElement t : entry.getAsJsonArray("pathTokens")) {
                    tokens.add(t.getAsString().toLowerCase(Locale.ROOT));
                }
            }
            out.computeIfAbsent(mod.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                    .add(new ModPatternRule(mod.toLowerCase(Locale.ROOT), tokens,
                            optString(entry, "category"), optString(entry, "subcategory")));
        }
    }

    private static EnumSet<ItemFacet> parseFacets(JsonObject entry, String key) {
        EnumSet<ItemFacet> result = EnumSet.noneOf(ItemFacet.class);
        if (entry.has(key) && entry.get(key).isJsonArray()) {
            for (JsonElement element : entry.getAsJsonArray(key)) {
                ItemFacet facet = ItemFacet.byId(element.getAsString().trim().toLowerCase(Locale.ROOT));
                if (facet != null) {
                    result.add(facet);
                }
            }
        }
        return result;
    }

    private static String optString(JsonObject entry, String key) {
        return entry.has(key) && entry.get(key).isJsonPrimitive() ? entry.get(key).getAsString() : null;
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesParseTest"`
Expected: PASS (both tests).

- [ ] **Step 5: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesParseTest.java
git commit -m "feat: parse classification override JSON into the registry"
```

---

### Task 3: Apply overrides at the top of resolve()

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java` (resolve(), around lines 505–524)
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideRoutingTest.java`

**Interfaces:**
- Consumes: `ClassificationOverrides.forItem`, `ClassificationOverrides.patternFor`; `CategoryAssignment(String,String,Map)`; the existing `route` (`CategoryRouteTrace`) and `attributes` (mutable `HashMap`) and `facets` (mutable `EnumSet`) in `resolve()`.
- Produces: a new route phase value `"classification_override"` with rule ids `"item_override"` / `"mod_pattern"`.

- [ ] **Step 1: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideRoutingTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassificationOverrideRoutingTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void perItemForceCategoryWinsOverRuntime() {
        ClassificationOverrides.install(
                Map.of("examplemod:confusing", new ClassificationOverride(
                        EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class), "decoration", "furniture")),
                Map.of());

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:confusing"),
                new FacetProfile(EnumSet.of(ItemFacet.MELEE_WEAPON), Map.of()));

        assertEquals("decoration", a.categoryId());
        assertEquals("furniture", a.subcategoryId());
        assertEquals("classification_override", a.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void perItemAddFacetChangesRouting() {
        ClassificationOverrides.install(
                Map.of("examplemod:mystery", new ClassificationOverride(
                        EnumSet.of(ItemFacet.MELEE_WEAPON), EnumSet.noneOf(ItemFacet.class), null, null)),
                Map.of());

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:mystery"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()));

        assertEquals("tools", a.categoryId());
    }

    @Test
    void modPatternRoutesByPathToken() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("botania", List.of(new ModPatternRule(
                        "botania", Set.of("spreader"), "magic", "reagents"))));

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("botania:mana_spreader"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()));

        assertEquals("magic", a.categoryId());
        assertEquals("reagents", a.subcategoryId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideRoutingTest"`
Expected: FAIL — `perItemForceCategoryWinsOverRuntime` gets `tools` (melee facet) not `decoration`; pattern/add tests get `misc`.

- [ ] **Step 3: Apply facet add/remove early**

In `PrimaryCategoryResolver.resolve(...)`, immediately after the `var attributes = new HashMap<>(...)` line (currently line 510) and before `PrimaryCategoryModFamily modFamily = ...`, insert:

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
```

- [ ] **Step 4: Apply forced category / pattern after the route is built**

In the same method, immediately after the `CategoryRouteTrace route = CategoryRouteTrace.start(...)` line (currently line 523) and before `if (shouldUseEarlyCompatRouteMetadata(context)) {`, insert:

```java
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

(`Optional` is already imported in this file via `import java.util.*;`.)

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideRoutingTest"`
Expected: PASS (all three tests).

- [ ] **Step 6: Run the existing classification suite to confirm no regressions**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.PrimaryCategoryResolverTest" --tests "com.sanhiruzu.ami.index.CategoryScorerTest" --tests "com.sanhiruzu.ami.index.ClassificationArchitectureGuardrailTest"`
Expected: PASS — with an empty registry (default), the override gate always skips, so existing behavior is unchanged.

- [ ] **Step 7: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideRoutingTest.java
git commit -m "feat: apply classification overrides at the top of resolve"
```

---

### Task 4: Load bundled override defaults from the classpath

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java`
- Create: `xplat/src/main/resources/assets/ami/classification_overrides.json`
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesLoadTest.java`

**Interfaces:**
- Produces: `ClassificationOverrides.loadBundledDefaults()` — reads `assets/ami/classification_overrides.json` from the classpath and installs it; safe (no-throw) when the file is missing or malformed.
- Consumes: the `AmiTaxonomyCatalog` classpath pattern (`getClassLoader().getResourceAsStream(...)`, `xplat/.../index/AmiTaxonomyCatalog.java:90-92`).

- [ ] **Step 1: Ship an empty bundled override file**

Create `xplat/src/main/resources/assets/ami/classification_overrides.json`:

```json
{
  "items": {},
  "modPatterns": []
}
```

- [ ] **Step 2: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesLoadTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesLoadTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void loadingBundledDefaultsDoesNotThrowAndLeavesRegistryQueryable() {
        ClassificationOverrides.loadBundledDefaults();
        // The shipped default file is empty, so no override should match, and no exception should be thrown.
        assertTrue(ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget")).isEmpty());
        assertTrue(ClassificationOverrides.patternFor("botania", "mana_spreader").isEmpty());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesLoadTest"`
Expected: FAIL — `loadBundledDefaults` is undefined (compile error).

- [ ] **Step 4: Add the classpath loader**

In `ClassificationOverrides.java`, add these imports:

```java
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
```

Add this method to the class:

```java
    public static void loadBundledDefaults() {
        try (var stream = ClassificationOverrides.class.getClassLoader()
                .getResourceAsStream("assets/ami/classification_overrides.json")) {
            if (stream == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
            }
            parseAndInstall(sb.toString());
        } catch (RuntimeException | java.io.IOException ignored) {
            // Missing or unreadable defaults must not break indexing.
        }
    }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesLoadTest"`
Expected: PASS.

- [ ] **Step 6: Wire the load into indexing startup**

In `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`, find the index-build entry method (the one that iterates items and calls `applyPrimaryCategoryMeta`, near line 990). Add a one-time guarded load at the very start of that method body, before the item loop:

```java
        ClassificationOverrides.loadBundledDefaults();
```

If `ItemProvider` already imports types from `com.sanhiruzu.ami.index`, no import is needed (same package root); otherwise add `import com.sanhiruzu.ami.index.ClassificationOverrides;`. This refreshes the registry from the bundled file each index build. (Datapack-overridable per-mod files via a reload listener are deferred — see Out of Scope.)

- [ ] **Step 7: Run the full index test package to confirm no regressions**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.*"`
Expected: PASS — defaults are empty, so classification is unchanged; all override tests green.

- [ ] **Step 8: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java xplat/src/main/resources/assets/ami/classification_overrides.json xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesLoadTest.java
git commit -m "feat: load bundled classification override defaults from classpath"
```

---

## Final verification

- [ ] **Run the full index test package**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.*"`
Expected: PASS — all new override tests plus the existing classification suite green; the 10 `@Disabled` WIP tests remain skipped.

## Self-Review notes

- **Spec coverage:** Implements Component 2's per-item (`add`/`remove`/`forceCategory`/`forceSubcategory`) and per-mod pattern-rule scopes, applied at the top of `resolve()` with override-wins precedence. `promoteMod` + dynamic top-level categories are deferred to Plan 2b (documented in Out of Scope). The bundled single-file loader is the shippable defaults path; datapack per-mod files are deferred.
- **Type consistency:** `ClassificationOverride(EnumSet,EnumSet,String,String)`, `ModPatternRule(String,Set,String,String)`, and the registry methods `forItem`/`patternFor`/`install`/`clear`/`parseAndInstall`/`loadBundledDefaults` are used identically across Tasks 1–4 and the resolver edit. `CategoryAssignment(String,String,Map)` matches the record.
- **Placeholder scan:** none — every step has concrete code and an exact command.
- **No-regression basis:** an empty default registry makes the override gate a no-op, so existing classification outcomes are unchanged; this is asserted in Tasks 3 and 4.
