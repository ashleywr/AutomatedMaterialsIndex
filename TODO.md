# AMI Project: Next Steps and Goals

This document outlines the current state and the upcoming tasks to fully realize the Suginami-style interface for the Automated Materials Index (AMI).

## ~~Immediate Goal: Better Visual Grouping and Colour Extraction~~
~~Right now, the `ItemProvider` sets all item `colorBucket` metadata to `"gray"`. This causes the variant group swatches in the list view (the little colored dots under the folder header) to always show up as gray squares, regardless of what's inside.~~

~~**Task:** Improve the colour parsing logic in `ItemProvider.java`.~~
~~- **Implementation Strategy:** Write a utility method that inspects an item's registry name, translation key, or tags for color keywords (e.g., "red", "blue", "cyan", "magenta").~~ 
~~- Map these keywords to the known buckets in `ResultsTreeView.bucketToArgb` so the UI can render accurate color dots for grouped variants (like a stack of coloured wool).~~

## Subtype Engine (Vanilla Edge Cases + Modded Hero Items)

Handle items where one registry ID covers many visual variants, without melting the client.

**Vanilla permutation loops** — write manual `ItemStack` generation for each known edge case:
- `minecraft:potion` — all potion effects × splash/lingering
- `minecraft:enchanted_book` — all enchantments × all levels
- `minecraft:spawn_egg` — iterate `BuiltInRegistries.ENTITY_TYPE` for all tagged eggs
- `minecraft:suspicious_stew` — all effect variants
- `minecraft:firework_rocket` — representative shapes/colors only
- `minecraft:goat_horn` — all instrument variants

**Hard cap** — if any generation loop exceeds **150 variants**, abort and register only the base item. Prevents poorly-coded mods from producing 10,000-entry batteries that crash the indexer.

**`IAmiPlugin` Hero Item API (modded)** — for mods like Silent Gear / Apotheosis that generate infinite modular variants, do *not* attempt to compute them. Expose an API hook where those mod devs (or our own compat layer) can hand us a curated list of "Hero Items" (e.g., all-diamond pick, all-wood pick) to stand in as representatives.

**Files to touch:** `ItemProvider.java`, `IAmiPlugin.java`, `AmiPluginRegistry.java`, possibly a new `SubtypeExpander.java`.

---

## Group By Engine (Condensing the Material List)

Dynamically fold items into collapsible parent nodes on three axes so the UI never becomes a chaotic wall of blocks.

### Group by Shape
- Rely exclusively on Mojang's `BlockTags` (`STAIRS`, `SLABS`, `WALLS`, `FENCES`, `DOORS`, `TRAPDOORS`, `BUTTONS`, `PRESSURE_PLATES`) to bucket shape variants together — no regex, no string matching.
- Already partly done in `ItemProvider.getVariantGroup`; needs to be promoted to the `GroupingEngine` as a first-class pass.

### Group by Color
- Hybrid approach: check `c:dyes/*` / `minecraft:*_wool` tags first, then fall back to lexical prefix matching (`red_`, `blue_`, etc.).
- Apply a **sorting weight** so the Base Block (e.g., Red Wool, Red Terracotta) is always chosen as the group representative icon — not a carpet, bed, or banner.

### Group by Material — Three-Phase Waterfall
Process in strict order; stop at the first hit:
1. **BlockFamilies API** — query 1.21.1's native `BlockFamilies` registry for guaranteed exact family membership (Copper Block → Cut Copper → Chiseled Copper, etc.).
2. **Stonecutter Heuristics** — reverse-engineer crafting intent: if a Stonecutter recipe maps `X Block → X Stair`, group them under the `X` material namespace.
3. **Tag-Lexical Fallback** — for tools and armors, intersect tags (`#c:ingots`, `#minecraft:pickaxes`) with suffix stripping (remove `_pickaxe`) to identify the root material namespace.

**Files to touch:** `GroupingEngine.java`, `ItemProvider.java`, `ResultsProcessor.java`.

---

## Bugs
- ~~Search box did not respond to typing — fixed by subscribing `ScreenEvent.CharacterTyped.Pre` and `ScreenEvent.MouseButtonPressed.Pre` in `InventoryOverlayHandler` to bypass the screen focus system (which EMI can clear). We now track focus with `searchBarInputActive` and forward events directly.~~

## UI/UX Polish
- Ensure the tree view scrolls smoothly and maintains high framerates even with large modpacks.
- Check the layout constraints so that switching between Grid and List view is seamless.
- Search bar: verify `Home`, `End`, `Ctrl+Left`, `Ctrl+Right`, and `Shift` selection behave like a normal text field in all container screens.
- Search bar: replace the current double-click highlight toggle with standard word/token selection.
- Search bar: add a right-click context menu for Cut, Copy, Paste, and Clear.
- Search bar: refine `Escape` so the first press only unfocuses the field and a second press can close the screen if needed.
- Search bar: add a clear button inside the field when text is present.
- Search bar: make cursor movement token-aware for AMI operators like `@mod`, `#tag`, and `-exclude`.
- Search bar: show a recent-search dropdown when the field is focused and empty.
- Search bar: decide whether the input filter should stay ASCII-only or allow broader Unicode / IME-friendly entry.
- Search bar: show placeholder guidance while focused and empty, not only when unfocused.
- ~~Removed the `tier` metadata feature which cluttered the UI with "TIER: MODDED".~~
- ~~Centralised semantic UI colours into `AmiColors` (MOD_COLOR, TAG_COLOR, EXCLUDE_COLOR); removed hardcoded hex values from `TokenColorizer`, `ResultsTreeView`, and the search bar formatter.~~
- ~~Mod group headers in the tree view now render in `AmiColors.MOD_COLOR` (blue, no shadow) to match the mod-ID subtitle colour.~~
- ~~`@modId` tokens in the search bar are now highlighted blue (matching MOD_COLOR) via `TokenColorizer`.~~
- ~~Removed the broken mod-filter multi-select dropdown from the toolbar (was displaying "0/N" and doing nothing).~~
- ~~Renamed sort option "Alphabetical" → "Name" to fit toolbar width.~~
- ~~Removed the active-field count badge from the "Fields" button label to prevent overflow.~~
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
