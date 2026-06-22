# BornInChaos Classification Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `BornInChaosCompat`'s path-token facet rules into bundled override JSON data, prove equivalence, then delete the Java plugin — following the same loop established by the Cnc pilot.

**Architecture:** Add ten mod-scoped `modPattern` entries to `classification_overrides.json` covering every path-based facet rule in `BornInChaosCompat`. Write a deterministic JUnit equivalence test that proves the override data reproduces the plugin's output for all path-based items, then delete the plugin and its `ItemProvider` hook. Class-based rules (sword/armor/drink detection by Java class name) are dropped as an accepted delta — those items get their facets from core evidence (weapon tags, `ArmorItem instanceof`, `FoodProperties`) in production.

**Tech Stack:** Java 21, JUnit 5 (NeoForge test source set), Gson, NeoForge. The override consumer (`ModPatternRule`, `ClassificationOverrides`, `PrimaryCategoryResolver`) already supports facet-only `modPatterns` from the Cnc pilot — no schema changes needed.

## Global Constraints

- No AI/assistant attribution in commit messages (no `Co-Authored-By`, no generated-by lines).
- Commit via PowerShell `git -C 'C:\WorkDir\AutomatedMaterialsIndex' ...` — Bash git-guard false-positives on the worktree path.
- The override JSON lives at `xplat/src/main/resources/assets/ami/classification_overrides.json`. The object structure is `{ "items": { "<id>": { ... } }, "modPatterns": [ ... ] }`. Patterns are matched in array order; `patternFor()` returns the first match for a mod — order is priority.
- `pathTokens` are single `[_/]`-split path components (no `_` or `/` inside a token). Patterns are mod-scoped — a `born_in_chaos_v1` pattern only fires for `born_in_chaos_v1:*` items.
- ItemFacet id strings (lowercase, underscore-separated): `ingredient_organic`, `magic_artifact`, `ingredient_mineral`, `ingot`, `nugget`, `projectile`, `utility_misc`, `magic_reagent`, `utility_tool`.
- Test class lives in `neoforge/src/test/java/com/sanhiruzu/ami/index/` (matches all existing equivalence tests).
- `@BeforeEach` calls `ClassificationOverrides.loadBundledDefaults()`; `@AfterEach` calls `ClassificationOverrides.clear()`.
- `resolveBare(id, meta)` helper: `PrimaryCategoryResolver.resolve(new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta)`.
- `hasFacet(a, facet)` helper: `a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id())`.
- Drop private meta keys (`bornInChaosItemKind`, `bornInChaosFacts`) and search tokens (`born_in_chaos_*`) as accepted deltas — no production consumer reads them.
- Run tests with `./gradlew :neoforge:test` from the repo root.

---

### Task 1: Author born_in_chaos override data

Append ten `modPattern` entries for `born_in_chaos_v1` to the bundled override JSON. The patterns reproduce every path-token rule in `BornInChaosCompat.addFacts()` + `applyKindMetadata()`. Patterns are ordered by BornInChaosCompat's kind priority (projectiles → structure_placers → artifacts → reagents → mineral ingots → mineral nuggets → mineral other → organic → utility), so the first-match semantics preserve the original priority.

**Files:**
- Modify: `xplat/src/main/resources/assets/ami/classification_overrides.json`

**Interfaces:**
- Consumes: existing Cnc patterns already in the file; the parser/consumer from the Cnc pilot.
- Produces: ten new `modPatterns` entries for Task 2's equivalence test.

**Mapping from BornInChaosCompat source to JSON tokens:**

| `addFacts` check | Token(s) | Facet(s) | Notes |
|---|---|---|---|
| `containsAny(path, "bomb", "dark_charge")` | `bomb`, `charge` | `projectile` | `dark_charge` splits to `dark`+`charge`; `charge` is specific in this mod |
| `containsAny(path, "spawn_structure", "spawn_structures")` | `structure` | `utility_misc` | both substrings contain `structure` token; mod-scoped safe |
| `containsAny(path, "charmof", "charm_", "totem", "icon", "orb")` | `charmof`, `charm`, `totem`, `icon`, `orb` | `magic_artifact` | `charmof` is one word (no `_`); `charm_` splits to `charm` |
| `containsAny(path, "dust", "spirit", "soul", "seedof_chaos")` | `dust`, `spirit`, `soul`, `seedof` | `magic_reagent` | `seedof_chaos` splits to `seedof`+`chaos`; `seedof` is uniquely specific |
| path `contains("ingot")` inside materials case | `ingot` | `ingredient_mineral`, `ingot` | listed before `metal`/`plate` pattern to take priority for `dark_metal_ingot` |
| path `contains("nugget")` inside materials case | `nugget` | `ingredient_mineral`, `nugget` | |
| `containsAny(path, "dark_metal", "armor_plate")` | `metal`, `plate` | `ingredient_mineral` | `dark_metal` → `metal`; `armor_plate` → `plate` |
| `containsAny(path, "claw", "skin", "flesh", "stomach", "fang", "bone", "horn")` | `claw`, `skin`, `flesh`, `stomach`, `fang`, `bone`, `horn` | `ingredient_organic` | |
| `containsAny(path, "bag", "gift", "evilometer")` | `bag`, `gift`, `evilometer` | `utility_tool` | |

Class-based rules (`Sword`/`ArmorItem`/`Elixir` → `MELEE_WEAPON`/`EQUIPPABLE`/`EDIBLE+FOOD_DRINK`) are **not expressed** in JSON — those items get those facets from core evidence (weapon tags, `instanceof ArmorItem`, `FoodProperties`) in production. This is a documented accepted delta.

- [ ] **Step 1: Update the override JSON**

The file currently has Cnc data. Append the ten born_in_chaos patterns after the cnc patterns, preserving existing content exactly:

```json
{
  "items": {
    "cnc:raw_turkey": { "addFacets": ["ingredient_organic"] }
  },
  "modPatterns": [
    { "mod": "cnc", "pathTokens": ["potofmouse", "kill"], "addFacets": ["magic_artifact"] },
    { "mod": "cnc", "pathTokens": ["buckskin", "antler", "tusk", "wishbone"], "addFacets": ["ingredient_organic"] },

    { "mod": "born_in_chaos_v1", "pathTokens": ["bomb", "charge"], "addFacets": ["projectile"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["structure"], "addFacets": ["utility_misc"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["charmof", "charm", "totem", "icon", "orb"], "addFacets": ["magic_artifact"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["dust", "spirit", "soul", "seedof"], "addFacets": ["magic_reagent"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["ingot"], "addFacets": ["ingredient_mineral", "ingot"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["nugget"], "addFacets": ["ingredient_mineral", "nugget"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["metal", "plate"], "addFacets": ["ingredient_mineral"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["claw", "skin", "flesh", "stomach", "fang", "bone", "horn"], "addFacets": ["ingredient_organic"] },
    { "mod": "born_in_chaos_v1", "pathTokens": ["bag", "gift", "evilometer"], "addFacets": ["utility_tool"] }
  ]
}
```

- [ ] **Step 2: Run the existing parse test to confirm the JSON is valid**

```
./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesParseTest" -i
```

Expected: `BUILD SUCCESSFUL`. If the JSON is malformed, the parse test's `blankOrMalformedJsonInstallsEmpty` branch will catch it.

- [ ] **Step 3: Commit**

```powershell
git -C 'C:\WorkDir\AutomatedMaterialsIndex' add xplat/src/main/resources/assets/ami/classification_overrides.json
git -C 'C:\WorkDir\AutomatedMaterialsIndex' commit -m "feat: bundle born_in_chaos path-token classification override data"
```

---

### Task 2: Write the BornInChaos equivalence test

Create `BornInChaosOverrideMigrationTest` proving the bundled override data reproduces `BornInChaosCompat`'s facet tagging for all path-based items, without referencing `BornInChaosCompat`. The test is designed to survive deletion of `BornInChaosCompat` — it is the durable equivalence proof.

**Files:**
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/index/BornInChaosOverrideMigrationTest.java`

**Interfaces:**
- Consumes: Task 1's override JSON (loaded via `ClassificationOverrides.loadBundledDefaults()`).
- Produces: equivalence proof that Task 3's deletion is safe.

**Design note:** While `BornInChaosCompat` still exists, `SynesthesiaCompatTest.bornInChaosMaterialsAndCharmsRouteSemantically` and `bornInChaosUnknownFamiliesExtendExistingCompatFacts` pin the plugin's category output. This new test pins the same categories/facets using only the override mechanism — two green tests on the same literals = equivalence locked. After deletion in Task 3, only this test remains as the durable proof.

**Tests to cover:**

| Item | Class hint | Asserts |
|---|---|---|
| `born_in_chaos_v1:dark_metal_ingot` | `DarkMetalIngotItem` | `hasFacet(INGREDIENT_MINERAL)`, `hasFacet(INGOT)`, `categoryId() == "ingredients"`, `subcategoryId() == "mineral"` |
| `born_in_chaos_v1:charmof_power` | `CharmofPowerItem` | `hasFacet(MAGIC_ARTIFACT)`, `categoryId() == "magic"`, `subcategoryId() == "artifacts"` |
| `born_in_chaos_v1:krampuss_bag` | `KrampussBagItem` | `hasFacet(UTILITY_TOOL)`, `categoryId() == "tools"`, `subcategoryId() == "utility"` |
| `born_in_chaos_v1:evilometer` | `EvilometerItem` | `hasFacet(UTILITY_TOOL)` |
| `born_in_chaos_v1:zombie_claw` | `Item` (generic) | `hasFacet(INGREDIENT_ORGANIC)` |
| `born_in_chaos_v1:chaos_dust` | `Item` (generic) | `hasFacet(MAGIC_REAGENT)` |
| `born_in_chaos_v1:dark_charge` | `Item` (generic) | `hasFacet(PROJECTILE)` |
| `born_in_chaos_v1:unknown_gem` | `Item` (generic) | `!hasFacet(INGREDIENT_ORGANIC)`, `!hasFacet(MAGIC_ARTIFACT)`, `!hasFacet(MAGIC_REAGENT)` — no pattern matches |

- [ ] **Step 1: Write the test class**

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves bundled born_in_chaos override data reproduces BornInChaosCompat's path-based facet
 * tagging WITHOUT referencing BornInChaosCompat -- survives the plugin's deletion.
 *
 * Class-based rules (Sword/ArmorItem/Elixir → MELEE_WEAPON/EQUIPPABLE/EDIBLE) are not
 * expressed in JSON; those items get those facets from core evidence in production.
 */
class BornInChaosOverrideMigrationTest {

    @BeforeEach
    void installBundled() {
        ClassificationOverrides.loadBundledDefaults();
    }

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> m = new HashMap<>();
        m.put(SearchNodeKeys.MOD_ID, modId);
        m.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return m;
    }

    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void mineralIngotGainsIngotAndMineralFacets() {
        // dark_metal_ingot -> tokens [dark, metal, ingot]; "ingot" pattern fires -> INGREDIENT_MINERAL + INGOT
        CategoryAssignment a = resolveBare("born_in_chaos_v1:dark_metal_ingot",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.DarkMetalIngotItem"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
        assertTrue(hasFacet(a, ItemFacet.INGOT));
        assertEquals("ingredients", a.categoryId());
        assertEquals("mineral", a.subcategoryId());
    }

    @Test
    void charmItemGainsMagicArtifactFacet() {
        // charmof_power -> tokens [charmof, power]; "charmof" pattern fires -> MAGIC_ARTIFACT
        CategoryAssignment a = resolveBare("born_in_chaos_v1:charmof_power",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.CharmofPowerItem"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertEquals("magic", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
    }

    @Test
    void utilityToolItemsGainUtilityToolFacet() {
        // krampuss_bag -> tokens [krampuss, bag]; "bag" pattern fires -> UTILITY_TOOL
        CategoryAssignment bag = resolveBare("born_in_chaos_v1:krampuss_bag",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.KrampussBagItem"));
        assertTrue(hasFacet(bag, ItemFacet.UTILITY_TOOL));
        assertEquals("tools", bag.categoryId());
        assertEquals("utility", bag.subcategoryId());

        // evilometer -> single token; "evilometer" pattern fires -> UTILITY_TOOL
        CategoryAssignment meter = resolveBare("born_in_chaos_v1:evilometer",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.EvilometerItem"));
        assertTrue(hasFacet(meter, ItemFacet.UTILITY_TOOL));
        assertEquals("tools", meter.categoryId());
        assertEquals("utility", meter.subcategoryId());
    }

    @Test
    void organicDropItemsGainOrganicFacet() {
        // zombie_claw -> tokens [zombie, claw]; "claw" pattern fires -> INGREDIENT_ORGANIC
        CategoryAssignment a = resolveBare("born_in_chaos_v1:zombie_claw",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void reagentItemsGainMagicReagentFacet() {
        // chaos_dust -> tokens [chaos, dust]; "dust" pattern fires -> MAGIC_REAGENT
        CategoryAssignment a = resolveBare("born_in_chaos_v1:chaos_dust",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void projectileItemsGainProjectileFacet() {
        // dark_charge -> tokens [dark, charge]; "charge" pattern fires -> PROJECTILE
        CategoryAssignment a = resolveBare("born_in_chaos_v1:dark_charge",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
    }

    @Test
    void unmatchedItemsGainNoOverrideFacets() {
        // unknown_gem -> tokens [unknown, gem]; no born_in_chaos pattern matches
        CategoryAssignment a = resolveBare("born_in_chaos_v1:unknown_gem",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
    }
}
```

- [ ] **Step 2: Run the test to confirm it passes**

```
./gradlew :neoforge:test --tests "com.sanhiruzu.ami.index.BornInChaosOverrideMigrationTest" -i
```

Expected: `BUILD SUCCESSFUL`, all 7 tests pass.

If any test fails, the JSON token mapping is wrong. Read the failure carefully: the message will show which facet was missing. Check the token mapping table in Task 1 and adjust the JSON entry for that group, then re-run.

- [ ] **Step 3: Commit**

```powershell
git -C 'C:\WorkDir\AutomatedMaterialsIndex' add neoforge/src/test/java/com/sanhiruzu/ami/index/BornInChaosOverrideMigrationTest.java
git -C 'C:\WorkDir\AutomatedMaterialsIndex' commit -m "test: add BornInChaosOverrideMigrationTest proving override equivalence"
```

---

### Task 3: Delete BornInChaosCompat

Remove the Java plugin file, its three-line `ItemProvider` hook, and the two `SynesthesiaCompatTest` test methods that directly call `BornInChaosCompat.enrichItem()`. The equivalence test added in Task 2 survives as the durable proof.

**Files:**
- Delete: `xplat/src/main/java/com/sanhiruzu/ami/compat/BornInChaosCompat.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`
- Modify: `neoforge/src/test/java/com/sanhiruzu/ami/index/SynesthesiaCompatTest.java`

**Interfaces:**
- Consumes: Task 2's equivalence test (must be green before deletion).
- Produces: a cleaner codebase where all born_in_chaos classification is data-driven.

**Verification before deleting:** Confirm `BornInChaosCompat` is not referenced anywhere except `ItemProvider` (line 425) and `SynesthesiaCompatTest`. Run:

```powershell
Select-String -Path "C:\WorkDir\AutomatedMaterialsIndex\xplat\src\main\java\**\*.java","C:\WorkDir\AutomatedMaterialsIndex\neoforge\src\**\*.java" -Pattern "BornInChaosCompat" -Recurse | Select-Object Filename, LineNumber, Line
```

Expected: exactly 3 hits — `ItemProvider.java:425`, `SynesthesiaCompatTest.java` (import + 2 usages).

- [ ] **Step 1: Remove the ItemProvider hook**

In `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`, delete these three lines (currently around line 424):

```java
        if (namespaceIs(id, "born_in_chaos_v1")) {
            ItemProviderCompatHooks.runCompatSafely("BornInChaosCompat", () -> BornInChaosCompat.enrichItem(id, meta));
        }
```

The file uses a wildcard import `com.sanhiruzu.ami.compat.*` so no import line needs removing.

- [ ] **Step 2: Remove the SynesthesiaCompatTest born_in_chaos test methods**

In `neoforge/src/test/java/com/sanhiruzu/ami/index/SynesthesiaCompatTest.java`:

1. Remove the import line:
   ```java
   import com.sanhiruzu.ami.compat.BornInChaosCompat;
   ```

2. Remove the entire `bornInChaosMaterialsAndCharmsRouteSemantically` test method (the one testing `dark_metal_ingot` and `charmof_power`, asserts `bornInChaosItemKind`).

3. Remove the entire `bornInChaosUnknownFamiliesExtendExistingCompatFacts` test method (the one testing `krampuss_bag` and `evilometer`, asserts `bornInChaosItemKind`).

- [ ] **Step 3: Delete BornInChaosCompat.java**

```powershell
Remove-Item "C:\WorkDir\AutomatedMaterialsIndex\xplat\src\main\java\com\sanhiruzu\ami\compat\BornInChaosCompat.java"
```

- [ ] **Step 4: Run the full test suite**

```
./gradlew :neoforge:test -i
```

Expected: `BUILD SUCCESSFUL`. All tests pass. `BornInChaosOverrideMigrationTest` proves the override works; `SynesthesiaCompatTest` no longer references the deleted class.

- [ ] **Step 5: Commit**

```powershell
git -C 'C:\WorkDir\AutomatedMaterialsIndex' add -u
git -C 'C:\WorkDir\AutomatedMaterialsIndex' commit -m "refactor: delete BornInChaosCompat; born_in_chaos classification now data-driven"
```
