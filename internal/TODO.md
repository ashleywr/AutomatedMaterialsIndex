# AMI Project: Next Steps and Goals

This document outlines the current state and the upcoming tasks to fully realize the Suginami-style interface for the Automated Materials Index (AMI).

## ~~Immediate Goal: Better Visual Grouping and Colour Extraction~~
~~Right now, the `ItemProvider` sets all item `colorBucket` metadata to `"gray"`. This causes the variant group swatches in the list view (the little colored dots under the folder header) to always show up as gray squares, regardless of what's inside.~~

~~**Task:** Improve the colour parsing logic in `ItemProvider.java`.~~
~~- **Implementation Strategy:** Write a utility method that inspects an item's registry name, translation key, or tags for color keywords (e.g., "red", "blue", "cyan", "magenta").~~ 
~~- Map these keywords to the known buckets in `ResultsTreeView.bucketToArgb` so the UI can render accurate color dots for grouped variants (like a stack of coloured wool).~~

[x] **Subtype Engine (Vanilla Edge Cases + Modded Hero Items)**

Handle items where one registry ID covers many visual variants, without melting the client.

**Vanilla permutation loops** — [DONE] manual `ItemStack` generation for each known edge case:
- `minecraft:potion` — all potion effects × splash/lingering
- `minecraft:enchanted_book` — all enchantments × all levels
- `minecraft:spawn_egg` — iterate `BuiltInRegistries.ENTITY_TYPE` for all tagged eggs
- `minecraft:suspicious_stew` — all effect variants
- `minecraft:firework_rocket` — representative shapes/colors only
- `minecraft:goat_horn` — all instrument variants

**Hard cap** — [DONE] if any generation loop exceeds **150 variants**, abort and register only the base item. Prevents poorly-coded mods from producing 10,000-entry batteries that crash the indexer.

**`IAmiPlugin` Hero Item API (modded)** — [DONE] for mods like Silent Gear / Apotheosis that generate infinite modular variants, do *not* attempt to compute them. Expose an API hook where those mod devs (or our own compat layer) can hand us a curated list of "Hero Items" (e.g., all-diamond pick, all-wood pick) to stand in as representatives.

**Files to touch:** `ItemProvider.java`, `IAmiPlugin.java`, `AmiPluginRegistry.java`, `SubtypeExpander.java`.

---

[x] **Group By Engine (Condensing the Material List)**

Dynamically fold items into collapsible parent nodes on three axes so the UI never becomes a chaotic wall of blocks.

### [x] Group by Shape
- Rely exclusively on Mojang's `BlockTags` (`STAIRS`, `SLABS`, `WALLS`, `FENCES`, `DOORS`, `TRAPDOORS`, `BUTTONS`, `PRESSURE_PLATES`) to bucket shape variants together — no regex, no string matching.
- [DONE] Promoted to the `GroupingEngine` as a first-class pass.
- [DONE] **Grid Union Rendering:** Expanded groups in Grid view are encapsulated in a contiguous gold border with a persistent, clickable header icon.

### [x] Group by Color
- [DONE] Hybrid approach: check `c:dyes/*` / `minecraft:*_wool` tags first (with dynamic discovery for modded colors), then fall back to lexical prefix matching.
- [DONE] Apply a **sorting weight** so the Base Block (e.g., Red Wool, Red Terracotta) is always chosen as the group representative icon.

### [x] Group by Family (Super-Families)
- [DONE] Created a new "Family" grouping axis that aggressively strips state and sub-material prefixes (`stripped_`, `waxed_`, `exposed_`, `chiseled_`, `cut_`).
- [DONE] Unifies entire franchises (All Mangrove, All Copper, All Cod) into single intuitive folders.

### [x] Group by Material — Three-Phase Waterfall
Process in strict order; stop at the first hit:
1. **Material Family Pass (Indexed)** — [DONE] Items are categorized into material groups (Stone, Wood, Soil, Glass) during indexing via `OntologyClassifier`.
2. **BlockFamilies API** — query 1.21.1's native `BlockFamilies` registry for guaranteed exact family membership.
3. **Stonecutter Heuristics** — [DONE] reverse-engineer crafting intent: if a Stonecutter recipe maps `X Block → X Stair`, group them under the `X` material namespace.
4. **Tag-Lexical Fallback** — [DONE] **Dynamic Discovery:** Engine now scans all registry tags during initialization to build a vocabulary of shape/container keywords (`bucket`, `spawn_egg`, `rail`), allowing it to scale to any modpack without hardcoded lists.

### [x] Universal Organization
- [DONE] **Bottom-Sorting:** All grouping axes now consistently push "Unknown", "Item", and "Block" categories to the bottom of the results.
- [DONE] **Count Sort:** Added a "Count" sorting option to quickly identify the largest or smallest material/shape groups.
- [DONE] **Performance:** Moved all indexing and heuristic discovery to a background thread with a centered "Indexing..." UI overlay to prevent main-thread stutters.
- [DONE] **Reactive Access:** Results automatically re-filter when player game mode or dev-mode config changes (e.g., spawn eggs vanishing when switching to Survival).

**Files to touch:** `GroupingEngine.java`, `ItemProvider.java`, `ResultsProcessor.java`, `UniversalResultsPanel.java`, `GlobalIndexCache.java`, `OverlayWidgetManager.java`.

---

## Bugs
- ~~Compact mode left a dead 22px header strip above the grid (toggle button row, empty on the left). Fixed: grid now spans the full panel height and the toggle button is overlaid in the top-right corner.~~
- ~~Entity icons invisible / grid draws bleeding outside panel. Root cause: `EntityIconRenderer.render()` called `g.enableScissor(0, 0, 16, 16)` using local pose coordinates instead of screen coordinates. The empty intersection with the grid scissor disabled GL scissor entirely, so `endBatch()` inside `renderEntityInInventory` flushed all pending grid draws without clipping. Fixed: removed the inner scissor calls; the grid's outer scissor provides sufficient containment.~~
- ~~Boats and minecarts appeared under "Mobs". Fixed: vehicle entity types (boats, minecarts, rafts) are now classified as Environment → Vehicles instead of Entities, with no dev-only restriction. `ONTOLOGY.md` updated to match.~~
- ~~Search box did not respond to typing — fixed by subscribing `ScreenEvent.CharacterTyped.Pre` and `ScreenEvent.MouseButtonPressed.Pre` in `InventoryOverlayHandler` to bypass the screen focus system (which EMI can clear). We now track focus with `searchBarInputActive` and forward events directly.~~

## UI/UX Polish
- Ensure the tree view scrolls smoothly and maintains high framerates even with large modpacks.
- Check the layout constraints so that switching between Grid and List view is seamless.
- ~~Search bar: verified `Home`, `End`, `Ctrl+Left`, `Ctrl+Right`, and `Shift` selection route through `EditBox` from `ScreenEvent.KeyPressed.Pre` for all `AbstractContainerScreen` instances.~~
- ~~Search bar: replaced the double-click highlight toggle with token selection on the current query token.~~
- [x] Search bar: right-click now clears the field, unfocuses, and triggers a search refresh (empty query).
- [x] Search bar: add a right-click context menu for Cut, Copy, and Paste. (Disabled by default via config).
- [x] Search bar: refined `Escape` so the first press only unfocuses the field and a second press can close the screen if needed.
- [x] Search bar: add a clear button inside the field when text is present.
- [x] Search bar: make cursor movement token-aware for AMI operators like `@mod`, `#tag`, and `-exclude`.
- [x] Search bar: show a recent-search dropdown when the field is focused and empty (disabled by default, configurable).
- [ ] Search bar: decide whether the input filter should stay ASCII-only or allow broader Unicode / IME-friendly entry.
- [x] Search bar: show placeholder guidance while focused and empty, not only when unfocused.
## UI/UX Polish
- Ensure the tree view scrolls smoothly and maintains high framerates even with large modpacks.
- Check the layout constraints so that switching between Grid and List view is seamless.
- ~~Search bar: verified `Home`, `End`, `Ctrl+Left`, `Ctrl+Right`, and `Shift` selection route through `EditBox` from `ScreenEvent.KeyPressed.Pre` for all `AbstractContainerScreen` instances.~~
- ~~Search bar: replaced the double-click highlight toggle with token selection on the current query token.~~
- [x] Search bar: right-click now clears the field, unfocuses, and triggers a search refresh (empty query).
- [x] Search bar: add a right-click context menu for Cut, Copy, and Paste. (Disabled by default via config).
- [x] Search bar: refined `Escape` so the first press only unfocuses the field and a second press can close the screen if needed.
- [x] Search bar: add a clear button inside the field when text is present.
- [x] Search bar: make cursor movement token-aware for AMI operators like `@mod`, `#tag`, and `-exclude`.
- [x] Search bar: show a recent-search dropdown when the field is focused and empty (disabled by default, configurable).
- [ ] Search bar: decide whether the input filter should stay ASCII-only or allow broader Unicode / IME-friendly entry.
- [x] Search bar: show placeholder guidance while focused and empty, not only when unfocused.
- [x] **Interactive Mod Filtering**: Refined to use the `SearchState` context object and a toggle mechanism in the search bar for better QoL.
- [x] **Search Bar Toggle Logic**: Extracted to `QueryUtils` and verified with unit tests (`SearchBarToggleTest`).
- ~~Removed the `tier` metadata feature which cluttered the UI with "TIER: MODDED".~~
- ~~Centralised semantic UI colours into `AmiColors` (MOD_COLOR, TAG_COLOR, EXCLUDE_COLOR); removed hardcoded hex values from `TokenColorizer`, `ResultsTreeView`, and the search bar formatter.~~
- ~~Mod group headers in the tree view now render in `AmiColors.MOD_COLOR` (blue, no shadow) to match the mod-ID subtitle colour.~~
- ~~`@modId` tokens in the search bar are now highlighted blue (matching MOD_COLOR) via `TokenColorizer`.~~
- ~~Removed the broken mod-filter multi-select dropdown from the toolbar (was displaying "0/N" and doing nothing).~~
- [x] **Unified Row Tooltips**: Appended "Required Tool" information to the item tooltip in the results list view. logic now builds a composite tooltip including vanilla lines and AMI-specific data.
- [x] **Synthetic Item Fix**: Resolved issue where potions, enchanted books, and other synthetic variants were missing icons due to renderer invalidation.
- [x] **High-Cardinality Grouping**: Implemented automatic collapsing of large item clusters (e.g. books) in both Grid and List views with visual gold borders and smart expansion.
- [x] **String Handling Pass**: Audited string-to-component conversions. Migrated dozens of literal strings to translatable components and fixed hardcoded formatting.
- [x] **Harvest Level Indicators**: Added numeric overlays to tool icons in the results view to indicate harvest levels (0-4).
- [x] **UI Consistency**: Port the "reserved tool slot" and ellipsis truncation logic to `CommandPaletteWidget.java` for layout parity.
- [x] **Biome & Structure Badges**: Added subtle borders and semi-transparent backgrounds to Biome and Structure icons to distinguish them as UI badges.
- [x] **Ontology Refactor**: Split "Social & Navigation" into two distinct categories. "Navigation" is now at the top for better accessibility, and "Social" is at the bottom for players/teams.
- [x] **Dynamic Badge Fields**: Connected the "Fields" toolbar dropdown to `ResultsTreeView` so badges (Mod, Storage, DPS) can be toggled by the user. logic now uses `RowFieldConfig` and supports per-field color highlighting.
- [x] **Centralized Search State**: Implemented a robust `SearchState` model that encapsulates query, sorting, grouping, and facets. UI widgets now use a listener pattern to stay synchronized with this single source of truth.
- [x] **Reset Button**: Added a localized "Reset" button to the search toolbar that clears all filters and restores the search state to its best-default "show everything" configuration.
- [x] **UI Localization Audit**: Conducted a thorough audit and replaced all hardcoded user-facing strings with translatable `Component` keys.
- [x] **Clipboard Copy**: Implemented `Ctrl+C` support to copy formatted, plain-text tooltips to the clipboard across all UI widgets. Uses `AmiClipboardHelper` to strip formatting codes for clean output.
- [x] **Fix Fields Picker**: Selecting multiple fields (e.g. Mod + DPS) now correctly persists and displays in the subtitle. Added ID and Type fields for better visibility.
- [x] **Fix Results Scrollbar**: Widened hitbox to 10px and added high-contrast track for reliable vertical scrolling.
- [x] **Fix Dropdown Overhaul**: Improved layout with dynamic widths, modern selection indicators (accent bars), and logic to prevent opening empty/single-option menus.
- [x] **EMI-Style Click Logic**: Support right-click to find usages vs left-click to find recipes (mimicking EMI behavior) in the results panel. (Fixed click routing in `UniversalResultsPanel.java`).
- [x] **Drag-and-Drop Filters**: Support for dragging icons from AMI results into mod filter slots (e.g., Create, Sophisticated Backpacks).
- [x] **Synchronized Favorites**: Favorite system (default key `A`) with an optional, configurable side panel, synchronized with EMI/JEI state.
- [x] **Advanced Sidebar Views**: Implemented Craftable, Lookup History, and Crafting History views for the sidebar, synchronized with EMI. Added an internal AMI history fallback for JEI.
- [x] **Split Sidebar Layout**: Support for splitting both left and right sidebars to show primary and secondary content simultaneously.
- [x] **Visual Polish**: Wiggle animations on hover/drag for improved interface feedback.
- **TODO:** Mod filtering is genuinely useful — revisit as a dedicated filter surface (e.g. a searchable popover or sidebar chip list) when the panel layout has more room.

## Long-term Vision
- **Robust Searching:** Improve tokenization and multi-field queries.
- **Provider Expansion:** Add or refine metadata for `BiomeProvider`, `StructureProvider`, and other node types so the rich list view can display more relevant badges/icons.
- **Recipe Integration:** Seamlessly bridge item clicks from the AMI tree view into EMI/JEI (via `RecipeViewerBridge`).

## Metric Compat Plugins

Storage metric support should expand through isolated `StorageMetricAdapter` implementations instead of adding mod-specific logic to the generic sniffer.

- **Functional Storage:** compute drawer capacity from drawer type, slot count, stack multiplier, and installed upgrades.
- **Applied Energistics 2:** compute ESM for storage cells from bytes and type limits; document the bytes-to-items assumption.
- **Refined Storage:** compute ESM from disk item capacity where the API exposes a fixed item count.
- **Sophisticated Backpacks:** replace current tier defaults with API/config-backed values when the mod is present.
