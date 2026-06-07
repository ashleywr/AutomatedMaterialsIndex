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

### 2026-05-31: Filled Creative Variants Use Runtime Data, Not Names

Creative-tab representative stacks can expose both empty and prefilled variants
for energy, fluid, chemical, or similar containers. Those prefilled copies are
cheat/dev-only because they represent spawned state, not survival identity. AMI
must not infer this from item names such as `tank`, `cell`, `battery`, or
`fluid`. The index now checks platform item energy and fluid capabilities first,
then reads tooltip resource amounts as a fallback for mods whose custom resource
state is only exposed there. The result is still explainable via
`variantAccessReason=prefilled_creative_stack`.
