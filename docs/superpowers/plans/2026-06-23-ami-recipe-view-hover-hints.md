# AMI Recipe View Hover Hints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add contextual hover hints in AMI's own recipe viewer for multi-variant ingredient slots, transfer affordances, and workstation entries without changing click behavior or adding persistent UI chrome.

**Architecture:** Keep hover hit-testing and tooltip rendering in `RecipeViewerScreen`, but extract the hint-selection rules into a tiny package-private helper so the policy can be covered with deterministic JVM tests. Reuse the existing transfer-capability gate and existing hovered slot/workstation paths, append only short extra tooltip lines, and leave the idle render path unchanged.

**Tech Stack:** Java 21, Minecraft `Component`/`ItemStack` tooltips, NeoForge JUnit 5 JVM tests, Gradle.

---

## Global Constraints

- Do not change any click behavior in `RecipeViewerScreen`; this pass is tooltip-only.
- Keep the AMI recipe viewer visually unchanged when nothing special is hovered.
- Ingredient slots with a single alternative keep their current plain item tooltip.
- Ingredient slots with multiple alternatives keep the existing `ami.recipe_viewer.ingredient_cycle` line and gain one extra action-oriented line.
- Output-slot transfer guidance only appears when `RecipeViewerBridge.canTransferRecipe(recipe, parentScreen)` is `true`.
- Workstation entries keep their current item tooltip and gain explicit left/right click guidance.
- Prefer a package-private helper in `xplat/src/main/java/com/sanhiruzu/ami/client/` over adding more conditional logic directly into `RecipeViewerScreen`.
- Keep the existing crafting-layout regression coverage in `neoforge/src/test/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelperLayoutTest.java`.
- Update `docs/feature-map.md` with the stable hover-hint entry point once the implementation is in place.
- Commit messages must not include any AI attribution.

### Task 1: Add a testable hover-hint policy helper

**Files:**
- Create: `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicy.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicyTest.java`

**Interfaces:**
- Produces: `RecipeViewerHoverHintPolicy.Hint`
- Produces: `RecipeViewerHoverHintPolicy.ingredientHints(SlotPosition slot)`
- Produces: `RecipeViewerHoverHintPolicy.transferButtonHints()`
- Produces: `RecipeViewerHoverHintPolicy.outputHints(boolean recipeCanTransfer)`
- Produces: `RecipeViewerHoverHintPolicy.workstationHints()`

- [ ] **Step 1: Write the failing test**

```java
// neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicyTest.java
package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicy.Hint;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerHoverHintPolicyTest {

    @Test
    void multiVariantIngredientSlotGetsScrollHint() {
        SlotPosition slot = new SlotPosition(4, 4, List.of(
                new ItemStack(Items.APPLE),
                new ItemStack(Items.CARROT),
                new ItemStack(Items.POTATO)
        ));

        assertEquals(List.of(Hint.INGREDIENT_SCROLL),
                RecipeViewerHoverHintPolicy.ingredientHints(slot));
    }

    @Test
    void singleVariantIngredientSlotGetsNoExtraHint() {
        SlotPosition slot = new SlotPosition(4, 4, List.of(new ItemStack(Items.APPLE)));

        assertEquals(List.of(), RecipeViewerHoverHintPolicy.ingredientHints(slot));
    }

    @Test
    void transferButtonAlwaysShowsTransferHint() {
        assertEquals(List.of(Hint.TRANSFER_BUTTON),
                RecipeViewerHoverHintPolicy.transferButtonHints());
    }

    @Test
    void outputSlotOnlyShowsShiftTransferHintWhenTransferIsAvailable() {
        assertEquals(List.of(Hint.OUTPUT_SHIFT_TRANSFER),
                RecipeViewerHoverHintPolicy.outputHints(true));
        assertEquals(List.of(),
                RecipeViewerHoverHintPolicy.outputHints(false));
    }

    @Test
    void workstationHoverShowsRecipesAndUsesHints() {
        assertEquals(List.of(Hint.WORKSTATION_LEFT_CLICK, Hint.WORKSTATION_RIGHT_CLICK),
                RecipeViewerHoverHintPolicy.workstationHints());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicyTest"
```

Expected: compile failure because `RecipeViewerHoverHintPolicy` does not exist yet.

- [ ] **Step 3: Write the minimal helper**

```java
// xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicy.java
package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;

import java.util.List;

final class RecipeViewerHoverHintPolicy {
    enum Hint {
        INGREDIENT_SCROLL,
        TRANSFER_BUTTON,
        OUTPUT_SHIFT_TRANSFER,
        WORKSTATION_LEFT_CLICK,
        WORKSTATION_RIGHT_CLICK
    }

    private RecipeViewerHoverHintPolicy() {
    }

    static List<Hint> ingredientHints(SlotPosition slot) {
        return slot.alternatives().size() > 1 ? List.of(Hint.INGREDIENT_SCROLL) : List.of();
    }

    static List<Hint> transferButtonHints() {
        return List.of(Hint.TRANSFER_BUTTON);
    }

    static List<Hint> outputHints(boolean recipeCanTransfer) {
        return recipeCanTransfer ? List.of(Hint.OUTPUT_SHIFT_TRANSFER) : List.of();
    }

    static List<Hint> workstationHints() {
        return List.of(Hint.WORKSTATION_LEFT_CLICK, Hint.WORKSTATION_RIGHT_CLICK);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicyTest"
```

Expected: `RecipeViewerHoverHintPolicyTest` reports 5 tests passed.

- [ ] **Step 5: Commit**

```powershell
git add xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicy.java
git add neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicyTest.java
git commit -m "test: add recipe viewer hover hint policy coverage"
```

---

### Task 2: Wire hover hints into `RecipeViewerScreen` and add translation keys

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`
- Modify: `xplat/src/main/resources/assets/ami/lang/en_us.json`

**Interfaces:**
- Consumes: `RecipeViewerHoverHintPolicy`
- Keeps: `RecipeViewerBridge.canTransferRecipe(recipe, parentScreen)` as the single transfer-availability gate
- Produces: hover-only tooltip lines for ingredient slots, output slot, transfer button, and workstation strip

- [ ] **Step 1: Extend the policy test with mapping guard assertions**

Add these assertions to the existing policy test so the render layer has concrete strings to target before editing the screen:

```java
// append to neoforge/src/test/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicyTest.java
import static org.junit.jupiter.api.Assertions.assertTrue;

    @Test
    void hintEnumNamesStayStableForTooltipMapping() {
        assertTrue(RecipeViewerHoverHintPolicy.transferButtonHints()
                .contains(Hint.TRANSFER_BUTTON));
        assertTrue(RecipeViewerHoverHintPolicy.outputHints(true)
                .contains(Hint.OUTPUT_SHIFT_TRANSFER));
        assertTrue(RecipeViewerHoverHintPolicy.workstationHints()
                .contains(Hint.WORKSTATION_LEFT_CLICK));
        assertTrue(RecipeViewerHoverHintPolicy.workstationHints()
                .contains(Hint.WORKSTATION_RIGHT_CLICK));
    }
```

- [ ] **Step 2: Run the policy test to keep the baseline green before screen edits**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicyTest"
```

Expected: 6 tests passed.

- [ ] **Step 3: Update `RecipeViewerScreen` tooltip rendering**

In `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`, make these focused changes:

```java
// 1) Output-slot hover inside the recipe-card render loop:
if (!layout.output().isEmpty()) {
    g.renderItem(layout.output(), outX + 1, outY + 1);
    g.renderItemDecorations(font, layout.output(), outX + 1, outY + 1);
    if (isHovering(mouseX, mouseY, outX, outY, 18, 18)) {
        renderOutputTooltip(g, layout.output(), recipeCanTransfer, mouseX, mouseY);
    }
}

// 2) Transfer-button tooltip in the same loop:
if (bHov) {
    g.renderTooltip(font,
            Component.translatable("ami.recipe_viewer.transfer"), mouseX, mouseY);
}

// 3) Replace renderIngredientTooltip(...) with a version that appends the new action line:
private void renderIngredientTooltip(GuiGraphics g, SlotPosition slot, int altIdx, int mouseX, int mouseY) {
    ItemStack stack = slot.alternatives().get(altIdx);
    if (slot.alternatives().size() <= 1) {
        g.renderTooltip(font, stack, mouseX, mouseY);
        return;
    }
    List<Component> lines = new ArrayList<>(
            net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, stack));
    lines.add(Component.empty());
    lines.add(Component.translatable("ami.recipe_viewer.ingredient_cycle",
            altIdx + 1, slot.alternatives().size())
            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    appendHoverHints(lines, RecipeViewerHoverHintPolicy.ingredientHints(slot), slot);
    g.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
}

// 4) Replace workstation item-tooltip rendering in drawWorkstationPanel(...):
if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
    renderWorkstationTooltip(g, ws, mouseX, mouseY);
}

// 5) Add these helper methods near the other tooltip helpers:
private void renderOutputTooltip(GuiGraphics g, ItemStack stack, boolean recipeCanTransfer,
                                 int mouseX, int mouseY) {
    List<Component> lines = new ArrayList<>(
            net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, stack));
    appendHoverHints(lines, RecipeViewerHoverHintPolicy.outputHints(recipeCanTransfer), null);
    g.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
}

private void renderWorkstationTooltip(GuiGraphics g, ItemStack stack, int mouseX, int mouseY) {
    List<Component> lines = new ArrayList<>(
            net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, stack));
    appendHoverHints(lines, RecipeViewerHoverHintPolicy.workstationHints(), null);
    g.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
}

private void appendHoverHints(List<Component> lines, List<RecipeViewerHoverHintPolicy.Hint> hints,
                              SlotPosition slot) {
    if (hints.isEmpty()) {
        return;
    }
    lines.add(Component.empty());
    for (RecipeViewerHoverHintPolicy.Hint hint : hints) {
        lines.add(switch (hint) {
            case INGREDIENT_SCROLL -> Component.translatable(
                    "ami.recipe_viewer.ingredient_variants_scroll",
                    slot.alternatives().size());
            case TRANSFER_BUTTON -> Component.translatable("ami.recipe_viewer.transfer");
            case OUTPUT_SHIFT_TRANSFER -> Component.translatable("ami.recipe_viewer.output_shift_transfer");
            case WORKSTATION_LEFT_CLICK -> Component.translatable("ami.recipe_viewer.workstation_left_click");
            case WORKSTATION_RIGHT_CLICK -> Component.translatable("ami.recipe_viewer.workstation_right_click");
        }.withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
```

Implementation notes for this step:
- Keep `TRANSFER_BUTTON` mapped even if the current render path still calls `g.renderTooltip(...)` directly; that avoids enum drift if later code wants to centralize button tooltips too.
- Pass `slot` only from `renderIngredientTooltip(...)`; `null` is acceptable for the other hint types because they do not read slot data.
- Do not append output or workstation hints when their policy method returns an empty list.

- [ ] **Step 4: Update the recipe-viewer translation strings**

In `xplat/src/main/resources/assets/ami/lang/en_us.json`, keep `ingredient_cycle` as-is and add the new keys next to the other `ami.recipe_viewer.*` strings:

```json
  "ami.recipe_viewer.transfer": "Click to transfer",
  "ami.recipe_viewer.ingredient_cycle": "%s/%s — scroll to cycle",
  "ami.recipe_viewer.ingredient_variants_scroll": "%s variants, scroll to cycle",
  "ami.recipe_viewer.output_shift_transfer": "Shift-click to transfer",
  "ami.recipe_viewer.workstation_left_click": "Left-click: recipes",
  "ami.recipe_viewer.workstation_right_click": "Right-click: uses",
```

- [ ] **Step 5: Run focused verification**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicyTest" --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperLayoutTest" :forge:compileJava :fabric:compileJava --rerun-tasks
```

Expected:
- `RecipeViewerHoverHintPolicyTest` passes
- `RecipeDisplayHelperLayoutTest` stays green
- Forge and Fabric shared-code compilation succeed

- [ ] **Step 6: Commit**

```powershell
git add xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java
git add xplat/src/main/resources/assets/ami/lang/en_us.json
git commit -m "feat: add AMI recipe viewer hover hints"
```

---

### Task 3: Update feature-map documentation and run final verification

**Files:**
- Modify: `docs/feature-map.md`

**Interfaces:**
- Documents: `RecipeViewerScreen` as the stable hover-hint entry point
- Documents: `RecipeViewerHoverHintPolicy` as the test seam for AMI-owned recipe tooltip behavior

- [ ] **Step 1: Add a dedicated feature-map section**

Insert a new section immediately after `## AMI Recipe Viewer Crafting Layout` in `docs/feature-map.md`:

```markdown
## AMI Recipe Viewer Hover Hints

- User surface: the AMI-owned recipe viewer should stay visually quiet at rest, but hovering certain recipe controls
  should explain their hidden interactions.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerHoverHintPolicy.java`
- Tests:
  - `.\gradlew.bat :neoforge:test --tests "*RecipeViewerHoverHintPolicyTest"`
  - `.\gradlew.bat :neoforge:test --tests "*RecipeDisplayHelperLayoutTest"`
- State contract:
  - Multi-variant ingredient slots keep the existing cycle-count tooltip line and add one extra hover-only action line.
  - Output-slot transfer guidance only appears when `RecipeViewerBridge.canTransferRecipe(...)` reports that transfer is
    currently available.
  - Workstation strip entries append left-click recipes and right-click uses guidance without changing navigation
    behavior.
```

- [ ] **Step 2: Run final verification**

```powershell
.\gradlew.bat :neoforge:test --tests "com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicyTest" --tests "com.sanhiruzu.ami.client.recipe.RecipeDisplayHelperLayoutTest" :forge:compileJava :fabric:compileJava --rerun-tasks
```

Expected: the same focused verification command remains green after the docs change.

- [ ] **Step 3: Commit**

```powershell
git add docs/feature-map.md
git commit -m "docs: map AMI recipe viewer hover hints"
```

---

## Self-Review

### Spec coverage

- Multi-variant ingredient slot hint is covered in Task 1 test cases and Task 2 screen wiring.
- Transfer button and output-slot transfer guidance are covered in Task 1 policy tests and Task 2 tooltip wiring.
- Workstation left/right click guidance is covered in Task 1 policy tests and Task 2 workstation tooltip wiring.
- No-click-behavior-change scope is preserved in the Global Constraints and in Task 2's tooltip-only edits.
- Feature-map follow-through is covered in Task 3.

### Placeholder scan

- No `TODO`, `TBD`, or "similar to above" placeholders remain.
- Every code-changing step includes concrete file paths, code blocks, and verification commands.

### Type consistency

- The helper name is consistently `RecipeViewerHoverHintPolicy`.
- The enum name is consistently `Hint`.
- The screen integration consistently uses `ingredientHints`, `transferButtonHints`, `outputHints`, and `workstationHints`.
- The new translation keys are consistent across the screen and language-file steps.
