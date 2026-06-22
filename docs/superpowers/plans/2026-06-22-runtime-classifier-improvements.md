# Runtime Classifier Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve AMI's live item classifier for every user — strengthen creative-tab evidence, add `minecraft:enchantable/*` tag evidence, and always record runner-up candidates — so more items classify correctly with zero per-pack curation.

**Architecture:** All three changes feed the existing evidence-scoring pipeline (`EvidenceCollector` → `CategoryScorer` → `PrimaryCategoryResolver`). No new data formats, no runtime recipe processing. Each change is additive evidence or extra trace metadata; the resolver's gate order is unchanged.

**Tech Stack:** Java 21, Minecraft 1.21.1 / NeoForge, JUnit 5. Classifier code lives in `xplat`; tests live in the `neoforge` module but exercise pure-`xplat` classes.

## Global Constraints

- Classifier code is pure xplat — no platform/client/server dependencies in `EvidenceCollector`, `CategoryScorer`, `PrimaryCategoryResolver`, `SearchNodeKeys`.
- Tests go under `neoforge/src/test/java/com/sanhiruzu/ami/index/`; construct ids with `new ResourceLocation("namespace:path")` and profiles with `new FacetProfile(EnumSet, Map)` (match existing `PrimaryCategoryResolverTest`).
- Run tests with: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.<ClassName>"`.
- Commit messages: plain, imperative, no AI/assistant attribution of any kind.
- Evidence weights: existing scale is ~35–130; `MIN_FALLBACK_SCORE=40`, `MIN_STRONG_SCORE=80` (`CategoryScorer.java:8-9`). Keep new weights inside this scale.
- Do not reorder the resolver gates in `PrimaryCategoryResolver.resolve` (`PrimaryCategoryResolver.java:532-565`).

## Out of Scope (deferred to later plans)

- Attribute-modifier evidence (attack_damage → weapon): deferred — tools also carry attack-damage modifiers, so it needs magnitude thresholds and a full regression sweep. Its own plan.
- The override layer and offline curation pipeline: separate plans (Plan 2, Plan 3).

---

### Task 1: Strengthen creative-tab evidence

The author's creative-tab grouping is the strongest prior for overloaded names, but `addCreativeTabEvidence` currently omits several high-value tab keywords (combat/weapons, armor/clothing, redstone) and weights some signals below `MIN_FALLBACK_SCORE`. Add the missing mappings and raise the floor so a clear tab signal can route an otherwise-featureless item.

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/EvidenceCollector.java:249-286` (the `addCreativeTabEvidence` method)
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/CreativeTabEvidenceTest.java` (create)

**Interfaces:**
- Consumes: `PrimaryCategoryResolver.resolve(ResourceLocation, FacetProfile) : CategoryAssignment`; `SearchNodeKeys.CREATIVE_TAB_LABEL` (`"creativeTabLabel"`).
- Produces: no new public symbols; only additional `ClassificationEvidence` rows from existing private `e(...)`.

- [ ] **Step 1: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/CreativeTabEvidenceTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreativeTabEvidenceTest {

    @Test
    void combatTabRoutesFeaturelessItemToTools() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:gizmo"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Combat")));

        assertEquals("tools", assignment.categoryId());
    }

    @Test
    void armorTabRoutesFeaturelessItemToArmor() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:widget"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Armor & Clothing")));

        assertEquals("armor", assignment.categoryId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.CreativeTabEvidenceTest"`
Expected: FAIL — both items resolve to `misc`/fallback because "combat" and "armor" tab keywords are not mapped (assertion mismatch, e.g. expected `tools` but was `misc`).

- [ ] **Step 3: Add the missing tab mappings**

In `EvidenceCollector.java`, inside `addCreativeTabEvidence` (after the existing `ingredients` block at line 285, before the closing brace at line 286), add:

```java
        if (containsAny(combined, "combat", "weapon", "weapons", "warfare")) {
            evidence.add(e("creative_tab.combat", "creative_tab", "tools", "melee", 50, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "armor", "armour", "clothing", "apparel", "wearable")) {
            evidence.add(e("creative_tab.armor", "creative_tab", "armor", "", 50, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "redstone")) {
            evidence.add(e("creative_tab.redstone", "creative_tab", "tech", "redstone", 50, "creative tab=" + tabLabel));
        }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.CreativeTabEvidenceTest"`
Expected: PASS (both tests).

- [ ] **Step 5: Run the existing classification suite to confirm no regressions**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.PrimaryCategoryResolverTest" --tests "com.sanhiruzu.ami.index.CategoryScorerTest"`
Expected: PASS (unchanged — these items have facets/identities that win before tab fallback).

- [ ] **Step 6: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/EvidenceCollector.java neoforge/src/test/java/com/sanhiruzu/ami/index/CreativeTabEvidenceTest.java
git commit -m "feat: add combat/armor/redstone creative-tab classification evidence"
```

---

### Task 2: Add `minecraft:enchantable/*` tag evidence

Vanilla 1.21 functional tags declare an item's role directly. Map the unambiguous ones to weapon/tool/armor evidence in `addTrustedTagEvidence` (the testable, attribute-driven evidence path that mirrors the existing trusted-tag handling).

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/EvidenceCollector.java:288-336` (the `addTrustedTagEvidence` method)
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/EnchantableTagEvidenceTest.java` (create)

**Interfaces:**
- Consumes: `SearchNodeKeys.TAGS` (`"tags"`); existing private `hasTrustedTag(String tags, String tag)` and `e(...)` in `EvidenceCollector`.
- Produces: no new public symbols.

- [ ] **Step 1: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/EnchantableTagEvidenceTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantableTagEvidenceTest {

    @Test
    void enchantableSharpWeaponRoutesToToolsMelee() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:mystery_edge"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.TAGS, "minecraft:enchantable/sharp_weapon")));

        assertEquals("tools", assignment.categoryId());
        assertEquals("melee", assignment.subcategoryId());
    }

    @Test
    void enchantableMiningRoutesToToolsHarvest() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:mystery_digger"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.TAGS, "minecraft:enchantable/mining")));

        assertEquals("tools", assignment.categoryId());
        assertEquals("harvest", assignment.subcategoryId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.EnchantableTagEvidenceTest"`
Expected: FAIL — no enchantable-tag handling exists; items fall back to `misc`.

- [ ] **Step 3: Add enchantable-tag evidence**

In `EvidenceCollector.java`, inside `addTrustedTagEvidence` (after the `power_bottles` block at line 335, before the closing brace at line 336), add:

```java
        if (hasTrustedTag(tags, "minecraft:enchantable/sharp_weapon")
                || hasTrustedTag(tags, "minecraft:enchantable/mace")
                || hasTrustedTag(tags, "minecraft:enchantable/trident")) {
            evidence.add(e("tag.enchantable_melee", "trusted_tag", "tools", "melee", 85, "enchantable melee-weapon tag"));
        }
        if (hasTrustedTag(tags, "minecraft:enchantable/bow")
                || hasTrustedTag(tags, "minecraft:enchantable/crossbow")) {
            evidence.add(e("tag.enchantable_ranged", "trusted_tag", "tools", "ranged", 85, "enchantable ranged-weapon tag"));
        }
        if (hasTrustedTag(tags, "minecraft:enchantable/mining")
                || hasTrustedTag(tags, "minecraft:enchantable/mining_loot")) {
            evidence.add(e("tag.enchantable_mining", "trusted_tag", "tools", "harvest", 80, "enchantable mining tag"));
        }
        if (hasTrustedTag(tags, "minecraft:enchantable/armor")) {
            evidence.add(e("tag.enchantable_armor", "trusted_tag", "armor", "", 80, "enchantable armor tag"));
        }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.EnchantableTagEvidenceTest"`
Expected: PASS (both tests).

- [ ] **Step 5: Run the existing classification suite to confirm no regressions**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.PrimaryCategoryResolverTest" --tests "com.sanhiruzu.ami.index.FacetIndexerTest"`
Expected: PASS (unchanged).

- [ ] **Step 6: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/EvidenceCollector.java neoforge/src/test/java/com/sanhiruzu/ami/index/EnchantableTagEvidenceTest.java
git commit -m "feat: classify weapons/tools/armor from minecraft enchantable tags"
```

---

### Task 3: Always record runner-up candidates in classification metadata

Today `classificationScores`/`classificationEvidence` are attached only when `CategoryScorer` decides (`CategoryScorer.java:46-49`). When a hard identity or primary rule wins, the competing scorer signal is lost — so the offline detector (Plan 3) can't see cross-signal contradictions. Expose a reusable candidate summary from `CategoryScorer` and attach it on every resolve path.

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/SearchNodeKeys.java` (add one constant)
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/CategoryScorer.java` (add `candidateSummary`, reuse `scoreSummary`)
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java:528-530` (attach summary into the shared attributes map)
- Test: `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationCandidateTraceTest.java` (create)

**Interfaces:**
- Produces:
  - `SearchNodeKeys.CLASSIFICATION_CANDIDATES` — `public static final String = "classificationCandidates"`.
  - `CategoryScorer.candidateSummary(ResourceLocation id, FacetProfile profile) : String` — package-private; returns the top scored `category/subcategory=total` pairs (max 6, `"; "`-joined), or `""` when there is no evidence.
- Consumes: existing `EvidenceCollector.collect`, the private `Score` class, and `scoreSummary(Iterable<Score>)`.

- [ ] **Step 1: Write the failing test**

Create `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationCandidateTraceTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationCandidateTraceTest {

    @Test
    void candidatesRecordedEvenWhenIdentityOrRuleWins() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:cake"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.PLACEABLE, ItemFacet.PLACEABLE_FOOD), Map.of()));

        String candidates = assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_CANDIDATES);
        assertNotNull(candidates);
        assertTrue(candidates.contains("nature/"), "expected a nature/* candidate, got: " + candidates);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationCandidateTraceTest"`
Expected: FAIL — `SearchNodeKeys.CLASSIFICATION_CANDIDATES` does not compile / is absent.

- [ ] **Step 3: Add the metadata key**

In `SearchNodeKeys.java`, add alongside the other classification keys (near `CLASSIFICATION_ROUTE_RULE` at line 18):

```java
    public static final String CLASSIFICATION_CANDIDATES = "classificationCandidates";
```

- [ ] **Step 4: Add `candidateSummary` to `CategoryScorer`**

In `CategoryScorer.java`, add this method after `resolve(...)` (after line 51). It rebuilds the score map exactly like `resolve` and reuses the existing `scoreSummary` helper:

```java
    static String candidateSummary(ResourceLocation id, FacetProfile profile) {
        List<ClassificationEvidence> evidence = EvidenceCollector.collect(id, profile);
        if (evidence.isEmpty()) {
            return "";
        }
        Map<String, Score> scores = new LinkedHashMap<>();
        for (ClassificationEvidence item : evidence) {
            scores.computeIfAbsent(item.categoryKey(), ignored -> new Score(item.categoryId(), item.subcategoryId()))
                    .add(item);
        }
        return scoreSummary(scores.values());
    }
```

- [ ] **Step 5: Attach the summary in `PrimaryCategoryResolver`**

In `PrimaryCategoryResolver.resolve` (`PrimaryCategoryResolver.java`), immediately after `routedProfile` is built (line 528) and before `ResolveContext` is constructed (line 529), insert:

```java
        String candidateSummary = CategoryScorer.candidateSummary(id, routedProfile);
        if (!candidateSummary.isBlank()) {
            attributes.put(SearchNodeKeys.CLASSIFICATION_CANDIDATES, candidateSummary);
        }
```

This writes into the shared `attributes` map that `ResolveContext`, every `assignment(...)`/`identityAssignment(...)` helper, and the scorer's `routedProfile` all reference — so the key flows through whichever gate wins and survives `CategoryRouteTrace.finish` (which copies `assignment.attributes()`).

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationCandidateTraceTest"`
Expected: PASS — cake wins via a primary rule but still carries `classificationCandidates` containing a `nature/*` entry.

- [ ] **Step 7: Run the existing classification suite to confirm no regressions**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.PrimaryCategoryResolverTest" --tests "com.sanhiruzu.ami.index.CategoryScorerTest" --tests "com.sanhiruzu.ami.index.ClassificationArchitectureGuardrailTest"`
Expected: PASS (additive metadata only; category/subcategory outcomes unchanged).

- [ ] **Step 8: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/SearchNodeKeys.java xplat/src/main/java/com/sanhiruzu/ami/index/CategoryScorer.java xplat/src/main/java/com/sanhiruzu/ami/index/PrimaryCategoryResolver.java neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationCandidateTraceTest.java
git commit -m "feat: always record runner-up classification candidates in metadata"
```

---

## Final verification

- [ ] **Run the full index test package**

Run: `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.*"`
Expected: PASS — all new tests plus the existing classification suite are green.

## Self-Review notes

- **Spec coverage:** This plan implements the runtime half of Component 1 from the spec (tab weighting, enchantable tags, richer decision trace). Attribute-modifier evidence is intentionally deferred (see Out of Scope). The override layer (Component 2) and offline pipeline (Components 3–4) are Plans 2 and 3.
- **Type consistency:** `candidateSummary` returns `String`; `CLASSIFICATION_CANDIDATES` is a `String` key; both used consistently in Task 3. New evidence rows reuse the existing `e(String,String,String,String,int,String)` factory and `hasTrustedTag(String,String)` helper — no undefined references.
- **Placeholder scan:** none — every step shows concrete code and an exact command.
