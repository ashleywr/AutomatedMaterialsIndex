# AMI Recipe View JEI Parity Geometry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tighten AMI's shared fallback recipe geometry so slots, arrow, and output read more like a JEI-aligned recipe unit, then apply a small generic-layout placement nudge in the screen without changing recipe behavior.

**Architecture:** Keep `RecipeDisplayHelper` as the source of truth for generic fallback geometry by extracting the row/grid fallback math into one small helper that both production code and tests can read directly. Keep `RecipeViewerScreen` behavior unchanged, but replace its repeated hard-coded generic layout anchor with one package-private placement helper so render-time and hit-test-time math stay in sync.

**Tech Stack:** Java 21, NeoForge JUnit 5 tests, shared `xplat` UI/layout code, Gradle.

---

## Global Constraints

- Do not change any click, hover, transfer, or workstation behavior in `RecipeViewerScreen`.
- Preserve the current 3x3 crafting placeholder behavior and its existing tests.
- This pass is only for shared fallback geometry and a small generic-layout placement nudge.
- Leave textured/dedicated background layouts on the legacy horizontal anchor.
- Do not touch `docs/feature-map.md` in this pass; the current worktree already has unrelated local edits there.
- Use focused JVM tests before touching production code, then compile all shared loaders after the changes.
- Commit messages must not include AI attribution.

## File Structure

- `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`
  Owns shared fallback slot, arrow, and output geometry. This plan adds one small package-private helper record plus one factory method so generic fallback math is testable directly.
- `neoforge/src/test/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelperGenericFallbackLayoutTest.java`
  New focused test file for the shared fallback row/grid geometry. Keeps `RecipeDisplayHelperLayoutTest` dedicated to crafting-placeholder parity.
- `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacement.java`
  New package-private helper for generic-layout horizontal placement inside a recipe card.
- `neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacementTest.java`
  New focused test file for the placement helper.
- `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`
  Replace all repeated `cardX + 24` layout anchors with the shared helper so render, hover, scroll, and click hit-testing stay aligned.

### Task 1: Extract and verify shared fallback geometry in `RecipeDisplayHelper`

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelperGenericFallbackLayoutTest.java`

**Interfaces:**
- Produces: `RecipeDisplayHelper.GenericFallbackLayout`
- Produces: `RecipeDisplayHelper.createGenericFallbackLayout(List<List<ItemStack>> ingredientAlternatives)`

- [ ] **Step 1: Write the failing fallback-geometry test**

```java
// neoforge/src/test/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelperGenericFallbackLayoutTest.java
package com.sanhiruzu.ami.client.recipe;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.GenericFallbackLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeDisplayHelperGenericFallbackLayoutTest {

    @Test
    void rowLayoutUsesSharedTighterArrowAndOutputGaps() {
        GenericFallbackLayout layout = RecipeDisplayHelper.createGenericFallbackLayout(List.of(
                List.of(new ItemStack(Items.APPLE)),
                List.of(new ItemStack(Items.CAKE)),
                List.of(new ItemStack(Items.CHEST))
        ));

        assertEquals(3, layout.gridWidth());
        assertEquals(1, layout.gridHeight());
        assertSlot(layout.inputs().get(0), 0, 0);
        assertSlot(layout.inputs().get(1), 18, 0);
        assertSlot(layout.inputs().get(2), 36, 0);
        assertEquals(58, layout.arrowX());
        assertEquals(1, layout.arrowY());
        assertEquals(84, layout.outputX());
        assertEquals(0, layout.outputY());
    }

    @Test
    void twoByTwoGridLayoutCentersArrowAndOutputAgainstGridFootprint() {
        GenericFallbackLayout layout = RecipeDisplayHelper.createGenericFallbackLayout(List.of(
                List.of(new ItemStack(Items.APPLE)),
                List.of(new ItemStack(Items.CAKE)),
                List.of(new ItemStack(Items.CHEST)),
                List.of(new ItemStack(Items.REDSTONE))
        ));

        assertEquals(2, layout.gridWidth());
        assertEquals(2, layout.gridHeight());
        assertSlot(layout.inputs().get(0), 0, 0);
        assertSlot(layout.inputs().get(1), 18, 0);
        assertSlot(layout.inputs().get(2), 0, 18);
        assertSlot(layout.inputs().get(3), 18, 18);
        assertEquals(40, layout.arrowX());
        assertEquals(10, layout.arrowY());
        assertEquals(66, layout.outputX());
        assertEquals(9, layout.outputY());
    }

    @Test
    void singleInputLayoutUsesTheSameSharedSpacingModel() {
        GenericFallbackLayout layout = RecipeDisplayHelper.createGenericFallbackLayout(List.of(
                List.of(new ItemStack(Items.APPLE))
        ));

        assertEquals(1, layout.gridWidth());
        assertEquals(1, layout.gridHeight());
        assertSlot(layout.inputs().get(0), 0, 0);
        assertEquals(22, layout.arrowX());
        assertEquals(1, layout.arrowY());
        assertEquals(48, layout.outputX());
        assertEquals(0, layout.outputY());
    }

    private static void assertSlot(SlotPosition slot, int x, int y) {
        assertEquals(x, slot.x());
        assertEquals(y, slot.y());
    }
}
```

- [ ] **Step 2: Run the new test to verify it fails**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperGenericFallbackLayoutTest"
```

Expected: compile failure because `GenericFallbackLayout` and `createGenericFallbackLayout(...)` do not exist yet.

- [ ] **Step 3: Add the minimal shared helper to `RecipeDisplayHelper`**

```java
// add near the generic fallback branch in xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java
static GenericFallbackLayout createGenericFallbackLayout(List<List<ItemStack>> ingredientAlternatives) {
    List<List<ItemStack>> nonEmpty = new ArrayList<>();
    for (List<ItemStack> alternatives : ingredientAlternatives) {
        if (alternatives != null && !alternatives.isEmpty()) {
            nonEmpty.add(alternatives);
        }
    }

    if (nonEmpty.isEmpty()) {
        return new GenericFallbackLayout(List.of(), 0, 0, 44, 0, 20, 4);
    }

    int inputCount = nonEmpty.size();
    int cols = switch (inputCount) {
        case 1 -> 1;
        case 2, 3 -> inputCount;
        case 4 -> 2;
        default -> 3;
    };
    int rows = (int) Math.ceil((double) inputCount / cols);

    List<SlotPosition> inputs = new ArrayList<>();
    for (int i = 0; i < inputCount; i++) {
        int col = i % cols;
        int row = i / cols;
        inputs.add(new SlotPosition(col * 18, row * 18, nonEmpty.get(i)));
    }

    int inputAreaWidth = cols * 18;
    int inputAreaHeight = rows * 18;
    int arrowX = inputAreaWidth + 4;
    int arrowY = Math.max(0, (inputAreaHeight - 16) / 2);
    int outputX = arrowX + 26;
    int outputY = Math.max(0, (inputAreaHeight - 18) / 2);

    return new GenericFallbackLayout(inputs, cols, rows, outputX, outputY, arrowX, arrowY);
}

public record GenericFallbackLayout(
        List<SlotPosition> inputs,
        int gridWidth,
        int gridHeight,
        int outputX,
        int outputY,
        int arrowX,
        int arrowY
) {
}
```

- [ ] **Step 4: Wire the generic fallback branch to the helper**

Replace the existing `else` generic-fallback branch in `RecipeDisplayHelper.getLayout(...)`:

```java
// BEFORE: ad hoc inline branching for inputCount == 0 / == 1 / <= 3 / grid

// AFTER:
GenericFallbackLayout fallbackLayout = createGenericFallbackLayout(
        ingredients.stream()
                .map(ingredient -> List.of(ingredient.getItems()))
                .toList());
inputs.addAll(fallbackLayout.inputs());
gridW = fallbackLayout.gridWidth();
gridH = fallbackLayout.gridHeight();
outputX = fallbackLayout.outputX();
outputY = fallbackLayout.outputY();
arrowX = fallbackLayout.arrowX();
arrowY = fallbackLayout.arrowY();
```

Implementation note for this step:
- Keep the surrounding crafting/special-layout branches untouched.
- Preserve the zero-input fallback return values from the helper exactly as shown above.

- [ ] **Step 5: Run the new test to verify it passes**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperGenericFallbackLayoutTest"
```

Expected: 3 tests PASS.

- [ ] **Step 6: Run the existing crafting-layout regression test**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperLayoutTest"
```

Expected: the existing crafting placeholder tests still PASS.

- [ ] **Step 7: Commit**

```powershell
git add xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java
git add neoforge/src/test/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelperGenericFallbackLayoutTest.java
git commit -m "test: cover AMI generic fallback recipe geometry"
```

---

### Task 2: Add a small shared placement helper and use it everywhere `RecipeViewerScreen` anchors generic layouts

**Files:**
- Create: `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacement.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacementTest.java`

**Interfaces:**
- Produces: `RecipeViewerLayoutPlacement.layoutOriginX(int cardX, RecipeLayout layout)`
- Consumes: `RecipeDisplayHelper.RecipeLayout`

- [ ] **Step 1: Write the failing placement-helper test**

```java
// neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacementTest.java
package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerLayoutPlacementTest {

    @Test
    void narrowGenericLayoutsReceiveASmallRightNudge() {
        RecipeLayout layout = genericLayout(66, 9, 40, 10);

        assertEquals(36, RecipeViewerLayoutPlacement.layoutOriginX(4, layout));
    }

    @Test
    void widerGenericLayoutsKeepTheLegacyAnchor() {
        RecipeLayout layout = genericLayout(84, 0, 58, 1);

        assertEquals(28, RecipeViewerLayoutPlacement.layoutOriginX(4, layout));
    }

    @Test
    void texturedLayoutsKeepTheLegacyAnchor() {
        RecipeLayout layout = new RecipeLayout(
                ResourceLocation.parse("ami:test"),
                ItemStack.EMPTY,
                "",
                List.of(new SlotPosition(0, 0, List.of(new ItemStack(Items.APPLE)))),
                ItemStack.EMPTY,
                1,
                1,
                false,
                61,
                19,
                24,
                18,
                ResourceLocation.parse("ami:textures/gui/test.png"),
                0,
                0,
                82,
                54,
                36,
                4,
                false
        );

        assertEquals(28, RecipeViewerLayoutPlacement.layoutOriginX(4, layout));
    }

    private static RecipeLayout genericLayout(int outputX, int outputY, int arrowX, int arrowY) {
        return new RecipeLayout(
                ResourceLocation.parse("ami:test"),
                ItemStack.EMPTY,
                "",
                List.of(
                        new SlotPosition(0, 0, List.of(new ItemStack(Items.APPLE))),
                        new SlotPosition(18, 0, List.of(new ItemStack(Items.CAKE))),
                        new SlotPosition(0, 18, List.of(new ItemStack(Items.CHEST))),
                        new SlotPosition(18, 18, List.of(new ItemStack(Items.REDSTONE)))
                ),
                ItemStack.EMPTY,
                2,
                2,
                false,
                outputX,
                outputY,
                arrowX,
                arrowY,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                true
        );
    }
}
```

- [ ] **Step 2: Run the new test to verify it fails**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerLayoutPlacementTest"
```

Expected: compile failure because `RecipeViewerLayoutPlacement` does not exist yet.

- [ ] **Step 3: Add the minimal placement helper**

```java
// xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacement.java
package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;

final class RecipeViewerLayoutPlacement {
    private static final int BASE_LAYOUT_X = 24;
    private static final int OUTPUT_SLOT_SPRITE_WIDTH = 26;
    private static final int ARROW_WIDTH = 22;
    private static final int GENERIC_LAYOUT_REFERENCE_WIDTH = 108;
    private static final int MAX_GENERIC_LAYOUT_NUDGE = 8;

    private RecipeViewerLayoutPlacement() {
    }

    static int layoutOriginX(int cardX, RecipeLayout layout) {
        if (layout.backgroundTexture() != null) {
            return cardX + BASE_LAYOUT_X;
        }

        int inputRight = layout.inputs().stream()
                .mapToInt(slot -> slot.x() + 18)
                .max()
                .orElse(0);
        int clusterWidth = Math.max(inputRight,
                Math.max(layout.outputX() + OUTPUT_SLOT_SPRITE_WIDTH, layout.arrowX() + ARROW_WIDTH));
        int nudge = Math.min(MAX_GENERIC_LAYOUT_NUDGE,
                Math.max(0, (GENERIC_LAYOUT_REFERENCE_WIDTH - clusterWidth) / 2));
        return cardX + BASE_LAYOUT_X + nudge;
    }
}
```

- [ ] **Step 4: Replace every repeated `cardX + 24` generic-layout anchor in `RecipeViewerScreen`**

In `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`, replace all four `rx` assignments:

```java
// BEFORE
int rx = cardX + 24;

// AFTER
int rx = RecipeViewerLayoutPlacement.layoutOriginX(cardX, layout);
```

Apply this replacement at each current `rx` anchor site used by:
- render-time recipe-card drawing
- hover hit-testing
- wheel-scrolling hit-testing
- mouse-click hit-testing

Implementation note for this step:
- Do not change the vertical `ry` math.
- Do not change the existing textured-background branch.
- Do not introduce a second placement formula anywhere else in the screen.

- [ ] **Step 5: Run the placement-helper test to verify it passes**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerLayoutPlacementTest"
```

Expected: 3 tests PASS.

- [ ] **Step 6: Run focused shared verification**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperGenericFallbackLayoutTest" --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperLayoutTest" --tests "com.sanhiruzu.ami.client.RecipeViewerLayoutPlacementTest" :forge:compileJava :fabric:compileJava --rerun-tasks
```

Expected:
- all 3 focused test classes PASS
- Forge and Fabric shared-code compilation succeed

- [ ] **Step 7: Commit**

```powershell
git add xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacement.java
git add xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java
git add neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerLayoutPlacementTest.java
git commit -m "feat: tighten AMI recipe viewer JEI-style geometry"
```

---

## Self-Review

### Spec coverage

- Shared fallback slot/arrow/output geometry is covered by Task 1.
- Small screen-level generic-layout centering is covered by Task 2.
- Crafting placeholder preservation is covered by re-running `RecipeDisplayHelperLayoutTest`.
- No behavior changes are preserved by keeping click/hover logic untouched and only swapping shared anchors.

### Placeholder scan

- No `TODO`, `TBD`, or “similar to Task N” placeholders remain.
- Each code-changing step includes exact file paths, code blocks, and commands.

### Type consistency

- `GenericFallbackLayout` and `createGenericFallbackLayout(...)` are named consistently across test and implementation steps.
- `RecipeViewerLayoutPlacement.layoutOriginX(...)` is named consistently in the helper, tests, and screen wiring.
- The placement helper always consumes `RecipeLayout`, and `RecipeViewerScreen` always uses that shared helper at each `rx` anchor.
