# AMI Changelog

User-facing changes are recorded here.

## Unreleased

## 1.1.0 - 2026-06-03

AMI 1.1.0 is a GregTech/GTCEu-focused feature release. GregTech packs are large enough that a normal item-name search is not enough, so AMI now indexes more of the structure GregTech players actually use: voltage tiers, circuit grades, machine/power roles, EU/t values, and GregTech-specific right-click filters.

### Added

- GregTech/GTCEu items now get focused compatibility metadata:
  - Item kinds such as machines, multiblocks, power, circuits, materials, tools, and covers.
  - Voltage and steam tiers such as Steam, ULV, LV, MV, HV, EV, IV, LuV, ZPM, UV, UHV, and higher GTCEu tiers.
  - Circuit grades such as Basic, Good, Advanced, Micro, Nano, Quantum, Crystal, and Wetware, based on actual item IDs and circuit tags instead of tooltip guessing.
  - Power roles such as consuming EU, generating EU, accepting EU input, and emitting EU output.
  - GT EU/t values and amperage for tiered machines, generators, transformers, and energy hatches.
- GregTech items now show more useful right-click filter actions in AMI results:
  - Filter by tier, for example LV or HV.
  - Filter by GregTech kind, for example Machines, Power, or Circuits.
  - Filter by behavior fact, for example power or inputs EU.
  - Filter by circuit grade, for example Basic or Good.
- List view now supports GregTech EU fields separately from FE/RF:
  - GT EU Generation
  - GT EU Consumption
  - GT EU Input
  - GT EU Output
  - GT Amps
- Power and Machines List lenses can now recognize GregTech EU metadata even when the item does not expose FE/RF metadata.
- GregTech EU facts are included in AMI tooltips, search suggestions, capability detection, behavior grouping, and numeric search.

### Search Examples

- `?gregtech` finds GregTech/GTCEu-family content.
- `?gregtechTier:lv`, `?gregtechTier:hv`, or `?voltage:zpm` filters by voltage tier.
- `?gregtechKind:machines`, `?gregtechKind:circuits`, or `?gregtechKind:power` filters by GregTech item kind.
- `?gregtechCircuit:basic` or `?gregtechGrade:good` filters circuit grades.
- `?gregtechEnergy` finds GregTech items with indexed EU behavior.
- `?gregtechEnergyRole:inputs_eu` finds EU input hatches and similar input-side power parts.
- `?gregtechEnergy:4a` finds indexed 4A GregTech power parts.
- `>eugen:500` finds GregTech items producing at least 500 EU/t.
- `=euconsume:32` finds GregTech items consuming 32 EU/t.
- `>euinput:8000` finds GregTech items accepting at least 8000 EU/t.
- `>euoutput:30000` finds GregTech items emitting at least 30000 EU/t.
- `=amps:4` finds indexed 4A GregTech power parts.
- Broad metadata search also works with indexed facts, for example `~inputs_eu`, `~basic_circuit`, or `~lv_tier`.

### Changed

- GregTech now stays under the GregTech top-level category by default, so large packs do not scatter thousands of GregTech parts across unrelated normal categories.
- AMI still lets obvious non-GregTech gameplay categories escape when they are clear and useful, such as food, armor, weapons, and tools.
- Generic FE/RF energy fields remain separate from GregTech EU/t fields. AMI does not label GregTech EU as FE.
- The vanilla theme now more closely matches Minecraft's recipe book, including vanilla-style search controls, beveled item slots, subdued scrollbars, and tighter favorites spacing.
- The recipe book button now continues to toggle AMI while the new Start AMI Hidden setting only controls whether AMI opens hidden on new inventory screens.

### Fixed

- GregTech circuit context actions now get tier and circuit-grade filters even when runtime item tags are missing for known GTCEu circuit IDs such as Good Electronic Circuit.
- Autocomplete now suggests GregTech-specific property filters such as `?gregtechTier:`, `?gregtechCircuit:`, `?gregtechEnergy:`, and `?gregtechEnergyRole:`.
- List view now has a combined `GT EU/t` sort that works across GregTech generators, consumers, input hatches, and output hatches.
- GregTech material fasteners such as bolts, screws, and nuts no longer appear in the Ranged or Weapons List lenses just because they look projectile-like to generic metadata.
- Forge inventory overlays now follow the same render path as NeoForge, so AMI toggles and recipe viewer suppression remain consistent.

### Compatibility Notes

- The GregTech logic targets GTCEu/GregTech-style registry IDs, item classes, and tags. The indexed tier and circuit behavior is based on stable GTCEu patterns seen across the 1.20.1 and 1.21.1 codebases.
- Pack-specific or addon-specific GregTech items should still benefit when they use GTCEu namespaces, tags, machine classes, tiered naming, or standard energy hatch/generator naming.
- If an addon invents entirely custom naming and does not expose GregTech tags/classes, AMI may still categorize it as GregTech-family content but may not infer every tier, circuit grade, or EU/t fact.

## 1.0.2 - 2026-06-03

AMI 1.0.2 is a compatibility hotfix for Forge EMI installs and recipe viewer favorites.

### Fixed

- Fixed a Forge 1.20.1 crash during EMI recipe loading in production modpacks.
- Kept AMI favorites in sync when favoriting items from EMI recipe screens.
- Avoided unnecessary quest panel tree refreshes when visible quest content has not changed.

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
