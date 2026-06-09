# Recipe Viewer Independence Plan

Goal: make AMI's index complete without needing JEI at runtime, while using JEI and EMI as audit oracles until native discovery reaches parity.

## Target State

- AMI owns item and resource discovery natively.
- JEI and EMI are optional integrations for recipe UI and parity auditing.
- Indexing starts early and enriches in phases.
- Visibility settings such as `devMode`, `showHiddenModItems`, and creative filters apply after discovery, not during it.

## Principles

- Discovery first, filtering second.
- Runtime-discovered stacks beat handwritten representative subsets.
- Same-name component variants must be preserved when the payload is player-meaningful.
- JEI parity reports drive native fixes, not permanent backfills.

## Current Findings

Recent parity work surfaced a few recurring failure modes:

1. Shared creative-stack discovery was incomplete.
   - AMI previously read only `CreativeModeTab.getDisplayItems()`.
   - JEI uses both `displayItems` and `searchTabDisplayItems`.
   - This caused search-only items such as `pneumaticcraft:plastic` to fall out of AMI's base coverage.

2. Same-name component variants are still collapsed too aggressively.
   - Current examples include `minecraft:enchanted_book`, `enderio:soul_vial`, and `mekanism:creative_fluid_tank`.
   - These are not missing item ids; they are underrepresented exact stacks.

3. Handwritten subtype generation is weaker than runtime-discovered stack sets for some high-cardinality families.
   - AMI should prefer authoritative runtime stack sets where available.

4. Non-item resource discovery is still asymmetric.
   - JEI-backed ingredient/resource indexing exists now.
   - EMI-only and viewer-independent resource discovery still need first-class support.

## Workstreams

### 1. Complete Native Stack Discovery

Main files:

- `xplat/src/main/java/com/sanhiruzu/ami/index/ItemFilter.java`
- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`

Actions:

- Keep the `displayItems` + `searchTabDisplayItems` collector behavior aligned with JEI.
- Verify that search-only items and variants reappear in AMI after fresh dumps.
- Audit whether any other shared item sources JEI uses are still absent from AMI.

Success criteria:

- Missing base item count returns to `0` for current dumps.
- New mods do not require per-mod patches just to appear in the base item index.

### 2. Fix Generic Exact-Stack Preservation

Main files:

- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/CreativeStackVariantExpander.java`
- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/SubtypeExpander.java`

Actions:

- Rework hidden-duplicate logic so same-name stacks are preserved when the payload is the identity a player cares about.
- Prefer generic heuristics over per-mod patches.

Candidate generic signals:

- stored enchantments
- stored entity type
- stored fluid or chemical
- stored potion or stew effect
- meaningful upgrade or configuration payload

Initial target families:

- `minecraft:enchanted_book`
- `enderio:soul_vial`
- `mekanism:creative_fluid_tank`
- PneumaticCraft tools and containers with meaningful payload

Success criteria:

- Exact-stack parity improves for these families without hardcoding each mod separately.

### 3. Reduce Reliance on Handwritten Subtype Generation

Main files:

- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/SubtypeExpander.java`
- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`

Actions:

- Reserve handwritten subtype generation for cases where runtime stacks are unavailable.
- Prefer discovered creative or runtime stack sets when available.
- Treat synthetic subtype generation as fallback, not primary truth, for high-cardinality families.

Success criteria:

- Subtype families stop drifting from JEI-visible stack sets.

### 4. Build First-Class Native Non-Item Discovery

Main files:

- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/RecipeViewerIngredientProvider.java`
- future EMI-native provider
- future AMI-native resource abstraction

Actions:

- Keep the JEI-backed ingredient bridge as a temporary parity aid.
- Add EMI-native ingredient/resource discovery for EMI-only environments.
- Long term, introduce an AMI-native resource model for gases, chemicals, pigments, slurries, and custom ingredient types.

Success criteria:

- Non-item resource parity works in both JEI and EMI environments.
- AMI no longer depends on a specific viewer runtime to know that a resource exists.

### 5. Split Indexing into Explicit Phases

Main files:

- `xplat/src/main/java/com/sanhiruzu/ami/index/ProviderRegistry.java`
- `xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java`

Target phases:

1. Early registry phase
   - items
   - blocks
   - fluids
   - entities
   - static metadata and facets

2. Mid client-runtime phase
   - creative-tab stacks
   - subtype and runtime-discovered variants
   - recipe outputs and uses

3. Late volatile phase
   - player, waypoint, and other world-dependent state

Success criteria:

- AMI becomes usable earlier.
- Exact parity improves progressively instead of waiting for late viewer runtime state.

### 6. Strengthen Parity Auditing

Main files:

- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/RecipeViewerItemAudit.java`
- future EMI/non-item audit extensions

Actions:

- Track parity by category, not only by raw counts.
- Separate:
  - missing base item
  - missing exact stack
  - missing non-item ingredient
  - collapsed meaningful variant

Success criteria:

- JEI and EMI reports become actionable design inputs, not just diagnostics.

### 7. Avoid Permanent Viewer Backfill in Release Architecture

Actions:

- Do not make JEI or EMI the authoritative source for release indexing.
- If a temporary parity backfill is ever needed, keep it behind debug or development settings while native discovery is hardened.

Success criteria:

- Removing JEI does not reduce AMI discovery coverage, only JEI UI integration.

## Implementation Order

1. Fix unrelated compile breakages currently blocking verification.
   - `xplat/src/main/java/com/sanhiruzu/ami/compat/ChemicalGroupingPlugin.java`
   - `xplat/src/main/java/com/sanhiruzu/ami/index/FacetIndexer.java`

2. Verify the shared creative-stack collector fix with fresh dumps.
   - Expected: `pneumaticcraft:plastic` base coverage restored.

3. Rework same-name variant collapse generically.
   - First target: books, soul vials, creative tanks.

4. Add EMI-native non-item ingredient and lookup support.

5. Split indexing into explicit phases.

6. Expand parity audits and drive the remaining misses down category by category.

## Success Criteria

- AMI base item parity with JEI reaches `0` missing.
- AMI exact-stack parity reaches near-zero except for explicitly accepted collapses.
- AMI non-item resource parity works in JEI and EMI environments.
- AMI can start indexing before recipe viewer runtime is available.
- Removing JEI no longer causes discovery loss.

