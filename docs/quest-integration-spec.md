# Quest Integration Spec

## Goals

Make AMI useful while playing and authoring quest-driven modpacks, with FTB Quests treated as the first-class target. The integration should not require AMI to own quest progression or replace the FTB Quests UI. AMI should connect quests to items, recipes, guide pages, search, and pack diagnostics.

## Non-Goals

- Do not hard-depend on FTB Quests in core AMI.
- Do not replace FTB Quests editing, reward claiming, team sync, or quest completion logic.
- Do not enumerate huge dynamic item spaces just because a quest task can match many stacks.
- Do not modify FTB screens unless AMI can get a stable, low-risk hook for a small entry point.

## Player Workflows

1. **Find quests for an item**
   - Right-click any AMI item result and show quests that require, reward, unlock, or mention that item.
   - Include chapter, quest title, status, and required count.
   - Open the owning quest screen when the source can provide an open action.

2. **Quest-aware recipe planning**
   - From a quest requirement, jump to recipes or uses for the exact required item.
   - When a task accepts multiple matching items, show the representative choices without expanding them all into the global index.
   - Optionally pin required ingredients into AMI favorites or a quest planning list.

3. **Search understands quest context**
   - Queries like `quest controller`, `@ftbquests steel`, or future structured filters should surface quest tasks and guide pages alongside item results.
   - Item results can show a subtle "Required by quest" evidence line when relevant.

4. **Discovery from questlines**
   - Quest chapters become browseable paths in AMI's sidebar.
   - Quests can link to guidebook pages by referenced item, mod id, chapter title, or explicit plugin metadata.
   - AMI can suggest "read this guide page before this task" when guide evidence matches the same item or mod topic.

## Pack Author Workflows

1. **Quest health diagnostics**
   - Missing item ids.
   - Quest item tasks with no indexed recipe and no obvious obtainability route.
   - Tasks that require creative/dev-only items.
   - Tasks whose matching filter expands to a very large item set.
   - Duplicate or conflicting requirements across a chapter.

2. **Quest coverage reports**
   - Mods with many indexed items but no quest coverage.
   - Quest-heavy mods with no guidebook hits.
   - Chapters whose tasks are mostly recipe-dead-ends.

3. **Authoring helpers**
   - Right-click an AMI item to copy a quest task template.
   - Right-click a category or search result group to export a starter chapter outline.
   - Produce JSON/SNBT-like reports for external pack tooling rather than editing FTB files directly at first.
   - Treat FTB Quests as the primary output target for item task templates.
   - Prefer "copy/export/apply through public APIs" over directly mutating FTB quest files until FTB's local API surface is verified.

4. **Recipe and progression editing bridges**
   - AMI should not become a full recipe scripting or GameStage editor.
   - AMI should make it easy to jump from an item/search result into the right external tool:
     - Copy KubeJS/CraftTweaker recipe starter snippets.
     - Copy FTB Quests item task snippets.
     - Copy GameStages stage ids or starter conditions when a compatible provider exposes them.
   - Public context-menu APIs are the right extension point for these integrations. Mods that own the scripting/progression domain can register authoritative actions while AMI supplies the searchable item context.

5. **Search-to-quest authoring**
   - A search result set should be usable as an authoring source:
     - Copy a single item task from an item row.
     - Copy a grouped task list from a material/category group.
     - Export a chapter outline from a search result set, preserving group labels as section hints.
   - The first implementation should start with single-item FTB task templates because it is deterministic and does not require visual FTB screen hooks.

## Public API Shape

The current `AmiQuestsApi` registers grouped item requirements. That is enough for the basic sidebar, but deeper integration needs a richer, source-agnostic model:

- `AmiQuestSource`
  - Stable source id, display name, optional reload/open hooks.
- `AmiQuestDocument`
  - Quest id, source id, chapter id/title, quest title, description/search text, status, dependencies, optional open action.
- `AmiQuestTaskDocument`
  - Task id, quest id, task kind, required items, accepted alternatives, count, progress, completion state, consume/crafting flags.
- `AmiQuestRewardDocument`
  - Reward id, quest id, item rewards, command/XP placeholders, searchable labels.

The existing `AmiQuestGroup` API should remain as the simple registration path. Rich sources can populate the extended model, and AMI can derive sidebar groups from it.

## FTB Quests Adapter

FTB Quests should be an optional compat layer:

1. Detect FTB Quests at runtime.
2. Read the client-synced quest file/team data through FTB APIs when present.
3. Convert chapters, quests, item tasks, and item rewards into AMI quest documents.
4. Refresh after FTB quest/team sync. Until a stable public sync event is identified, poll the client quest file on a throttled client tick and replace AMI's FTB-sourced documents only when the mirrored signature changes.
5. Provide open actions back into the FTB quest screen through `ClientQuestFile.openBookToQuestObject(long)`.

Local source references used for the runtime hook:

- `vendor-sources/FTB-Quests`
- `vendor-sources/FTB-Library`
- `vendor-sources/FTB-Teams`

The first implementation should avoid screen injection. AMI can be useful through its own sidebar, result evidence, and context menu actions before we add an FTB-side button.

## FTB Item Task Mapping

FTB item tasks expose the concepts AMI needs:

- Required `ItemStack`.
- Required count.
- Consume-items behavior.
- Crafting-only behavior.
- Component matching behavior.
- Valid display items for filters or alternatives.

AMI should store the canonical requirement plus a bounded list of accepted display stacks. Large filter expansions must be capped and marked as high-cardinality.

The runtime mirror must not eagerly call FTB's `ItemTask.getValidDisplayItems()` for every task because that method expands filters before AMI can cap them. The initial runtime hook indexes the canonical `ItemStack`; accepted alternatives need a later lazy/capped adapter.

Real pack data shows FTB Filter System is common in large 1.21.1 questbooks:

- ATM10 7.0: 63 chapter files, 7,865 item task markers, 808 filter-system references.
- FTB Skies 2: 41 chapter files, 5,974 item task markers, 472 filter-system references.

AMI should special-case `ftbfiltersystem:smart_filter` when FTB Filter System is
present. Read the stored filter string through FTB Filter System's runtime API,
extract explicit `item(namespace:path)` entries up to a bounded cap, and mark
tag or capped filters as high-cardinality. Do not index the smart-filter item
itself when explicit accepted item ids are available.

## Search And Ranking

Quest search should be a separate lightweight index, like guidebook search:

- Always index quest title, chapter title, source id, quest id, item ids, and mod ids.
- Optionally index descriptions and task notes under a config toggle.
- Rank exact item/task hits above description-only hits.
- Keep quest results below direct item matches unless the query explicitly includes quest context.
- Surface matching quest documents as their own lightweight result rows above normal item results in the full main panel.
- Suppress quest result rows in compact/favorites panels to preserve dense item workflows.

## UI

### Result Evidence

Item results should expose quest provenance in every item layout without changing
row height or grid geometry:

- List rows render a small right-aligned `Qn` badge before existing subtitle fields.
  Blue means at least one requirement match; gold means reward-only.
- Grid and compact layouts render a small corner marker on the item cell. A white
  corner pixel indicates more than one quest match.
- Item tooltips list the first few matching quests with role, chapter, quest title,
  required count, and progress where available.

Tooltip examples:

- `Quests: 2 requirements, 1 reward`
- `Requirement: Chapter > Quest Name (4x, 0/4)`
- `Reward: Chapter > Quest Name (Diamond, 0/1)`

Quest result rows should show:

- quest title;
- `Source > Chapter > Quest`;
- status plus requirement/reward counts;
- tooltip evidence for matched task/item/description text.

### Right-Click Actions

Add actions only when quest data exists for the target:

- `Show Quests for Item`
- `Open Quest`
- `Pin Quest Requirements`
- `Copy Quest Task Template`
- `Copy Quest ID`

Pack-author actions can appear in AMI Pack Author Mode, AMI dev mode, or
through context-menu config:

- `Copy FTB Item Task`
- `Copy FTB Quest Skeleton`
- `Copy KubeJS Recipe Stub`
- `Copy CraftTweaker Recipe Stub`
- `Copy GameStage Condition`
- `Copy Pack Author Item Report`
- `Copy Pack Author Group Report`

The first authoring slice provides clipboard templates from AMI item and group
targets. Item rows can copy an FTB item task, a one-quest FTB skeleton, a KubeJS
recipe stub, and a GameStages recipe-gate starter. Group rows can copy an FTB
quest skeleton with up to 64 item tasks gathered from the visible group tree.
AMI does not write these files directly; pack authors paste and adjust them in
FTB Quests or their script workspace.

Pack author reports are AMI-owned diagnostics, not editor operations. Item
reports summarize id, access, obtainability, recipe counts, quest references,
and immediate author notes. Group reports summarize quest coverage, no-recipe
evidence, restricted-access items, mod coverage, missing quest item ids, and
high-cardinality quest tasks for the selected group.

### Quest Sidebar

The quest sidebar should support:

- Chapter > Quest > Task grouping.
- Completion/progress state where available.
- Missing/unindexed fallback rows.
- High-cardinality filter tasks as collapsed rows.
- Recipe and uses actions on task rows.

## Memory And Performance

- Do not duplicate full FTB quest objects.
- Store stable ids, text summaries, counts, and item ids/stacks only.
- Cap accepted item alternatives per task.
- Rebuild quest indexes on quest sync/reload, not every render.
- Keep progress/status updates incremental if the source can report them cheaply.

## Test Plan

- Deterministic projector tests for chapter/quest/task tree shape.
- Index tests for item id, quest title, chapter title, and description modes.
- FTB fixture adapter tests using reduced task/chapter fixtures.
- High-cardinality filter tests to ensure alternatives cap correctly.
- Context menu tests for item rows with and without quest references.
- Generated runtime questbook smoke:
  - `.\gradlew.bat writeVanillaFtbQuestFixture`
  - `.\scripts\smoke-ftb-quests-fixture.ps1 -RestartClient -StopAfter`
  - `.\scripts\smoke-ftb-quests-fixture.ps1 -IncludeSmartFilters -RestartClient -StopAfter`
  - Uses AutoMine `ami_quest_status` checks instead of screenshots.
- Runtime smoke test after UI wiring with a local FTB Quests dev run.

## Implementation Order

1. Keep the current simple quest group API stable.
2. Add a richer quest document model and query index.
3. Add item-to-quest reverse lookup and context menu actions.
4. Add FTB fixture adapter tests based on representative chapter/task data.
5. Add optional runtime FTB adapter once exact 1.21.1 APIs are available locally.
6. Add pack-author diagnostics/reporting.
7. Consider an FTB screen button only if the AMI-side entry points are not discoverable enough.

## First Implementation Slice

The first non-visual implementation should include:

- Rich source-agnostic quest document/task records.
- `AmiQuestsApi` registration and item reverse lookup.
- A lightweight quest search index.
- Item context menu actions:
  - show/copy quest matches when quest data exists for an item;
  - copy an FTB item task template for any item when pack-author tooling is enabled.
- Dev-mode authoring actions:
  - copy single-item FTB quest skeletons;
  - copy grouped FTB quest skeletons from category/material/search groups;
  - copy KubeJS recipe and GameStages starter snippets from item rows.
  - copy item/group pack author diagnostics reports without mutating pack files.
- JVM tests for registration, lookup, indexing, and context-menu action shape.

This stops before:

- Drawing quest search results in the main panel.
- Injecting into FTB Quests screens.
- Editing FTB quest files from AMI.
- Claiming rewards or mutating team progress.
