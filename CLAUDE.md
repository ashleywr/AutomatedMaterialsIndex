# Automated Materials Index (AMI)

## Project Vision
The next-generation recipe and progression UI for modded Minecraft.

### The Problem
Modern modpacks have outgrown simple database dumps. Players are overwhelmed by 40 pages of micro-components when trying to build, and progression systems rely on clumsy external Quest Books.

### The AMI Solution
AMI is a smart, faceted recipe UI that bridges the gap between factory progression and interior design without requiring pack developers to write thousands of lines of manual scripts.

## Core Features

### 1. Material Root UI
- Eliminates "Chisel bloat" automatically
- Natively infers blockstates and semantic tags (like #forge:stone)
- Collapses variants (stairs, slabs, pillars) into single, clean base texture nodes
- Click the material, pick the shape from a sub-menu

### 2. Curio-Powered "Ghost Crafting"
- Building shouldn't break automation
- Equip the Architect's Gauntlet curio to seamlessly select sub-shapes from the UI
- Automatically craft the hard-coded item directly into your hand from base materials in inventory

### 3. Automated Indexing Pipeline
- No more manual KubeJS arrays
- Lightweight map-reduce style client job runs on load
- Automatically categorizes items by:
  - Color hex values
  - Mod origin
  - Material tier

### 4. Directed Progression Graph
- Say goodbye to the Quest Book
- Integrates natively with GameStages
- Hides endgame machinery behind a visually branching tech-tree
- Guides players naturally from Stone Age to Space Age

## Tech Stack
- **Mod Framework**: NeoForge 21.1.228
- **Minecraft Version**: 1.21.1
- **Language**: Java 21
- **Build System**: Gradle with ModDevGradle
- **IDE**: IntelliJ IDEA (recommended)

## Project Structure
- `src/main/java/com/example/ami/` - Main mod source code
- `src/main/resources/` - Assets, lang files, recipes
- `src/generated/resources/` - Data-generated content (recipes, tags, etc.)

## Development Setup
1. Open in IntelliJ IDEA → Select "Open" on this directory
2. IntelliJ auto-detects Gradle project and downloads dependencies
3. Run configurations available: `client`, `server`, `data` (datagen)
4. Refresh dependencies if needed: `gradlew --refresh-dependencies`

## Key Dependencies
- **Curios API**: For the Architect's Gauntlet curio integration
- **GameStages**: For progression gating
- **NeoForge**: Core mod framework

## Next Steps
1. Rename package from `examplemod` to `ami` (in progress)
2. Create core UI framework
3. Implement Material Root indexing system
4. Add Architect's Gauntlet curio item
5. Build progression graph visualization

## Development Workflow
- Use `gradlew runClient` to test in-game
- Data generation: `gradlew runData`
- Clean build: `gradlew clean`
- Build JAR: `gradlew build`
