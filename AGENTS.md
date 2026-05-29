# Agent Notes

This is a NeoForge Minecraft mod targeting Minecraft 1.21.1.

Before changing Minecraft/NeoForge API usage, try to refresh the local reference sources first:

```powershell
.\gradlew.bat syncReferenceSources
```

If that task is unavailable in this repo, use the checked-in reference sources already present under:

```text
internal/reference-sources/
```

Before changing EMI/JEI integration behavior, refresh local recipe viewer sources:

```powershell
.\gradlew.bat syncRecipeViewerSources
```

Resolved dependency source jars are extracted under:

```text
vendor-sources/resolved/
```

Full local vendor checkouts, when present, live under:

```text
vendor-sources/emi/
vendor-sources/JustEnoughItems-1.21.1/
```

Inspect the exact local reference sources there instead of guessing from older tutorials or memory. Check superclass contracts for registrations, events, attributes, renderers, data components, recipe viewer integration, and runtime-only Minecraft behavior.

Compilation is not enough for Minecraft runtime contracts. Prefer a smoke test or GameTest for code paths involving registries, entity construction, resource reloads, client setup, or generated data.

For result UI grouping/tree changes, prefer deterministic JVM tests before using the game client. Most of the visible result shape is pure data (`SearchNode` inputs through `ResultsProcessor`, `ResultsGroupingPostProcessor`, and `ResultsTreeNormalizer` into `TreeNode`s). Use or extend the test helpers under:

```text
neoforge/src/test/java/com/sanhiruzu/ami/client/results/
```

`ResultsTreeDump` can snapshot the tree shape with expansion/cardinality markers, which is the preferred way to catch regressions such as duplicate nested groups (`Banners > Banners`), collapsed default groups, or flattened/extra material subgroups. Use `ItemGridViewTest` row-model assertions for grid layout projection issues that do not require actual draw calls.

For runtime-exact result fixtures, run `/ami dump-search-nodes` in a client after indexing. This writes `ami_dumps/search_nodes.jsonl` under that run directory. `RuntimeMirrorResultsShapeExplorerTest` loads `run/neoforge-emi/ami_dumps/search_nodes.jsonl` by default, or a path supplied with `AMI_SEARCH_NODES_DUMP` / `-Dami.searchNodesDump=...`, then runs the same `ResultsViewProjector` used by the UI. Prefer this mirror when debugging mismatches between synthetic fixtures and in-game result shape.
