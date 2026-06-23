# AMI Feature Map

## Item Sources View

- User surface: right-clicking an item result can open a dedicated `Sources: <item>` list that explains direct and
  shallow indirect acquisition routes instead of filtering the normal search results. The same view is addressable from
  the search bar with `?sources=<item>`, such as `?sources=minecraft:leather` or `?sources=leather`.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceResolver.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/sources/ItemSourceListView.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultContextMenuActionBuilder.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/providers/LootTableProvider.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/providers/LootTableDropIndexer.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/providers/SpawnProvider.java`
- Tests:
  - `.\gradlew.bat :neoforge:test --tests "*ItemSourceResolverTest" --tests "*ItemSourceListViewTest" --tests "*ItemSourceQueryTest" --tests "*GlobalIndexGraphRevisionTest" --tests "*AmiIndexerServiceRecipeGraphSourceTest" --tests "*AmiIndexerServiceSourceDemandSourceTest" --tests "*ProviderRegistryLootRevisionSourceTest" --tests "*ResultContextMenuActionBuilderTest.itemNodesExposeSourcesActionWhenPanelCanOpenSourcesView" --tests "*SpawnProviderSourceTest" --tests "*LootTableDropIndexerTest" --tests "*LootTableProviderDeferredSourceTest"`
- State contract:
  - `Sources` is a normal context-menu action (`ami:sources`) gated by the existing context-menu policy and visible for
    item nodes when the owning panel can open the source view.
  - Source rows render as route cards, not plain filtered search rows: the primary source node owns the icon/name and
    card click target, the route line uses a compact action label plus output icon/name for the acquisition hop, and
    biome annotations render as separate icon chips instead of being folded into the mob text. Left-clicking a biome
    chip attempts AMI's normal `/locate biome` action when the player has command permission; right-clicking opens the
    normal AMI context menu for that biome node. Biome locate is permission-gated, not AMI cheat-mode-gated.
    Recipe rows are grouped under `Recipes` and show the recipe method/workstation/category as the primary icon/name
    (`Crafting Table`, `Smoker`, modded station block when AMI can identify one) instead of listing recipe ingredients.
  - Mob-drop rows should keep the player in the Sources view unless a specific biome chip is clicked. Clicking the
    general mob card body must not dump the player into a plain mob search; it may open external recipe-viewer entity
    information only when `RecipeViewerBridge` can prove a valid entity page exists.
  - Source rows are grouped in usefulness order: direct mob drops, recipes, one-hop indirect sources, salvage, structure
    loot, then trades. Recipe-graph rows stay under `Recipes` even when the underlying recipe type is smelting,
    drying, cutting, crushing, or another modded processing category; full ingredient layouts belong in the recipe
    viewer/details, not the source list.
  - The resolver walks the existing `SearchNode` graph only shallowly: direct output recipes and direct mob drops for the
    target, plus one useful ingredient hop for ingredient items that are mob drops. It skips recipes that require the
    target item to avoid recipe loops.
  - Source resolving canonicalizes clicked/search result nodes back to the live indexed node before reading graph edges,
    so projected UI nodes and copied result nodes do not lose `OUTPUT_OF`, `DROPS`, or spawn edges.
  - Mob spawn biome text comes from `ENTITY --SPAWNS_IN--> BIOME` edges populated from live biome spawn settings by
    `SpawnProvider`. Cached index restores must rebuild these graph-only spawn edges before source rows are resolved, so
    mob-drop rows keep their biome chips after a normal cached launch. If the initial AMI rebuild/restore happened
    before a client `Level` was available, opening a Sources route retries the spawn graph in the deferred Sources
    loading path and refreshes the open view when those biome edges arrive. `SpawnProvider` attaches both unresolved
    edge IDs and resolved biome nodes, and logs normal-run counts for scanned biomes, spawn entries, and recorded
    entity-to-biome edges.
  - If mob-drop rows exist but no mob-drop row has biome links, the Sources view shows a concise diagnostic above the
    groups (`Mob drops found, but no spawn biomes are indexed.`). This keeps partial source data visible while making
    the missing enrichment layer explicit.
  - Recipe graph nodes/edges are world/datapack-specific and are not serialized in the global index cache. Cached index
    restores must rebuild the recipe graph after restoring/rebuilding the runtime recipe index, otherwise Sources will
    have no recipe rows after a normal cached launch.
  - Generic mob drops are resolved from `ENTITY --DROPS--> ITEM` graph edges. `LootTableProvider` keeps the primary
    provider pass cheap, then `AmiIndexerService` schedules deferred loot source indexing after the main search service
    is published. The deferred scanner reads loaded `loot_table`/`loot_tables` JSON resources from the integrated
    server/datapack resource manager one at a time, indexes only `entities/<mob>` tables, and adds drop edges for
    explicit `minecraft:item` entries whose item and entity nodes are already indexed. It does not evaluate
    conditions/functions or index structure/block loot yet. The scanner must not use the client asset resource manager,
    which has no server loot-table JSON and will report a misleading zero-resource pass.
  - Opening a source route calls `AmiIndexerService.ensureSourcesForItem(<target id>)`. If recipe graph work is still
    pending it is started when a client level is available; if deferred loot source indexing has not run yet, it is
    started immediately rather than waiting for the passive tail task. While those source indexes are still pending, the
    source list renders a loading row.
  - Deferred graph-only source edges call `GlobalIndex.markGraphChanged()` when edges are added so an already-open source
    route can refresh after loot/source data arrives.
  - Empty source reports should explain why AMI thinks the panel is empty. The report carries diagnostic text rows under
    `No sources found`, including whether source indexes are still loading, the route target could not be resolved, mob
    drop data is unavailable on this client, server datapacks exposed no loot table JSON, no entity loot tables matched,
    or loot item references did not match indexed AMI nodes.
  - The source list is route-backed panel state. Opening `Sources` from a context menu replaces the search bar with
    `?sources=<registry id>` while remembering the previous query for the source-view back button. Clicking a linked row
    replaces the query with that node's registry id for normal AMI navigation/search.
  - Pasting or typing a `?sources=<target>` route into the search bar bypasses the normal search debounce on every loader
    so the panel enters the source view immediately instead of briefly treating the route as ordinary search text.
  - Search suggestions/help treat `?sources=` as static route syntax, not warmed source data. Typing `?s` can suggest
    `?sources=`, and the help popup advertises `?sources=leather`; this does not require item-source cache prefill or
    source-index warmup.

## Release Artifact Metadata

- User surface: launchers and mod managers should show AMI's icon, description, author, homepage/source links, and issue
  tracker consistently for Fabric, Forge, and NeoForge jars.
- Main files:
  - `fabric/src/main/resources/fabric.mod.json`
  - `forge/src/main/resources/META-INF/mods.toml`
  - `xplat/src/main/templates/META-INF/neoforge.mods.toml`
  - `xplat/src/main/resources/icon.png`
  - `build.gradle`
- Tests:
  - `.\gradlew.bat verifyLauncherMetadata`
  - `.\gradlew.bat verifyReleaseArtifacts`
- State contract:
  - `icon.png` lives at the root of the packaged jar via shared `xplat` resources.
  - Fabric declares `"icon": "icon.png"` plus `contact.homepage`, `contact.sources`, and `contact.issues`.
  - Forge and NeoForge declare `logoFile = "icon.png"`, `logoBlur = false`, `displayURL`, and `issueTrackerURL`.
  - `verifyReleaseArtifacts` depends on `verifyLauncherMetadata` so publishing fails before upload if launcher metadata
    regresses.

## Result Projection Performance

- User surface: inventory overlay result panels should not rebuild the full result tree every frame while the visible
  source results are unchanged.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsViewProjector.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsTreeBuilder.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/AmiOntologyKinds.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/index/AmiOntologyKindsTokenTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ResultsProcessorTest.java`
- State contract:
  - Empty-query projections are cached by the actual resolved source signature plus view/filter/sort settings, not merely
    by volatile runtime provider revision.
  - Category kind classification is memoized per node/category/subcategory and relevant ontology metadata.

## Search Suggestion Performance

- User surface: search-bar completions should remain cheap during indexing, config changes, and large modpack metadata
  rebuilds.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/index/query/SearchSuggestions.java`
- Tests:
  - Compile both loaders after shared query changes: `.\gradlew.bat :neoforge:compileJava :forge:compileJava`.
- State contract:
  - Suggestion vocabulary is cached by index revision and visibility config.
  - Metadata tokenization uses a direct comma/whitespace scanner instead of regex splitting in the vocabulary build hot
    path.

## Search Bar Suggestions And Query History

- User surface: the AMI search bar should let users recall submitted queries with `Up`/`Down` when no suggestion popup
  is open, while still allowing the popup to own arrow-key navigation when suggestions are visible.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/overlay/AbstractSearchBarWidget.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/SearchQueryHistory.java`
- Tests:
  - `.\gradlew.bat :neoforge:test --tests "*SearchBarClearFocusContractTest"`
  - `.\gradlew.bat :neoforge:compileJava :forge:compileJava :fabric:compileJava`
- State contract:
  - Visible suggestions take priority over query-history recall for `Up`/`Down`.
  - Clearing the search bar with the mouse keeps focus but dismisses the suggestion popup, so the next `Up`/`Down`
    recalls submitted history instead of reopening stale empty-query suggestions.
  - Typing after a clear continues to rebuild suggestions normally from the current query and cursor position.

## Item Stack Icon Render Path

- User surface: item result grids should render real `ItemStack` icons through Minecraft's normal item model path, avoiding
  cold-cache bitmap pop-in and framebuffer thumbnail artifacts in large result sets.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ItemGridView.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/ItemIconBatchRenderer.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/icon/ItemIconRenderer.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ItemGridViewTextureCachePolicyTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ItemGridViewTest.java`
- State contract:
  - Real `ItemStack` icons are not converted into AMI-owned framebuffer screenshots or dynamic thumbnail textures.
    Minecraft already caches item models and atlas textures; AMI should batch same-frame item draws instead of building a
    second bitmap cache that can pop in after world join or break GL state.
  - Stable, non-hovered item stacks use `ItemIconBatchRenderer`, which groups normal item model draws by lighting mode
    and lets Minecraft's item renderer own model, texture, depth, and custom renderer behavior.
  - Hovered items and recipe-viewer drag states keep direct `GuiGraphics.renderItem` rendering so wiggle/drag affordances
    remain live.
  - Entity, player, and other non-item generated images may use their own atlas/cache systems. Do not route normal item
    stacks through those framebuffer thumbnail paths.
  - Do not reintroduce an `ami.itemIconCache` flag or equivalent boolean switch. The policy is structural: item stacks use
    the normal Minecraft item render path.

## Discovery Checklist

- User surface: biome, structure, and edible item results can show local discovery state when `features.enable-discovery-checklist` is enabled.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/discovery/AmiDiscoveryState.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsViewProjector.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ListLens.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/DiscoveryVisuals.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/neoforge/AMIClient.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/DiscoveryChecklistProjectionTest.java`
- State contract:
  - Client-local, feature-gated, and stored per world/dimension/player scope under the AMI discovery config folder.
  - Biomes are marked discovered when the local player enters them.
  - Structures are marked discovered in singleplayer/local integrated-server sessions when the player's current block position is inside a structure reported by `ServerLevel.structureManager().getAllStructuresAt(...)`.
  - Edible item results are marked discovered/tasted when the local player finishes consuming a food item; MVP identity is the consumed item registry ID.
  - Runtime query terms: `discovered` / `undiscovered` match all checklist nodes, `visited` / `unvisited` match biomes and structures, and `tasted` / `untasted` match edible item results. These terms can be combined with normal searches, e.g. `untasted apple`.

## Runtime Waypoint Compat

- User surface: live map-mod waypoints appear as merged AMI search results with provider-aware tooltips and context-menu actions.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/player/PlayerWaypointProviders.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/player/FtbChunksWaypointProvider.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/util/tooltip/PlayerTooltipFact.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultContextMenuActionBuilder.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/config/AmiConfig.java`
- Tests:
  - `.\gradlew.bat :neoforge:test --tests "*PlayerWaypointProvidersTest" --tests "*AmiTooltipComposerTest" --tests "*ResultContextMenuActionBuilderTest" --tests "*AmiConfigTest"`
  - `.\gradlew.bat :neoforge:compileJava :forge:compileJava :fabric:compileJava`
- State contract:
  - Runtime waypoints merge across providers only when dimension, integer coordinates, and normalized name all match; otherwise AMI keeps them separate.
  - Left-click uses the configured provider priority. Right-click preserves provider-specific actions from all merged providers.
  - Runtime refresh is light-cadence plus explicit invalidation after AMI-triggered edits and external event hooks; AMI does not run a permanent watcher thread.

## Category Result Grouping

- User surface: category-grouped result trees should show item ontology buckets plus explicitly categorized world/entity
  buckets, without dumping auxiliary recipe-viewer ingredients, fluids, recipes, or internal entity helpers into terminal
  `Misc`.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsTreeBuilder.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsGroupingPostProcessor.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/AmiOntology.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsViewProjector.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ResultsProcessorTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/RuntimeMirrorCategoryGroupAuditTest.java`
- State contract:
  - Terminal `misc/unknown` is an item fallback in category view. Non-item nodes that only reach `AmiOntology.MISC`
    through fallback classification are skipped from category grouping.
  - Non-item nodes with concrete categories still render in category view, such as `bestiary` entities and
    `environment` biomes/structures.
  - Masonry shape buckets with many sibling variants (`slab`, `stairs`, and `wall`) may add compact material-root groups
    from `subtypeOf` / `materialGroup`; `full_block` stays flat unless another explicit category split or collapse
    family applies.
  - `INGREDIENT`, `FLUID`, and `RECIPE` nodes remain searchable and renderable in non-category contexts; the guard only
    prevents category view from manufacturing a visible terminal `Misc` bucket for them.

## Search Document Result Rows

- User surface: full-size search panels can show non-item document hits above the normal result tree as scrollable
  top-of-results rows, including advancement, quest, and guide-page matches.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsViewProjector.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/AdvancementRuntimeDocuments.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/AdvancementResultsProjector.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/RecipeViewerBridge.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/JeiRecipeBridge.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/config/AmiConfig.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/QuestResultsProjector.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/GuideResultsProjector.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/AmiAdvancementSearchIndex.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/AmiQuestSearchIndex.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/index/AmiGuideSearchIndex.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/AdvancementResultsProjectorTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ResultsViewProjectorAdvancementTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/QuestResultsProjectorTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/GuideResultsProjectorTest.java`
- State contract:
  - Document rows are separate from normal `SearchNode` indexing/search; they do not reintroduce advancements into the
    atlas item/tree index.
  - Search source config toggles can suppress guide, quest, advancement, player, and waypoint sources independently.
  - The section stack is part of the result view scroll range, not pinned above it.
  - Advancement rows are built from the live client advancement tree when present. If the client has no advancement data
    or no visible matching advancements, the advancement section is omitted.
  - Advancement rows include client-known player progress state (`In progress`, `Not started`, `Completed`, or
    `Status unknown`). Completed advancements remain searchable, but unfinished advancements sort before completed ones
    when lexical relevance is otherwise tied.
  - Advancement rows are suppressed for blank searches, even when advancement data exists.
  - Opening an advancement row first tries Just Enough Advancements' JEI ingredient view when `jea` and JEI are present;
    otherwise it opens the vanilla advancements screen with the advancement's root tab selected.
  - Hidden advancement displays are skipped to avoid exposing vanilla/mod spoiler entries outside the advancement UI's
    normal visibility rules.

## AMI Recipe Viewer Crafting Layout

- User surface: the AMI-owned recipe view should always show a full 3x3 crafting placeholder grid for crafting recipes,
  even when the recipe only uses a subset of slots.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeViewerScreen.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`
- Tests:
  - `.\gradlew.bat :neoforge:test --tests "*RecipeDisplayHelperLayoutTest"`
- State contract:
  - Crafting layouts expose nine `SlotPosition`s so the AMI recipe screen can render all placeholder slots; changing only
    `gridWidth`/`gridHeight` is not enough because `RecipeViewerScreen` draws input slot chrome from `layout.inputs()`.
  - Shaped crafting recipes use JEI-style placement inside the 3x3 placeholder: `1x1` centers, `1x2` and `1x3` use the
    center column, `2x1` and `3x1` use the middle row, `2x2` stays in the top-left quadrant, and `3x2` uses the bottom
    two rows.
  - The crafting arrow and output slot stay centered against the full 3x3 placeholder, not the recipe's minimal shaped
    footprint.

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

## Craftables Sidebar

- User surface: the Craftables sidebar should behave like a practical "what can I make now?" list, not a creative/dev
  catalog.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/overlay/AmiSidebarSyncHandler.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/VanillaCraftablesService.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/client/overlay/OverlayWidgetManager.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/client/overlay/SidebarPanelWidget.java`
- Tests:
  - `.\gradlew.bat :neoforge:test --tests "*AmiSidebarSyncHandlerTest"`
- State contract:
  - The craftable source is Minecraft's current client recipe-book craftability state, resolved from the local player's
    actual inventory contents.
  - AMI filters craftable outputs to survival-visible item nodes only; creative, cheat, dev, and hidden indexed nodes are
    not shown in Craftables even while the player is in Creative mode or AMI cheat/dev visibility is enabled.
  - Blank Craftables output order is deterministic and alphabetical by display name.
  - When the AMI search bar has a query, sidebar panels receive the same query/search service as main result panels, so
    Craftables is narrowed by the active search while retaining the current craftable source as the scope.

## Static JEI Plugin Compat

- User surface: when JEI is absent, mods that only publish static JEI plugins can still have AMI discover their recipe
  registrations and surface captured vanilla-style recipes through AMI lookups on NeoForge, Fabric, and Forge 1.20.1.
- Main files:
  - `jei-compat-api/src/main/java/mezz/jei/api/`
  - `jei-compat-api/src/main/java/com/sanhiruzu/ami/compat/jei/`
  - `jei-compat-api-forge/src/main/java/`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/jei/runtime/`
  - `forge/src/main/java/com/sanhiruzu/ami/forge/AMIClient.java`
  - `forge/src/main/java/com/sanhiruzu/ami/forge/ForgePlatformHelper.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/neoforge/AMIClient.java`
  - `fabric/src/main/java/com/sanhiruzu/ami/fabric/AmiFabricClient.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/jei/runtime/CompatJeiRecipeService.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/jei/runtime/CompatJeiCategoryLayoutResolver.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/compat/jei/runtime/CompatJeiGuiResolver.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/RecipeLookupKeyHandler.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/recipe/RecipeDisplayHelper.java`
- Tests:
  - `.\gradlew.bat :jei-compat-api:test --tests "*JeiCompatArtifactBoundaryTest" :neoforge:test --tests "*CompatJeiPluginLoaderTest" --tests "*CompatJeiRegistrationCaptureTest" --tests "*CompatJeiRecipeServiceTest" :forge:test --tests "*ForgeJeiStaticCompatSourceTest"`
  - Viewer layout capture smoke at JVM level: `.\gradlew.bat :neoforge:test --tests "*RecipeDisplayHelperCompatLayoutTest"`
  - Focused GUI handler lookup coverage: `.\gradlew.bat :neoforge:test --tests "*CompatJeiGuiResolverTest" --tests "*RecipeLookupKeyHandlerTest"`
  - Build the Forge shim artifact after JEI surface changes: `.\gradlew.bat :jei-compat-api-forge:jar`
  - Compile all active loaders after shared compat changes: `.\gradlew.bat :neoforge:compileJava :fabric:compileJava :forge:compileJava`
- State contract:
  - `jei-compat-api` is the 1.21.1 shim artifact for NeoForge/Fabric. `jei-compat-api-forge` is the 1.20.1 Forge shim
    artifact. Both package JEI-namespaced API classes plus the AMI-owned static-plugin capture/registry helpers that
    compile against those shim contracts.
  - `CompatJeiPluginLoader` now splits registration into startup-safe phases and world-ready deferred phases. GUI
    handlers, transfer handlers, subtype interpreters, categories, catalysts, and aliases register immediately; extra
    ingredients and recipes retry after a client world exists.
  - `CompatJeiPluginRegistry` tracks a monotonic revision so deferred registrations can mutate the installed registry
    without swapping the singleton instance.
  - `CompatJeiRecipeService` lives under `xplat/.../compat/jei/runtime/` and rebuilds AMI-side recipe/usage indexes
    from the installed compat plugin registry when either the registry instance or its revision changes.
  - `CompatJeiCategoryLayoutResolver` uses captured `IRecipeCategory` registrations as an AMI-native layout source:
    for a live AMI recipe entry, it matches the compat category by recipe type id, adapts the recipe to the category's
    expected class when needed (including `RecipeHolder` on 1.21.1), replays `setRecipe(...)` through a recording JEI
    layout builder, and feeds the captured title/slot positions back into `RecipeDisplayHelper` before the generic
    fallback renderer runs.
  - NeoForge discovers compat plugins from `@JeiPlugin` scan data; Fabric mirrors JEI's `jei_mod_plugin` entrypoint
    contract instead of annotation scanning; Forge mirrors JEI Forge's `ModList.get().getAllScanData()` annotation scan.
  - The shared capture surface now records more than recipe/category basics: subtype interpreters, extra ingredients, GUI
    handlers, recipe transfer handlers, universal transfer handlers, catalysts, and ingredient aliases all land in the
    installed compat registry for later AMI-native translation.
  - JEI GUI handler parity is not limited to recipe click areas. `CompatJeiGuiResolver` must also inspect captured
    `addGuiContainerHandler` / `addGenericGuiContainerHandler` registrations, reflect `getClickableIngredientUnderMouse`,
    and prefer the returned ingredient stack over hovered-slot fallback when opening recipes or uses. This is required
    for mods such as Create that expose lookup hotspots only through JEI container handlers.
  - Captured compat recipes are merged into AMI recipe/usage lookups on NeoForge, Fabric, and Forge. Forge currently
    excludes the live `AmiJeiPlugin` runtime bridge class from its 1.20.1 compile path while static compat is being
    brought up.
  - Runtime smoke for JEI-absent Create click areas should target the dedicated NeoForge run directory
    `run/neoforge-create-compat`, which is intended to hold AMI, Create, and AutoMine Testing without JEI.

## Pack-Author Dev Visibility Markers

- User surface: when `cheat.dev-mode` is enabled, result icons for items hidden from normal players show a red `X` badge so pack authors can distinguish dev/creative/cheat/hidden entries at a glance.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/AccessLevelVisuals.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsTreeView.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ItemGridView.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/AccessLevelVisualsTest.java`
- State contract:
  - Marker is visual-only and does not change search, grouping, or tooltip behavior.
  - Marker appears only while `AmiConfig.devMode` is true.
  - Normal-player hidden status is inferred from `accessLevel != survival` or `visibility = hidden`.

## Inventory Overlay Render Order

- User surface: AMI side panels, search bar, bottom-left AMI button, result icons, AMI tooltips, context menus, and vanilla container tooltips/status-effect tooltips.
- Main files:
  - `forge/src/main/java/com/sanhiruzu/ami/client/InventoryOverlayHandler.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/client/InventoryOverlayHandler.java`
  - `forge/src/main/java/com/sanhiruzu/ami/client/overlay/OverlayWidgetManager.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/client/overlay/OverlayWidgetManager.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/AmiRenderPhase.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ItemGridView.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultsTreeView.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/tooltip/AmiResultTooltipElements.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/tooltip/AmiTooltipRenderer.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/platform/IPlatformHelper.java`
- Tests:
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/ContainerTooltipOwnershipPolicyTest.java`
  - `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ItemResultTooltipPolicyTest.java`
  - Runtime visual smoke through AutoMine Testing is preferred for tooltip/panel z-order because vanilla tooltip timing depends on the live screen render path.
  - Compile all loaders after shared tooltip/platform render changes: `.\gradlew.bat :neoforge:compileJava :forge:compileJava :fabric:compileJava`.
- State contract:
  - Container screens split ownership by render timing, not z-fighting. AMI's durable body (panels, result icons, search bar, buttons) renders from `ContainerScreenEvent.Render.Foreground` — before vanilla/status tooltips — so those tooltips win wherever they overlap AMI. AMI-owned transient UI (AMI tooltips, dropdowns, context menus, hints) renders from `ScreenEvent.Render.Post` — after vanilla tooltips — so it wins over AMI's own body.
  - The foreground handler translates by `-getGuiLeft(), -getGuiTop()` to undo the container's `leftPos/topPos` translation, restoring the screen-space coordinates AMI layouts are computed in. Partial tick is captured in `ScreenEvent.Render.Pre` (the foreground event carries none).
  - Durable z values (`OverlayLayers`: panels 200, search bar 201, icons ~350 via the item GUI offset) stay below `VANILLA_TOOLTIP = 400`, so the later-drawn vanilla tooltip wins by paint order. Note that `g.renderItem` writes real 3D model depth — block-item icons reach past z=400 — so paint order alone is not enough: `renderBase` clears `GL_DEPTH_BUFFER_BIT` after its final flush to leave no depth residue. This is residue cleanup of AMI's own layer (generalizing `IconRenderState`'s promise), not the forbidden practice of clearing depth to reorder vanilla tooltips.
  - Scheduled screen reinits are applied in `ScreenEvent.Render.Pre`, never from the foreground event, so AMI never reinitializes a screen mid-render.
  - While AMI is visible, `ScreenEvent.RenderInventoryMobEffects` forces compact status-effect indicators, matching JEI's side-overlay behavior. When the mouse owns the status-effect strip, AMI suppresses its own hover (mouse routed to `Integer.MIN_VALUE`) so the vanilla status tooltip is not covered by AMI transient UI.
  - AMI recipe/custom screens render base and top layers together from `Render.Post` because AMI owns the whole screen. Non-container `Screen`s have no foreground hook, so for **external** recipe viewers (JEI `RecipesGui`, EMI `RecipeScreen`) — which draw an opaque ~75% background then their own tooltips inside their `render()` — AMI cannot render before their tooltip. Instead AMI captures the external tooltip via `RenderTooltipEvent.GatherComponents`/`Pre`, cancels the external draw, and re-hosts it above AMI's base in `Render.Post` (`renderPendingExternalTooltip`, at `TRANSIENT_TOOLTIP` z). This replay is scoped to external recipe screens only (`isExternalRecipeScreen`); container tooltips are never replayed — they rely on the foreground/post split. AMI's own `RecipeViewerScreen` is excluded because AMI controls its render order directly.
  - Do not fix tooltip ordering by replaying vanilla tooltips, re-rendering status effects via reflection, or adding extra tooltip z translations; rely on the foreground/post timing split and Minecraft's normal `GuiGraphics.renderTooltip` path. (Clearing AMI's own depth residue at the end of `renderBase` is allowed and required — see above — but never clear depth to force a *vanilla* tooltip above or below AMI.)
  - AMI result item tooltips are rendered through `AmiResultTooltipElements` as mixed `Either<FormattedText, TooltipComponent>` elements, then handed to `IPlatformHelper.renderTooltipElements`. Forge/NeoForge call the native `renderComponentTooltipFromElements` path so `RenderTooltipEvent.GatherComponents` additions such as food overlays remain component-aware. Fabric has an explicit platform fallback to vanilla's text-plus-single-component tooltip overload.
  - Result item tooltip footers and AMI-added mod names are appended in the result tooltip element builder, not through a temporary global `ItemTooltipEvent` context. The global item tooltip handler is limited to tooltip work that is safe for normal inventory tooltips too.
  - `AmiRenderPhase` guards durable result rendering from the top layer; use `-Dami.strictRenderPhase=true` to make violations fatal during debugging.

## Entity Icon Atlas

- User surface: entity result icons should render from cached atlas sprites after initial bake without causing recurring
  client tick hitches. Synesthesia-scale regression runs should measure indexing, entity icon warmup, FPS/tick timing,
  memory growth, and entity icon failures without manual UI steps.
- Main files:
  - `xplat/src/main/java/com/sanhiruzu/ami/client/EntityIconCache.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/AmiClientTelemetry.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/client/EntityIconWarmupMetrics.java`
  - `xplat/src/main/java/com/sanhiruzu/ami/api/AmiRuntimeDebugApi.java`
  - `forge/src/main/java/com/sanhiruzu/ami/client/icon/EntityIconRenderer.java`
  - `forge/src/main/java/com/sanhiruzu/ami/client/icon/PlayerModelRenderer.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/client/icon/EntityIconRenderer.java`
  - `neoforge/src/main/java/com/sanhiruzu/ami/neoforge/AMIClient.java`
  - `tools/profile-synesthesia-ami.ps1`
  - `tools/export-ami-benchmark-release.ps1`
- Tests:
  - Guard Forge-vs-NeoForge facing contracts with `.\gradlew.bat :forge:test --tests "*EntityFacingConstantsTest" --tests "*ForgeEntityRenderContractTest"`.
  - Compile the NeoForge client sources after cache/render-thread changes: `.\gradlew.bat :neoforge:compileJava`.
  - Compile both loaders after shared telemetry or scheduler changes: `.\gradlew.bat :neoforge:compileJava :forge:compileJava`.
  - Run focused scheduler/cache tests: `.\gradlew.bat :neoforge:test --tests "*AdaptiveTickSchedulerTest" --tests "*AmiClientWorkSchedulerTest" --tests "*EntityIcon*Test" --tests "*AmiIndexerServiceBusyTest"`.
  - Use a runtime profiler or AutoMine inventory screenshot smoke when validating visible atlas behavior in-game.
  - For Synesthesia regression capture, run `.\tools\profile-synesthesia-ami.ps1 -StartJfr`; compare against a prior
    `summary.json` with `-BaselineSummary <path> -FailOnRegression` when a pass/fail result is needed.
  - To publish a compact GitHub release benchmark asset, run
    `.\tools\export-ami-benchmark-release.ps1 -Summary <current summary.json> -BaselineSummary <baseline summary.json>`.
- State contract:
  - Entity framebuffer baking, atlas pixel mutation, dynamic texture upload, and cached icon blits stay on the client/render
    thread.
  - Persistent cache writes are best-effort per-icon PNG writes on the daemon `AMI Entity Icon Atlas Writer`; they must
    not encode or write the full runtime atlas from `ClientTickEvent.Post`.
  - Persistent cache reads are lazy and bounded by the existing bake budget. Do not eagerly load every cached entity icon
    at atlas creation or world join; large modpacks can contain many expensive or buggy entity renderers.
  - Keep the number of runtime atlas sizes small. Each active 2048x2048 atlas is roughly 16 MiB of CPU-side native image
    memory plus the corresponding GL texture; normal grid/list rendering should continue to use the shared 16px size
    unless a larger UI surface has a concrete need.
  - Pending entity bake requests are capped by `ami.entityIconAtlasPendingBakeLimit` to avoid retaining many queued
    renderer lambdas and entity instances when warmup discovers icons faster than the adaptive lane can process them.
    Live `LivingEntity` instances used by the renderer are held in a bounded access-order cache controlled by
    `ami.entityIconEntityCacheLimit`, and are also cleared on renderer invalidation.
  - Visible cache misses are prioritized ahead of passive background warmup requests. Passive warmup does not advance past
    a renderable entity when the bounded bake queue is full; it retries later so `warmup.done` means every candidate was
    actually cached, queued, skipped, or failed, not merely observed once.
  - Atlas baking uses the central `AmiClientWorkScheduler` lane `entityIconAtlasBake`, backed by
    `AdaptiveTickScheduler`: it starts from `ami.entityIconAtlasBakeIntervalTicks`, can speed up toward
    `ami.entityIconAtlasAdaptiveMinIntervalTicks` after cheap samples, and backs off toward
    `ami.entityIconAtlasAdaptiveMaxIntervalTicks` when a cache load/render bake exceeds the backoff budget. Rendering
    remains single-threaded; only persistence is asynchronous. Each scheduler run can process multiple cheap queued bakes
    up to `ami.entityIconAtlasBakePerTick` and the soft `ami.entityIconAtlasBakeBudgetMs` time budget; expensive bakes
    still complete one at a time and then feed the adaptive backoff logic.
  - Entity atlas warmup and pending bake processing are paused while `AmiIndexerService.isBusy()` reports an active
    primary rebuild, deferred namespace index, deferred guide index, recipe index rebuild, or pending recipe index
    rebuild. This prevents entity construction/render bakes from competing with AMI's own high-impact indexing phases.
  - Baked entity icons should preserve alpha. The bake target clears to transparent black, then the cache strips
    transparent/opaque-black edge-connected background pixels before atlas insertion and per-icon PNG persistence.
  - Forge and NeoForge do not share the same `InventoryScreen.renderEntityInInventory` contract. Forge static entity and
    player renders must use Forge's patched `renderEntityInInventoryFollowsAngle(...)` helper for non-hover/front-facing
    renders; NeoForge uses the quaternion/vector overload directly with a different default facing.
  - If entity rendering, texture validation, or atlas baking fails, render a concrete item fallback first: the entity's
    spawn egg when one can be resolved, then special proxy items such as `experience_bottle`. If no item fallback exists,
    render a red error marker rather than the generic text tile so broken entity icons are visibly diagnosable.
    Cache misses use this same fallback while an entity thumbnail is still pending, so large packs get immediate visible
    icons instead of blank cells during a long background fill.
  - The persistent cache version changes when the on-disk icon layout changes so old full-atlas files do not mix with the
    per-icon cache format.
  - The entity icon cache fingerprint is computed once per active atlas generation and cleared on renderer/resource
    invalidation. Do not call the full mod-list/resource-pack fingerprint builder from normal icon blits; large grids can
    otherwise sort/hash loaded mods and packs once per visible entity icon.
  - `/ami/status` is the stable telemetry surface for the runtime harness. It reports index readiness/busy state,
    node-type counts, JVM heap/process CPU, AMI client tick timing, frame interval/FPS estimate on NeoForge, entity icon
    warmup progress, cache counters, atlas occupancy, pending bake count, pending writer count, and failure counts.
  - `tools/profile-synesthesia-ami.ps1` launches or attaches to the Prism Synesthesia instance, installs AutoMine Testing,
    opens the configured world and inventory, optionally forces AMI reindexing, samples `/ami/status` plus process memory,
    captures heap/JFR/log/screenshot artifacts, and stops only after the minimum sample window and entity icon work are
    idle or the warmup timeout is reached.
  - `tools/export-ami-benchmark-release.ps1` converts a profiler `summary.json` and optional baseline into Markdown and
    JSON benchmark assets for a GitHub release. The GitHub Actions release runner should not try to execute the full
    Synesthesia benchmark because the pack, Prism instance, GPU/runtime state, and local world are machine-specific.
