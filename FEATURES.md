# AMI Feature Roadmap

## Phase 1: JEI Parity (MVP)

### Core Recipe System
- [ ] Recipe registry - load and index all recipes from registries
- [ ] Multiple recipe type support (Crafting, Smelting, Furnace, etc.)
- [ ] Recipe data structures and lookup

### Item List & Search
- [ ] Item list panel - display all available items
- [ ] Search bar with live filtering
- [ ] Search operators (@mod, -exclude, #tag filters)
- [ ] Sorting (alphabetical, mod, quantity)

### Recipe Lookup UI
- [ ] Recipe viewer - show ingredients, outputs, and recipe details
- [ ] **R key** - view recipes to craft an item
- [ ] **U key** - view recipes that use this item as ingredient
- [ ] Recipe navigation (next/previous recipe)

### Inventory Integration
- [ ] Item list overlay in inventory screen
- [ ] Hovering items shows recipes
- [ ] Click item to view recipes
- [ ] **Ctrl+O** - toggle panel visibility

### Basic Features
- [ ] Bookmarks/Favorites - **A key** to bookmark items
- [ ] Hide items - **Ctrl+Click** to hide items/mods
- [ ] Cheat mode - spawn items (if admin/cheats enabled)
- [ ] Keyboard shortcuts help overlay

## Phase 2: AMI-Specific Enhancements

### Material Root UI
- [ ] Blockstate inference and collapsing
- [ ] Semantic tag grouping (#forge:stone, etc.)
- [ ] Material variants as sub-menus (stairs, slabs, pillars)

### Ghost Crafting (Architect's Gauntlet)
- [ ] Curio integration
- [ ] Sub-shape selection from UI
- [ ] Auto-craft to hand from inventory materials

### Automated Indexing
- [ ] Color hex categorization
- [ ] Mod origin classification
- [ ] Material tier detection
- [ ] Automatic material grouping

### Progression Graph
- [ ] GameStages integration
- [ ] Tech-tree visualization
- [ ] Stage-gated recipe hiding
- [ ] Player progression tracking

## Implementation Strategy
1. Start with basic recipe system and item list
2. Add search/filter functionality
3. Implement recipe lookup keybinds (R/U)
4. Polish UI and add bookmarks/hide
5. Then layer on AMI-specific features

## Current Status
- ✅ Project setup with NeoForge
- ✅ Basic mod initialization
- ⏳ Phase 1 features in progress
