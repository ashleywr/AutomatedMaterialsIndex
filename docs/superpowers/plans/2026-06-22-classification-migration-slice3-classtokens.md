# Plan 3b Slice 3 — classTokens Schema Extension + Batch Migration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `classTokens` substring-matching support to `ModPatternRule`, then migrate 7 compat plugins (CgsCompat, NtglCompat, MowziesMobsCompat, MinecoloniesCompat, HpmCompat, McTradePostCompat, CataclysmCompat) to override JSON data and delete the Java files.

**Architecture:** `ModPatternRule` gains an optional `classTokens: Set<String>` field. `ClassificationOverrides.patternFor()` gains an `itemClass` parameter and fires a rule when any `classToken` is a substring of the (lowercased) item class, in addition to the existing path-token logic. All other pipeline wiring is unchanged. The Python curation tools are updated to allow `classTokens` in place of (or alongside) `pathTokens` in modPattern proposals.

**Tech Stack:** Java 21 records, Gson JSON parsing, JUnit 5, Python 3 stdlib.

## Global Constraints

- `ModPatternRule` record must remain a Java record (no class conversion).
- `classTokens` matching is case-insensitive substring: `rule.classTokens().stream().anyMatch(t -> itemClass.contains(t))` where `itemClass` is already lowercased by the caller.
- `patternFor()` keeps the same first-match-wins semantics (order of JSON declarations is priority order within a mod).
- `pathTokens` remains optional when `classTokens` is non-empty (validate_proposals.py must allow either or both; at least one must be non-empty).
- Each migrated plugin: (a) its JSON entries must reproduce the facet tagging from the Java `enrichItem()` method; (b) an equivalence JUnit test class named `<Mod>OverrideMigrationTest` must prove equivalence without importing the deleted compat class; (c) the Java compat file must be fully deleted; (d) the `ItemProvider` hook block must be removed; (e) `SynesthesiaCompatTest` import and test method(s) for that compat must be removed.
- All existing tests must remain green. Run with `./gradlew :neoforge:test` from the repo root.
- Commit style: plain messages, no AI attribution.

---

### Task 1: Add `classTokens` to `ModPatternRule`, `ClassificationOverrides`, and curation tools

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java` (line 512 — patternFor call)
- Modify: `tools/classification-curation/validate_proposals.py`
- Modify: `tools/classification-curation/apply.py`
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesClassTokenTest.java` (new)

**Interfaces:**
- Produces: `ModPatternRule.classTokens()` returns `Set<String>` (empty set if not specified); `ClassificationOverrides.patternFor(String modId, String path, String itemClass)` — three-arg form replaces two-arg.
- `PrimaryCategoryResolver` calls the new three-arg form, passing `attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT)`.

- [ ] **Step 1: Update `ModPatternRule` record**

```java
// xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java
package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.Set;

/**
 * Per-mod classification rule. Fires when an item from {@code modId} matches any path token
 * OR any class token (substring of the item class name). Applies facets and — when
 * {@link #hasCategory()} — routes to {@code category}/{@code subcategory}. A rule with no
 * category applies facets and falls through.
 */
public record ModPatternRule(String modId, Set<String> pathTokens, Set<String> classTokens,
                             EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                             String category, String subcategory) {

    /** Convenience constructor for rules that only match path tokens (no class tokens). */
    public ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
        this(modId, pathTokens, Set.of(), EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                category, subcategory);
    }

    public boolean hasCategory() {
        return category != null && !category.isBlank();
    }
}
```

- [ ] **Step 2: Update `ClassificationOverrides.patternFor()` and parser**

Replace the two-arg `patternFor()` with a three-arg form. Update the parser to read `classTokens`.

In `ClassificationOverrides.java`:

```java
// Replace:
public static Optional<ModPatternRule> patternFor(String modId, String path) {

// With:
public static Optional<ModPatternRule> patternFor(String modId, String path, String itemClass) {
    if (modId == null || path == null) {
        return Optional.empty();
    }
    List<ModPatternRule> rules = modPatternRules.get(modId.toLowerCase(Locale.ROOT));
    if (rules == null) {
        return Optional.empty();
    }
    String normClass = itemClass == null ? "" : itemClass.toLowerCase(Locale.ROOT);
    String[] tokens = path.toLowerCase(Locale.ROOT).split("[_/]");
    for (ModPatternRule rule : rules) {
        for (String token : tokens) {
            if (!rule.pathTokens().isEmpty() && rule.pathTokens().contains(token)) {
                return Optional.of(rule);
            }
        }
        if (!rule.classTokens().isEmpty()) {
            for (String ct : rule.classTokens()) {
                if (normClass.contains(ct)) {
                    return Optional.of(rule);
                }
            }
        }
    }
    return Optional.empty();
}
```

In the JSON parser section (around line 135-145, where `ModPatternRule` is constructed), add classTokens parsing:

```java
// After the existing pathTokens parsing block, read classTokens:
Set<String> classTokens = new LinkedHashSet<>();
if (entry.has("classTokens") && entry.get("classTokens").isJsonArray()) {
    for (JsonElement t : entry.getAsJsonArray("classTokens")) {
        if (t.isJsonPrimitive()) {
            classTokens.add(t.getAsString().toLowerCase(Locale.ROOT));
        }
    }
}
```

And update the `ModPatternRule` construction to pass classTokens:

```java
rules.add(new ModPatternRule(mod.toLowerCase(Locale.ROOT), tokens,
        Collections.unmodifiableSet(classTokens),
        parseFacets(entry, "addFacets"), parseFacets(entry, "removeFacets"),
        optString(entry, "category"), optString(entry, "subcategory")));
```

Also update the existing two-arg convenience calls (in constructor calls within ClassificationOverrides itself, if any) to pass `Set.of()` for classTokens.

- [ ] **Step 3: Update `PrimaryCategoryResolver` call site (line 512)**

```java
// Before:
Optional<ModPatternRule> patternRule = ClassificationOverrides.patternFor(modId, path);

// After:
String itemClass = attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
Optional<ModPatternRule> patternRule = ClassificationOverrides.patternFor(modId, path, itemClass);
```

- [ ] **Step 4: Update `validate_proposals.py`**

The validator currently requires `pathTokens` as non-empty. Change it so that `pathTokens` OR `classTokens` (or both) is acceptable, but at least one must be non-empty:

```python
# In validate_proposal(), replace the modPattern block:
elif scope == "modPattern":
    mod = override.get("mod")
    if not isinstance(mod, str) or not mod.strip():
        errors.append("modPattern mod must be a non-empty string")
    path_tokens = override.get("pathTokens", [])
    class_tokens = override.get("classTokens", [])
    if not _is_str_list(path_tokens):
        errors.append("pathTokens must be a string array")
    else:
        for token in path_tokens:
            if "_" in token or "/" in token:
                errors.append(f"pathToken '{token}' must be a single token (no '_' or '/')")
    if not _is_str_list(class_tokens):
        errors.append("classTokens must be a string array")
    if not path_tokens and not class_tokens:
        errors.append("modPattern must have at least one pathToken or classToken")
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

- [ ] **Step 5: Update `apply.py`**

Update `_pattern_key()` to include classTokens, and the pattern builder to write classTokens when present:

```python
def _pattern_key(pattern):
    return (pattern["mod"],
            tuple(sorted(pattern.get("pathTokens", []))),
            tuple(sorted(pattern.get("classTokens", []))),
            pattern.get("category", ""), pattern.get("subcategory", ""),
            tuple(sorted(pattern.get("addFacets", []))),
            tuple(sorted(pattern.get("removeFacets", []))))
```

In the merge() function, update pattern builder:

```python
pattern = {"mod": override["mod"]}
if override.get("pathTokens"):
    pattern["pathTokens"] = list(override["pathTokens"])
if override.get("classTokens"):
    pattern["classTokens"] = list(override["classTokens"])
```

- [ ] **Step 6: Write `ClassificationOverridesClassTokenTest`**

Proves classTokens matching works end-to-end:

```java
// neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesClassTokenTest.java
package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverridesClassTokenTest {

    @BeforeEach
    void setup() {
        ClassificationOverrides.clear();
        // Install a synthetic rule: mod "testmod", classToken "SwordItem" → MELEE_WEAPON facet
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "classTokens": ["sworditem"], "addFacets": ["melee_weapon"] }
                  ]
                }
                """);
    }

    @AfterEach
    void teardown() { ClassificationOverrides.clear(); }

    @Test
    void classTokenSubstringMatchFiringProducesFacet() {
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "com.example.testmod.SwordItem");
        assertTrue(rule.isPresent());
        assertTrue(rule.get().addFacets().contains(ItemFacet.MELEE_WEAPON));
    }

    @Test
    void classTokenMatchIsCaseInsensitive() {
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "COM.EXAMPLE.TESTMOD.SWORDITEM");
        assertTrue(rule.isPresent());
    }

    @Test
    void nonMatchingClassProducesEmpty() {
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "com.example.testmod.AxeItem");
        assertFalse(rule.isPresent());
    }

    @Test
    void pathTokensAndClassTokensAreIndependent() {
        // Install a rule with both pathTokens and classTokens
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["bow"], "classTokens": ["arrowitem"],
                      "addFacets": ["projectile"] }
                  ]
                }
                """);
        // Fires via path token
        var byPath = ClassificationOverrides.patternFor("testmod", "testmod_bow", "com.example.GenericItem");
        assertTrue(byPath.isPresent());
        // Fires via class token
        var byClass = ClassificationOverrides.patternFor("testmod", "wooden_stick", "com.example.testmod.ArrowItem");
        assertTrue(byClass.isPresent());
    }
}
```

- [ ] **Step 7: Run tests**

```
./gradlew :neoforge:test
```

All existing tests must pass. The 4 new ClassificationOverridesClassTokenTest tests must pass.

- [ ] **Step 8: Commit**

```
git add xplat/src/main/java/com/sanhiruzu/ami/index/ModPatternRule.java \
        xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java \
        xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java \
        tools/classification-curation/validate_proposals.py \
        tools/classification-curation/apply.py \
        neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesClassTokenTest.java
git commit -m "feat: add classTokens substring matching to ModPatternRule + ClassificationOverrides"
```

---

### Task 2: Migrate CgsCompat, NtglCompat, MowziesMobsCompat, MinecoloniesCompat

Migrate 4 class-token-only compat plugins to override JSON data.

**Files:**
- Modify: `xplat/src/main/resources/assets/ami/classification_overrides.json`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/CgsOverrideMigrationTest.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/NtglOverrideMigrationTest.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/MowziesMobsOverrideMigrationTest.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/MinecoloniesOverrideMigrationTest.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/CgsCompat.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/NtglCompat.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/MowziesMobsCompat.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/MinecoloniesCompat.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java` (remove 8 lines across 4 if-blocks)
- Modify: `neoforge/src/test/java/com/sanhiruzu/ami/index/SynesthesiaCompatTest.java` (remove 4 imports + 4 test methods)

**Interfaces:**
- Consumes: `ClassificationOverrides.patternFor(modId, path, itemClass)` from Task 1.
- Equivalence tests use `PrimaryCategoryResolver.resolve()` + `ClassificationOverrides.loadBundledDefaults()`.

#### JSON entries to add to `classification_overrides.json` (in the `modPatterns` array)

Add these after the existing `born_in_chaos_v1` entries, keeping mods in alphabetical order. Add in this order: `cataclysm`, `cgs`, `minecolonies`, `mowziesmobs`, `ntgl` (alphabetical by mod id).

Wait — Task 2 covers cgs, ntgl, mowziesmobs, minecolonies. Task 3 covers hpm, mctradepost, cataclysm. Insert JSON in alphabetical order within the modPatterns array.

**cgs entries (CgsCompat lines 26-33):**
```json
{ "mod": "cgs", "classTokens": ["gatlingitem"], "addFacets": ["ranged_weapon"] },
{ "mod": "cgs", "classTokens": ["scopeitem", "attachmentitem", "barrelitem"], "addFacets": ["tech_component", "upgrade"] }
```
Source: `if (CompatMetaUtil.containsAny(context.itemClass, "GatlingItem")) → RANGED_WEAPON`. `ScopeItem/AttachmentItem/BarrelItem → TECH_COMPONENT + UPGRADE`.

**minecolonies entries (MinecoloniesCompat lines 28-35):**

Priority order (deployer → colony_tool → ammo → potion → organic → component → token):

```json
{ "mod": "minecolonies", "classTokens": ["supplychestdeployer", "supplycampdeployer"], "category": "minecolonies", "subcategory": "settlements" },
{ "mod": "minecolonies", "classTokens": ["itemscananalyzer", "itemscepterpermission", "itemscepterguard", "itemscepterlumberjack", "itemscepterbeekeeper", "itemassistanthammer"], "addFacets": ["utility_tool"] },
{ "mod": "minecolonies", "classTokens": ["itemfirearow"], "addFacets": ["projectile"] },
{ "mod": "minecolonies", "classTokens": ["itemmagicpotion"], "addFacets": ["potion"] },
{ "mod": "minecolonies", "classTokens": ["itemcompost", "itemmistletoe"], "addFacets": ["ingredient_organic"] },
{ "mod": "minecolonies", "classTokens": ["itemsiftermesh"], "addFacets": ["tech_component"] },
{ "mod": "minecolonies", "classTokens": ["itemadventuretoken"], "addFacets": ["utility_misc"] }
```

Note: `ItemFireArrow` lowercases to `itemfirearow` — one `r`, because the class is `ItemFireArrow` → `itemfirearow`. Confirm by checking the actual class name in MinecoloniesCompat line 29.

**mowziesmobs entries (MowziesMobsCompat lines 26-29):**

Priority order (ammo > tool > artifact > organic_material):

```json
{ "mod": "mowziesmobs", "classTokens": ["itemdart"], "addFacets": ["projectile"] },
{ "mod": "mowziesmobs", "classTokens": ["itembluffrod", "itemsandrake", "itemmobremover"], "addFacets": ["utility_tool"] },
{ "mod": "mowziesmobs", "classTokens": ["itemelokosapaw", "itemgrantsunsblessing", "itemcapturedgrottol"], "addFacets": ["magic_artifact"] },
{ "mod": "mowziesmobs", "classTokens": ["itemnagafang"], "addFacets": ["ingredient_organic"] }
```

**ntgl entries (NtglCompat lines 32-38):**

Priority order (power_armor > weapon > attachment):

```json
{ "mod": "ntgl", "classTokens": ["chassisarmor"], "category": "ntgl", "subcategory": "power_armor" },
{ "mod": "ntgl", "classTokens": ["weaponitem"], "addFacets": ["ranged_weapon"] },
{ "mod": "ntgl", "classTokens": ["scopeitem", "stockitem", "gripitem", "magazineitem", "barrelitem", "chassisitem"], "addFacets": ["tech_component", "upgrade"] }
```

Note: `ChassisArmor` must come FIRST (before `ChassisItem` / attachment rule) because `classifyKind()` in the Java code gives power_armor highest priority. Since `chassisarmor` does NOT appear in `chassisitem`, there's no overlap — but ordering is still important for the first-match-wins semantics.

#### SynesthesiaCompatTest deletions (Task 2)

Remove these imports (lines in the import block):
```java
import com.sanhiruzu.ami.compat.CgsCompat;
import com.sanhiruzu.ami.compat.MinecoloniesCompat;
import com.sanhiruzu.ami.compat.MowziesMobsCompat;
import com.sanhiruzu.ami.compat.NtglCompat;
```

Remove these test methods (and their `@Test` annotations):
- `cgsUnknownFamiliesGainWeaponAndAttachmentFacts()` (~line 597)
- `minecoloniesUnknownFamiliesGainColonyToolFacts()` (~line 615)
- `mowziesMobsUnknownFamiliesGainArtifactAndAmmoFacts()` (~line 661)
- `ntglUnknownFamiliesGainWeaponAndAttachmentFacts()` (~line 572)

#### ItemProvider deletions (Task 2)

Remove these 8 lines (4 if-blocks) from ItemProvider.java:
- Lines 454-455: `if (namespaceIs(id, "ntgl")) { ... NtglCompat.enrichItem ... }`
- Lines 457-458: `if (namespaceIs(id, "cgs")) { ... CgsCompat.enrichItem ... }`
- Lines 460-461: `if (namespaceIs(id, "minecolonies")) { ... MinecoloniesCompat.enrichItem ... }`
- Lines 466-467: `if (namespaceIs(id, "mowziesmobs")) { ... MowziesMobsCompat.enrichItem ... }`

#### Equivalence test structure

Each test class follows the same pattern as `BornInChaosOverrideMigrationTest`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CgsOverrideMigrationTest {
    @BeforeEach void installBundled() { ClassificationOverrides.loadBundledDefaults(); }
    @AfterEach void reset() { ClassificationOverrides.clear(); }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> m = new HashMap<>();
        m.put(SearchNodeKeys.MOD_ID, modId);
        m.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return m;
    }
    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }
    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void gatlingItemGainsRangedWeaponFacet() {
        var a = resolveBare("cgs:gatling_pistol",
                meta("cgs", "com.nukateam.cgs.common.foundation.item.GatlingItem"));
        assertTrue(hasFacet(a, ItemFacet.RANGED_WEAPON));
    }

    @Test
    void attachmentItemsGainTechComponentAndUpgradeFacets() {
        var a = resolveBare("cgs:red_dot_scope",
                meta("cgs", "com.nukateam.cgs.common.foundation.item.attachment.ScopeItem"));
        assertTrue(hasFacet(a, ItemFacet.TECH_COMPONENT));
        assertTrue(hasFacet(a, ItemFacet.UPGRADE));
    }

    @Test
    void unmatchedCgsItemsGainNoOverrideFacets() {
        var a = resolveBare("cgs:steel_ingot",
                meta("cgs", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.RANGED_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.TECH_COMPONENT));
    }
}
```

Write equivalent test classes for Ntgl, MowziesMobs, and Minecolonies. Key test cases per plugin:

**NtglOverrideMigrationTest:**
- `weaponItemGainsRangedWeaponFacet()` — class `WeaponItem` → RANGED_WEAPON
- `attachmentItemsGainTechComponentAndUpgradeFacets()` — class `ScopeItem` → TECH_COMPONENT + UPGRADE
- `chassisArmorRoutesToPowerArmorCategory()` — class `ChassisArmor` → category "ntgl", subcategory "power_armor"
- `unmatchedNtglItemsGainNoOverrideFacets()` — class `net.minecraft.world.item.Item` → no facets

**MowziesMobsOverrideMigrationTest:**
- `dartGainsProjectileFacet()` — class `ItemDart` → PROJECTILE
- `toolItemsGainUtilityToolFacet()` — class `ItemBluffRod` → UTILITY_TOOL
- `artifactItemsGainMagicArtifactFacet()` — class `ItemElokosaPaw` → MAGIC_ARTIFACT
- `nagaFangGainsOrganicFacet()` — class `ItemNagaFang` → INGREDIENT_ORGANIC
- `unmatchedMowziesItemsGainNoOverrideFacets()` — class `net.minecraft.world.item.Item` → no facets

**MinecoloniesOverrideMigrationTest:**
- `supplyDeployerRoutesToSettlementsCategory()` — class `ItemSupplyChestDeployer` → category "minecolonies", subcategory "settlements"
- `colonyToolsGainUtilityToolFacet()` — class `ItemScepterGuard` → UTILITY_TOOL
- `fireArrowGainsProjectileFacet()` — class `ItemFireArrow` → PROJECTILE
- `compostGainsOrganicFacet()` — class `ItemCompost` → INGREDIENT_ORGANIC
- `unmatchedMinecoloniesItemsGainNoOverrideFacets()` — class `net.minecraft.world.item.Item` → no facets

- [ ] **Step 1: Add entries to `classification_overrides.json`**

Insert the 16 new modPattern entries (cgs ×2, minecolonies ×7, mowziesmobs ×4, ntgl ×3) into the `modPatterns` array in alphabetical order by mod. The array currently ends with the `born_in_chaos_v1` entries. Insert new entries maintaining alphabetical order: cgs entries should come before born_in_chaos (no — "cgs" > "born_in_chaos_v1" alphabetically), so insert after the born_in_chaos entries in alphabetical mod order: born_in_chaos_v1 → cgs → cnc → minecolonies → mowziesmobs → ntgl.

Wait — the current array has cnc entries first, then born_in_chaos_v1 entries. The alphabetical order is: born_in_chaos_v1 < cgs < cnc < minecolonies < mowziesmobs < ntgl. Reorder the ENTIRE modPatterns array to be alphabetical by mod when writing the new entries. Or, more practically: insert each mod's block in its alphabetical position.

Current order in the file: cnc (entries 1-2), born_in_chaos_v1 (entries 3-11). After this task: born_in_chaos_v1, cgs, cnc, minecolonies, mowziesmobs, ntgl (alphabetical).

- [ ] **Step 2: Write 4 equivalence test classes**

Create:
- `neoforge/src/test/java/com/sanhiruzu/ami/index/CgsOverrideMigrationTest.java`
- `neoforge/src/test/java/com/sanhiruzu/ami/index/NtglOverrideMigrationTest.java`
- `neoforge/src/test/java/com/sanhiruzu/ami/index/MowziesMobsOverrideMigrationTest.java`
- `neoforge/src/test/java/com/sanhiruzu/ami/index/MinecoloniesOverrideMigrationTest.java`

Using the exact test structure shown above.

- [ ] **Step 3: Run tests to verify equivalence tests pass**

```
./gradlew :neoforge:test
```

All new tests must pass before proceeding to deletion.

- [ ] **Step 4: Delete 4 compat Java files**

```
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/CgsCompat.java
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/NtglCompat.java
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/MowziesMobsCompat.java
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/MinecoloniesCompat.java
```

- [ ] **Step 5: Remove 4 ItemProvider hook blocks**

In `ItemProvider.java`, remove:
```java
// Remove these 4 if-blocks (lines ~454-461, ~466-467):
if (namespaceIs(id, "ntgl")) {
    ItemProviderCompatHooks.runCompatSafely("NtglCompat", () -> NtglCompat.enrichItem(id, meta));
}
if (namespaceIs(id, "cgs")) {
    ItemProviderCompatHooks.runCompatSafely("CgsCompat", () -> CgsCompat.enrichItem(id, meta));
}
if (namespaceIs(id, "minecolonies")) {
    ItemProviderCompatHooks.runCompatSafely("MinecoloniesCompat", () -> MinecoloniesCompat.enrichItem(id, meta));
}
if (namespaceIs(id, "mowziesmobs")) {
    ItemProviderCompatHooks.runCompatSafely("MowziesMobsCompat", () -> MowziesMobsCompat.enrichItem(id, meta));
}
```

- [ ] **Step 6: Remove SynesthesiaCompatTest imports and methods**

Remove 4 imports and 4 test methods as listed above.

- [ ] **Step 7: Run tests**

```
./gradlew :neoforge:test
```

All tests must still pass after deletions.

- [ ] **Step 8: Commit**

```
git commit -m "refactor: migrate cgs/ntgl/mowziesmobs/minecolonies compat to override JSON data"
```

---

### Task 3: Migrate HpmCompat, McTradePostCompat, CataclysmCompat

Migrate 3 more compat plugins that use a mix of path tokens and class tokens.

**Files:**
- Modify: `xplat/src/main/resources/assets/ami/classification_overrides.json`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/HpmOverrideMigrationTest.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/McTradePostOverrideMigrationTest.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/CataclysmOverrideMigrationTest.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/HpmCompat.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/McTradePostCompat.java`
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/CataclysmCompat.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java` (remove 6 lines across 3 if-blocks)
- Modify: `neoforge/src/test/java/com/sanhiruzu/ami/index/SynesthesiaCompatTest.java` (remove 3 imports + 3 test methods)

#### JSON entries for Task 3

**cataclysm entries (CataclysmCompat — priority order: ranged > melee > defensive > dungeon_eye > reagent > material > component > curio):**

```json
{ "mod": "cataclysm", "classTokens": ["laser", "shoulder_weapon", "bow"], "addFacets": ["ranged_weapon"], "category": "cataclysm", "subcategory": "weapons" },
{ "mod": "cataclysm", "classTokens": ["sword", "spear", "bardiche", "athame", "incinerator", "annihilator", "immolator"], "addFacets": ["melee_weapon"], "category": "cataclysm", "subcategory": "weapons" },
{ "mod": "cataclysm", "classTokens": ["shield", "targe", "bulwark", "gauntlet"], "addFacets": ["magic_artifact"], "category": "cataclysm", "subcategory": "artifacts" },
{ "mod": "cataclysm", "classTokens": ["dungeoneye"], "addFacets": ["magic_artifact"], "category": "cataclysm", "subcategory": "dungeon_eyes" },
{ "mod": "cataclysm", "pathTokens": ["eye"], "addFacets": ["magic_artifact"], "category": "cataclysm", "subcategory": "dungeon_eyes" },
{ "mod": "cataclysm", "pathTokens": ["lacrima", "void", "ember", "ashes", "horn"], "addFacets": ["magic_reagent"] },
{ "mod": "cataclysm", "pathTokens": ["ingot"], "addFacets": ["ingredient_mineral", "ingot"] },
{ "mod": "cataclysm", "pathTokens": ["nugget"], "addFacets": ["ingredient_mineral", "nugget"] },
{ "mod": "cataclysm", "classTokens": ["iteminventoryonly"], "addFacets": ["tech_component"] },
{ "mod": "cataclysm", "classTokens": ["curiositem"], "addFacets": ["curio"], "category": "cataclysm", "subcategory": "artifacts" }
```

Accepted delta: `cataclysm:witherite_ingot` path="witherite_ingot" → tokens ["witherite", "ingot"]. The "ingot" rule fires and adds INGREDIENT_MINERAL + INGOT facets, which matches the Java behavior. ✓

Accepted delta: class token "bow" is a substring that could theoretically match non-bow class names (e.g., `ElbowPad`). For cataclysm-scoped items, this is acceptable.

**hpm entries (HpmCompat — pure path tokens):**

```json
{ "mod": "hpm", "pathTokens": ["cannonball"], "addFacets": ["projectile"] },
{ "mod": "hpm", "pathTokens": ["mortar"], "addFacets": ["ranged_weapon"] },
{ "mod": "hpm", "pathTokens": ["hull", "mast"], "addFacets": ["transport"] },
{ "mod": "hpm", "pathTokens": ["cutter", "swashbuckler", "corvette", "pirate"], "category": "hpm", "subcategory": "ships" }
```

Accepted delta: `hpm:mortar_ball` (ammo in Java) → path tokens ["mortar", "ball"]. The "mortar" token fires the ranged_weapon rule (not ammo/projectile). This is a minor one-item deviation; the replay gate will confirm it causes no unexpected category change in the Synesthesia dump.

**mctradepost entries (McTradePostCompat — mixed class + path):**

```json
{ "mod": "mctradepost", "classTokens": ["advancedclipboarditem", "currencyexchangeitem", "outpostclaimmarkeritem"], "addFacets": ["utility_tool"] },
{ "mod": "mctradepost", "classTokens": ["souveniritem"], "addFacets": ["utility_misc"] },
{ "mod": "mctradepost", "pathTokens": ["wish"], "addFacets": ["magic_artifact"] },
{ "mod": "mctradepost", "pathTokens": ["napkin", "mortar"], "addFacets": ["ingredient_mineral"] },
{ "mod": "mctradepost", "pathTokens": ["nugget"], "addFacets": ["utility_currency"] }
```

Note: The Java code uses `path.contains("copper_nugget")` for currency. Using pathToken "nugget" is broader (catches any mctradepost item with "nugget" in path), but since the mod is scoped, this is an acceptable delta.

#### SynesthesiaCompatTest deletions (Task 3)

Remove imports:
```java
import com.sanhiruzu.ami.compat.CataclysmCompat;
import com.sanhiruzu.ami.compat.HpmCompat;
import com.sanhiruzu.ami.compat.McTradePostCompat;
```

Remove test methods:
- `cataclysmDungeonEyesAndIngotsRouteSemantically()` (~line 365)
- `hpmUnknownFamiliesGainShipAndAmmoFacts()` (~line 706)
- `mcTradePostUnknownFamiliesGainUtilityAndWishFacts()` (~line 731)

#### ItemProvider deletions (Task 3)

Remove:
```java
if (namespaceIs(id, "cataclysm")) {
    ItemProviderCompatHooks.runCompatSafely("CataclysmCompat", () -> CataclysmCompat.enrichItem(id, meta));
}
if (namespaceIs(id, "hpm")) {
    ItemProviderCompatHooks.runCompatSafely("HpmCompat", () -> HpmCompat.enrichItem(id, meta));
}
if (namespaceIs(id, "mctradepost")) {
    ItemProviderCompatHooks.runCompatSafely("McTradePostCompat", () -> McTradePostCompat.enrichItem(id, meta));
}
```

#### Equivalence test key cases

**HpmOverrideMigrationTest:**
- `cannonballGainsProjectileFacet()` — id `hpm:cannonball`, class `net.minecraft.world.item.Item` → PROJECTILE
- `mortarGainsRangedWeaponFacet()` — id `hpm:mortar` → RANGED_WEAPON
- `hullGainsTransportFacet()` — id `hpm:wooden_hull` → TRANSPORT
- `pirateTokenRoutesToShipsCategory()` — id `hpm:pirate_flag` → category "hpm", subcategory "ships"
- `unmatchedHpmItemsGainNoOverrideFacets()` — id `hpm:compass` → no override facets

**McTradePostOverrideMigrationTest:**
- `clipboardItemGainsUtilityToolFacet()` — class `AdvancedClipboardItem` → UTILITY_TOOL
- `souvenirGainsUtilityMiscFacet()` — class `SouvenirItem` → UTILITY_MISC
- `wishItemGainsMagicArtifactFacet()` — id `mctradepost:wish_scroll` → MAGIC_ARTIFACT
- `nuggetGainsUtilityCurrencyFacet()` — id `mctradepost:copper_nugget` → UTILITY_CURRENCY
- `unmatchedItemsGainNoOverrideFacets()` — id `mctradepost:table`, class `net.minecraft.world.item.Item` → no facets

**CataclysmOverrideMigrationTest:**
- `dungeonEyeClassRoutesToCataclysmDungeonEyes()` — class `DungeonEyeItem` → category "cataclysm", subcategory "dungeon_eyes", has MAGIC_ARTIFACT
- `eyePathRoutesToCataclysmDungeonEyes()` — id `cataclysm:mech_eye` (path token "eye") → same
- `ingotGainsIngotAndMineralFacets()` — id `cataclysm:witherite_ingot` → INGREDIENT_MINERAL + INGOT
- `swordClassGainsMeleeWeaponFacet()` — class `CataclysmSword` → MELEE_WEAPON, category "cataclysm", subcategory "weapons"
- `rangedWeaponHasPriorityOverMelee()` — class name containing "laser" → RANGED_WEAPON (not melee even if it also had "blade")
- `lacrima reagentGainsMagicReagentFacet()` — id `cataclysm:lacrima` → MAGIC_REAGENT
- `unmatchedCataclysmItemsGainNoOverrideFacets()` — id `cataclysm:stone_block`, class `net.minecraft.world.item.Item` → no facets

- [ ] **Step 1: Add entries to `classification_overrides.json`**

Insert cataclysm, hpm, mctradepost entries in alphabetical order within modPatterns (cataclysm before cgs, hpm between cgs and minecolonies, mctradepost between minecolonies and mowziesmobs).

- [ ] **Step 2: Write 3 equivalence test classes**

Using the exact structure above.

- [ ] **Step 3: Run tests to verify equivalence tests pass**

```
./gradlew :neoforge:test
```

- [ ] **Step 4: Delete 3 compat Java files**

```
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/CataclysmCompat.java
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/HpmCompat.java
git rm xplat/src/main/java/com/sanhiruzu/ami/compat/McTradePostCompat.java
```

- [ ] **Step 5: Remove 3 ItemProvider hook blocks and 3 SynesthesiaCompatTest methods**

As specified above.

- [ ] **Step 6: Run tests**

```
./gradlew :neoforge:test
```

- [ ] **Step 7: Commit**

```
git commit -m "refactor: migrate hpm/mctradepost/cataclysm compat to override JSON data"
```
