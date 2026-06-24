# AMI Classification Routing Log

This document records design decisions for AMI item classification so we do not
repeat the same failed heuristics while adding major mod compat.

## Current Routing Model

Classification has three separate concerns:

1. Family ownership: which compat ecosystem owns or strongly contextualizes the
   item, such as `cobblemon`, `create`, `ae2`, or `mekanism`.
2. Semantic ontology: what the item objectively is, such as `tools/melee`,
   `geology/stone`, `tech/machines`, `ingredients/ingots`, or `nature/meals`.
3. Family enrichment: extra metadata, search tokens, grouping, tooltips, and row
   fields that apply after ownership is established.

These concerns must not collapse into one score. A family can add metadata
without winning the ontology category.

Representative stack variants are an indexing/presentation concern, not a
family ownership signal. When one registered item advertises multiple creative
tab stacks with different components, AMI may index those stacks as subtype
nodes and mark them with `variantSource=creative_tab` plus
`variantCollapseMode=auto`. This gives search access to color/wood/material
variants while allowing the result UI to collapse high-cardinality families by
`subtypeOf`. Do not use those variants as evidence that a compat family owns an
unrelated item.

Creative-tab variants should only be generated when the base item is visible for
the current access settings. Admin/creative-only base items such as
`creative_*` tanks, batteries, or bins should not leak hundreds of component
variants into normal survival results.

## Routing Gates

Use ordered gates instead of open-ended scoring whenever possible:

1. Hard identity gate: exact item type, Java type, data component, trusted tag,
   or known mod-specific identity.
2. Semantic verb gate: high-confidence semantic verbs from runtime evidence or
   classification overrides, after hard identity and before strong scorer or
   primary fallback rules.
3. Semantic gate: facets and block facts such as weapon, armor, food, storage,
   machine, cable, stone, soil, or placeable block shape.
4. Compat policy gate: decide whether a compat family is allowed to override the
   semantic category for this item kind.
5. Fallback evidence gate: weighted lexical, creative-tab, class-name, and
   metadata evidence when the earlier gates do not decide.
6. Unknown fallback: `misc/unknown` with debug evidence.

Every gate should write enough debug evidence to explain why it won.

### 2026-06-24: Override semantic verbs route before weak placeable fallback

Classification overrides can now add or remove semantic verbs. `PrimaryCategoryResolver`
applies those mutations before routing, then checks the `semantic_verb` phase after
`hard_identity` and before `evidence_strong`/`primary_rule` fallback routing. The first
slice maps `stores_items` to `storage/misc`, `settlement_worksite` to
`utility/workstations`, and `sleep_rest` to `decoration/furniture`.

### 2026-06-09: Partial light-source blocks → decoration/lighting; dragon egg fix; amethyst buds fix

**Partial non-functional LIGHT_SOURCE blocks → decoration/lighting.**
Previously, `isLikelyDecorativeMicroPlaceable` explicitly returned `true` for partial-shaped LIGHT_SOURCE blocks (torches, end rods, amethyst buds, etc.), routing them all to `decoration/other_building`. Two problems:

1. `end_rod` has `LIGHT_SOURCE` but no `DECORATIVE_BLOCK`, so the earlier `decoration facets` rule skipped it and the micro-placeable rule caught it.
2. `shouldResolveDecorationFacetPrimary` required either `DECORATIVE_BLOCK` or (`LIGHT_SOURCE` AND a specific named path like "torch", "lantern", "glowstone") — this was too narrow; end rod was not in that named set.

Fixes applied:
- `isLikelyDecorativeMicroPlaceable`: changed LIGHT_SOURCE branch from `return true` to `return false`. Light-emitting blocks should never route to `other_building`.
- `shouldResolveDecorationFacetPrimary` accepts `attributes` param and now returns true for `LIGHT_SOURCE` blocks that are partial-shaped + non-functional (no `HAS_BLOCK_ENTITY`, `INTERACTIVE_BLOCK`, `MACHINE`, `WORKSTATION`). Full-block light sources like glowstone/shroomlight/beacons are handled by the retained `isPrimaryLightingPath` check.

Items affected:
- **end_rod**: `decoration/other_building` → `decoration/lighting` ✓
- **small/medium/large_amethyst_bud, amethyst_cluster**: `decoration/other_building` → `decoration/lighting` ✓

No `instanceof EndRodBlock` added — that class is not in the neoforge test runtime classpath.

Failed approaches to avoid: do NOT add `instanceof EndRodBlock` or `instanceof IronBarsBlock` to FacetIndexer — those classes are not in the neoforge test runtime classpath and cause `NoClassDefFoundError` at test time. Do NOT make `LIGHT_SOURCE` alone sufficient in `shouldResolveDecorationFacetPrimary` without the partial-shape guard — full-block light emitters like the quark matrix enchanter (HAS_BLOCK_ENTITY + full_block) must stay in masonry.

**Known gap: iron_bars stays in decoration/other_building.** There is no vanilla `BlockTags` for iron bar-type pane blocks. The correct fix requires either a common tag (`c:bars` or similar) from NeoForge/mods, or checking `IronBarsBlock` in the game runtime (not test-safe). Defer until a tag is available.

**Dragon egg: remove spurious INGREDIENT_ORGANIC.**
`isOrganicIngredientPath` used `containsPathToken(path, "egg", "eggs")`, which matched `dragon_egg`, `turtle_egg`, and `sniffer_egg`. Those are block items (hatchable/trophy), not crafting ingredients.
- Changed `"egg"` token match to exact `path.equals("egg")` — only the vanilla throwable chicken egg.
- Modded organic eggs should use the `c:eggs` tag (already handled by the tag scan at line 523).
- Dragon egg now routes as `decoration/lighting` (partial block with LIGHT_SOURCE).

### 2026-06-09: c:silicon → ingredients/mineral; automation compat excludes glass blocks

**`c:silicon` → INGREDIENT_MINERAL.**
Silicon items (EnderIO silicon, etc.) have the `c:silicon` common tag but no standard `c:gems` or `c:ingots` membership. Without a facet, they fell to `fallback:unknown`. Added a direct `tag.equals("c:silicon")` check in `FacetIndexer.applyTagFacts` emitting `INGREDIENT_MINERAL`, giving silicon items strong ingredients/mineral evidence.

**`shouldBiasAutomationFamilyToTech` now excludes plain decorative glass.**
EnderIO's `clear_glass` and its colored variants have `c:glass_blocks/clear` common tag → `GLASS_BLOCK` facet. Despite this, the `automation family tech` compat rule was capturing them and routing to `tech/parts`. Vanilla glass routes to `masonry/full_block` via "remaining placeables". Glass blocks from automation mods should follow the same semantic path unless the glass is itself functional (e.g., `pneumaticcraft:pressure_chamber_glass` has `HAS_BLOCK_ENTITY` and legitimately stays in `tech/machines`). Added an early-exit in `shouldBiasAutomationFamilyToTech`: `if (GLASS_BLOCK && !HAS_BLOCK_ENTITY) return false`.

Items corrected (after next game run):
- **enderio:silicon**: `unknown` → `ingredients/mineral`
- **enderio:clear_glass** (and 17 colored variants): `tech/parts` → `masonry/full_block`

### 2026-06-09: Raw materials and nuggets → ingredients/mineral

`c:raw_materials` and `c:nuggets` common tags previously only set `RAW_MATERIAL` and `NUGGET` facets respectively, with no `INGREDIENT_MINERAL`. `CategoryScorer` already emitted `ingredients/mineral` evidence for ingots/gems, but raw ores and nuggets were inconsistent — they fell through to `tech/ingots` via the `tech facets` rule (which accepts `NUGGET`/`RAW_MATERIAL`).

Fix: `FacetIndexer.applyTagFacts` now also sets `INGREDIENT_MINERAL` for `c:nuggets` and `c:raw_materials`. `netherite_scrap`'s path-based branch similarly gains `INGREDIENT_MINERAL` alongside `RAW_MATERIAL`. This makes `CategoryScorer` emit strong `ingredients/mineral` evidence for all of them, which the resolver resolves before the `tech facets` rule fires.

Items corrected:
- **raw_iron, raw_copper, raw_gold**: `tech/ingots` → `ingredients/mineral`
- **gold_nugget, iron_nugget**: `tech/ingots` → `ingredients/mineral`
- **netherite_scrap**: `tech/ingots` → `ingredients/mineral`

The fix is tag-driven (c:raw_materials, c:nuggets) so modded raw ores and nuggets using those standard common tags also benefit automatically.

### 2026-06-09: Remove ingredients/fuel subcategory; coal/charcoal → ingredients/mineral

**`ingredients/fuel` subcategory removed.**
The `fuel recipe uses` primary rule fired on any item with `ami:fuel` in `recipeUseCategories`, capturing 44 items: all 16 wool variants, all 18 signs (regular + hanging), bamboo mosaic, bamboo blocks, ladder, scaffolding, coal, charcoal, stick, and blaze rod. This was too broad — most of those items have far stronger primary identities (decorative block, building material, colored variant family).

The rule is removed entirely. Items now route naturally:
- **Coal, charcoal**: `FacetIndexer` now assigns `INGREDIENT_MINERAL` instead of `DUST`. Routes to `ingredients/mineral` via `clear ingredients before incidental equipment or tech` primary rule.
- **Stick, blaze rod**: Already had `ingredient_organic`. Route to `ingredients/organic` via the same rule.
- **Wool (16 variants)**: `placeable` only → falls through to masonry/decoration rules.
- **Signs (18 variants)**: Had `decorative_block` facet all along → `decoration facets` rule → `decoration/furniture`.
- **Bamboo blocks, bamboo mosaic, ladder, scaffolding**: `placeable` → masonry or partial-placeable rules.
- **Block of coal**: `placeable + ingot` → `tech facets` → tech/minerals.

Failed approaches to avoid: do not re-add a broad `ami:fuel` recipe-use guard. Any item that burns in a furnace is not necessarily a "fuel ingredient" — that signals only an incidental use. Use `INGREDIENT_MINERAL` or `INGREDIENT_ORGANIC` facets based on item identity.

### 2026-06-09: Vanilla Geology, Magic Structures, and Workstation Routing

**Ore blocks → geology/stone** (was masonry/full_block or tech/redstone for redstone_ore)
All blocks with the `c:ores` tag now receive a `STONE_BLOCK` facet and are intercepted by
`resolveVanillaIdentity` before CategoryScorer. This covers all normal stone-layer ores
(`coal_ore`, `iron_ore`, etc.), `redstone_ore` (which previously got ACTIVE_REDSTONE_LOGIC from
the path token "redstone"), and `ancient_debris`. Deepslate variants were already correct (they
got STONE_BLOCK from `blocksMaterial: stone`).

**Sandstone → masonry/full_block** (was geology/terrain)
`FacetIndexer.applyBlockFacts` was setting `SOIL_BLOCK` whenever `path.contains("sand")`.
"sandstone" satisfies that substring check. Fixed by excluding `path.contains("sandstone")`.
Base sand, red_sand, and suspicious_sand remain soil as intended. Smooth/cut sandstone variants
are no longer terrain.

**Enchanting table, beacon, conduit, end_portal_frame → magic/artifacts**
Previously routed to decoration/furniture (enchanting_table), decoration/lighting (beacon),
utility/misc (conduit via hardcoded UTILITY_MISC in FacetIndexer), or tech/redstone
(end_portal_frame via `eye` block state → analogOutputSignal). All four are vanilla magic
structures and belong in `magic/artifacts`. Added to `resolveVanillaIdentity`.

**Crafting table and other vanilla workstations → tech/machines**
CategoryScorer was routing crafting_table to decoration/furniture despite `MACHINE+WORKSTATION`
facets. Vanilla items with `machine+workstation+placeable` facets are now caught by
`resolveVanillaIdentity` → tech/machines. Lectern is explicitly excluded (book-stand furniture).

**Jukebox → decoration/furniture** (was tech/redstone)
`has_record` block state → `isSignalSource()` → `ACTIVE_REDSTONE_LOGIC` facets. CategoryScorer
routed to tech/redstone. Added explicit vanilla identity in `resolveVanillaIdentity`.

**Monster spawner → bestiary/hostile** (was masonry/full_block fallthrough)
Added explicit vanilla identity in `resolveVanillaIdentity`.

### 2026-06-08: Snowballs Route as Thrown Ammo

`minecraft:snowball` was the only `projectile` item that still reached
`fallback:unknown` when no lexical ammo context existed. It now matches the
projectile context by `SnowballItem` class and resolves to `tools/ammo` through the
existing `tools and weapons` primary rule, preserving the existing projectile
family behavior.

`PrimaryCategoryResolver.resolve` is the entry point for final item routing.
Each assignment now writes:

- `classificationRoute`: compact input-to-output route trace.
- `classificationRoutePhase`: final gate, such as `hard_identity`,
  `semantic_verb`, `evidence_strong`, `primary_rule`, `evidence_fallback`, or
  `fallback`.
- `classificationRouteRule`: final rule or scorer id.
- `compatCategoryPolicy`: resolved family policy when the item has a compat
  family.

Keep this trace as the visual language for tree/routing debugging. If a new
compat policy changes a final category, it should become a named route phase or
rule instead of being hidden inside enrichment, facet indexing, or ad hoc score
changes.

## Ownership Rules

Family ownership can come from:

- Exact namespace: `cobblemon:*`, `create:*`.
- Known addon namespaces.
- Mod metadata or description that clearly names the ecosystem.
- Creative tab identity that clearly names the ecosystem.
- Package or class identity that clearly names the ecosystem.

Family ownership must not come from:

- Item path terms alone, such as `berry`, `mega`, `press`, `casing`,
  `andesite`, `brass`, `sword`, or `cell`.
- Tags from another mod alone. A tag such as
  `cobblemon:held/container_held_items` means Cobblemon references the item; it
  does not mean Cobblemon owns the item.
- Recipe category participation alone.

Path terms and tags may refine item kind after ownership is already known.

Single-token ownership terms must be near-unique family names. Ambiguous tokens
such as `press`, `casing`, `cell`, `drive`, `gear`, `plate`, `core`, `module`,
`terminal`, `controller`, `cable`, `pipe`, `tank`, `berry`, `gem`, `stone`,
`map`, `claim`, `waypoint`, `badge`, `mega`, `poke`, `apricorn`, `kinetic`, or
`brass` are never family ownership evidence. They may be used only in anchored
phrases or as refinement after ownership is already established.

## Compat Policy Defaults

The desired long-term config shape is a per-family category policy:

```text
compat.<family>.categoryPolicy = focused | semantic | hybrid
```

Policy meanings:

- `focused`: owned items are kept together in the compat category when possible.
- `semantic`: owned items stay in normal ontology categories; family remains
  searchable/filterable metadata.
- `hybrid`: strong family-specific gameplay identities route to the compat
  category; generic materials, tools, blocks, and foods stay semantic.

Recommended defaults:

- Cobblemon: `focused` or `hybrid` leaning focused. Pokemon-specific items such
  as Poke Balls, medicine, berries, apricorns, evolution items, held items, and
  Pokemon machines belong under `Cobblemon`.
- Create: `hybrid` or `semantic`. Kinetic/contraption machinery can be
  Create-focused, but swords, ingots, ores, sheets, and generic materials should
  remain semantic.
- AE2, Mekanism, GregTech: likely `hybrid`.
- Sophisticated Storage/Backpacks: likely `focused` for backpacks and upgrades,
  semantic for any generic materials or ordinary blocks.
- Map mods use one general `mapping` family/category rather than separate
  Xaero/JourneyMap/FTB categories. Future indexing should target map items,
  markers, claims, death points, dimensions, and sharing/state rather than
  ordinary vanilla map items. Runtime waypoint nodes are a separate semantic
  case and should route to `World > Waypoints`, not `Mapping > Waypoints`.

## Known Bad Patterns

Do not reintroduce these:

- `omega` matching `mega` via substring and claiming Cobblemon.
- Vanilla shulker boxes becoming Cobblemon because Cobblemon has held-item tags
  for containers.
- Vanilla andesite becoming Create because Create uses andesite.
- AE2 processor presses becoming Create because the item path includes `press`.
- GTCEu casings becoming Create because the item path includes `casing`.
- AE2 name/processor presses becoming Create because the path or class includes
  `press`.
- Farmer's Delight berry foods becoming Cobblemon because the item path includes
  `berry`.
- FTB Quests becoming Mapping. FTB Chunks belongs to mapping/claims; FTB Quests
  does not.
- Storage capacity tables for modded containers. Storage sizes can be changed by
  mod configs, datapacks, KubeJS, or upstream updates. AMI should use item/block
  capabilities, container components, or other runtime APIs. If runtime evidence
  is unavailable, leave capacity unknown instead of guessing from an item id.

## Decision History

### 2026-05-31: Token Matching For Family Terms

Changed family themed terms from arbitrary substring matching to normalized token
or phrase matching. This fixed `omega` matching `mega`.

### 2026-05-31: Tags Are Not Ownership

Removed family ownership scoring from foreign mod tag prefixes. This fixed
vanilla shulker boxes with Cobblemon held-item tags being classified as
Cobblemon.

### 2026-05-31: Item Paths Are Not Ownership

Removed item-path-only ownership scoring from the family detector. This fixed
Create/Cobblemon false ownership for ordinary item names such as `andesite`,
`press`, `casing`, and `berry`.

### 2026-05-31: JEI Optional Recipe Layouts Fail Closed

AMI now checks JEI category handling before asking JEI to create a recipe layout
for transfer probing. Unhandled optional recipe layouts are treated as
non-transferable instead of producing scary JEI error logs during tooltip
rendering.

### 2026-05-31: Resolver Routes Are First-Class Diagnostics

Added `CategoryRouteDecision` and `CategoryRouteTrace` so final category
assignments expose the route gate and winning rule. This is intentionally the
place for future compat category policy; optional compat enrichment should not
silently override ownership or semantic category decisions elsewhere.

### 2026-05-31: Compat Category Policy Layer

Added a config-backed policy resolver for `focused`, `semantic`, and `hybrid`
compat category behavior. Defaults preserve current behavior: Cobblemon is
focused, Create and the planned major compat families are hybrid. Cobblemon
identity routing now consults this policy before claiming the top-level
Cobblemon category.

### 2026-05-31: Major Compat Category Shells

Added starter top-level ontology categories for Create, AE2, Mekanism,
GregTech, MineColonies, Apotheosis, Botania, Sophisticated Storage/Backpacks,
and a general Mapping category. These categories intentionally start with empty
match patterns; routing into them should happen through explicit compat identity
rules and policy, not broad lexical matching.

### 2026-06-08: Runtime Waypoints Route To World

Live waypoint result nodes are now classified as `environment/waypoints`, which
renders as `World > Waypoints`. This keeps runtime waypoints out of the generic
mapping bucket while leaving other map-style compat items free to use mapping
ownership and subcategories where appropriate.

### 2026-06-08: Runtime Waypoints Normalize To Waypoints And Lodestones Stop Pretending To Be Claims

Runtime waypoint providers now normalize every live waypoint node to
`environment/waypoints` at the node-construction boundary, even when a provider
forgets to stamp ontology metadata or tries to use a provider-specific
subcategory like `waystones`. This keeps the result tree stable at
`World > Waypoints` for Xaero, JourneyMap, FTB Chunks, Waystones, and manual
runtime waypoint nodes.

Vanilla `minecraft:lodestone` no longer emits the `social_claims` facet or
routes to `social/claims`. Its stable semantic role is navigation, so it now
uses the existing utility navigation path instead of creating a one-item
`Public Server Claims` subgroup under Social.

### 2026-05-31: Ownership Terms Must Be Family Names

Tightened Create ownership to the family name itself, known namespaces, or known
addons. Broad themed terms such as `press`, `casing`, `andesite`, `brass`, and
`kinetic` can classify an already-owned item kind later, but they do not claim
family ownership. Narrowed mapping ownership to actual map/claim namespaces
instead of all FTB namespaces.

### 2026-05-31: Ambiguous Terms Are Explicitly Guarded

Family detector ownership terms are now separated from ambiguous refinement
terms. The detector refuses ambiguous ownership terms in its configured
ownership sets, and tests cover common ambiguous single tokens so future compat
families do not regress into broad substring/token ownership.

### 2026-05-31: Create Facts Before Create Names

Create compat now enriches confirmed Create-family items with behavior facts
before routing: kinetic package/class evidence, Create tags, Create processing
recipe roles, FE metrics, and Ponder resource presence where available. Hybrid
policy routes only strong Create gameplay kinds (`kinetics`, `machines`,
`logistics`, `trains`, `contraptions`, `fluids`) to the top-level Create
category; materials, tools, and building/decor stay semantic unless the user
selects focused policy.

### 2026-05-31: Recipe Participation Is Not Machine Identity

Create processing recipe roles are searchable facts, not ownership or machine
identity by themselves. A sheet, coin, dust, food, or decorative block that is
made by pressing/cutting/crushing should not land in `Create > Machines` unless
it also has stronger machine evidence such as kinetic behavior, logistics,
contraption behavior, fluid machinery, or machine-like FE/block-entity traits.
Create metadata detection also requires addon/integration phrases instead of
the bare word `create`, because mod descriptions commonly use it as a verb.

### 2026-05-31: AE2 Facts Before AE2 Buckets

Added AE2 enrichment as facts/kinds rather than broad string routing. Exact AE2
namespaces establish family ownership, then AE2-specific path/class/tag facts
classify network, storage, terminals, crafting, channels, spatial, materials,
tools, and building. Hybrid policy routes the AE2-native systems to the top-level
AE2 category while leaving quartz tools, sky-stone block shapes, crystals, and
processor materials in semantic categories.

### 2026-05-31: Mekanism Energy Is Not Generic Machinery

Added Mekanism enrichment after the Create false-positive fix. Exact Mekanism
namespaces own the family, then Mekanism facts classify machines, energy,
chemicals, logistics, upgrades, tools, gear, and materials. Hybrid policy routes
Mekanism-native systems such as machines, QIO/logistics, energy containers,
chemical content, and upgrades to the top-level Mekanism category, while Meka
tools, armor, dusts, ingots, gems, and other strong semantic identities remain
discoverable in normal ontology.

### 2026-06-09: Facetless Mekanism Workflow Tools Stay Together

The AMICompat dump showed three Mekanism workflow items still scattered into
`utility/tools`: `dictionary`, `dosimeter`, and `configuration_card`. They are
Mekanism-owned operator tools with no stronger generic gameplay identity, and in
one case (`configuration_card`) the compat enricher was not even recording the
existing tool fact.

Mekanism compat now recognizes `ItemConfigurationCard` / `configuration_card` as
tool facts. Hybrid Mekanism identity also treats `mekanismItemKind=tools` as
focused only when the item does not already expose a concrete generic semantic
role such as harvest tool, melee/ranged weapon, utility tool, armor, or food.
This keeps facetless workflow tools grouped under `Mekanism > Tools` while
energy tools and other strongly semantic equipment still stay in their normal
ontology buckets.

### 2026-05-31: Sophisticated Storage Is Focused Storage Identity

Added Sophisticated Storage/Backpacks enrichment using exact namespaces and
class/tag evidence. Backpacks, storage blocks, upgrades, and filter upgrades
route to the top-level Sophisticated category under hybrid policy because those
items are primarily meaningful inside that mod family. Utility leftovers from
Sophisticated Core, such as generic buckets/tools, stay semantic unless the user
chooses focused policy.

### 2026-05-31: Recognized Create Components Should Not Stay Misc

The Create pass intentionally stopped treating recipe participation as machine
identity, but the large addon dump showed many facetless, strongly Create-owned
items falling all the way to `misc/unknown`: precision mechanisms, propellers,
whisks, cannon breech parts, addon drills, scanners, tires, sprayers, and tanks.
Create enrichment now records path/class facts for Create-specific components,
materials, tools, fluid handling, and addon machine parts. Hybrid policy still
lets strong semantic items route normally, but if no semantic route exists,
recognized Create kinds fall back to the relevant Create bucket instead of misc.

### 2026-05-31: Compat Kind Fallback Is Shared

The Create-only `misc/unknown` fallback was generalized for recognized AE2,
Mekanism, and Sophisticated kinds. The fallback still runs after strong
semantic rules and scorer evidence, so swords, ingots, stone variants, storage,
and food keep normal ontology routes. It only catches owned compat items that
already have a family-specific kind but no better semantic route, such as AE2
paint balls, Mekanism alloys, and facetless addon Create components.

Ambiguous path terms should stay token/phrase based even inside compat
enrichment. AE2 processor press detection now uses path tokens and specific
phrases instead of arbitrary `press` substring matching.

### 2026-05-31: Cobblemon Addon Components Use Pokemon Buckets

The addon dump showed legitimate Cobblemon ecosystem items stuck in
`cobblemon/misc`: Poke Ball lids/bases/mechanisms, incomplete ball parts,
apricorn halves/bits, potion buckets, and Mega Showdown rings/orbs/candy. These
are now classified as existing Cobblemon buckets (`poke_balls`, `apricorns`,
`medicine`, `utility`, `consumables`, or `held_items`) instead of adding new UI.
This keeps Create+Cobblemon hybrid addons owned by Cobblemon when the item is a
Pokemon gameplay object, while Create facts can still annotate mechanical parts.

### 2026-05-31: GregTech Is Isolated by Default

GregTech/GTCEu produces enough materials, components, machines, and variants to
hide signal from other compat work. GregTech now defaults to focused routing and
all owned GregTech-family items route to the top-level GregTech category unless
the policy is explicitly set to `SEMANTIC`. The subcategory classifier is coarse
on purpose: machines, multiblocks, power, circuits, materials, tools, covers, or
misc. This is an isolation pass, not final GregTech ontology work.

### 2026-06-04: GregTech Work Utility Tools Stay With GregTech

GregTech work utility tools such as screwdrivers, wire cutters, and mortars are
primarily GregTech workflow objects rather than broadly useful vanilla-style
tools. Under the default focused GregTech policy they remain in
`gregtech/tools`. Items with concrete general gameplay roles, such as drills,
saws, chainsaws, weapons, food, and armor, may still use semantic categories.
Setting the GregTech policy to `SEMANTIC` remains the explicit opt-out.

### 2026-05-31: Apotheosis Modules Route Together

The dump showed `apothic_enchanting` content outside the Apotheosis family, so
bookshelves and tomes were scattered into masonry, armor, tools, or blank
groups. `apothic_enchanting` and `apothic_spawners` now claim the Apotheosis
family, and Apotheosis-family items route to existing buckets: enchanting, gems,
sockets, affixes, spawners, bosses, or misc. This keeps the module ecosystem
together without adding new UI.

### 2026-05-31: Cobblemon Misc Is Only For True Oddballs

After addon part routing, the remaining Cobblemon misc group was mostly
manufacturing vocabulary: tumblestone lids, stamped lids, apricorn punches,
tumblestone dust/coatings, addon candy ores, EXP quartz blocks, and Mega
Showdown pedestals. These now use existing Cobblemon buckets (`poke_balls`,
`archaeology`, `building`, or `decor`). Internal placeholders like
`pokemon_model` can remain misc.

### 2026-05-31: Botania Gets Focused Buckets

Botania exact ownership, plus common addon namespaces (`mythicbotany`,
`botanicalmachinery`, `extrabotany`), now route to the Botania top-level
category unless the policy is explicitly `SEMANTIC`. The first pass keeps the
existing buckets only: mana, generating flowers, functional flowers, runes,
baubles, tools, materials, and misc.

### 2026-06-01: Exact Token Fallbacks For Lexical Routing

Added `PathTokens` for low-confidence lexical fallback. Fallback string evidence
should use exact normalized tokens or phrases, not arbitrary substrings, so
`gear` does not match `gearbox` unless the rule explicitly asks for `gearbox`.
Facet/component/tag/class facts still outrank names. Remaining raw
`path.contains(...)` family-prior blocks in `PrimaryCategoryResolver` are
tracked as TODO work and should migrate to facets/facts first, then
`PathTokens` exact token/phrase sets with false-positive tests.

### 2026-06-01: Refined Storage Uses Storage-Family Tech Identity

Refined Storage blocks with weak runtime facets, such as grids, importers,
exporters, storage monitors, disk manipulators, and storage blocks, were falling
through to the generic no-recipe terrain rule. Storage-family technical identity
now wins before fallback scoring for exact storage-family namespaces, while
still assigning normal semantic `tech` buckets. Storage media and housings route
to `tech/parts`, processors and cores to `tech/circuits`, quartz-enriched iron
to `tech/ingots`, and placeable network devices to `tech/machines`.

### 2026-06-02: Food-Family Cooking Vessels Beat Incidental Tool Identity

Farmer's Delight skillet exposed only `placeable,has_block_entity` in runtime
metadata, so it fell through to the generic masonry full-block fallback. It can
also carry combat/tool evidence, but its primary gameplay role is a cooking
vessel placed on kitchen blocks. Food-family placeable block entities with exact
cooking-station identity (`skillet`, `stove`, `cooking_pot`, or matching
item/block classes) now route to `tech/machines` before hard tool identity.
Prepared food blocks still route to meals, crop/storage blocks still route to
nature, and decorative textiles still route to decoration.

### 2026-06-04: Concrete Facts Beat Cable And Flora Name Noise

Runtime mirror data showed vanilla saplings carrying trusted `minecraft:saplings`
tags and vanilla conduits carrying a real `ConduitBlock` class while also having
path words that looked like flora or cables. Saplings now route to
`nature/seeds` from sapling identity/tag evidence, while leaves remain
`nature/flora`. Vanilla conduits route through concrete `ConduitBlock` class
evidence to `utility/misc`.

Path-only cable words no longer create concrete `ItemFacet.CABLE` facts. They
remain lexical evidence for scoring, while actual cable facets must come from
trusted tags, classes, capabilities, or other concrete runtime facts.

Create-family focused tech subcategory routing now follows the same rule:
`tech/cables` in the Create compat branch requires `ItemFacet.CABLE`. Generic
path-only cable/wire/tube words can still participate in evidence scoring, but
compat-owned subcategory shortcuts should not reintroduce raw cable-name
decisions.

### 2026-06-04: Remaining Family Priors Use Exact Tokens

The remaining high-risk family-prior helpers in `PrimaryCategoryResolver` now
use facts first and exact `PathTokens` fallback instead of raw
`path.contains(...)` checks. This specifically covers GregTech, Apotheosis,
Botania, storage or automation families, portable-storage upgrades, food-family
ingredient intermediates, Create Enchantment Industry experience items, and the
geology or masonry fallback blocks.

The practical guardrail is partial-word false positives. `powerless_ore` should
not become GregTech power, `socketed_tablet` should not become Apotheosis
sockets, `bandolier` should not become a Botania bauble, `keyboard` should not
become storage tech because it contains `key`, and stone names like
`keyboardite` or `windowseat_basalt` should still be allowed to route through
their actual geology facts.

### 2026-06-04: Reactive Bottles Expose Overbroad Terrain Fallback

The Reactive dump showed `reactive:soul_bottle` and related power bottles
routing to `geology/terrain` because they were placeable, no-recipe blocks with
only weak facets. The fix is not an item-id override: the terrain fallback now
requires actual terrain-like material/facets (`soil` or `stone`) and refuses
custom blocks with stronger semantic facts such as block entities, lighting,
redstone, utility, magic, tech, or decoration.

Facet/evidence extraction now also understands reusable signals that Reactive
exposed: non-food bottle/flask item classes become utility containers,
`power_bottles` tags and `PowerBottle*` classes become magic artifacts,
crucible/plinth classes become workstation-like machines, symbol classes become
magic artifacts, and active/powered/charged/enabled block-state properties
contribute redstone facts. Alchemy-like recipe categories such as
`transmutation`, `dissolve`, and `brewing` now provide fallback magic evidence
for item-only materials.

### 2026-06-04: Generic Shards Are Material Evidence, Not Magic Facts

Quark glass shards exposed a stale `magic_reagent` facet because the facet
indexer treated any registry path token `shard` as magic. That is too broad:
shard is a material shape word unless a concrete magic fact is also present.
Generic `shard`/`shards` tokens now contribute lexical mineral-ingredient
evidence instead of concrete magic facets. The vanilla Ingredients creative tab
adds weak ingredient evidence, strong enough to combine with shard material
evidence and rescue old dumps, but too weak to classify items by itself.

### 2026-06-04: Entity Armor Slots Are Not Player Armor Slots

Modded entity equipment can expose humanoid-looking equipment slots even when
the stack is not wearable by a player. Modular Golems dog golem armor reports a
`chest` equipment slot through its item class, but the concrete owner is a
golem/dog armor item, not a player chestplate. Facet extraction now treats known
entity armor item classes and vanilla animal armor as non-player armor, keeping
them in the armor family without promoting them to `armor_chest`.

### 2026-06-04: Modular Golems Uses Mod-Owned Equipment Vocabulary

Modular Golems exposes many item families whose meaning comes from its own
classes and tags: `GolemPart`, `GolemHolder`, `GolemFacade`,
`modulargolems:parts`, `modulargolems:holders`, and golem-scoped Curios tags
such as `curios:golem_skin` and `curios:golem_route`. Those Curios tags are
not player wearable armor by themselves. A focused Modular Golems compat
enricher now writes golem item-kind/fact metadata, routes confirmed golem-only
equipment to existing semantic AMI categories, and supplies collapse metadata
for generated `/variant/` part, holder, and facade families. Twilight Forest armor
compatibility classes now also resolve to confirmed golem-armor identities when
their compat-material armor classes and slot paths match the expected pattern.

### 2026-06-04: Partial Placeables Are Decorative Building Elements

Partial-shape placeables such as hedges, posts, ladders, panes, rods, and
similar micro blocks should not fall through to full masonry block routing, and
their exact path tokens should not be reused as generic furniture categories.
The partial-placeable resolver now keeps confirmed functional partials in tech
and nature partials in nature, while plain decorative/micro partials route to
`decoration/other_building`.

### 2026-06-04: Rechiseled Blocks Collapse By Mod-Owned Material Tags

The Rechiseled runtime dump contained 3,628 item nodes, almost all generated
decorative block variants. Primary categories were mostly already semantic
(`masonry`, `nature/wood`, `geology`, `decoration`, and related buckets), but
3,463 rows had no `collapseFamily`, leaving large category branches noisy.
Rechiseled exposes stable mod-owned material tags such as
`rechiseled:acacia_planks`, `rechiseled:cobblestone`, and
`rechiseled:stone`; shape tags such as `_stairs` and `_slabs` are incidental.

A reusable generated-palette collapse helper now writes default-collapsed UI
families while preserving existing semantic routing. Rechiseled feeds it
mod-owned base material tags; Rechiseled: Create feeds it path roots for
connected generated sets such as window families. This is intentionally
compat-scoped because the evidence comes from generator vocabulary and repeated
classes, not a universal Minecraft API fact. The lone `rechiseled:chisel` is
also tagged as a utility tool instead of falling through to unknown.

### 2026-06-04: Chipped Blocks Collapse By Mod-Owned Material Tags

The Chipped AMICompat dump contained 6,973 item nodes. Nearly all are generated
placeable block variants, and the repeated evidence is Chipped-owned family tags
such as `chipped:andesite`, `chipped:mud`, `chipped:oak_planks`, and
`chipped:white_wool`. These variants may route to different semantic buckets
(`masonry`, `geology`, `nature/wood`, `decoration`, and related categories),
but they still share a stable generated-palette family.

Chipped now uses the generated-palette collapse helper to write
default-collapsed `collapseFamily` and `collapseLabel` metadata from those
mod-owned base tags while preserving semantic category routing. Shape-only tags
such as `_stairs`, `_slab`, `_wall`, `_pane`, `_button`, and
`_pressure_plate` are ignored as family roots. The five Chipped workbench items
that were previously falling through to unknown initially routed to
`utility/misc` from `WorkbenchItem` class metadata; see the 2026-06-06 Utility
cleanup entry for the later tool-bucket refinement.

### 2026-06-05: Book And Guidebook Utility Header

Book and guidebook facets now route to `utility/books` instead of the generic
`utility/misc` fallback. This keeps ordinary books, manuals, lexicons, codices,
and inferred guidebook candidates visible under a specific Utility header while
leaving enchanted books in `magic/books` through the higher-priority magic rule.

### 2026-06-06: Utility Header Misc Cleanup

The AMICompat runtime dump showed several families entering `utility/misc` from
stale generic signals. Book and guidebook class/path facts now emit concrete
book facets without also carrying `utility_misc`, so they route to
`utility/books`. Bookshelf/bookcase/shelf/rack placeables are decorative display
blocks, not book items, and now receive decorative facets so generated
bookshelves route to `decoration/furniture` while preserving their collapse
families. Generic bottle/flask lexical and class evidence now yields to stronger
food or magic facets, keeping milk bottles in `nature/drinks` and bottles of
enchanting in magic while plain glass bottles remain utility containers.
Otherwise-unclassified Chipped `WorkbenchItem` selectors now fall back to
`tools/utility`, matching similar chisel compat behavior, while book-shaped
selectors can still use `utility/books`. Medical path matching now uses exact
tokens so names such as `splinterspawn` no longer produce the
`utility_medical` facet. Vanilla and modded `SaddleItem` classes now route to
`armor/animal` as animal equipment instead of generic Utility misc, and the
legacy ontology keyword hint moved with them.

### 2026-06-06: Edible Brewing Reagents Stay Searchable

Spider Eye keeps the cross-cutting `edible` facet for `?edible` and
`?fact:edible` searches, but its primary identity now routes to
`magic/reagents` when edible magic-reagent facts also have brewing or
spider-eye context. This keeps food search useful without forcing a potion
ingredient into `nature/snacks`.

### 2026-06-06: GuideME Guidebook Metadata

The AMICompat dump showed `ae2:guide` and `guideme:guide` as guide-book
candidates without a concrete GuideME book/open contract. `GuideItem` classes
now participate in guide-book facet detection, and AE2 compat writes
`guideBookSystem=guideme` plus `guideBookId=ae2:guide` for the AE2 guide item.
GuideME markdown pages are indexed as deferred guide documents and use the
installed GuideME `PageAnchor` opener contract for page-exact opening.

### 2026-06-06: Tomes Are Not Inferred Guidebooks

Apothic Enchanting tomes are enchantment-storage/apply items, not guidebooks.
The global path rule no longer treats `tome` as a guidebook token. Tomes still
receive the normal `book` facet for search/grouping, while actual guide terms
such as guide, manual, handbook, lexicon, codex, journal, compendium, and
chronicle remain guidebook candidates.

### 2026-06-06: Modonomicon Guide Documents

Spectrum's guidebook and handbook/cookbook items are backed by Modonomicon
resources under `data/<mod>/modonomicon/books`. AMI now indexes Modonomicon
entries as deferred guide documents, resolves normal `lang/<locale>.json`
translation keys for titles and body text, records referenced item icons/page
items, and opens guide hits through Modonomicon's `BookAddress`/`BookGuiManager`
entry screen contract. Modonomicon item stacks and Spectrum cookbook-style items
now carry `guideBookSystem=modonomicon`, a concrete `guideBookId`, and a native
`guideBookPageId` when the item exposes a `BookAddress`.

### 2026-06-06: GuideME Client Assets And Silent Gear Material Book

GuideME pages such as AE2's guide live under client assets
`assets/<mod>/ae2guide`, not server datapack resources. AMI now reads GuideME
markdown from the client resource manager so AE2 pages like Processors index
front-matter item references such as `ae2:silicon` and open to the GuideME page.

Silent Gear's `guide_book` item is not a page-backed in-game guide in
Silent Gear 4.2.1.1; it sends a wiki link and uses `guide_book.unimplemented`
tooltip text. AMI now treats the actual in-game `material_book` as the
page-backed Silent Gear guide source by indexing `silentgear_materials` as
openable material-book documents with `guideBookSystem=silentgear_materials`.

GuideME open actions now resolve the requested markdown-derived page id against
GuideME's runtime page list before opening. This handles AE2's rooted page ids
such as `ae2:ae2guide/items-blocks-machines/fluix_crystal` when AMI's indexed
page id is `items-blocks-machines/fluix_crystal`.

Patchouli guide indexing now merges server datapack resources with client asset
resources and resolves client lang keys. This covers Cobblepedia's mixed layout:
`data/cobblepedia/patchouli_books/cobblepedia/book.json` plus localized entries
and categories under `assets/cobblepedia/patchouli_books/cobblepedia/en_us/`.
Patchouli guide documents are still indexed for diagnostics/page counts, but
guide search results now ask Patchouli's live `BookEntry` whether the entry is
locked, hidden, or addable before showing it. This lets advancement-gated books
such as Nature's Aura sync search/openability with the player's current unlock
state. Untranslated Patchouli keys now fall back to readable labels instead of
leaking raw keys such as `item.cobblemon.master_ball`.
Guidebook item variants now set `variantCollapseMode=never` after creative
variant metadata is merged. Patchouli guide items are runtime variants of the
single base item `patchouli:guide_book`; when four or more books are present
they used to trip the generic high-cardinality variant collapse even though each
variant is a distinct guide, not a color/material variant.

### 2026-06-06: TConstruct Utility And Book Compat Triage

The AMICompatForge TConstruct dump had 3,362 item nodes with only ten
`fallback:unknown` rows. The existing modular gear compat remains the right
owner for TConstruct tools, parts, modifiers, stations, and generated gear
variants. Creative-slot variants now use the TConstruct `CreativeSlotItem`
class as focused modifier evidence, while TConstruct gadget/fluid classes and
the `tconstruct:throwable` tag emit concrete utility, projectile, ranged, or
fluid-container facets instead of falling through. The one-off `venombone`
material gets an organic-ingredient facet under the TConstruct namespace rather
than broadening global bone-name matching. TConstruct guide items were
already indexing under `utility/books` with `guideBookSystem=mantle_book`,
concrete `guideBookId`s, and `variantCollapseMode=never`; regression coverage
now locks that book contract. Guidebook item tooltips are also opted into the
bounded tooltip-search token index even when general tooltip indexing is
disabled so localized subtitles/authors such as TConstruct's "reference book"
and "Selena" text can find the book item.
TConstruct Mantle books also use language-scoped generated page files loaded by
book-specific `extraData` processors rather than direct section references. The
Mantle guide source now indexes unreferenced language-scoped page JSON under a
book after the explicit section pass, preserving referenced section pages while
making generated TConstruct tool/material/modifier pages searchable.

### 2026-06-06: Guidebook Facets Beat Generic Tool Identity

The AMICompatForge dump showed Immersive Engineering's `ManualItem` carrying
`book` and `guide_book` facets but routing to `tools/utility` because the same
item also had a generic `utility_tool` facet. Guide-book facts now hard-route
to `utility/books` before the generic tool hard identity. Enchanted books still
route to `magic/books`, and functional non-guide tomes/grimoires keep their
stronger mod or curio identities.

### 2026-06-07: Silent Gear Material Book Native Opening

The ATM10 dump showed `silentgear:material_book` carrying `book,guide_book` and
`guideBookSystem=silentgear_materials`, but modular-gear compat also marked it
as a focused `parts` item. Silent Gear guide-book facts now avoid assigning a
modular-gear item kind, so the material book routes through the normal
`utility/books` guide-book identity. The item context-menu documentation action
also recognizes Silent Gear's material-book system and opens the native
material-book screen, while material search-result tooltips split categories,
stats, and traits into colored scan lines.

### 2026-06-06: Guide Content Search Defaults To Summaries

Guide documents were indexed separately from normal item nodes, and the default
guide indexing mode only searched document titles, chapters, tags, book ids, and
referenced items. The default and reset config now use `SUMMARY` mode so capped
page text participates in guide searches by default. `TITLES` remains available
as a lower-memory option. Immersive Engineering manual items now also report
`guideBookSystem=immersiveengineering_manual`, matching the existing IE manual
text parser. Hexerei's `Book of Shadows` pages are now parsed from
`data/hexerei/book/book_entries.json`, `book_pages`, and lang-backed
`passage_text` keys as `hexerei_book` guide documents.

### 2026-06-06: TacZ Attachments Are Focused Weapon Upgrades

The AMICompatForge dump showed `tacz` had 186 item nodes and 95
`fallback:unknown` rows. Every repeated fallback row was a generated
`com.tacz.guns.item.AttachmentItem` variant under `tacz:attachment/variant/...`.
TacZ guns (`ModernKineticGunItem`) already routed to `tools/ranged`, and ammo
already routed to `tools/ammo`, so the missing evidence was specific to TacZ's
attachment API rather than a global ranged-weapon rule.

AMI first treated TacZ `AttachmentItem` rows as `taczItemKind=attachments` with
an `upgrade` facet and a hard identity route to `tech/upgrades`. Generated
attachment variants shared `collapseFamily=tacz:attachment`,
`collapseLabel=Attachments`, and `variantCollapseMode=default_collapsed`.

TacZ then moved to a focused top-level category. The mod's guns, ammo,
attachments, and workstations are self-contained gun-system content and are not
meaningfully consumed by vanilla or unrelated mod items. AMI now routes
`taczItemKind=guns|ammo|attachments|workstations` to
`tacz/guns`, `tacz/ammo`, `tacz/attachments`, and `tacz/workstations`. Addon
namespaces using `com.tacz.guns.*` item classes are treated as TacZ-family
content by the family detector.

### 2026-06-06: MNA Construct Parts, Motes, Patches, And Runes

The AMICompatForge MNA dump showed 1,028 item nodes with hundreds of
`fallback:unknown` rows. The largest repeated fallback patterns were MNA-owned
classes and tags: `com.mna.items.constructs.parts.*`, `com.mna.items.ritual.Mote`,
`com.mna.items.ritual.PractitionersPatch`, rune classes, and `mna:lesser_motes`,
`mna:greater_motes`, `mna:runes`, and `mna:stone_runes`. These are focused
Mana and Artifice API facts, not global lexical evidence.

AMI now records `mnaItemKind` and `mnaFacts` for those families. Construct
parts route by hard identity to `tech/parts` and default-collapse under
`Construct Parts`. Practitioner patches route to `magic/artifacts` and
default-collapse under `Practitioner Patches`. Motes and runes route to
`magic/reagents`. This first pass deliberately avoids a broad
`manaweaving-recipe-type` rule because MNA uses those recipes for heterogeneous
items including tools and equipment.

The next MNA slice keeps the same constraint and adds class/path-owned facts for
known artifice and sorcery items. MNA mana gems, vellum, animus dust, and sight
unguents route to `magic/reagents`; faction horns, patterning prism, ender disk,
ledger/manifest, healing poultice, thaumaturgic link, transitory tunnel, and
similar artifice/relic classes route to `magic/artifacts`; exact MNA material
paths such as Vinteum ingots/coated iron, runic/infused silk, witherbone, and
ironbark route to `ingredients/mineral`; runesmith tools route to
`tools/utility`; MNA weapon classes route to `tools/melee`; and magic brooms
route to `tech/transport`.

### 2026-06-07: MNA Staves, Relics, Utility Badges, And Dusts

The AMICompatForge MNA dump also showed repeated fallback rows for MNA-owned
spell and relic surfaces: `com.mna.items.sorcery.MagicStaff`, `mna:staves`,
`mna:wands`, `mna:generated_spell_items`, `mna:relics`,
`com.mna.items.relic.AstroBlade`, `com.mna.items.ritual.WizardChalk`, HUD badge
paths, and MNA dust/crystal tags. These are still focused MNA facts rather than
global lexical rules.

AMI now records MNA ranged weapon facts for staves/wands and routes them to
`tools/ranged`; relic melee classes such as AstroBlade route to `tools/melee`
before artifact fallback; ritual utility paths such as animated quill and wizard
chalk route to `magic/artifacts`; HUD badge paths route to `utility/misc`; and
MNA dust/crystal tags route to `magic/reagents`.

### 2026-06-07: Player Waypoint Providers Use Registry-Owned Actions

The player utility search/map action work now keeps map integrations behind
`PlayerWaypointProvider` implementations. Providers may expose copy/export data
and native add actions; the registry filters unavailable providers and wraps
provider failures so map mod breakage cannot crash the AMI context menu.

FTB Chunks uses a reflective client waypoint API from the installed NeoForge
jar. JourneyMap keeps copy/export support and only exposes native add actions
when its waypoint API classes can load in the running client. Xaero remains a
detected copy/export provider for now because the available jar exposes internal
GUI/session classes rather than a stable public add-waypoint API. A manual
coordinate provider is always registered as a copy-only fallback when exact
coordinates are known.

### 2026-06-07: Standalone Fluid Containers Get A Semantic Fallback

The AMICompatForge TConstruct dump showed `tconstruct:copper_can/variant/...`
falling through to `fallback:unknown` even though the facet indexer correctly
emitted `fluid_container`. AMI deliberately does not treat copper cans as a
focused Modular Gear item, but a standalone fluid container still needs a stable
semantic bucket. The generic utility facet route now treats `fluid_container` as
`utility/misc` when no stronger category rule has already claimed the item.
Generated subtype nodes now re-run primary category assignment after runtime
metric sniffers add facets such as `fluid_container`, so generated fluid
containers inherit the same semantic route as base items.

### 2026-06-07: Fuel Recipe Uses Route Without Mod Branches

The AMICompat NeoForge Silent Gear dump showed `silentgear:netherwood_charcoal`
as the only Silent Gear `fallback:unknown` row. The stable evidence was not a
Silent Gear item class or path family; it was recipe-use metadata containing
`ami:fuel`. AMI now routes exact `ami:fuel` / `fuel` recipe-use metadata to
`ingredients/fuel`, which also covers future modded fuels without adding
mod-specific branches.

### 2026-06-07: AppMek Uses AE2 Storage And Channel Identity

The AMICompat NeoForge dump showed all twelve `appmek` rows falling through to
`fallback:unknown`. The repeated classes were `ChemicalStorageCell`,
`ChemicalPortableCellItem`, `appeng.items.materials.MaterialItem`, and
`appeng.items.parts.PartItem`; item paths were chemical storage cells, portable
chemical cells, a chemical cell housing, and a chemical P2P tunnel. These are
Applied Energistics network/storage concepts with a Mekanism chemical medium, so
AMI now treats `appmek` as an AE2-family namespace and records AE2 storage,
channel, tier, and `chemical` medium/facts from string metadata only.

### 2026-06-07: Ars Nouveau Glyphs And Ritual Tablets

The AMICompat NeoForge dump showed 133 `ars_nouveau` fallback rows. The repeated
families were mod-owned classes: `com.hollingsworth.arsnouveau.common.items.Glyph`
for spell glyphs and `RitualTablet` for ritual tablets. Glyphs also expose a
`glyph` recipe category. AMI now treats `ars_nouveau` as a focused compat family
for metadata enrichment, routes glyphs to `magic/reagents`, routes ritual tablets
to `magic/artifacts`, and default-collapses those two large result families using
only namespace/path/class/recipe metadata.

### 2026-06-07: Spectrum Reagents, Pure Resources, And Structure Placers

The AMICompat NeoForge dump showed repeated Spectrum fallback rows backed by
mod-owned tags, classes, and recipe categories rather than general Minecraft API
facts. AMI now records `spectrumItemKind` and `spectrumFacts` for structure
placers, reagents, pastel-node upgrades, and pure resources. Structure placers
route to `utility/misc` and default-collapse under `Structure Placers`; reagents
route to `magic/reagents`; pastel-node upgrades route to `tech/upgrades`; and
pure resources route to `ingredients/mineral`.

### 2026-06-07: Nature's Aura Generated Powders And Utility Artifacts

The AMICompat NeoForge dump showed 31 `naturesaura` fallback rows. The repeated
families were mod-owned classes and namespace paths: `ItemEffectPowder`
generated variants, `ItemStructureFinder`, finder staffs, aura tokens, spirits,
the mover cart, tainted gold/gold fiber materials, and several utility/artifact
items. AMI now records `naturesAuraItemKind` and `naturesAuraFacts`, collapses
effect powders under `Effect Powders`, routes powders/tokens/spirits to
`magic/reagents`, structure and staff finders to `utility/navigation`, aura
transport to `tech/transport`, materials to `ingredients/mineral`, templates to
`tech/parts`, and remaining utility artifacts to either `magic/artifacts` or
`utility/misc` from class/path/recipe metadata only.

### 2026-06-07: Ars Nouveau Uses A Focused Compat Header

The AMICompat NeoForge dump has 425 Ars Nouveau-owned item nodes: 85 glyphs,
24 ritual tablets, many Ars workstations/source blocks, familiar scripts,
equipment, materials, and generated/building variants. Those rows were spread
across `magic`, `tech`, `masonry`, `decoration`, `nature`, and
`fallback:unknown`, even though players usually need Ars glyphs, source
workstations, and ritual pieces together. This is a mod-owned gameplay system,
not a generic magic-material heuristic.

AMI now exposes an `ars_nouveau` top-level category with subgroups for glyphs,
rituals, spellcasting, source, automation, familiars, equipment, materials,
building, and misc. The default Ars category policy is `FOCUSED`, while
`SEMANTIC` still opts out. `CompatFamilyDetector` also recognizes Ars-addon
ownership by namespace, metadata, creative tab, and Ars package names so future
addons can use the same compat path instead of resolver-specific `if` chains.
Focused tests cover glyph collapse/routing, ritual tablet routing, Ars
workstations staying under the Ars header, and semantic opt-out behavior.

### 2026-06-06: Alex's Mobs Drops, Food Tags, And Custom Tools

The AMICompatForge Alex's Mobs dump had 278 mod-owned item nodes and 73
`fallback:unknown` rows. The repeated wrong families were mod-owned facts:
plain `net.minecraft.world.item.Item` drops marked with
`alexsmobs:animal_dictionary_ingredient`, food/taming tags such as
`*_foodstuffs`, `*_breedables`, `*_tameables`, `*_offerings`, custom item
classes including `ItemEcholocator`, `ItemHemolymphBlaster`, `ItemStraddleboard`,
`ItemPigshoes`, and hidden `ItemInventoryOnly` render variants. These should
not become global lexical rules because their meaning comes from Alex's Mobs
tags/classes.

AMI now records `alexsMobsItemKind` and `alexsMobsFacts`. Animal dictionary
drops and exact untagged body-part drops route to `ingredients/organic`; mod
food/taming tags and fish tags route to `nature/proteins`; echolocators route
to `utility/navigation`; custom ranged items route to `tools/ranged`; darts and
pocket sand route to `tools/ammo`; straddleboards route to `tech/transport`;
pigshoes route to `armor/feet`; and hidden inventory-only/debug variants keep
their existing `dev`/`hidden` access metadata while resolving to
`utility/misc`.

### 2026-06-06: Alex's Caves Materials, Cave Gear, And Codex Pages

The AMICompatForge Alex's Caves dump had 639 nodes, 555 mod-owned item nodes,
and 61 `fallback:unknown` item rows. The repeated unresolved patterns were
mod-owned cave resource paths, custom classes such as `CaveInfoItem`,
`QuarrySmasherItem`, `HolocoderItem`, `ThrownProjectileItem`, `SubmarineItem`,
`RadioactiveItem`, `RadiationRemovingFoodItem`, `OccultGemItem`, and hidden
inventory-only bow/lance rows. These are Alex's Caves facts rather than general
Minecraft API facts, so AMI handles them in focused compat.

AMI now records `alexsCavesItemKind` and `alexsCavesFacts`. Cave info tablet
and codex variants route to `utility/books` and collapse under `Cave Info`;
neodymium raw/ingot resources route to `tech/ingots`; cave components such as
telecore, polymer plate, fissile core, and charred remnant route to
`tech/parts`; quarry smashers route to `tools/harvest`; submarines route to
`tech/transport`; thrown cave projectiles route to `tools/ammo`; occult/dark
reagents route to `magic/reagents`; fish and gelatin/candy foods route to
`nature/proteins` or `nature/snacks`; organic cave drops route to
`ingredients/organic`; and hidden weapon inventory variants keep their existing
`dev`/`hidden` access metadata while resolving to their weapon bucket.

Alex's Caves `books/...` resources already use the shared Alex-style guide
adapter. A focused fixture now covers Cave Codex JSON pages plus localized text
files so page body text is searchable through guide `summaryText`, with
referenced items and an `Open Guide Page` action when the runtime screen opener
is available.

### 2026-06-09: Decorative blocks skip fuel routing; candle root joins collapse family

**Signs routed to ingredients/fuel (stale dump):** Flammable-wood signs (`oak_sign` etc.) have
`recipeUseCategories: ami:fuel` because they can be used as furnace fuel. The old "fuel recipe uses"
primary rule fired before decoration rules, sending them to `ingredients/fuel` instead of
`decoration/furniture`. The fuel rule was already removed from `PRIMARY_RULES` in the current
working code, so any item with `DECORATIVE_BLOCK` facet that was incorrectly captured there now falls
through to `decoration facets` → `decoration/furniture`. All 10 wood-type signs (regular and
hanging) now land in furniture. Gold labels added for `oak_sign`, `bamboo_sign`, `oak_hanging_sign`.

**Plain candle missing from its own collapse family:** `minecraft:candle` is the uncolored root of a
colorized family (white\_candle…black\_candle). The colored candles get `collapseFamily:
minecraft:candle` via `classifyColorizedGeneratedFamily` because they have a non-empty `colorBucket`
and a `materialGroup` pointing to the root. The root itself has no `colorBucket`, so it was skipped
by the early return. Fix: when `colorBucket` is blank, detect the root case by checking that
`materialGroup == id` AND the item has a plural-form tag (`minecraft:candles` for `minecraft:candle`).
This generalizes to any vanilla colorized-root item with the same structure. Gold label added for
`minecraft:candle → decoration/lighting`.

## Open Work

- Split Cobblemon identity routing into strong Pokemon gameplay identities
  versus generic addon blocks/items.
- Add comparable policy tests for Create: Create sword stays `tools/melee`,
  Create zinc ingot stays `ingredients` or material category, Create kinetic
  machine stays `tech` or Create-focused depending policy.
- Move scattered substring checks in classification/facet code toward shared
  token utilities and explicit context gates.
- Replace remaining raw lexical category checks in `PrimaryCategoryResolver`
  family-prior helpers with structured evidence first and `PathTokens` fallback
  second. Start with GregTech, Apotheosis, Botania, storage/automation,
  food-family ingredient, and geology/masonry fallback blocks.
- Do not add new `isLikely*` gates or ambiguous path/name substring checks for
  ownership, access level, top-level category, or cheat/dev visibility. Use
  capability/component/tag/class data first; use exact compat APIs next; use
  exact registry/tag IDs after that; tokenized lexical fallback is only
  low-confidence evidence and needs a false-positive regression test.
- Update this log for every classification redesign or policy change.

### 2026-06-22: Standalone fluid containers score semantically; Tide fishing gadgets use focused compat facts

The Synesthesia dump still had a `utility/misc` bucket dominated by lexical `category_scorer` wins for items that
already had concrete container or mod-class evidence, plus a separate Tide family of fishing gadgets stuck at
`fallback:unknown`. These were two different problems.

**Standalone fluid containers now score as utility/misc from the facet itself.**
`FLUID_CONTAINER` already participates in the primary utility rule, but strong scorer evidence ran earlier and could still
pick misc from bottle/flask words instead of the concrete container facet. `EvidenceCollector` now emits
`facet.fluid_container -> utility/misc` at strong weight when the item does not also have stronger food/potion/magic
container context. This keeps plain utility containers such as large bottles, cans, and similar refillables on a
semantic route without changing edible drinks, potions, mob buckets, or magical containers that already have better
identity evidence.

**Tide fishing gadgets use focused compat class facts rather than global fishing words.**
The Tide dump had custom classes such as `FishingHookItem`, `FishingLineItem`, `DepthMeterItem`, `WeatherRadioItem`,
`PocketWatchItem`, and `FishingJournalItem` falling to `fallback:unknown`. Those meanings come from Tide's own API
vocabulary, not a Minecraft-wide standard, so AMI now handles them in focused compat. Tide hooks/lines become
`tools/utility`, Tide meters/watch/radio/calendar-style gadgets become `utility/navigation`, and journal/note items
become `utility/books`. Avoid broad lexical rules for `hook`, `line`, `meter`, `watch`, or `radio`; those are not
reliable category signals across mods.

**Pastel decay bottles extend existing Pastel compat instead of creating a new misc header.**
Inside Synesthesia's Pastel-owned `utility/misc` rows, the only clearly compat-shaped family was the five
`earth.terrarium.pastel.items.DecayPlacerItem` bottles (`bottle_of_fading`, `bottle_of_failing`, `bottle_of_ruin`,
`bottle_of_forfeiture`, `bottle_of_decay_away`). They are mod-specific progression items backed by Pastel decay block
classes/tags and pedestal-style crafting, not generic utility bottles. Pastel already has a focused compat surface, so
the fix is to extend `PastelCompat`, not create a separate top-level misc bucket. `DecayPlacerItem`, `pastel:decay/*`
block tags, and `bottle_of_*` paths now mark these items as a Pastel magic family and route them to `pastel/magic`.
Keep ordinary buckets, music discs, and the existing ink-flask pigment family semantic or compat-routed as they already
are; do not use a blanket `bottle` rule.

### 2026-06-22: Doggy Talents repeated unknowns use focused compat families

The Synesthesia Doggy Talents audit showed `62` repeated `fallback:unknown` item rows, but they were not one generic
category bug. The repeated families came from mod-owned classes: `TreatItem`, `CanineTrackerItem`, `LocatorOrbItem`,
`WhistleItem`, `ThrowableItem`, `FrisbeeItem`, `PianoItem`, and the `doggytalents.common.entity.accessory.*` package.

AMI now handles those families in focused compat rather than global pet-item heuristics. Doggy treats route to
`nature/snacks`, tracker/orb/whistle navigation tools route to `utility/navigation`, fetch toys route to
`tools/utility`, decorative piano/plush-style items route to `decoration/furniture`, and entity accessory-package items
gain `CURIO` so they resolve as `armor/curios`. Avoid global rules for words like `treat`, `tracker`, `orb`,
`frisbee`, or `flatcap`; those meanings are specific to the Doggy Talents API surface in this dump.

### 2026-06-22: Hexerei and Hexalia use focused semantic compat, not blanket witch-item lexicon

The Synesthesia audits for `hexerei` and `hexalia` both showed repeated `fallback:unknown` clusters driven by
mod-owned item classes and tags rather than a missing cross-mod fact. For Hexerei the stable vocab was broom items,
`hexerei:herbs`, `hexerei:sigils`, `DowsingRodItem`, `WhistleItem`, `CrowFluteItem`, and other broom utility classes.
For Hexalia the stable vocab was `HexFocusItem`, `WeatherIdolItem`, `PurityIdolItem`, `AthameItem`,
`ThrownSacItem`, `PurifyingSacItem`, `SpiritrootTetherItem`, `hexalia:crushed_herbs`, and `hexalia:offhand_equipment`.

AMI now handles those families with focused compat facts. Hexerei brooms resolve as transport-style tools, sigils as
`magic/artifacts`, herbs and moon-dust-style crafting items as `ingredients/organic`, and dowsing/whistle utilities as
navigation or utility tools. Hexalia foci, idols, and pendants resolve as `magic/artifacts`; thrown sacs resolve as
projectile ammo; athames as melee tools; utility sacs/tethers as utility tools; crushed-herb powders and node items as
`magic/reagents`; and resin/herb materials as organic ingredients. Avoid global lexical rules for `broom`, `idol`,
`focus`, `sac`, `sigil`, `mandrake`, or `powder`; their meaning here comes from mod-owned APIs, not standard Minecraft
semantics.

### 2026-06-13: Synesthesia full-block split and activation evidence

The Synesthesia dump showed functional full-block leftovers such as
`pastel:ender_dragon_idol` and `alexscaves:nuclear_bomb` still sitting under
`masonry/full_block` with only the `placeable` facet. The idol carried
`blockStateProperties=cooldown`; the bomb carried an activation tag
(`alexscaves:remote_detonator_activates`). Those are concrete runtime metadata
signals that the block is stateful or triggerable, not a plain decorative cube.

`FacetIndexer` now promotes activation-like block state properties
(`cooldown`, `triggered`, `armed`) and activation tags to `interactive_block`,
letting the existing primary resolver move reindexed functional blocks out of
the plain masonry fallback. Category view also splits mixed
`Building > Full Blocks` buckets into `Glass`, `Special Blocks`, and
`Plain Blocks` from metadata so older dumps with stale facets still stop mixing
glass and stateful/triggerable blocks into one flat full-block list.

Follow-up audit: do not turn every notable block property into a behavior facet.
Proof/resistance tags, WIP tags, hidden-from-recipe-viewer tags, tooltip-only
warnings, and passive material properties are not `interactive_block`,
`machine`, or `storage` evidence by themselves. Narrowed the functional full
block work to concrete runtime facts: workstation/shop block classes,
transformer/energy block classes, Tom's Simple Storage trim class/tag, real
activation state/tag evidence, and explicit vanilla identities such as
`respawn_anchor` as a magic interactive block. Removed the old
`honey_block`/`beehive`/`bee_nest`/`respawn_anchor` catch-all that treated
notable functional blocks as `machine`; beehives now get nature evidence, and
respawn anchors get magic/interactive evidence instead of machine identity.

### 2026-06-13: Main item pass uses final compat facets for primary routing

The Synesthesia 2026-06-13 dump still showed 627 item rows at
`fallback:unknown`. Many of the repeated rows already had concrete facets in
metadata, such as SWEM metal components with `tech_component,ingredient_mineral`
and Supplementaries bombs with `projectile`, but their route trace was still
`fallback:unknown`.

The bug was provider ordering, not missing facet extraction: compat hooks wrote
additional facets into `SearchNodeKeys.FACETS`, but the main item indexing pass
called `PrimaryCategoryResolver.resolve` with only the original
`FacetIndexer` result. The helper used by other paths already merged
metadata-backed facets before routing. The main pass now uses that helper, so
compat-added semantic facts participate in final ontology routing.

Items corrected after the next runtime dump:
- **swem:plate_copper / swem:rivet_copper family**:
  `fallback:unknown` -> `ingredients/mineral` via
  `clear ingredients before incidental equipment or tech`.
- **supplementaries:bomb family**:
  `fallback:unknown` -> `tools/ammo` via projectile/tool context.

### 2026-06-11: Synesthesia first-pass compat and generated variant collapse

The Synesthesia modpack dump showed two separate problems: focused mod-owned
item families falling to `fallback:unknown`, and generated creative-tab stacks
being indexed as standalone variants without stable collapse metadata or audit
coverage. The largest item compat hotspots were SWEM, Malum, Pastel,
Born in Chaos, and Cataclysm. The recipe-viewer item audit also showed JEI
base/exact-stack misses dominated by generated variants such as
`alexscaves:jelly_bean`, `malum:geas`, Domum Ornamentum panels/posts/doors, and
Sophisticated backpacks.

AMI now adds focused metadata enrichers for those Synesthesia families using
namespace, item class, and mod-owned tags rather than global path ownership.
SWEM tack/feed/currency/components, Malum geasa/spirits/impetus/augments,
Pastel structure placers/resources/pigments/drinks/upgrades, Born in Chaos
materials/charms/reagents/weapons/structure placers, and Cataclysm dungeon
eyes/materials/weapons/artifacts all receive semantic facets before primary
routing. This keeps generic materials in normal `ingredients`, weapons in
`tools`, and magic artifacts in `magic` instead of creating broad lexical rules.

Generated subtype nodes with `/variant/` now get a fallback
`collapseFamily=<subtypeOf>` when no focused compat already supplied a family.
Guide books still opt out via `variantCollapseMode=never` /
`guideBookCandidate`. The JEI item audit also falls back to `subtypeOf` and the
synthetic variant id hash when a generated node's registered icon stack is not
available, improving parity reporting for component-backed generated stacks.

Pastel, Malum, SWEM, and Cataclysm now also participate in the shared
`focused` / `semantic` / `hybrid` compat category policy model. Defaults are
`hybrid`: progression-defining content such as Pastel structure placers and
node upgrades, Malum geasa/impetus/spirits, SWEM tack/care/feed, and Cataclysm
dungeon eyes/boss artifacts can group under their own top-level categories,
while generic components, ordinary blocks, and other vanilla-like content can
still route through semantic AMI categories. This keeps Synesthesia progression
authoring coherent without making kitchen-sink packs or standalone installs
lose normal material/tool/building organization.

### 2026-06-13: Explicit Halcyon/SWEM routes beat generic item identity

The Synesthesia replay showed Datanessence/Halcyon and SWEM items carrying
explicit compat route metadata but still landing in generic headers because
`hard_identity` and strong scorer routes ran before the `compat route metadata`
primary rule. Examples included Halcyon tools/templates/armor/components
landing in `tools`, `tech`, `armor`, or `magic`, and SWEM horse tack/feed/armor
landing in `nature`, `armor`, or `tech`.

Focused compat route metadata now runs before hard identity/scorer routes, with
guide books still allowed to keep their normal `utility/books` handling. SWEM's
hybrid policy also treats its explicit `swem/*` route metadata as early because
the compat class writes those routes only for horse/stable identities, not for
generic metal components. Copper plates/rivets and similar component families
therefore continue to route semantically to `ingredients/mineral`, while horse
armor, riding gear, tack, feed, grain bins, feeders, stable jump/placeable
helpers, and care items stay under the SWEM header.

### 2026-06-14: Concrete class facets for remaining functional full blocks

The fresh Synesthesia dump still showed Full Blocks with block entities but no
more precise facet. AMI must not treat every block entity as interactive or
functional: decorative carrier blocks such as framed/dyeable variants can have
block entities purely for material/color state. The new facet pass therefore
uses concrete class and mod-owned evidence only.

Added storage facets for Tom's Simple Storage connector/interface/proxy/crate
blocks and SWEM grain bins, plus MineColonies-style racks and colony supply
depots. Added workstation/machine facets for Candlelight stove classes,
interactive/utility facets for FTB Quests screen/barrier/opener classes,
interactive/magic facets for obvious trap/boss-spawner/altar classes, machine
facets for quarry/engine blocks, and nature/magic facets for natural dens and
altar-style blocks. Halcyon/Datanessence compat now also enriches its own item
classes before choosing the Halcyon subheader: swords, bombs, utility rods,
grappling/warp tools, data drives, upgrades, and essence shards now expose
matching gameplay facets instead of landing as generic `halcyon/items`.

These are intentionally not inferred from resistance/proof tags, WIP/hidden
tags, or tooltip text. Runtime dump replay can show the compat-route impact from
stored metadata, but newly extracted block facets require a fresh in-game reindex
and `/ami dump-search-nodes` because old dumps contain the old facet strings.

### 2026-06-09: nature category fixes — rose_bush, golden_carrot, ominous_bottle

Three misrouting bugs in the `nature` category.

**rose_bush → nature/crops** (fixed → nature/flora)
`FacetIndexer.isCropLikePlaceable` used `path.endsWith("_bush")` as a crop signal.
`rose_bush` matched but is a `TallFlowerBlock` — a decorative tall flower, not a farmable bush.
Fixed by adding `if (className.contains("flower")) return false;` in `FacetIndexer.isCropLikePlaceable`
and adding a `FLOWER` facet early-exit in `PrimaryCategoryResolver.isCropLikePlaceable` so that
an incorrectly set `CROP` facet cannot win over `FLOWER`.
Failed approach to avoid: narrowing `_bush` to a specific path allowlist — that's fragile for modded
berry bushes and other legitimate farmable bushes.

**golden_carrot → nature/crops** (fixed → nature/snacks)
`FacetIndexer` had an `isProduceFoodTag` helper that set `CROP` for any item tagged `c:foods/vegetable`.
`golden_carrot` is tagged `c:foods/vegetable` but is crafted from carrot + gold, not farmable.
Fixed by removing `isProduceFoodTag` entirely. Raw farmable crops already get `CROP` from the
`c:crops` tag check (line 522) and `BlockTags.CROPS` (block items). Crafted/processed vegetable
foods no longer incorrectly inherit the crop routing.

**ominous_bottle → nature/drinks** (fixed → utility/misc)
`OminousBottleItem` has food data purely for the consumption animation; it applies bad omen, not
nutrition. Its `EDIBLE` facet caused `hasActualFoodIdentity` to win before `UTILITY_MISC`.
Fixed by adding an identity gate in `resolveHardIdentity` (before `hasActualFoodIdentity`) that
fires when `UTILITY_MISC` is set and the item carries the `c:drinks/ominous` tag, routing to
`utility/misc`. honey_bottle is unaffected — it lacks `c:drinks/ominous`.

### 2026-06-16: MC 1.21.5 Item API Migration — Armor, Swords, Bricks, Magic Drinks

**30 vanilla armor items with empty subcategory** (copper_*, chainmail_*, iron_*, diamond_*, golden_*, turtle_helmet)
In MC 1.21.4+, armor no longer extends `ArmorItem` — the equipment slot is stored in the `Equippable` data component.
`applyEquipmentFacts` used `getEquipmentSlot()` reflection which returned empty for plain `Item` armor.
Fixed by: (1) reading `DataComponents.EQUIPPABLE` component directly in `applyEquipmentFacts` as a fallback,
(2) adding `minecraft:head_armor`, `minecraft:chest_armor`, `minecraft:leg_armor`, `minecraft:feet_armor`
vanilla slot tags to `applyTagFacts`.

**All swords → tools/utility** (copper_sword, iron_sword, diamond_sword, etc.)
In MC 1.21.4+, swords are plain `Item` with `WEAPON` data component, not `SwordItem` subclass.
`applyTypeFacts` used `isInstanceOf(item, "SwordItem")` which returned false.
Fixed by: adding `minecraft:swords` and `c:tools/melee_weapon` to `isMeleeToolTag()`,
and extending the inline check at line ~655 to also match singular `melee_weapon` tag suffix.

**minecraft:resin_brick → misc/unknown**
New 1.21.5 brick ingredient. `FacetIndexer` hardcoded `path.equals("brick")` and `path.equals("nether_brick")`
but had no `c:bricks` tag check. Fixed by: adding `c:bricks` and `c:bricks/*` to `applyTagFacts` → `INGREDIENT_MINERAL`.

**ominous_bottle variants → nature/snacks** (regression from prior utility/misc fix)
Prior fix relied on `UTILITY_MISC` facet + `c:drinks/ominous` tag gate. In 1.21.5,
`OminousBottleItem` was replaced by plain `Item` with Consumable component; no `UTILITY_MISC` facet is set.
`c:foods` tag score (+70) + Food & Drinks creative tab (+45) = 115 beats any magic signal.
Fixed by: adding `c:drinks/magic` → +120 `magic/potions` trusted tag evidence in `EvidenceCollector`,
which overrides the food evidence. Routes to `magic/potions` (previously `utility/misc`).

### 2026-05-31: Filled Creative Variants Use Runtime Data, Not Names

Creative-tab representative stacks can expose both empty and prefilled variants
for energy, fluid, chemical, or similar containers. Those prefilled copies are
cheat/dev-only because they represent spawned state, not survival identity. AMI
must not infer this from item names such as `tank`, `cell`, `battery`, or
`fluid`. The index now checks platform item energy and fluid capabilities first,
then reads tooltip resource amounts as a fallback for mods whose custom resource
state is only exposed there. The result is still explainable via
`variantAccessReason=prefilled_creative_stack`.

### 2026-06-22: Witchery and Forbidden Arcanus Unknown Families Use Focused Compat Facts

`witchery` had a broad cluster of true `fallback:unknown` rows that were not one
single family: broom variants, stakes, poppets/stones/contracts, and many witch
reagents all lived together. This was a better fit for focused compat facts than
for a new global "occult words" heuristic. `WitcheryCompat` now routes brooms to
transport, stakes to melee, witch texts to books, vessels to utility/misc, and
the remaining stones/poppets/reagents to magic artifacts or reagents.

`forbidden_arcanus` was similar but dominated by one repeated collapse family:
`quantum_catcher`, plus soul/prism/artifact items and a few specialized vessels.
`ForbiddenArcanusCompat` now gives those families semantic facets so colored
catchers stop falling into `misc/unknown`, soul items score as magic reagents,
artifact-like prisms/stella/orbs score as artifacts, and the aureal tank is
treated as a utility vessel instead of a generic misc fallback.

### 2026-06-22: Eternal Starlight and Malum Unknown Families Use Focused Semantic Buckets

`eternal_starlight` had one especially clear missed family: `starlit_painting` creative
variants, plus a smaller set of accessories, eye/orb artifacts, dew/gel/petal style
reagents, and a couple item-form bricks. `EternalStarlightCompat` now collapses paintings
cleanly and routes those repeated unknowns into focused Eternal Starlight subgroups instead
of leaving them in `misc/unknown`.

`malum` already had a compat route, but several true unknown leftovers still sat outside its
existing geas/impetus/spirit/material logic. The compat now also recognizes sapballs as
organic materials, weaves/spools as a focused weave subgroup, tool curios like the catalyst
lobber and lamplighter's tongs as equipment, and nucleus/lens/poppet style curios as Malum
artifacts.

### 2026-06-22: Power Grid and Enigmatic Legacy Plus Unknowns Stop Falling Through

`powergrid` had a very repeatable set of electronics parts and utility devices that were
all still reaching `fallback:unknown`: cords, coils, resistors, diodes, bulbs, gizmos,
the multimeter, the electro-zapper, and punch cards. `PowerGridCompat` now treats the
circuit/electrical pieces as tech components, the devices as utility tools, and punch cards
as programming-adjacent tech items instead of generic misc.

`enigmaticlegacyplus` had a concentrated cluster of hearts, stones, eyes, mirrors, mixtures,
and a book bag that all behave like magic artifacts, reagents, or storage rather than
unclassified miscellany. `EnigmaticLegacyPlusCompat` now scores those families semantically,
including ring-style accessories as curios.

### 2026-06-22: NTGL, Create Gunsmithing, Minecolonies, Zen Colony, and Mowzie's Mobs Lose Repeated Unknown Families

`ntgl` and `cgs` each had a very obvious gun-mod shape: standalone ranged weapons plus a repeated
attachment family of scopes, stocks, barrels, magazines, and grips. Focused compat now marks the
weapons as ranged tools and the attachment pieces as tech upgrades instead of leaving them in
`misc/unknown`. `ntgl` chassis armor also gets a dedicated compat route so power-armor parts stop
floating as unclassified items.

`minecolonies` had a small but coherent set of colony deployers, scepters, analyzer tools, meshes,
magic potions, and helper items that were still falling through despite the compat family marker.
Those now route semantically as settlement items, utility tools, potions, materials, or tokens.

`zen_colony` was dominated by supply-pack items, so it now gets a focused pack route rather than a
broad heuristic. Because it is a MineColonies addon, those pack items now route under the
`minecolonies` browsing surface rather than claiming their own top-level addon header.
`mowziesmobs` similarly had a compact bucket of darts, rods, paws, and captured or ritual items
that now route as ammo, utility tools, artifacts, or organic materials.

### 2026-06-22: Born in Chaos, SWEM, HPM, Critters n' Crawlers, and MC Trade Post Unknowns Get Focused Routes

This pass finished another mixed tail of repeated `fallback:unknown` families. `born_in_chaos_v1`
and `swem` already had compat hooks, so they were extended rather than split into more global
heuristics: Born in Chaos utility curios like the evilometer and Krampus bag now route as utility
items, while SWEM tools like the tracker, measurement tool, hose, and mortar/pestle now join the
existing horse-care grouping.

`hpm` had a tight pirate/ship shape: cannonballs and mortar balls as ammo, hand mortars as ranged
weapons, hulls and masts as transport parts, and named ships as a focused ship subgroup. `cnc`
items like buckskin, antlers, tusks, turkey, and wishbones now route as organic materials, while
oddities like the Pot of Mouse stop falling through as misc. `mctradepost` now treats clipboards,
claim markers, and currency exchangers as utility tools, wishes as magic artifacts, and its small
trade leftovers as semantic utility or material items instead of generic unknowns.
