# AMI Compat Support Matrix

This document answers a simple release-support question:

- Which compat surfaces are intended to work on Forge, NeoForge, or both?
- Which exact third-party mod versions were tested and recorded for this branch?
- Which compat behaviors are only family-level heuristics and therefore not pinned to one exact upstream version yet?

Use this before telling players "AMI supports mod X". If the exact upstream version is not recorded here, do not imply
that a newer or older mod build was verified. Say that AMI has family-level support or best-effort reflective support
instead.

## Status Meanings

- `Exact tested`: this repo has a local source of truth for the tested version, such as a pinned dev dependency, a local
  runtime log, or a checked-in fixture/source checkout tied to the current branch work.
- `Family tested`: AMI has compat code and targeted tests for the mod family, but this branch does not yet record one
  exact upstream mod version in a user-facing support ledger.
- `Best effort`: AMI uses reflective or metadata-only integration that is designed to fail closed when the upstream mod
  changes. This is safer than a hard dependency, but it is not a promise that every newer build still behaves the same.

## Platform Baseline

These are exact because AMI builds and dev runs pin them directly in `gradle.properties`.

| Surface | Forge | NeoForge | Evidence |
| --- | --- | --- | --- |
| Minecraft | `1.20.1` | `1.21.1` | `gradle.properties` |
| Loader | Forge `47.4.20` | NeoForge `21.1.228` | `gradle.properties` |
| JEI | `15.20.0.130` | `19.27.0.340` | `gradle.properties` |
| EMI | `1.1.24+1.20.1` | `1.1.24+1.21.1` | `gradle.properties`, `forge/build.gradle`, `neoforge/build.gradle` |

## Compat Matrix

| Compat family / mod | Intended scope | Support level | Exact tested version(s) recorded here | Notes |
| --- | --- | --- | --- | --- |
| Cobblemon | NeoForge-focused runtime compat, shared xplat classification/search | Family tested | None recorded yet | Classification, species indexing, Pokedex opening, and Cobblemon-specific actions exist, but the branch does not yet record one exact Cobblemon build in a release-facing ledger. Do not claim blanket support for "latest Cobblemon". |
| GregTech / GTCEu | Shared xplat classification/search | Family tested | None recorded yet | Family detection normalizes `gtceu`/`gregtech` and the branch has focused JVM coverage, but exact tested upstream jars are not recorded in a user-facing place yet. |
| Botania / MythicBotany / Botanical Machinery / ExtraBotany | Shared xplat classification/search and guide indexing | Family tested | None recorded yet | Current support is primarily category/routing behavior plus guide docs. Record exact pack/mod builds before presenting this as version-pinned support. |
| Ars Nouveau | Shared xplat classification/search | Family tested | None recorded yet | Focused compat class and tests exist. Exact upstream version still needs to be logged for release support. |
| Spectrum | Shared xplat classification/search and guide indexing | Family tested | None recorded yet | Focused compat class and tests exist. Exact upstream version not yet recorded. |
| Nature's Aura | Shared xplat classification/search and guide/openability surfaces | Family tested | None recorded yet | Focused compat class and tests exist. Exact upstream version not yet recorded. |
| Mana and Artifice | Shared xplat classification/search | Family tested | None recorded yet | Focused compat class and tests exist. Exact upstream version not yet recorded. |
| Alex's Caves | Shared xplat classification/search and guide indexing | Family tested | None recorded yet | Branch work was validated against runtime dumps, but the exact Alex's Caves build is not surfaced in a stable support ledger. |
| Alex's Mobs | Shared xplat classification/search | Family tested | None recorded yet | Same gap as Alex's Caves: tested behavior exists, exact released upstream build not yet recorded. |
| TacZ / Timeless and Classics Zero | Shared xplat classification/search | Family tested | None recorded yet | Focused compat class and tests exist. Exact upstream version not yet recorded. |
| Applied Energistics 2 | Shared xplat classification/search and GuideME guide integration | Family tested | None recorded yet | AE2-family routing is covered; exact AE2 build should be recorded alongside the GuideME build if users are expected to rely on a pinned statement. |
| AppMek | Shared xplat classification/search | Family tested | None recorded yet | AMI treats `appmek` as AE2-family storage/channel content. Exact upstream version not yet recorded. |
| Mekanism family | Shared xplat classification/search | Family tested | None recorded yet | Core family routing is present, but this branch did not add a version ledger for exact supported Mekanism builds. |
| Silent Gear | Shared xplat classification/search and material-book guide integration | Family tested | None recorded yet | Guide/content support is real, but exact upstream version is not surfaced in a support ledger yet. |
| Tinkers Construct | Forge-focused family validation in this branch, shared xplat routing | Family tested | None recorded yet | Branch notes cite Forge dump validation and Mantle guide handling, but no exact TConstruct build is recorded here yet. |
| Waystones | Shared xplat classification plus runtime waypoint/screen actions | Exact tested | NeoForge: `21.1.34` | Exact NeoForge version observed in `run/neoforge-emi/logs/latest.log`. Current runtime actions are still loader/mod-ABI sensitive and should be treated as exact-version support unless more versions are logged. |
| JourneyMap | Runtime waypoint provider | Best effort | None recorded yet | Reflective provider with copy/export fallback and native add action only when local API classes resolve. |
| FTB Chunks | Runtime waypoint provider | Best effort | None recorded yet | Reflective provider with copy/export fallback and native add action only when the client API shape matches. |
| Xaero Minimap / Xaero World Map | Runtime waypoint detection/export | Best effort | None recorded yet | Detection and copy/export are present; direct native waypoint creation is still deferred. |

## Release Policy

For future 1.4.x work:

1. When a compat fix is validated against a concrete upstream mod build, add that exact version here in the same commit.
2. If the compat is runtime-reflective or loader-specific, say so explicitly instead of implying broad cross-version support.
3. If the exact version is unknown, leave the cell blank and keep the support level at `Family tested` or `Best effort`.
4. Release notes should link to this file whenever they mention a mod compat family by name.

The AMI release checklist and `ami-release-publisher` skill should treat this file as required release metadata, not an
optional follow-up doc.

## Current Gaps

- Most 1.4.0 compat families are covered by code and JVM tests, but not by a user-facing exact-version ledger.
- NeoForge runtime evidence is present locally for Waystones; similar exact logs or manifest captures should be added for
  Cobblemon, GregTech/GTCEu, Ars Nouveau, Spectrum, Nature's Aura, MNA, Alex's Caves/Mobs, TacZ, and the map mods if
  AMI is going to advertise those as tested combinations.
- Forge and NeoForge may intentionally support different mod/version lines for the same family. Record them as separate
  cells rather than forcing one shared "supported version" value.

## Monitoring Upstream Changes

The safest policy is to treat compat updates as something AMI should proactively notice, not something players have to
discover first.

- For GitHub-hosted mods, prefer repository watch settings scoped to release notifications so AMI maintainers hear about
  new upstream releases without subscribing to every issue and pull request.
- For Modrinth-hosted mods, prefer polling the official project versions API on a schedule and comparing the newest
  version against the last version recorded here.
- For reflective map/runtime integrations such as JourneyMap, FTB Chunks, and Xaero, react to upstream releases faster
  because these are the most likely to drift without compile-time failures.
- When no exact tested version is recorded yet, treat a new upstream release as a prompt to either test and record it or
  keep the support level at `Family tested` / `Best effort`.

The repo now includes:

- `.github/compat-watchlist.json` as the machine-readable upstream watch baseline
- `scripts/check_compat_updates.py` for scheduled upstream checks
- `.github/workflows/compat-watch.yml` to open or refresh a single compat-watch issue when tracked upstream versions move
