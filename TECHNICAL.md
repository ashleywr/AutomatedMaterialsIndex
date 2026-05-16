# Automated Materials Index (AMI)

## Project Vision
The next-generation recipe and progression UI for modded Minecraft. Target environment: large modpacks (ATM-scale, 200-500 mods).

### The Problem
Modern modpacks have outgrown simple database dumps. Players are overwhelmed by thousands of micro-variants, progression is spoiled on Day 1, and complex mods require external spreadsheets.

### The AMI Solution
AMI is a smart, faceted recipe UI that indexes the entire game world at load time and surfaces it through a hierarchical Navigation Tree. Sidebar panel on inventory (JEI-style). No server required.

## Core Features (Roadmap)

### 1. Material Root UI (Phase 1 - in progress)
- Collapses stairs/slabs/walls/etc under a single material node
- Faceted "Group By" pivot: by Mod, Material, Tier, or Color
- Color-based "vibe" filtering (texture hex scanning)

### 2. World Atlas (Phase 1 - implemented)
- Biome, Structure, and Entity registry viewer
- Cycled via Tab key in the inventory overlay
- Populated from level registry on world load

### 3. Assembly Lab (Phase 2)
- Real-time stat simulator for Silent Gear / Tinkers' Construct
- Sandbox tool assembly with live DPS/durability calculation
- Parametric recipe templates to replace thousands of unique entries

### 4. Ghost Crafting & Architect's Gauntlet (Phase 3)
- Curio-slotted item enabling "Architect Mode"
- Select shapes from UI, craft directly into hand from inventory materials

### 5. Directed Progression Graph (Phase 4)
- GameStages integration: hides endgame items until unlocked
- Visual tech-tree replacing the Quest Book
- Operator controls for biome/mob weighting

## Tech Stack
- **Mod Framework**: NeoForge 21.1.228
- **Minecraft Version**: 1.21.1 only (no multi-version)
- **Language**: Java 21
- **Build System**: Gradle with ModDevGradle 2.0.141
- **IDE**: IntelliJ IDEA

## Architecture
- **Client-side only** — all indexing runs on the client thread
- Overlay renders via `ScreenEvent.Render.Post` on `AbstractContainerScreen`
- Compatible with JEI and EMI as optional integrations (bridges deferred)
- Standalone shell UI when neither JEI nor EMI is present

## Package Structure
```
com.sanhiruzu.ami
├── AMI.java              — @Mod entry point
├── AMIClient.java        — Client init, world-load indexing trigger
├── AMIConfig.java        — ModConfigSpec (enableAutoIndexing, etc.)
├── client/
│   ├── AMIKeyMappings.java       — Keybinds: I (open), Tab (cycle atlas)
│   ├── AMIScreen.java            — Full-screen fallback (I keybind)
│   ├── AtlasGridWidget.java      — Shared grid/list widget for all entry types
│   ├── InventoryOverlayHandler.java — Sidebar panel on container screens
│   └── RecipeViewerScreen.java   — Stub recipe detail screen
└── index/
    ├── AMIIndex.java             — Item index singleton
    ├── IndexCategory.java        — Enum: BY_COLOR, BY_MOD, BY_TIER, BY_VARIANT_GROUP
    ├── Indexer.java              — Item indexing pipeline (BuiltInRegistries)
    ├── MaterialEntry.java        — Record: item, modId, colorBucket, tier, variantGroup
    ├── WorldAtlasIndex.java      — Biome/structure/entity index singleton
    └── WorldAtlasIndexer.java    — Atlas indexing from level.registryAccess()
```

## Development Workflow
- `gradlew build` — builds and auto-copies JAR to PrismLauncher test instance
- `gradlew runClient` — launch dev client
- `gradlew clean` — clean build outputs

## Key Bindings
- `I` — open standalone AMIScreen (full-screen)
- `Tab` (in inventory) — cycle overlay between Items → Biomes → Structures → Entities → Items
