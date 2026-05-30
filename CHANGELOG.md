# Changelog

All notable user-facing changes should be recorded here.

## Unreleased

## 0.9.0 - 2026-05-30

### Added

- Added AMI panel recipe transfer support through JEI and EMI when the current screen accepts recipe transfers.
- Added crafting actions from favorites, lookup history, crafting history, and craftable side panels.
- Added AMI-recorded JEI crafting history for crafts started from AMI panels.
- Added cheat-mode tooltip hints for panel items when cheat hotkeys are available.
- Added persistent client config storage for AMI UI and behavior settings.

### Changed

- Moved more shared client, recipe, result, tooltip, and indexing code into the xplat source set.
- Updated AMI sidebar panel controls so alternate-content swap buttons only show when another panel mode is available.
- Refined sidebar button styling and placement to align with the surrounding result panel controls.
- Improved recipe viewer integration state syncing for EMI and JEI favorites, hidden stacks, and sidebars.

### Fixed

- Fixed AMI config changes reverting after restarting the game.
- Fixed JEI recipe transfer from AMI lookup history and favorites.
- Fixed crafting history panels not responding to AMI crafting actions.
- Fixed AMI crafting transfer being blocked when AMI's own recipe index was not ready but the recipe viewer could still transfer.
- Removed the old Shift-hover debug id tooltip from normal item tooltip flow.
- Fixed tooltip modifier help so Shift and Ctrl update the AMI action hints while hovering panel entries.

## 0.3.1-alpha.1 - 2026-05-28

### Added

- Headless AMI search benchmark suite backed by the live Minecraft item registry.
- Local benchmark history written to `run/config/ami_benchmark_history.jsonl`.
- Gradle `check` now runs unit tests and the AMI benchmark suite.

### Performance

- Record indexed item count, query executions, average search latency, P99 latency, skipped anomalies, and total
  benchmark duration for each benchmark run.
