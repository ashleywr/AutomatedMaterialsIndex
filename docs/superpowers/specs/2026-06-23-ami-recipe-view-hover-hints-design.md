# AMI Recipe View Hover Hints Design

## Goal

Improve discoverability in AMI's own recipe viewer without adding persistent UI noise by showing contextual hover hints only for the least obvious interactions.

## Context

AMI's recipe viewer already supports several useful interactions, but some of them are easy to miss:

- ingredient slots can contain multiple alternatives and support cycling
- output/transfer affordances can support transfer behavior
- workstation entries on the left support navigation with different mouse buttons

The recent crafting-layout change also made the full 3x3 placeholder more explicit, which makes interaction affordances more visible and worth clarifying. The user chose the quieter "hover-first" direction rather than always-on instructional chrome.

## Approved UX Direction

The recipe viewer should remain visually clean at rest. Hints should appear only when the player hovers a control whose behavior is not obvious from the current chrome.

### In Scope Hover Hints

1. Multi-variant ingredient slots
   - Show a hint that the slot has multiple variants.
   - Explicitly mention scroll-to-cycle behavior.
   - Example wording: `3 variants, scroll to cycle`

2. Transfer affordances
   - Transfer button hover should explain the direct transfer action.
   - Output slot hover should mention shift-transfer only when transfer is available for the current recipe/screen.
   - Example wording:
     - transfer button: `Click to transfer`
     - output slot: `Shift-click to transfer`

3. Workstation strip entries
   - Hover should explain that the workstation entry is navigable.
   - Hint should make the left/right click split explicit, since that behavior is otherwise hidden.
   - Example wording:
     - `Left-click: recipes`
     - `Right-click: uses`

### Explicitly Out of Scope

- changing any recipe-view click behavior
- adding always-visible instructional rails, badges, or footer copy across every card
- redesigning tabs, page navigation, or card layout
- broader empty-state improvements
- broader generic mod recipe fallback polish

## Implementation Shape

### Primary File

- `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`

This screen already owns:

- hover hit-testing for ingredient slots, outputs, transfer buttons, and workstation entries
- tooltip rendering for ingredient/output/workstation content
- slot cycling logic and transfer gating

That makes it the natural place to choose and render contextual hover hints.

### Supporting File

- `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`

Only touch this helper if the hover hints need small amounts of extra layout metadata. Do not move general interaction policy into the layout builder unless testing pressure makes a tiny extraction worthwhile.

### Preferred Code Shape

Keep the render path visually unchanged when nothing special is hovered.

Add a small hover-hint selection layer that:

- inspects the currently hovered interactive target
- decides whether that target needs an extra instruction
- appends or renders a concise hint near the existing tooltip path

The logic should reuse the existing hit-test passes instead of introducing a second parallel hover model.

### Suggested Internal Structure

If `RecipeViewerScreen` becomes too tangled, extract a tiny package-private helper/policy with responsibilities like:

- determine whether the hovered ingredient slot is multi-variant
- determine whether transfer is available for the hovered recipe/output
- produce the contextual hint text for workstation entries

The extraction should remain narrow and data-driven. This pass does not justify a large UI abstraction.

## Interaction Rules

### Ingredient Slots

- Single-option ingredient slots keep the current tooltip behavior.
- Multi-option ingredient slots keep the current item tooltip plus the existing cycle count information.
- Add one clearer action-oriented line telling the player to scroll to cycle.

### Output Slot

- Keep the current output tooltip.
- Only add transfer text when recipe transfer is actually available.
- Do not add generic click instructions for ordinary output navigation; the goal is to clarify the hidden transfer shortcut, not narrate every standard click action.

### Transfer Button

- Keep the current transfer button.
- Add direct explanatory wording on hover so the button reads as an action, not just an icon.

### Workstation Entries

- Keep the existing left/right click behavior.
- Add hover text that explains the left/right split explicitly.
- This is the highest-value hidden interaction to clarify because the workstation strip currently reads closest to static decoration.

## Testing Strategy

Prefer deterministic JVM tests for hint-selection policy rather than trying to verify rendered pixels.

### New Tests

Add focused `neoforge` tests for the hover-hint selection logic. The tests should cover:

- multi-variant ingredient slots produce a cycle hint
- single-option ingredient slots produce no extra hint
- transfer button hover produces a transfer hint
- output slot hover only produces transfer guidance when transfer is actually available
- workstation hover produces left/right click guidance

### Existing Coverage To Keep

- retain the recent crafting-layout regression coverage in `RecipeDisplayHelperLayoutTest`

### Extraction Guidance

If testing the logic directly in `RecipeViewerScreen` is awkward, extract the minimal helper required to make the rules testable. The helper should be justified by testability, not by speculative architecture.

## Risks

### Over-instruction

If hint text appears too often, the quiet "B" direction loses its value. The implementation should therefore stay constrained to uncertain interactions only.

### Tooltip Clutter

Appending too much text to existing tooltips could make them noisy. Hints should be short, action-oriented, and limited to one extra line where possible.

### Logic Drift

Transfer hints must reflect actual transfer availability, not assumed availability. The hint-selection logic should rely on the same transfer-capability checks already used by the screen.

## Success Criteria

This design is successful when:

- the AMI recipe viewer still looks clean when idle
- hovering a multi-variant ingredient reveals that scrolling cycles variants
- hovering transfer-related affordances explains transfer behavior only when valid
- hovering workstation entries explains what left and right click do
- no existing recipe-view interaction behavior changes
