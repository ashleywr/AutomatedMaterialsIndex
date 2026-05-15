# AMI Project: Next Steps and Goals

This document outlines the current state and the upcoming tasks to fully realize the Suginami-style interface for the Automated Materials Index (AMI).

## Immediate Goal: Better Visual Grouping and Colour Extraction
Right now, the `ItemProvider` sets all item `colorBucket` metadata to `"gray"`. This causes the variant group swatches in the list view (the little colored dots under the folder header) to always show up as gray squares, regardless of what's inside.

**Task:** Improve the colour parsing logic in `ItemProvider.java`.
- **Implementation Strategy:** Write a utility method that inspects an item's registry name, translation key, or tags for color keywords (e.g., "red", "blue", "cyan", "magenta"). 
- Map these keywords to the known buckets in `ResultsTreeView.bucketToArgb` so the UI can render accurate color dots for grouped variants (like a stack of coloured wool).

## UI/UX Polish
- Ensure the tree view scrolls smoothly and maintains high framerates even with large modpacks.
- Check the layout constraints so that switching between Grid and List view is seamless.
- (Completed) Removed the `tier` metadata feature which cluttered the UI with "TIER: MODDED".

## Long-term Vision
- **Robust Searching:** Improve tokenization and multi-field queries.
- **Provider Expansion:** Add or refine metadata for `BiomeProvider`, `StructureProvider`, and other node types so the rich list view can display more relevant badges/icons.
- **Recipe Integration:** Seamlessly bridge item clicks from the AMI tree view into EMI/JEI (via `RecipeViewerBridge`).
