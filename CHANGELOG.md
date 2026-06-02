# AMI Changelog

User-facing changes are recorded here.

## Unreleased

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
