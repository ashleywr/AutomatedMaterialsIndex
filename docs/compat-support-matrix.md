# AMI Compatibility Support Matrix

This document is the player-facing source of truth for AMI support statements.

Support levels:

- `Exact tested`: the exact upstream version is recorded from a repo-local source of truth.
- `Family tested`: AMI has focused compat code/tests for the family, but this document does not record one exact upstream version.
- `Best effort`: AMI integrates defensively through reflective or runtime-only paths and does not claim a fixed tested upstream version here.

## Recipe Viewers

| Integration | Loader / Minecraft | Support | Exact version / evidence | Notes |
| --- | --- | --- | --- | --- |
| JEI | NeoForge 1.21.1 | Exact tested | `19.27.0.340` from `gradle.properties` (`neoforge_jei_version`) | Primary external recipe viewer dependency used by local and release builds. |
| JEI | Fabric 1.21.1 | Exact tested | `19.27.0.340` from `gradle.properties` (`fabric_jei_version`) | Fabric release pipeline publishes the AMI Fabric jar with JEI marked optional. |
| JEI | Forge 1.20.1 | Exact tested | `15.20.0.130` from `gradle.properties` (`forge_jei_version`) | Forge release packaging uses the reobfuscated release jar. |
| EMI | NeoForge 1.21.1 | Exact tested | `1.1.24` from `gradle.properties` (`neoforge_emi_version`) | Supported as an optional external recipe viewer. |
| EMI | Fabric 1.21.1 | Exact tested | `1.1.24` from `gradle.properties` (`fabric_emi_version`) | Supported as an optional external recipe viewer. |
| EMI | Forge 1.20.1 | Exact tested | `1.1.24` from `gradle.properties` (`forge_emi_version`) | Supported as an optional external recipe viewer. |
| REI | Fabric 1.21.1 | Exact tested | `16.0.799` from `gradle.properties` (`fabric_rei_version`) | Fabric-only viewer path; REI is mutually exclusive with JEI/EMI in local runtime setup. |

## Player Utility And Waypoint Families

| Integration family | Loader / Minecraft | Support | Exact version / evidence | Notes |
| --- | --- | --- | --- | --- |
| JourneyMap | NeoForge 1.21.1 | Exact tested | `1.21.1-6.0.0-beta.83+neoforge` from `CHANGELOG.md` 1.5.0 compatibility notes | Native add-waypoint actions remain best-effort at runtime when JourneyMap API classes are available. |
| Xaero's Minimap | Forge 1.20.1 / NeoForge 1.21.1 | Exact tested | `forge-1.20.1-26.1.0` and `neoforge-1.21.1-26.1.0` from `CHANGELOG.md` 1.5.0 compatibility notes | Mapping-family support claim is scoped to the recorded versions only. |
| Xaero's World Map | Forge 1.20.1 / NeoForge 1.21.1 | Exact tested | `forge-1.20.1-1.41.0` and `neoforge-1.21.1-1.41.0` from `CHANGELOG.md` 1.5.0 compatibility notes | Mapping-family support claim is scoped to the recorded versions only. |
| FTB Chunks waypoint actions | Current supported loaders | Best effort | No exact upstream version recorded in this repo | Runtime integration is reflective/runtime-driven; do not describe it as exact-version support without new evidence. |
| Waystones waypoint actions | Current supported loaders | Best effort | No exact upstream version recorded in this repo | Runtime integration depends on installed API availability and should stay conservative in player-facing claims. |

## Guide And Document Families

| Integration family | Loader / Minecraft | Support | Exact version / evidence | Notes |
| --- | --- | --- | --- | --- |
| Patchouli guide indexing/opening | Current supported loaders | Family tested | Focused compat code and tests in `xplat/.../PatchouliRuntimeGuideSource.java` and `neoforge/.../Patchouli*Test.java` | Do not imply exact tested upstream coverage until a specific Patchouli version is recorded here. |
| GuideME / AE2 guide indexing/opening | Current supported loaders | Family tested | Focused compat code and tests in `xplat/.../GuideMeRuntimeGuideSource.java` and `neoforge/.../AmiGuideOpenersTest.java` | Exact upstream version not yet recorded in this matrix. |
| Modonomicon guide indexing/opening | Current supported loaders | Family tested | Focused compat code and tests in `xplat/.../ModonomiconRuntimeGuideSource.java` and `neoforge/.../Modonomicon*Test.java` | Exact upstream version not yet recorded in this matrix. |
