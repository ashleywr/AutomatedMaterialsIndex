# AMI Project: Next Steps and Goals

This document outlines the current state and the upcoming tasks to fully realize the Suginami-style interface for the Automated Materials Index (AMI).

## Immediate Goal: Better Visual Grouping and Colour Extraction
Right now, the `ItemProvider` sets all item `colorBucket` metadata to `"gray"`. This causes the variant group swatches in the list view (the little colored dots under the folder header) to always show up as gray squares, regardless of what's inside.

**Task:** Improve the colour parsing logic in `ItemProvider.java`.
- **Implementation Strategy:** Write a utility method that inspects an item's registry name, translation key, or tags for color keywords (e.g., "red", "blue", "cyan", "magenta"). 
- Map these keywords to the known buckets in `ResultsTreeView.bucketToArgb` so the UI can render accurate color dots for grouped variants (like a stack of coloured wool).

## Bugs
- ~~Search box did not respond to typing — fixed by subscribing `ScreenEvent.CharacterTyped.Pre` and `ScreenEvent.MouseButtonPressed.Pre` in `InventoryOverlayHandler` to bypass the screen focus system (which EMI can clear). We now track focus with `searchBarInputActive` and forward events directly.~~

## UI/UX Polish
- Ensure the tree view scrolls smoothly and maintains high framerates even with large modpacks.
- Check the layout constraints so that switching between Grid and List view is seamless.
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
