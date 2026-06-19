# FTB Chunks Waypoint Compat Design

Date: 2026-06-19
Status: Draft for review

## Goal

Add first-class FTB Chunks map/waypoint compat to AMI without taking on claims support in the same slice.

The feature should:

- detect and support FTB Chunks as a waypoint/map provider
- merge duplicate live waypoints that represent the same real location across multiple map mods into one AMI result
- improve waypoint tooltip and interaction surfaces for merged/provider-backed results
- refresh runtime waypoint state on a light cadence so external edits in map mods eventually appear in AMI
- expose a small config surface for dedupe, refresh cadence, and open-provider priority

Out of scope for this spec:

- searchable FTB Chunks claims or force-loaded chunks
- server-side claim management
- a permanent background watcher thread

## Current State

AMI already has runtime waypoint infrastructure:

- `xplat/src/main/java/com/sanhiruzu/ami/player/PlayerWaypointProviders.java`
- `xplat/src/main/java/com/sanhiruzu/ami/player/FtbChunksWaypointProvider.java`
- `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultContextMenuActionBuilder.java`
- `xplat/src/main/java/com/sanhiruzu/ami/util/tooltip/PlayerTooltipFact.java`
- `xplat/src/main/java/com/sanhiruzu/ami/index/runtime/RuntimeSearchProviders.java`

Current gaps:

- provider results are emitted one-per-provider rather than one-per-real-waypoint
- external edits in map mods are not reflected reliably enough in AMI
- FTB Chunks support is mostly a thin reflective adapter with minimal tooltip/menu detail
- left-click and right-click semantics are provider-local instead of merged-result aware

## Recommended Approach

Implement a provider-merged waypoint runtime layer.

Raw waypoint providers continue to enumerate their own live waypoints. AMI then normalizes and merges equivalent waypoints into one canonical runtime result before the search/result projector sees them.

Why this approach:

- fixes duplicate search results at the data boundary instead of hiding them in the UI
- keeps favorites, open actions, tooltips, and refresh revision logic aligned to one result identity
- fits the existing waypoint-provider architecture and focused JVM test coverage
- keeps FTB Chunks support extensible without hardcoding it as the only canonical map mod

## Architecture

### 1. Raw Provider Enumeration

Keep `PlayerWaypointProvider` as the provider contract for raw waypoint enumeration and actions.

`FtbChunksWaypointProvider` remains responsible for:

- detecting whether FTB Chunks is loaded
- enumerating raw FTB Chunks live waypoints
- adding/deleting/opening FTB Chunks waypoints through reflective API access
- extracting FTB-specific metadata such as visibility, color, deathpoint state, and transient state when available

The provider should be hardened so missing methods or API drift fail closed and simply suppress the provider rather than breaking AMI runtime search.

### 2. Merged Waypoint Aggregation

Add a merge stage inside `PlayerWaypointProviders` that groups raw live waypoints into canonical AMI waypoint entries.

Each merged waypoint should preserve:

- canonical display name
- canonical dimension and coordinates
- provider membership list
- provider-specific waypoint ids
- provider labels
- primary provider id used for default open behavior
- merged provider metadata for tooltip and context menu rendering

The merge key should be conservative:

- exact dimension match
- exact integer block position match
- same normalized name, or a name match rule strict enough to avoid collapsing unrelated waypoints at the same coordinates

If confidence is insufficient, AMI should keep results separate rather than over-merge.

### 3. Primary Provider Selection

Each merged waypoint result needs one primary provider for left-click/open behavior.

Default policy:

- use configured provider priority order
- prefer FTB Chunks when it is present and part of the merged group
- otherwise use the first available provider in the configured order

This provider choice should be explicit metadata on the merged node so tooltip, open action, and tests all read the same answer.

### 4. Refresh Contract

Use a light refresh model rather than a permanent watcher.

Contract:

- refresh waypoint data when the AMI result surface becomes relevant, such as panel open or first waypoint query
- recheck periodically while the result/search surface remains active
- invalidate immediately after AMI-triggered add/delete waypoint actions

The refresh implementation should not do a constant background thread loop. A short-lived async refresh trigger is acceptable if it only gathers data for the next revision and hands results back safely, but correctness matters more than thread novelty. If provider APIs are not safe off-thread, enumeration should remain on the client thread and rely on lightweight cadence plus caching.

### 5. Result Interactions

Merged waypoint results should expose AMI-level generic actions plus provider-specific actions.

Left-click:

- open the merged waypoint using the primary provider action

Right-click:

- generic actions first, such as copy/chat and any existing teleport support
- provider-specific actions grouped by provider
- examples: `Open in FTB Chunks`, `Delete from FTB Chunks`, `Open in JourneyMap`

Provider-specific destructive actions such as delete should remain explicit to the owning provider rather than pretending a merged delete affects every source.

### 6. Tooltip Surface

Merged waypoint tooltips should clearly communicate multi-provider ownership.

Minimum fields:

- waypoint coordinates and dimension
- primary provider
- contributing providers
- provider-specific state when useful

FTB Chunks should contribute richer detail when available:

- hidden/visible
- deathpoint
- transient
- color/theme hints if already available from provider metadata

Provider details may be abbreviated by default and expanded on Shift if configured that way.

### 7. Config Surface

Add a small config surface rather than a broad compat page.

Settings:

- `waypoints.mergeDuplicateProviders` boolean
- `waypoints.refreshIntervalSeconds` numeric interval
- `waypoints.openProviderPriority` ordered list or equivalent enum-driven priority
- `waypoints.showProviderDetailsInTooltip` mode such as `always` or `shift`

FTB Chunks support itself should remain auto-detected rather than gated behind a separate compat enable toggle.

## Data And Metadata Contract

Merged waypoint nodes should continue to use `NodeType.WAYPOINT` and stay under `environment/waypoints`.

New or clarified metadata should include:

- canonical primary provider id
- canonical provider label list
- merged provider id list
- merged provider waypoint ids
- optional provider-state summaries for tooltip rendering
- a stable merge identity separate from any single provider-local waypoint id

The merged identity must stay stable enough for favorites and runtime revision snapshots, but should still change when the underlying canonical waypoint materially changes.

## Testing Strategy

Prefer deterministic JVM tests first.

Add or extend tests for:

- merge behavior in `PlayerWaypointProviders`
- conservative no-merge cases
- primary provider selection from configured priority
- refresh invalidation and snapshot revision changes
- tooltip rendering for merged waypoint provider metadata
- provider-grouped context-menu actions
- FTB Chunks provider degradation when reflective methods are missing or unexpected

Likely test areas:

- `neoforge/src/test/java/com/sanhiruzu/ami/player/PlayerWaypointProvidersTest.java`
- `neoforge/src/test/java/com/sanhiruzu/ami/client/results/ResultContextMenuActionBuilderTest.java`
- tooltip tests covering waypoint/provider facts

Runtime smoke should verify:

- external FTB Chunks waypoint add/edit/delete eventually appears in AMI within the refresh interval
- left-click opens the expected primary provider action
- right-click exposes provider-specific submenu actions for merged waypoints

## Implementation Notes

Likely entry points:

- `xplat/src/main/java/com/sanhiruzu/ami/player/PlayerWaypointProviders.java`
- `xplat/src/main/java/com/sanhiruzu/ami/player/FtbChunksWaypointProvider.java`
- `xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultContextMenuActionBuilder.java`
- `xplat/src/main/java/com/sanhiruzu/ami/util/tooltip/PlayerTooltipFact.java`
- config classes and config screen wiring for the new waypoint settings

Possible follow-up if this slice works well:

- claim indexing and search as a separate feature
- broader merged handling for other runtime map entities beyond waypoints

## Risks

- over-aggressive merge heuristics could hide distinct waypoints
- under-aggressive refresh could still feel stale
- provider APIs may not be thread-safe for off-thread enumeration
- cross-provider action semantics can become confusing if the tooltip/menu wording is vague

Mitigations:

- keep the merge key conservative
- keep refresh cadence configurable
- prefer explicit provider labeling in tooltip and menus
- keep provider destructive actions scoped and named
