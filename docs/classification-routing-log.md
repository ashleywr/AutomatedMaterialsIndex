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
2. Semantic gate: facets and block facts such as weapon, armor, food, storage,
   machine, cable, stone, soil, or placeable block shape.
3. Compat policy gate: decide whether a compat family is allowed to override the
   semantic category for this item kind.
4. Fallback evidence gate: weighted lexical, creative-tab, class-name, and
   metadata evidence when the earlier gates do not decide.
5. Unknown fallback: `misc/unknown` with debug evidence.

Every gate should write enough debug evidence to explain why it won.

`PrimaryCategoryResolver.resolve` is the entry point for final item routing.
Each assignment now writes:

- `classificationRoute`: compact input-to-output route trace.
- `classificationRoutePhase`: final gate, such as `hard_identity`,
  `evidence_strong`, `primary_rule`, `evidence_fallback`, or `fallback`.
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
  Xaero/JourneyMap/FTB categories. Future indexing should target waypoints,
  markers, claims, death points, dimensions, and sharing/state rather than
  ordinary vanilla map items.

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

## Open Work

- Split Cobblemon identity routing into strong Pokemon gameplay identities
  versus generic addon blocks/items.
- Add comparable policy tests for Create: Create sword stays `tools/melee`,
  Create zinc ingot stays `ingredients` or material category, Create kinetic
  machine stays `tech` or Create-focused depending policy.
- Move scattered substring checks in classification/facet code toward shared
  token utilities and explicit context gates.
- Do not add new `isLikely*` gates or ambiguous path/name substring checks for
  ownership, access level, top-level category, or cheat/dev visibility. Use
  capability/component/tag/class data first; use exact compat APIs next; use
  exact registry/tag IDs after that; tokenized lexical fallback is only
  low-confidence evidence and needs a false-positive regression test.
- Update this log for every classification redesign or policy change.

### 2026-05-31: Filled Creative Variants Use Runtime Data, Not Names

Creative-tab representative stacks can expose both empty and prefilled variants
for energy, fluid, chemical, or similar containers. Those prefilled copies are
cheat/dev-only because they represent spawned state, not survival identity. AMI
must not infer this from item names such as `tank`, `cell`, `battery`, or
`fluid`. The index now checks platform item energy and fluid capabilities first,
then reads tooltip resource amounts as a fallback for mods whose custom resource
state is only exposed there. The result is still explainable via
`variantAccessReason=prefilled_creative_stack`.
