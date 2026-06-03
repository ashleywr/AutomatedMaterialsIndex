# AMI Changelog

User-facing changes are recorded here.

## Unreleased

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
