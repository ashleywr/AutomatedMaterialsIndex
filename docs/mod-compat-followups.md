# Mod Compatibility Follow-ups

Checkpoint from the context-menu and overlay pass.

Completed:

- GUI right-click context menu for item and group results.
- Context menu action builder and policy config gates.
- Contextual recipe/use visibility based on indexed recipe evidence.
- Craft 1 and Craft Stack actions when recipe transfer is available.
- Cheat actions when cheat mode is enabled.
- Direct vanilla Minecraft Wiki links for biome pages.
- Overlay z-layer cleanup for menus, dropdowns, and tooltips.
- Depth isolation for 3D entity and Pokemon icons.

Follow-ups to continue later:

- Tinkers Construct and Silent Gear compatibility:
  - Index focused metadata for tool parts, materials, traits, stats, upgrades, and modifiers.
  - Add deterministic category/search tests so these mods do not collapse into generic tools/materials poorly.
  - Add search help examples once metadata is stable.
- Maps and waypoints:
  - Test Xaero, JourneyMap, and FTB map-style integrations when available.
  - Decide per-mod behavior for Open Map/Waypoint actions: open map UI, copy coordinates, create waypoint, or search/locate.
  - Add context-menu actions only when the relevant map mod is detected.
- Context-menu submenus:
  - Add tiered entries such as Add to > Favorites / Quest / Chat.
  - Consider grouping Open > Recipes / Uses / Wiki / Map if the menu grows.
- Runtime visual smoke:
  - Use AutoMine screenshots once a NeoForge EMI client and world are loaded.
  - Recheck z-layer ordering, entity icon depth, context menu keyboard mnemonics, and recipe/cheat actions in-game.
- Menu polish:
  - Consider separators, icons, disabled-state tooltips, and clearer labels for advanced actions.
