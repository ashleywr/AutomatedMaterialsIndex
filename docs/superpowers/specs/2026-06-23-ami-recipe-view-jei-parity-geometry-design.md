# AMI Recipe View JEI Parity Geometry Design

## Goal

Improve the AMI-owned recipe viewer so the shared slot-to-arrow-to-output geometry feels cleaner and closer to JEI, without changing recipe behavior, adding new chrome, or doing a full per-category layout audit.

## Context

AMI's own recipe viewer now has:

- JEI-style 3x3 crafting placeholder behavior for crafting layouts
- contextual hover hints for hidden interactions

The next requested pass is not about adding more features. It is about making the actual recipe layout read more like JEI at a glance, especially for the common generic fallback layouts where the slot grid, arrow, and output slot can still feel slightly more spread out or card-centered than JEI.

The user asked to optimize for:

- a clean design
- parity with JEI
- a first pass focused on layout and slot polish before labels/navigation polish

## Problem Statement

The remaining visual mismatch is primarily geometric:

- the shared fallback input grid, arrow, and output slot do not always read as one tight aligned cluster
- spacing rules differ between row-style and grid-style fallback layouts
- `RecipeViewerScreen` centers the layout inside a recipe card in a way that can emphasize the outer card more than the actual recipe unit

This creates a subtle “AMI card with a recipe inside it” feel where JEI more often reads as a compact recipe layout first.

## Chosen Approach

Use a shared geometry consistency pass rather than a minimal one-off tweak or a large per-category audit.

### Why This Approach

- It fixes the highest-value parity issue in one place.
- It preserves the recent crafting-grid work and the hover-hint work.
- It avoids brittle category-by-category offset tuning.
- It is testable with deterministic JVM layout assertions.

## Alternatives Considered

### 1. Minimal Anchor Tweak

Adjust only one or two fallback offsets in `RecipeDisplayHelper`.

Pros:

- smallest change surface
- lowest immediate risk

Cons:

- likely leaves row layouts and grid layouts inconsistent
- does not address the “card-first” centering feel

### 2. Shared Geometry Consistency Pass

Normalize shared fallback spacing and slightly improve centering of the displayed layout block.

Pros:

- best balance of fidelity, scope, and maintainability
- closer to JEI without overfitting every category
- easy to cover with focused tests

Cons:

- touches both layout generation and screen placement

### 3. Per-Category JEI Mirroring

Tune each recipe family individually against JEI.

Pros:

- highest theoretical fidelity

Cons:

- too broad for this pass
- brittle when recipe families or captured layouts change
- easy to overfit visual trivia instead of improving the common shared case

## In Scope

### Shared Fallback Geometry

Adjust the common fallback layout math in `RecipeDisplayHelper` so:

- row-style recipes and grid-style recipes use the same visual spacing model
- the gap from input area to arrow is tighter and more JEI-like
- the gap from arrow to output slot is tighter and more JEI-like
- the output slot remains vertically aligned against the input area in a way that matches the recipe footprint

### Layout-Block Centering

Adjust `RecipeViewerScreen` layout placement so the recipe layout block is centered more intentionally inside the card, reducing the feeling that the outer card chrome is the primary visual anchor.

This should be a small centering pass, not a card redesign.

### Regression Coverage

Add or extend deterministic layout tests that assert the new fallback geometry.

## Out of Scope

- workstation or machine label polish
- page/tab navigation polish
- favorite button or transfer button chrome cleanup
- tooltip or hover-copy changes
- per-category custom offset tuning for every recipe family
- reworking custom-background recipe screenshots or captured compat layouts

## Primary Files

### `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`

This remains the primary geometry source of truth.

Expected responsibility in this pass:

- keep shared fallback spacing rules together
- ensure row and grid fallback layouts derive arrow/output spacing from the same design intent
- preserve current crafting placeholder semantics

### `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`

This remains responsible for placing the finished layout into the recipe card.

Expected responsibility in this pass:

- slightly refine how the generated layout block is centered within the card
- avoid changing click targets or tooltip logic

### `neoforge/src/test/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelperLayoutTest.java`

Keep the existing crafting-layout coverage and extend nearby tests or add a focused sibling test for generic fallback geometry if that keeps responsibilities clearer.

## Design Rules

### Rule 1: Preserve Behavior

No recipe interactions change in this pass:

- clicking ingredients still navigates
- clicking outputs still navigates or shift-transfers
- workstation interactions stay the same
- transfer availability logic stays the same

### Rule 2: Preserve Crafting Placeholder Parity

The recent full 3x3 crafting placeholder behavior stays intact.

This pass may adjust where the whole fallback layout cluster sits, but it must not reduce crafting layouts back to a partial-slot visual.

### Rule 3: Prefer Shared Math Over Ad Hoc Offsets

If row and grid fallback layouts need different formulas, the difference should be because of recipe shape, not because of unrelated magic-number drift.

### Rule 4: Leave Dedicated Background Layouts Alone Unless Centering Helps Them For Free

Recipe families with their own textured background layouts are not the target of this pass.

The only acceptable change there is a small screen-level centering improvement that applies uniformly.

## Expected Visual Outcome

After this pass:

- the input slots, arrow, and output slot should visually read as a tighter single recipe unit
- generic fallback recipes should feel less horizontally loose
- row layouts and grid layouts should feel related rather than separately tuned
- AMI should still look like AMI, but the slot geometry should feel more JEI-like

## Testing Strategy

Prefer deterministic JVM tests first.

### Required Coverage

- keep the existing crafting-grid layout tests
- add focused assertions for generic fallback row-layout geometry
- add focused assertions for generic fallback grid-layout geometry

The tests should validate exact shared layout coordinates for:

- input slot positions
- arrow position
- output slot position

### Verification Commands

Use focused verification first:

```powershell
.\gradlew.bat :neoforge:test --tests "*RecipeDisplayHelperLayoutTest" :forge:compileJava :fabric:compileJava --rerun-tasks
```

If a second test class is introduced for generic fallback geometry, include it in the same focused verification command.

### Optional Runtime Follow-Up

After JVM coverage is green, a quick runtime smoke can visually confirm the geometry feels right in the actual AMI recipe screen.

That smoke is helpful but not the primary proof for this pass.

## Risks

### Overfitting To One Recipe Shape

If the pass is judged only against crafting or only against one compact recipe shape, other fallback shapes may regress.

Mitigation:

- cover both row and grid fallback layouts in tests

### Mixing Geometry With Chrome Cleanup

It would be easy to start “fixing” favorite/transfer/button visuals during this pass.

Mitigation:

- keep this pass strictly about geometry and centering

### Unintended Custom-Layout Drift

Screen-level centering changes could make dedicated-background layouts feel worse if pushed too far.

Mitigation:

- keep the screen-side adjustment small
- let `RecipeDisplayHelper` remain the main geometry source

## Success Criteria

This design is successful when:

- generic fallback recipes in AMI read more like JEI in spacing and alignment
- the shared slot/arrow/output cluster feels tighter and cleaner
- the full 3x3 crafting placeholder behavior remains correct
- no interaction behavior changes
- focused layout tests and shared-loader compilation pass
