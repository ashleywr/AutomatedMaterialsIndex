# AMI Changelog

User-facing changes are recorded here.

## 1.7.0 - 2026-06-17

AMI 1.7.0 finalizes Fabric release prep, keeps loader packaging aligned across Fabric, Forge, and NeoForge, and tightens recent Fabric-side UI behavior before publication.

### Added

- Added Fabric 1.21.1 to AMI's normal release-prep pipeline so the release workflow builds, publishes, and uploads Fabric artifacts alongside Forge and NeoForge.
- Added a compatibility support matrix documenting the exact tested recipe-viewer dependency versions and the current support level for key AMI compat families.

### Changed

- Changed release metadata to `1.7.0` across the shared build so all loader manifests, publishing tasks, and GitHub release assets resolve from the same version source.
- Changed release verification expectations to treat Fabric as a first-class release artifact instead of a local-only loader build.

### Fixed

- Fixed Fabric inventory-result scrollbar rendering so the loader now matches the current AMI panel behavior expected on the release line.
- Fixed recent Fabric favorites persistence debugging work so release-prep validation covers the current saved-favorites path before publishing.

## 1.6.0 - 2026-06-13

AMI 1.6.0 adds advancement search, expands document-result controls, and improves large-modpack entity icon performance and release profiling.

### Added

- Added client-visible Minecraft advancement search as document rows in AMI search results, including progress state, match evidence, context actions, and opening through Just Enough Advancements when available.
- Added configurable search-source toggles for advancement, guide, quest, player, and waypoint document/runtime rows.
- Added localized advancement-result labels and configuration text across supported languages.
- Added a reusable adaptive client-work scheduler for expensive render-thread background tasks.
- Added Synesthesia-scale profiling tooling that captures indexing duration, entity icon warmup, FPS/tick telemetry, memory/process samples, screenshots, logs, and optional JFR recordings.
- Added a release benchmark exporter for compact GitHub release benchmark Markdown/JSON assets.
- Added Spanish localization and translation delimiter sentinels for localization maintenance.

### Changed

- Changed entity icon atlas warmup to use prioritized, bounded, adaptive scheduling so visible icons are favored while background warmup backs off under frame pressure.
- Changed entity icon cache persistence from full-atlas writes to per-icon persistent PNG cache files, with asynchronous best-effort writes and lazy bounded reads.
- Changed entity icon cache keys to include a stable mod/resource-pack fingerprint without recomputing that fingerprint during normal icon blits.
- Changed runtime `/ami/status` telemetry to report index busy state, node-type counts, heap/process CPU, AMI tick/frame timing, and entity icon cache/warmup counters.
- Changed document row rendering to clean up scissor state reliably when guide, quest, and advancement rows are rendered.

### Fixed

- Fixed entity icon atlas writes causing render-thread hitches by removing full-atlas PNG writes from client tick processing.
- Fixed entity icon cache misses and render failures to show concrete fallbacks, preferring spawn eggs or proxy items before a red error marker.
- Fixed entity icon warmup completion accounting so a full queue no longer makes unqueued renderable entities look complete.
- Fixed visible entity icon requests churning the bounded bake queue when the queue is already full of visible-priority work.
- Fixed entity icon warmup competing with AMI indexing by pausing atlas work while high-impact indexing phases are busy.
- Fixed retained entity-renderer memory pressure by bounding cached `LivingEntity` instances and pending bake tasks.
- Fixed the Synesthesia profiler harness to tolerate optional status fields and to pass `jcmd` JFR arguments correctly.
- Fixed runtime result-debug projection so guide, quest, and advancement row counts respect their config toggles.

## 1.5.1 - 2026-06-12

AMI 1.5.1 is a hotfix release for the Forge 1.20.1 packaged build published in 1.5.0.

### Fixed

- Fixed a Forge 1.20.1 crash when AMI rendered inventory result-grid sprite batches from the packaged production jar by adding production SRG fallbacks for reflected Blaze3D rendering methods.
- Fixed the Forge release verification gate to catch missing production render-method fallbacks before upload.

## 1.5.0 - 2026-06-12

AMI 1.5.0 focuses on UI rendering and tooltip layering stability, result-tree consistency, and search/ontology indexing correctness.

### Added

- Added a new render-phase model (`AmiRenderPhase`) and split overlay painting into `renderBase`/`renderTopLayer` paths so durable panels (buttons, search bars, result rows) and transient overlays (tooltips, dropdowns, context menus) no longer race each other.
- Added batched sprite rendering utilities (`ColoredQuadBatch`, `TexturedQuadBatch`) and generated GUI sprite helpers for lightweight result-grid visuals, quest markers, scrollbars, and dev-only/access overlays.
- Added an entity icon atlas cache in `EntityIconCache` with on-demand bake queueing, disk persistence, deterministic cache keys, and bounded per-tick bake throughput for smoother hover and scroll behavior.
- Added entity icon warmup pass (`EntityIconRenderer.tickAtlasWarmup`) to pre-bake icon atlas entries progressively after index changes.
- Added ontology classification memoization in `AmiOntologyKinds` with per-scope rule/index caches for deterministic, reusable kind assignment.
- Added `descriptionSearchTokens` extraction (`TooltipSearchTokens#extractDescription`) and wired it into item indexing metadata.
- Added validation coverage for:
  - `JeiIngredientBridge` non-browseable ingredient filtering
  - ontology cache behavior
  - misc-category group pruning in category mode results.

### Changed

- Changed inventory overlay lifecycle and tooltip orchestration to better handle container screens, external recipe viewers, and status-effect ownership so AMI no longer steals tooltip ownership at the wrong layer.
- Changed result icon and scrollbar rendering to sprite-backed batches in grid/tree views while preserving hover and theme behavior.
- Changed item search indexing to include description block tokens as a dedicated plain-search source in addition to existing tooltip metadata tokens.
- Changed result tree builder grouping so misc-node terminal buckets only include item nodes, removing non-item noise from misc terminal categories.
- Changed `UniversalResultsPanel` projection keying to use a source signature so cache invalidation can track content changes more accurately.

### Fixed

- Fixed tooltip-layer z/focus flicker from mixed render-stack transitions by removing ad-hoc transient push/pop blocks around AMI tooltip components and using explicit phase separation.
- Fixed visual/interaction regressions from status-effects and external-tooltip handling by re-hosting external recipe tooltips when needed and making overlay hover ownership explicit.
- Fixed ingredient-compat indexing pollution by skipping panel entry ingredient types from the AMI global ingredient index path.
- Fixed dev/access markers and discovery overlays to be rendered through shared sprite renderers instead of inline raw fill logic, reducing drift and repeated render state churn.
- Fixed `ItemGridView` rendering helpers to lazily initialize `GridCellSpriteBatchRenderer` so non-rendering test/runtime paths no longer fail on client-only texture classes.
- Fixed plain-text search regressions in tooltip token coverage by restoring `tooltipSearchTokens` in plain search indexing while retaining description token indexing.
- Fixed NeoForge recipe-book mixin crash caused by enum switch bytecode in `RecipeBookComponentMixin` by removing synthetic switch-map dependence and using direct mode checks.

### Compatibility Notes

- Verified the current best-effort mapping-mod compatibility paths against the latest compat-watch updates from issue #35: JourneyMap `1.21.1-6.0.0-beta.83+neoforge`, Xaero's Minimap `forge-1.20.1-26.1.0` / `neoforge-1.21.1-26.1.0`, and Xaero's World Map `forge-1.20.1-1.41.0` / `neoforge-1.21.1-1.41.0`.

## 1.4.2 - 2026-06-08

AMI 1.4.2 is a hotfix release for result-panel refresh lag triggered by recipe lookups.

### Fixed

- Fixed recipe clicks and lookup-history runtime updates so they no longer force a full AMI result entry rebuild on the render thread.
- Fixed AMI result panels to treat runtime-only search revisions separately from indexed search-service revisions, avoiding repeated list-lens rescans during normal recipe browsing.

## 1.4.1 - 2026-06-08

AMI 1.4.1 is a hotfix release for the Forge 1.20.1 packaged build published in 1.4.0.

### Fixed

- Fixed Forge recipe-book mixin shadows so the packaged production jar targets the correct obfuscated `RecipeBookComponent` method names instead of crashing during mixin application.
- Fixed the Forge release process to verify packaged mixin shadow names against production mappings before Modrinth, CurseForge, or GitHub release assets are published.

## 1.4.0 - 2026-06-08

AMI 1.4.0 expands mod compatibility, adds runtime player and waypoint search utilities, and tightens AMI's overlay/favorites behavior when sharing inventory screens with EMI, JEI, and the vanilla recipe book.

### Added

- Added runtime `^player` search utilities for player heads, online-player entries, lookup history, and provider-backed waypoint/player actions without requiring those volatile entries to live in the static global index.
- Added player utility actions for copying names, giving player heads, copying waypoint data, creating native JourneyMap and FTB Chunks waypoints when available, and admin-gated teleport actions where the client can determine that the action is valid.
- Added optional full 3D player model rendering for player-search results, including async offline skin/profile lookup and hover-driven model spin, with fallback rendering when offline skin lookup fails.
- Added persistent AMI-owned favorites storage for runtime-only favorites such as player and waypoint entries, including offline/unavailable fallback rows so those favorites remain visible when the source is offline.
- Added favorite reordering for the favorites sidebar and runtime-favorite tooltip states so stale entries explain why the live target is unavailable.
- Added a provider registry and compat facade for player/waypoint integrations, including detected support for JourneyMap, FTB Chunks, Waystones, Xaero map/minimap detection, and manual coordinate export fallbacks.
- Added focused compat routing for Ars Nouveau, Spectrum, Nature's Aura, additional Mana and Artifice slices, AE2/AppMek-related items, Waystones, and more generated/runtime item families that previously fell into generic buckets.
- Added waypoint deletion that removes waypoints immediately from search results, favorites, and sidebars without requiring manual reindex.
- Added detection of Waystones-synced waypoints in JourneyMap, with read-only display and automatic source deletion to prevent re-sync on game restart.
- Added provider-based tooltip customization API (`PlayerWaypointProvider.getTooltipLabel()`) so waypoint providers control their own display labels, enabling generic support for any mod's waypoint integrations without hardcoding mod names.
- Added Russian localization and refreshed Chinese localization coverage.

### Changed

- Reworked AMI/EMI/JEI inventory-screen ownership around a shared visibility-layer model so Alt+V, the recipe-book button, and start-hidden behavior consistently switch between AMI, external viewers, and fully hidden states.
- Improved the favorites sidebar with header controls, a collapsible sidebar rail, better drag/drop behavior, and improved interaction between AMI favorites and external viewer favorites.
- Improved player-search suggestions so online players rank first, then local history, with bounded suggestion/result counts and synchronized `^` help text/config descriptions.
- Expanded the config surface for player utility search and recipe-book behavior, including the recipe-book action selector and updated option tooltips.
- Refined several search/routing heuristics to prefer compat facts, runtime providers, and exact token matches over broader fallback path matching.

### Fixed

- Fixed a config dropdown interaction crash and the shared enum-dropdown tooltip path so config option descriptions render without depending on protected screen internals.
- Fixed recipe-book interception ordering so AMI's mixins win cleanly against EMI/Forge recipe-book hooks when AMI is configured to own the button behavior.
- Fixed overlay suppression and sidebar sync edge cases that could leave AMI, EMI, JEI, or the vanilla recipe book in the wrong visible state after toggles or screen transitions.
- Fixed player-head search/history coverage gaps with deterministic tests that keep live/history suggestion ordering aligned with the current search document contract.
- Fixed several runtime and generated-item classification gaps, including additional mod families and utility items that previously landed in noisy fallback categories.

## 1.3.2 - 2026-06-07

AMI 1.3.2 is a hotfix release for recent runtime, UI, and guide-integration regressions.

### Fixed

- Fixed AMI runtime category indexing updates to avoid extra full rebuilds in normal operation.
- Fixed AMI panel behavior so the panel configuration editor reliably re-opens and stays usable.
- Fixed guide integration for Silent Gear's material book metadata path.
- Fixed drag-start initiation from result icons to prevent false starts while interacting with result items.
- Fixed an AMI concurrency regression during release flows that could trigger duplicate result processing.
- Fixed AMI-prefixed command suggestion visibility so those entries are now gated behind dev-mode when appropriate.

## 1.3.1 - 2026-06-07

AMI 1.3.1 is a hotfix for guide search/opening edge cases, JEI search sync stability, and tooltip rendering polish after the 1.3.0 compatibility release.

### Changed

- Improved AMI tooltip rendering so long localized tooltip lines use the normal Minecraft/NeoForge wrapping and visual-order handling.
- Improved debug and grouped grid tooltip handling so escaped or embedded newline text is normalized consistently across result views.

### Fixed

- Fixed AE2 guide indexing and ME Guide context actions so GuideME pages resolve and open more reliably from AMI results.
- Fixed stale guide/search cache behavior by tracking guide index revisions in the global index cache.
- Fixed guide translation-key leakage by sanitizing unresolved guide title, chapter, and summary keys before they appear in indexed guide content.
- Fixed JEI search sync crashes when a very long search string is pasted or pushed into JEI from AMI.
- Fixed AMI result tooltips that displayed literal escaped newline text, including multiline modded item tooltips such as Occultism's Divination Rod.

## 1.3.0 - 2026-06-06

AMI 1.3.0 is a broad modpack compatibility and indexing-speed release. It adds first-class guidebook search, focused routing for several large mod families, faster new index builds in heavy packs, and cleaner coexistence with EMI/JEI and the vanilla recipe book.

### Added

- Added guidebook content indexing by default. AMI can now search guide titles, chapters, referenced items, and capped page summaries, then show guide-page results separately from normal items.
- Added guide opening and indexing support for Patchouli, GuideME/AE2, Modonomicon, Mantle/Tinkers Construct books, Silent Gear's material book, Immersive Engineering's manual, Hexerei's Book of Shadows, Resource Book-style guides, Apotheosis guide content, and Alex's Caves codex pages.
- Added shared searchable compat provider APIs so mods can expose guide, item, and action data through viewer-neutral hooks while AMI keeps its own overlay-specific behavior.
- Added focused compatibility routing for:
  - Alex's Caves cave resources, cave gear, cave codex items, submarines, projectiles, foods, and hidden weapon variants.
  - Alex's Mobs drops, animal dictionary ingredients, taming foods, echolocators, straddleboards, pigshoes, darts, and custom ranged items.
  - Mana and Artifice construct parts, motes, runes, patches, mana gems, artifice tools, relics, and magic materials.
  - Timeless and Classics Zero guns, ammo, attachments, workstations, and addon namespaces using TacZ item classes.
  - Tinkers Construct / Silent Gear / modular gear-style tools, parts, modifiers, stations, guidebooks, generated variants, and material pages.
  - Chipped and Rechiseled generated block families, including better default collapse behavior.
- Added top-level/category routing support for major compat families including Create, AE2, Mekanism, GregTech/GTCEu, MineColonies, Apotheosis, Botania, Sophisticated Storage/Backpacks, Mapping mods, Modular Gear, and TacZ.
- Added inventory visual filtering and tooltip-search token coverage.
- Added Chinese localization.

### Changed

- Replaced the spawn-egg-only setting with `Show Creative Items`, a display-time toggle for creative-only results such as spawn eggs and spawners.
- Improved new-index build time in large packs, often by more than 50% in observed heavy-pack rebuilds, by deferring expensive namespace work, reducing broad compat enrichment, avoiding unnecessary tooltip metric scans, and using fast paths for high-cardinality generated items.
- Improved search help with compat-aware examples and guidebook/property filters.
- Improved guide result projection so item searches can show useful related guide evidence without letting guide text overpower exact item, registry, recipe, or structured metadata matches.
- Improved category routing diagnostics with explicit route phase/rule metadata for easier pack-author debugging.

### Fixed

- Creative-only items are now indexed regardless of the toggle, so changing `Show Creative Items` applies immediately without a restart.
- Entity results now inherit localized spawn egg names as plain search aliases, so searching for a localized spawn egg name can still find AMI's entity model result.
- Fixed unresolved Modonomicon guide translations in indexed guide content.
- Fixed recipe book and recipe viewer visibility interactions, including AMI toggle lock behavior in start-hidden mode and recipe-book hidden-all screens.
- Fixed JEI chrome suppression so JEI hides consistently when AMI owns the screen.
- Fixed AMI overlay behavior when auto-indexing is disabled.
- Fixed GuideME page opening for AE2's rooted page ids.
- Fixed Patchouli guide visibility/openability checks so advancement-gated entries follow the live book state instead of appearing as stale search hits.
- Fixed Patchouli and Modonomicon language-key leaks by resolving client/server resource translations or falling back to readable labels.
- Fixed guidebook variants collapsing together when multiple Patchouli/Mantle books share one base item.
- Fixed several high-cardinality generated/runtime dump families that previously fell into `fallback:unknown` or noisy generic categories.

## 1.2.0 - 2026-06-04

### Fixed

- [#3] Added universal Cyrillic/Chinese/Japanese search input acceptance (including Japanese width variants) by using a centralized input filter and locale-safe Unicode normalization for search indexing and matching.
- Fixed JEI/EMI recipe lookups for generic AMI item entries by falling back to AMI-known subtype and creative-tab stacks when the base stack itself does not expose the modded recipe output.
- Fixed AMI's compact result layout switching too aggressively when recipe viewers temporarily reduce available screen space.
- Fixed Forge recipe book / Not Enough Recipe Book interactions so AMI does not fight external recipe-book managers.
- Fixed mixed EMI+JEI installs by selecting EMI consistently and suppressing JEI chrome only when JEI is the selected viewer.
- Fixed EMI recipe screens opened from AMI so EMI recipe content remains usable while sidebars/search stay hidden until AMI is toggled off.
- Fixed Alt+V toggling from EMI recipe screens so EMI items/search return when AMI is disabled.
- Fixed duplicate creative-stack item variants caused by hidden component-only differences.
- Reduced indexing time in large packs by avoiding broad compat enrichment and no-op tooltip metric scans for unrelated items.

## 1.1.3 - 2026-06-03

AMI 1.1.3 is a Forge compatibility hotfix.

### Fixed

- Fixed a Forge 1.20.1 crash when opening inventory with GTCEu/GregTech installed. AMI's Forge recipe book mixin now targets Forge's SRG runtime names directly instead of applying the shared NeoForge/named mixin without a refmap.
- Fixed Forge + EMI result context menus for GTCEu/GregTech items. AMI now asks EMI for recipe/use availability before hiding `Show Recipes` or `Show Uses`, and lets EMI's recipe screen render and tick after AMI opens it.

## 1.1.2 - 2026-06-03

AMI 1.1.2 is a Forge release packaging hotfix.

### Fixed

- Fixed Forge Modrinth, CurseForge, and GitHub release packaging to upload a verified reobfuscated Forge jar instead of the raw development jar.
- Added a Forge release-jar verification step so publishing fails before upload if the Forge jar still contains named Minecraft runtime calls.

## 1.1.1 - 2026-06-03

AMI 1.1.1 is a Forge release packaging hotfix.

### Fixed

- Fixed Forge Modrinth and CurseForge publishing so release uploads run Forge reobfuscation before the jar is uploaded.
- Replaced the broken Forge 1.1.0 release path that could ship named/development bytecode and crash production Forge 1.20.1 clients during startup.

## 1.1.0 - 2026-06-03

AMI 1.1.0 is a GregTech/GTCEu-focused feature release. GregTech packs are large enough that a normal item-name search is not enough, so AMI now indexes more of the structure GregTech players actually use: voltage tiers, circuit grades, machine/power roles, EU/t values, and GregTech-specific right-click filters.

### Added

- GregTech/GTCEu items now get focused compatibility metadata:
  - Item kinds such as machines, multiblocks, power, circuits, materials, tools, and covers.
  - Voltage and steam tiers such as Steam, ULV, LV, MV, HV, EV, IV, LuV, ZPM, UV, UHV, and higher GTCEu tiers.
  - Circuit grades such as Basic, Good, Advanced, Micro, Nano, Quantum, Crystal, and Wetware, based on actual item IDs and circuit tags instead of tooltip guessing.
  - Power roles such as consuming EU, generating EU, accepting EU input, and emitting EU output.
  - GT EU/t values and amperage for tiered machines, generators, transformers, and energy hatches.
- GregTech items now show more useful right-click filter actions in AMI results:
  - Filter by tier, for example LV or HV.
  - Filter by GregTech kind, for example Machines, Power, or Circuits.
  - Filter by behavior fact, for example power or inputs EU.
  - Filter by circuit grade, for example Basic or Good.
- List view now supports GregTech EU fields separately from FE/RF:
  - GT EU Generation
  - GT EU Consumption
  - GT EU Input
  - GT EU Output
  - GT Amps
- Power and Machines List lenses can now recognize GregTech EU metadata even when the item does not expose FE/RF metadata.
- GregTech EU facts are included in AMI tooltips, search suggestions, capability detection, behavior grouping, and numeric search.

### Search Examples

- `?gregtech` finds GregTech/GTCEu-family content.
- `?gregtechTier:lv`, `?gregtechTier:hv`, or `?voltage:zpm` filters by voltage tier.
- `?gregtechKind:machines`, `?gregtechKind:circuits`, or `?gregtechKind:power` filters by GregTech item kind.
- `?gregtechCircuit:basic` or `?gregtechGrade:good` filters circuit grades.
- `?gregtechEnergy` finds GregTech items with indexed EU behavior.
- `?gregtechEnergyRole:inputs_eu` finds EU input hatches and similar input-side power parts.
- `?gregtechEnergy:4a` finds indexed 4A GregTech power parts.
- `>eugen:500` finds GregTech items producing at least 500 EU/t.
- `=euconsume:32` finds GregTech items consuming 32 EU/t.
- `>euinput:8000` finds GregTech items accepting at least 8000 EU/t.
- `>euoutput:30000` finds GregTech items emitting at least 30000 EU/t.
- `=amps:4` finds indexed 4A GregTech power parts.
- Broad metadata search also works with indexed facts, for example `~inputs_eu`, `~basic_circuit`, or `~lv_tier`.

### Changed

- GregTech now stays under the GregTech top-level category by default, so large packs do not scatter thousands of GregTech parts across unrelated normal categories.
- AMI still lets obvious non-GregTech gameplay categories escape when they are clear and useful, such as food, armor, weapons, and tools.
- Generic FE/RF energy fields remain separate from GregTech EU/t fields. AMI does not label GregTech EU as FE.
- The vanilla theme now more closely matches Minecraft's recipe book, including vanilla-style search controls, beveled item slots, subdued scrollbars, and tighter favorites spacing.
- The recipe book button now continues to toggle AMI while the new Start AMI Hidden setting only controls whether AMI opens hidden on new inventory screens.

### Fixed

- GregTech circuit context actions now get tier and circuit-grade filters even when runtime item tags are missing for known GTCEu circuit IDs such as Good Electronic Circuit.
- Autocomplete now suggests GregTech-specific property filters such as `?gregtechTier:`, `?gregtechCircuit:`, `?gregtechEnergy:`, and `?gregtechEnergyRole:`.
- List view now has a combined `GT EU/t` sort that works across GregTech generators, consumers, input hatches, and output hatches.
- GregTech material fasteners such as bolts, screws, and nuts no longer appear in the Ranged or Weapons List lenses just because they look projectile-like to generic metadata.
- Forge inventory overlays now follow the same render path as NeoForge, so AMI toggles and recipe viewer suppression remain consistent.

### Compatibility Notes

- The GregTech logic targets GTCEu/GregTech-style registry IDs, item classes, and tags. The indexed tier and circuit behavior is based on stable GTCEu patterns seen across the 1.20.1 and 1.21.1 codebases.
- Pack-specific or addon-specific GregTech items should still benefit when they use GTCEu namespaces, tags, machine classes, tiered naming, or standard energy hatch/generator naming.
- If an addon invents entirely custom naming and does not expose GregTech tags/classes, AMI may still categorize it as GregTech-family content but may not infer every tier, circuit grade, or EU/t fact.

## 1.0.2 - 2026-06-03

AMI 1.0.2 is a compatibility hotfix for Forge EMI installs and recipe viewer favorites.

### Fixed

- Fixed a Forge 1.20.1 crash during EMI recipe loading in production modpacks.
- Kept AMI favorites in sync when favoriting items from EMI recipe screens.
- Avoided unnecessary quest panel tree refreshes when visible quest content has not changed.

## 1.0.1 - 2026-06-03

AMI 1.0.1 is a hotfix for large Forge and NeoForge modpacks.

### Fixed

- Reduced the delay when focusing or clearing an empty AMI search box in large modpacks.
- Reused empty-query result projections so clearing search no longer rebuilds the full results tree unnecessarily.
- Restored Forge FTB Library sidebar exclusion handling so AMI avoids and reports the FTB sidebar area correctly.

## 1.0.0 - 2026-06-03

AMI 1.0.0 focuses on broad modpack compatibility, inventory overlay polish, and safer external integrations.

### Added

- Context actions for guides, quests, pack-author diagnostics, and web/wiki documentation.
- FTB Quests, guide indexing, and pack-author helper integrations.
- Cobblemon Pokemon indexing and action support when Cobblemon is present.
- External link confirmation before AMI opens browser URLs, with a config toggle for trusted setups.
- Publishing tasks for Forge and NeoForge release builds.

### Changed

- Inventory overlays now reserve exclusion zones for recipe viewers and third-party UI, including FTB Library sidebars.
- AMI result panels, sidebars, search controls, and visual hierarchy have been polished for denser modpack inventories.
- Compatibility indexing and categorization were refined for major mod families, storage items, food machines, and hidden or technical entries.
- Overlay rendering now does less repeated work while projecting and drawing AMI result panels.
- Item icon caching and projection were hardened across Forge and NeoForge.

### Fixed

- AMI sidebars now refresh correctly when reopening inventory screens.
- Tooltip and overlay render order no longer lets inventory slot outlines bleed through AMI tooltips.
- Client-only installs now avoid server-side class loading failures.
- Forge block entity capacity probing and Cobblemon reflection paths are more defensive.
- Cobblemon Pokemon cheat actions now route through the loader network path correctly.
- Search grouping and behavior post-processing no longer duplicate or misplace several result groups.

## 0.9.0 - 2026-05-30

AMI 0.9.0 focuses on recipe viewer support, panel actions, and persistent client settings.

### Added

- Recipe transfer from AMI panels through JEI and EMI when the current screen accepts transfers.
- Crafting actions in favorites, lookup history, crafting history, and craftable side panels.
- JEI crafting history entries for crafts started from AMI panels.
- Cheat-mode tooltip hints for panel items when cheat hotkeys are available.
- Saved client configuration for AMI UI and behavior settings.

### Changed

- Sidebar swap buttons now appear only when another panel mode is available.
- Sidebar buttons now line up more cleanly with the surrounding result panel controls.
- EMI and JEI favorites, hidden stacks, and sidebars now stay in sync more reliably with AMI.

### Fixed

- AMI config changes no longer revert after restarting the game.
- JEI recipe transfer now works from AMI lookup history and favorites.
- Crafting history panels now respond to AMI crafting actions.
- AMI now allows recipe viewer transfers even while AMI's own recipe index is still loading.
- The old Shift-hover debug ID tooltip no longer appears in normal item tooltips.
- Shift and Ctrl now update AMI action hints correctly while hovering panel entries.

## 0.3.1-alpha.1 - 2026-05-28

### Added

- Headless AMI search benchmark suite backed by the live Minecraft item registry.
- Local benchmark history written to `run/config/ami_benchmark_history.jsonl`.
- Gradle `check` now runs unit tests and the AMI benchmark suite.

### Performance

- Record indexed item count, query executions, average search latency, P99 latency, skipped anomalies, and total
  benchmark duration for each benchmark run.
