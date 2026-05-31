# Cobblemon Compatibility Spec

Working branch: `feature/major-mod-compat`

Runtime dump used for initial audit:

```text
C:\Users\ashle\AppData\Roaming\PrismLauncher\instances\AMICompat\minecraft\ami_dumps
```

Initial dump summary:

- Total AMI nodes: 13,459
- Cobblemon nodes: 761
- Cobblemon items: 706
- Cobblemon structures: 48
- Cobblemon entities: 7
- Per-species Pokemon nodes: not present; the runtime entity dump only exposes generic `cobblemon:pokemon`.
- Unclassified Cobblemon nodes: 172, mostly held items, consumables, evolution items, archaeology, and agriculture.

## Goal

Make Cobblemon feel intentionally indexed in AMI instead of falling through generic Minecraft item heuristics.

Support should improve:

- Ontology categories and grouping.
- Search parameters and synthetic search tokens.
- Numeric filters for medicine, dex data, move data, and species stats where available.
- Tooltip/debug metadata for why an item or species was classified.
- Recipe-viewer style tabs for spawns, moves, evolutions, drops, and pasture production.
- Optional progression-gated visibility for servers that do not want full Pokedex information exposed immediately.
- Deterministic JVM coverage using runtime dump fixtures where possible.

## User-Facing Feature Targets

### Where Do I Find It?

Add a spawn-conditions tab for Pokemon species.

The tab should answer:

- Which vanilla or modded biomes can this Pokemon spawn in?
- Which biome tags or biome predicates are involved?
- What time of day, weather, light level, altitude, nearby block, fluid, or underground/open-sky context is required?
- What level range and spawn bucket/rarity applies?
- Which anti-conditions prevent the spawn?

This should be sourced from Cobblemon spawn pool data, not from hardcoded wiki tables.

### Move Tutor and TM Matrix

Treat move learning as a reverse lookup graph.

Views to support:

- From a Pokemon species: show learnable moves by source, including level-up, TM, tutor, egg, and other source types Cobblemon exposes.
- From a move or TM item: show all Pokemon that can learn it.
- From an egg move: show the target Pokemon as output and compatible parents with that move as recipe-like inputs.

This likely needs pseudo recipe categories rather than ordinary item recipes.

### Evolution Path Recipes

Render evolutions as recipe-like displays.

Inputs and conditions may include:

- Item trigger: evolution stone, held item, scroll, armor, etc.
- Level or friendship threshold.
- Time/weather/biome/light/block context.
- Known move requirement.
- Trade or other interaction requirement.

The output should be the evolved Pokemon species node.

### Drops and Loot

Expose defeat drops and passive/pasture production.

Views to support:

- From a Pokemon species: show item drops, quantity, and chance.
- From an item: show Pokemon that can drop or produce it.
- From a Pasture Block production context: show periodic outputs by Pokemon where Cobblemon exposes that data.

### Advanced Search Syntax

Add Pokemon-specific search modifiers that feel natural to JEI/EMI users.

Desired examples:

```text
@type:grass
#move:earthquake
$stat:speed>100
%egg:monster
```

AMI already uses `@mod`, `#tag`, and `$category`, so implementation must preserve existing behavior:

- `@cobblemon` remains a mod filter.
- `@type:grass` can be parsed as a Pokemon type filter because it has a recognized field prefix.
- `#move:earthquake` can be parsed as a Pokemon move filter because it has a recognized field prefix.
- `$stat:speed>100` can be parsed as a Pokemon numeric stat filter because it has a recognized field prefix.
- `%egg:monster` requires a new prefix token or a pre-parser transform into a property filter.

Equivalent AMI-native property forms should also work:

```text
?pokemonType:grass
?pokemonMove:earthquake
>pokemonSpeed:100
?pokemonEggGroup:monster
```

### Pokedex-Gated Progression

Support optional fog-of-war behavior for server-friendly progression.

Modes to consider:

- `off`: full data visible.
- `seen`: reveal basic identity/type after seen; hide spawn, drops, and full moves until caught.
- `caught`: reveal full species details only after caught.
- `server_forced`: server controls reveal policy and client cannot override it.

The implementation should not scrape player save files directly. Player party/PC/Pokedex state may be NBT, JSON, or external DB depending on Cobblemon server config, so AMI should request progression state through server-side Cobblemon APIs or AMI packets when running on a server.

## Compatibility Model

Add Cobblemon support as an optional compatibility layer, not a hard dependency.

Preferred shape:

```java
interface ModCompatIndexer {
    boolean isAvailable();
    void enrichItem(ResourceLocation id, ItemStack stack, Map<String, String> meta);
    void contributeNodes(GlobalIndex index, Level level);
}
```

Cobblemon support should be guarded by mod presence and should fail closed. AMI must still load normally when Cobblemon is absent.

Use the actual local Cobblemon jar and runtime dumps as source of truth. Avoid relying on older tutorials or assumptions about Cobblemon internals.

Compat should also understand parent-mod ecosystems. Cobblemon add-ons should keep their own `modId` for normal mod filters, but AMI can mark them with `compatFamily=cobblemon` and classify clearly Cobblemon-themed content under the Cobblemon top-level experience. Use evidence such as creative tab labels, item classes, tags, and strongly themed ids; avoid treating vanilla/shared blocks as Cobblemon solely because they carry Cobblemon block tags.

## Ontology Plan

Cobblemon gets a dedicated top-level AMI category. The runtime dump shows Cobblemon content is broad enough that scattering it across Utility, Nature, Magic, Ingredients, Tech, and Decoration hides the player workflow.

Initial item-oriented category shape:

- `cobblemon/poke_balls`: Poke Balls and ancient balls.
- `cobblemon/medicine`: potions, revives, status cures, remedies, vitamins, mints, mochi, elixirs/ethers.
- `cobblemon/berries`: berries and berry foods.
- `cobblemon/apricorns`: apricorns, apricorn seeds/sprouts.
- `cobblemon/evolution`: evolution stones, scrolls, armor, held evolution trigger items.
- `cobblemon/fossils`: fossils, fossilized parts, archaeology materials.
- `cobblemon/machines`: PC, healing machine, fossil analyzer, restoration tank, pasture, monitor.
- `cobblemon/decor`: plaques, tatami, campfire pots, display/decorative blocks.
- `cobblemon/held_items`: held items and battle/training/type items.
- `cobblemon/utility`: Pokedex variants, Poke Rods, and related tools.
- `cobblemon/consumables`: candies, sweets, regional food, Aprijuice, Moomoo Milk, and non-medicine consumables.
- `cobblemon/agriculture`: herbs, mulch, mint leaves/seeds, and growable Cobblemon crops.
- `cobblemon/building`: Cobblemon building blocks such as apricorn/saccharine/tumblestone construction blocks.
- `cobblemon/archaeology`: relics, sherds, tumblestone materials, and archaeology items that are not fossils or held items.
- `cobblemon/misc`: fallback for recognized Cobblemon content that is not yet specialized.

Later species/data additions should extend the same top-level category:

- `cobblemon/species`
- `cobblemon/moves`
- `cobblemon/spawns`
- `cobblemon/drops`

## Metadata Keys

Use stable AMI metadata keys so existing `?key:value`, `~metadata`, grouping, row fields, and dump tooling can consume the data.

Item keys:

- `cobblemonItemKind`: `poke_ball`, `medicine`, `status_cure`, `vitamin`, `mint`, `mochi`, `held_item`, `evolution_item`, `fossil`, `berry`, `apricorn`, `machine`, `decor`
- `pokemonBallFamily`: `standard`, `ancient`, `special`
- `pokemonBallTier`: `poke`, `great`, `ultra`, `master`, etc.
- `pokemonMedicineKind`: `heal`, `revive`, `status_cure`, `pp_restore`, `stat_boost`
- `pokemonHealing`: numeric HP restored where known.
- `pokemonStatusCure`: `poison`, `burn`, `paralysis`, `sleep`, `freeze`, `all`
- `pokemonHeldItemRole`: `type_boost`, `choice`, `training`, `battle`, `incense`, `utility`
- `pokemonEvolutionTrigger`: `stone`, `trade`, `held_item`, `scroll`, `armor`, `fossil`, etc.
- `pokemonType`: comma-separated type ids when item is type-specific.

Species keys:

- `pokemonSpecies`: stable species id.
- `pokemonDexNumber`: numeric national dex number where available.
- `pokemonType`: comma-separated type ids.
- `pokemonPrimaryType`
- `pokemonSecondaryType`
- `pokemonGeneration`
- `pokemonAbilities`
- `pokemonEggGroups`
- `pokemonEvolvesFrom`
- `pokemonEvolvesTo`
- `pokemonSpawnContext`: `ground`, `water`, `air`, etc.
- `pokemonSpawnBiome`
- `pokemonSpawnBiomeTags`
- `pokemonSpawnTime`
- `pokemonSpawnWeather`
- `pokemonSpawnLight`
- `pokemonSpawnMinLevel`
- `pokemonSpawnMaxLevel`
- `pokemonSpawnWeight`
- `pokemonSpawnBucket`
- `pokemonSpawnConditions`
- `pokemonSpawnAntiConditions`
- `pokemonBaseHp`
- `pokemonBaseAttack`
- `pokemonBaseDefense`
- `pokemonBaseSpecialAttack`
- `pokemonBaseSpecialDefense`
- `pokemonBaseSpeed`
- `pokemonCatchRate`

Move keys, if moves become searchable nodes:

- `pokemonMove`
- `pokemonMoveType`
- `pokemonMoveCategory`: `physical`, `special`, `status`
- `pokemonMovePower`
- `pokemonMoveAccuracy`
- `pokemonMovePp`
- `pokemonMoveLearnMethod`: `level`, `tm`, `tutor`, `egg`, etc.
- `pokemonMoveLearners`: for move/TM nodes, a compact count or lookup link rather than a huge inline list.

Drop/production keys:

- `pokemonDropItem`
- `pokemonDropChance`
- `pokemonDropMin`
- `pokemonDropMax`
- `pokemonPastureProduct`
- `pokemonPastureChance`

Progression/fog keys:

- `pokemonKnowledgeState`: `unknown`, `seen`, `caught`, `complete`
- `pokemonDataHidden`: comma-separated fields hidden by server/client policy.

## Search Plan

Existing syntax should work after metadata enrichment:

```text
@cobblemon
?cobblemonItemKind:medicine
?pokemonStatusCure:poison
?pokemonType:fire
?pokemonType:grass ?pokemonType:poison
>heal:50
>dex:150
```

Advanced Pokemon syntax should also work after parser support:

```text
@type:grass
#move:earthquake
$stat:speed>100
%egg:monster
```

Add numeric aliases to `NumericMetadataResolver`:

- `heal`, `healing`, `hprestore` -> `pokemonHealing`
- `dex`, `pokedex`, `dexnumber` -> `pokemonDexNumber`
- `hp`, `basehp` -> `pokemonBaseHp` for species nodes
- `attack`, `atk` -> `pokemonBaseAttack` for species nodes
- `defense`, `def` -> `pokemonBaseDefense` for species nodes
- `specialattack`, `spatk`, `spa` -> `pokemonBaseSpecialAttack` for species nodes
- `specialdefense`, `spdef`, `spd` -> `pokemonBaseSpecialDefense` for species nodes
- `speed`, `spe` -> `pokemonBaseSpeed` for species nodes
- `movepower`, `power` -> `pokemonMovePower` when move nodes exist
- `accuracy`, `moveaccuracy` -> `pokemonMoveAccuracy` when move nodes exist
- `pp`, `movepp` -> `pokemonMovePp` when move nodes exist

Property aliases to add or verify:

- `?type:fire` should match `pokemonType` for Pokemon-specific nodes while preserving current generic metadata fallback.
- `?status:poison` should match `pokemonStatusCure`.
- `?move:earthquake` should match learnable move metadata or move graph edges.
- `?egg:monster` and `%egg:monster` should match `pokemonEggGroups`.
- `?drop:ender_pearl` should match species that drop or produce the item.
- `?medicine`, `?pokeball`, `?helditem`, `?evolution`, `?fossil`, `?berry`, `?apricorn` should match `cobblemonItemKind` or search tokens.

## Runtime Data Sources

Use these in order:

1. Runtime AMI dump for current user-visible shape.
2. Cobblemon jar resources under `data/cobblemon/`.
3. Cobblemon classes via guarded reflection only if resource data cannot provide enough information.

Known jar resource areas:

- `data/cobblemon/species/`
- `data/cobblemon/spawn_pool_world/`
- `data/cobblemon/action_effects/`
- `assets/cobblemon/lang/en_us.json`
- item/block models and tags for berries, potions, blocks, and archaeology items

Cobblemon data model notes:

- Species data is static datapack data under `data/cobblemon/species/`, for example `data/cobblemon/species/generation1/bulbasaur.json`.
- Species data is loaded by Cobblemon through Minecraft's datapack system, so modpacks and servers can override or add Pokemon. Runtime APIs should be preferred over reading only the mod jar, otherwise AMI will miss datapack changes.
- Spawn data is separate under `data/cobblemon/spawn_pool_world/`.
- Runtime Pokemon all share the generic entity type `cobblemon:pokemon`; species, level, gender, shiny state, etc. live in entity data/NBT.
- Player party/PC/Pokedex state is world/server data and may be NBT, JSON, or Mongo-backed depending on Cobblemon config. Treat this as server-owned data.

Potential guarded runtime APIs/classes found in the local jar:

- `com.cobblemon.mod.common.Cobblemon`
- `com.cobblemon.mod.common.api.pokemon.PokemonSpecies`
- `com.cobblemon.mod.common.api.moves.Moves`
- `com.cobblemon.mod.common.api.moves.MoveTemplate`
- `com.cobblemon.mod.common.api.pokemon.evolution.*`
- `com.cobblemon.mod.common.api.pokemon.egg.EggGroup`

Before using these APIs directly, inspect signatures from local bytecode/source and keep AMI load-safe when Cobblemon is absent.

## Current Issues From Dump

High-priority misclassification and missing-data issues:

- 172 Cobblemon nodes have no ontology category/subcategory.
- Many consumables are classified as `geology/terrain` because they are placeable item/block hybrids:
  - `cobblemon:potion`
  - `cobblemon:super_potion`
  - `cobblemon:hyper_potion`
  - `cobblemon:max_potion`
  - `cobblemon:full_restore`
  - `cobblemon:antidote`
  - `cobblemon:awakening`
  - `cobblemon:burn_heal`
  - `cobblemon:ice_heal`
  - `cobblemon:paralyze_heal`
  - `cobblemon:ether`
  - `cobblemon:elixir`
- Held items are split across `armor/head`, `ingredients`, null ontology, and other generic buckets.
- Evolution items are mostly null ontology.
- Fossils/archeology items are mostly null ontology or geology without fossil-specific semantics.
- Poke Balls are currently `utility/tools` and lack capture-specific metadata.
- Pokemon species are absent as individual AMI nodes.

## Implementation Todo

### Phase 1: Audit and Fixtures

- [ ] Save the current `search_nodes.jsonl` as a local test fixture or document how to point tests at it with `AMI_SEARCH_NODES_DUMP`.
- [ ] Add a focused Cobblemon runtime mirror/audit test that reports Cobblemon category counts.
- [ ] Add snapshot expectations for key Cobblemon families: balls, medicines, berries, apricorns, held items, evolution items, fossils, machines.
- [ ] Add query tests for existing behavior before changing classification.

### Phase 2: Item Metadata Enrichment

- [x] Add Cobblemon optional compat enricher.
- [x] Mark Cobblemon add-on content with `compatFamily=cobblemon` when strong ecosystem evidence exists.
- [x] Classify Poke Balls with `cobblemonItemKind=poke_ball`, ball family, and tier.
- [x] Classify medicine and status cures with medicine kind, healing amount, and status cure.
- [x] Classify vitamins, mints, mochi, PP restorers, and battle stat items.
- [x] Classify berries and apricorns separately.
- [x] Classify held items with role/type metadata where detectable.
- [x] Classify evolution items and fossils.
- [x] Classify machines/stations: PC, healer, fossil analyzer, restoration tank, pasture, monitor.
- [x] Classify display/decor blocks such as display case, plaques, tatami, and campfire pots.
- [x] Classify Pokedex/Poke Rod utility items, type gems, non-medicine consumables, agriculture items, Cobblemon building blocks, and archaeology leftovers.
- [x] Add synthetic search tokens for common user terms: `pokeball`, `poke_ball`, `capture`, `medicine`, `heal`, `revive`, `status_cure`, `held_item`, `evolution`, `fossil`, `berry`, `apricorn`.

### Phase 3: Ontology and Grouping

- [x] Add a dedicated top-level `cobblemon` ontology category.
- [x] Add focused Cobblemon subcategories for Poke Balls, medicine, berries, apricorns, evolution, fossils, machines, decor, held items, utility, consumables, agriculture, building, archaeology, and misc.
- [x] Add translations for new category/subcategory labels.
- [x] Add primary classification rules based on Cobblemon metadata before generic placeable/geology fallback.
- [x] Add deterministic grouping metadata for ball families, berry families, apricorn colors, medicines, and evolution triggers.
- [x] Ensure Cobblemon placeable consumables no longer collapse into `geology/terrain`.

### Phase 4: Search

- [x] Add numeric aliases for healing and dex number.
- [x] Add numeric aliases for Pokemon base stats: HP, attack, defense, special attack, special defense, and speed.
- [x] Add property aliases for type, status cure, medicine, Poke Balls, held items, evolution items, fossils, berries, and apricorns.
- [x] Add parser support for Pokemon-specific advanced syntax: `@type:grass`, `#move:earthquake`, `$stat:speed>100`, and `%egg:monster`.
- [x] Preserve existing AMI behavior for `@mod`, `#tag`, and `$category`.
- [x] Add tests for combined queries such as `@cobblemon ?medicine >heal:50`.
- [x] Add tests for Pokemon queries such as `@type:grass #move:earthquake`, `$stat:speed>100`, and `%egg:monster`.
- [x] Add tests for negative filters, e.g. `@cobblemon ?helditem -?type:fire`.

### Phase 5: Species Nodes

- [x] Represent species as enriched entity-like nodes for the first pass.
- [x] Prefer Cobblemon runtime/datapack APIs over jar-only JSON parsing so datapack-added Pokemon are indexed.
- [x] Parse Cobblemon species data from guarded runtime APIs.
- [x] Create one synthetic AMI node per Pokemon species using ids like `cobblemon:species/bulbasaur`.
- [x] Add type, dex, generation, base stats, ability, egg group, height, weight, and learnset metadata where available.
- [x] Add `Cobblemon > Species` grouping.
- [x] Ensure species nodes do not collide with the generic runtime `cobblemon:pokemon` entity node.
- [x] Add species icon strategy or acceptable fallback renderer.
- [x] Add species query tests for type, dex, move, egg group, and stat filters.
- [ ] Add results tree snapshot for species browsing.

### Phase 6: Spawn, Move, Evolution, and Drop Views

- [ ] Add a spawn-conditions projection for `Where Do I Find It?`.
- [ ] Parse or consume spawn pool biome predicates, biome tags, time, weather, block, light, altitude, level, bucket, and anti-condition data.
- [ ] Add reverse lookup from biome/tag/condition to Pokemon species.
- [ ] Add move learnset metadata and reverse move lookup.
- [ ] Add TM/tutor item lookup to Pokemon species.
- [ ] Add egg move pseudo recipe projection.
- [ ] Add evolution pseudo recipe projection with item/context conditions.
- [ ] Add Pokemon drop lookup from species to item and item to species.
- [ ] Add Pasture Block production lookup if Cobblemon exposes production data.
- [ ] Decide whether these views live in AMI's native recipe viewer, EMI/JEI bridge categories, or both.

### Phase 7: Pokedex Progression

- [ ] Define config values for progression visibility: `off`, `seen`, `caught`, `server_forced`.
- [ ] Add server-side capability/API bridge for seen/caught state.
- [ ] Add AMI network packet for species knowledge state.
- [ ] Hide or redact spawn locations, drops, stats, moves, and evolutions according to configured policy.
- [ ] Make redaction visible in UI without exposing hidden values in tooltips, search tokens, or dumps when server-forced.
- [ ] Add tests for redacted search behavior so hidden data cannot be discovered through query side channels.

### Phase 8: Tooltips and Row Fields

- [x] Add AMI tooltip facts for medicine healing/status cure.
- [x] Add tooltip facts for Poke Ball tier/family.
- [x] Add tooltip facts for Pokemon type and held-item role.
- [ ] Add tooltip facts for spawn bucket/rarity and known spawn conditions.
- [ ] Add tooltip facts for evolution trigger summaries.
- [ ] Add tooltip facts for drop chance/quantity summaries.
- [x] Add optional row fields for Pokemon type, healing, dex number, and base speed.
- [ ] Add optional row fields for move power and spawn bucket once move/spawn projections exist.

### Phase 9: Runtime Validation

- [ ] Build a debug jar with `-Pami_debug_build=true`.
- [ ] Install into `AMICompat`.
- [ ] Run `/ami dump-search-nodes`.
- [ ] Run `/ami dump-results-tree @cobblemon`.
- [ ] Run targeted dumps for species, spawn, move, evolution, and drop projections once those commands exist.
- [ ] Compare category counts and tree snapshots against the previous dump.
- [ ] Smoke test inventory/search UI if grouping or row display changes are visible.

## Acceptance Criteria

- Cobblemon item families are discoverable with clear AMI categories.
- No major Cobblemon consumable family lands in `geology/terrain`.
- Poke Balls, medicine, held items, evolution items, fossils, berries, and apricorns have stable metadata.
- Searches like `?medicine`, `?pokeball`, `?status:poison`, `>heal:50`, and `@cobblemon ?berry` work.
- Advanced searches like `@type:grass`, `#move:earthquake`, `$stat:speed>100`, and `%egg:monster` work without breaking existing AMI `@mod`, `#tag`, and `$category` behavior.
- Runtime dump has materially fewer null Cobblemon ontology assignments.
- Species support, when implemented, provides per-species search by Pokemon type and dex number.
- Spawn, move, evolution, drop, and pasture views are backed by Cobblemon data/API output, not handwritten wiki tables.
- Server-forced Pokedex progression does not leak hidden data through search tokens, tooltips, debug dumps, or recipe projections.
