# AMI Recipe Viewer AMI-Native Special Layouts Design

## Summary

AMI's owned recipe viewer should keep JEI's readability and layout discipline while presenting recipe cards in AMI's
own visual language. The current special-recipe path mixes AMI chrome with cropped vanilla GUI textures and raw recipe
entries, which causes several user-visible problems:

- the connector between inputs and outputs reads like a rendering bug instead of a recipe affordance
- the favorite star and workstation strip feel visually off-center and inconsistent with the rest of AMI
- anvil-family workstations show redundant chipped and damaged variants
- duplicate-looking anvil and enchanting pages consume pagination
- some captured special recipes can surface as visually empty cards
- pagination counts raw entries instead of meaningful visible recipe cards

This change introduces an AMI-native display model for recipe cards with a safe fallback for recipe types that cannot be
restyled cleanly.

## Goals

- Use AMI-native slot, output, connector, and favorite visuals for AMI-owned recipe cards.
- Preserve JEI-style spacing, alignment, and legibility where that improves scanability.
- Normalize workstation strips so recipe families show the workstation AMI actually wants to communicate.
- Filter or collapse recipe entries before pagination so the page count reflects meaningful visible cards.
- Keep a safe fallback path for recipe layouts that still require texture-backed rendering.

## Non-Goals

- Rebuild every recipe-family renderer from scratch in one pass.
- Change EMI or JEI's own recipe screens.
- Change recipe discovery/indexing semantics outside the AMI-owned display list for one selected tab.
- Introduce lossy collapsing for recipes whose visible inputs/output differ in a meaningful player-facing way.

## User-Facing Outcomes

### Visual Language

AMI-owned cards should render with AMI-native chrome first:

- Replace the current procedural blue connector with a dedicated AMI sprite asset.
- Replace the text-glyph favorite star with a sprite or sprite-backed badge treatment that centers cleanly in the
  existing hit area.
- Center workstation icons against the actual framed slot bounds instead of relying on the current fixed offsets.
- Prefer AMI-native slots/output/connector treatment for anvil, enchanting, and disenchanting-style cards.

When a captured or unusual recipe layout does not map cleanly onto the AMI-native treatment, the viewer may keep the
existing texture-backed fallback rather than inventing a broken bespoke renderer.

### Display Semantics

Before the selected tab is paginated, AMI should build a visible recipe-card list that:

- canonicalizes workstation display variants such as `anvil`, `chipped_anvil`, and `damaged_anvil` to the single AMI
  workstation icon the viewer should advertise
- drops recipe cards whose resolved layout is visually empty or otherwise not meaningful to show
- collapses duplicate-looking special recipes when the visible recipe family, visible inputs, visible output, and
  displayed workstation are equivalent

Pagination should run on this visible-card list instead of the raw tab recipe list.

## Design

### 1. Visible Recipe Entry Pipeline

Add a small AMI-side display pipeline for `RecipeViewerScreen` tab contents.

For each raw `AmiRecipeHolder<?>` in the selected tab:

1. Resolve its `RecipeLayout`.
2. Derive a display family key suitable for viewer deduping.
3. Derive a visible signature from the rendered inputs, output, and normalized workstation identity.
4. Decide whether the entry is:
   - renderable and unique
   - renderable but visually duplicate of an already-kept entry
   - not meaningfully renderable and should be skipped

This pipeline should produce a stable ordered list so AMI keeps the existing recipe ordering as much as possible while
removing duplicates and blank entries.

### 2. Workstation Normalization

Normalize workstation lists for special families before drawing the workstation strip and before building display
signatures.

Initial normalization rules:

- `ami:anvil_repairing` -> `[minecraft:anvil]`
- `ami:enchanting` -> `[minecraft:anvil]`
- existing single-workstation families such as grindstone, crafting table, stonecutter, smithing table, and brewing
  stand keep their current canonical workstation

This normalization is display-only. It does not change recipe lookup behavior or runtime recipe matching.

### 3. Duplicate Collapse Rules

Collapse only when the player-facing card would read as the same recipe page.

The dedupe signature should include:

- recipe display family
- normalized workstation identity
- visible input alternatives by slot position
- visible output stack identity

This intentionally allows multiple raw recipes to collapse into one page when they differ only in backend recipe source
or variant workstation item but present the same visible card.

Collapse should be conservative:

- keep distinct pages when slot positions differ
- keep distinct pages when output differs
- keep distinct pages when the visible inputs differ
- do not dedupe across different display families

### 4. Empty/Unrenderable Card Policy

Some special recipe entries currently surface as effectively blank cards. AMI should skip a card when all of the
following are true:

- it has no meaningful visible input stacks
- it has no meaningful visible output stack
- it has no family-specific visual context that makes the card useful anyway

The goal is to avoid wasting a page on a card that communicates nothing to the player. This filter is display-local and
should not delete or mutate the underlying recipe registration.

### 5. AMI-Native Special Card Rendering

`RecipeViewerScreen` should keep the current JEI-style card geometry and alignment rules, but special-family cards should
favor AMI-native chrome:

- dedicated AMI connector sprite between input area and output slot
- centered favorite badge art instead of the current text glyph
- corrected workstation strip centering inside the outer framed panel
- continued use of AMI slot and output slot assets where the recipe layout permits

Texture-backed fallback remains valid for layouts whose background conveys required context that AMI has not yet modeled.

## Main Files

- `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`
- `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`
- `xplat/src/main/resources/assets/ami/textures/gui/recipe_viewer_*.png`
- new AMI-side helper(s) for display-entry filtering/deduping if needed

## Testing Strategy

### JVM Tests

Add focused tests for:

- workstation normalization for special recipe families
- visible-card dedupe for duplicate-looking anvil/enchanting entries
- blank/unrenderable-card filtering
- pagination operating on the filtered visible-card list rather than raw recipe count
- existing crafting-grid layout and hover-hint tests continue to pass

Prefer deterministic unit tests over client-only validation for the new display pipeline.

### Runtime Smoke

After JVM coverage is green, run a NeoForge AMI smoke pass to confirm:

- connector sprite clearly reads as recipe flow
- favorite marker is visually centered
- workstation icon is centered in its outer frame
- anvil family shows only the regular anvil in the left strip
- duplicate-looking anvil/enchanting pages are gone
- the blank final page no longer appears
- page count drops when duplicate or empty cards are removed

## Risks And Mitigations

- Over-aggressive dedupe could hide legitimately different recipes.
  Mitigation: dedupe on visible slot positions plus visible IO, not merely recipe type or output.

- Over-aggressive blank-card filtering could hide recipes whose texture background alone conveys meaning.
  Mitigation: keep the filter conservative and allow texture-backed family-specific fallback to mark an entry as
  renderable.

- AMI-native restyling could regress odd captured layouts.
  Mitigation: preserve the existing texture-backed fallback path for recipe types/layouts that do not restyle cleanly.

## Open Decisions Resolved In This Spec

- AMI should prefer AMI-native visuals with a fallback, not slavish vanilla texture parity.
- Redundant anvil/enchanting workstation variants should collapse to the regular anvil for display.
- Duplicate-looking anvil or enchanting pages should be hidden from the visible page list.
- The connector between inputs and output should become a sprite asset rather than a procedural blue shape.
