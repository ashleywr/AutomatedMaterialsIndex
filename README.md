# Automated Materials Index (AMI)

Automated Materials Index is a search and recipe browsing mod for large Minecraft modpacks. It builds a client-side index of items, entities, biomes, structures, and recipe data, with optional JEI and EMI integration.

AMI targets NeoForge 1.21.1 and Forge 1.20.1.

## Features

- **Material grouping**: Groups related block variants such as stairs, slabs, walls, colors, and other material families so large item lists are easier to scan.
- **Structured search**: Supports normal text search plus filters for tags, mods, categories, properties, and numeric metadata such as `>energy:50000`, `>damage:15`, or `>dps:8`.
- **Item facts**: Indexes useful details such as durability, tool stats, armor stats, food values, storage capacity, energy capacity, and fluid capacity when AMI can detect them.
- **Entity search**: Adds searchable entity entries with category, health, attack damage, spawn egg support, and hints such as mountable or tamable when available.
- **World entries**: Includes indexed biomes and structures alongside item and entity results.
- **Recipe viewer support**: Works with AMI's built-in viewer, JEI, or EMI. AMI panels support lookup history, favorites, cheat actions, and recipe transfer when the current screen supports it.
- **Large pack support**: Keeps search and browsing responsive across large registries.

## Compat Notes

AMI includes both broad family-level compat heuristics and narrower runtime integrations. Those are not the same thing.

If you need to know whether a specific third-party mod version was actually tested for this branch, check
[`docs/compat-support-matrix.md`](docs/compat-support-matrix.md). It separates exact tested versions from broader
family-level support and from best-effort reflective integrations that are meant to fail closed when upstream mods
change.

Release and support statements should use that matrix, not memory. If a compat mod updates and AMI is retested against a
new upstream build, update the exact tested version in the matrix in the same change set as the release notes.

AMI-owned compat is a practical short-term path, not the ideal long-term ownership model for every mod ecosystem. Mod
authors and maintainers are welcome to report gaps, contribute fixes, expose shared providers from their own mod, or
move the compat surface into their own project when that makes more sense. When a mod can own its own searchable guides,
metadata enrichment, representative items, or result actions, that is usually a better long-term fit than leaving AMI to
carry fragile downstream heuristics forever.

## Performance and Benchmarks

We use a headless NeoForge GameTest suite to keep track of how the search index performs.

To run the tests and the benchmark locally:

```bash
./gradlew check
```

The results are saved to `run/config/ami_benchmark_history.jsonl` as JSON Lines. This file is ignored by git so your
local results don't clutter up the repo.

If you need a larger sample size for more accurate numbers, you can increase the iteration count:

```bash
./gradlew check -Pami_benchmark_iterations=500
```

When putting together release notes, the latest entry in that JSONL file provides the stats for things like indexed item
counts and search latency (average and P99).

## Development Setup

This project includes NeoForge and Forge modules. Opening it in IntelliJ or Eclipse should handle most of the setup
automatically.

If dependencies stop resolving cleanly, refresh them and rebuild:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

### Mappings

We use Mojang's official mappings. You can find the license for those here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Pack Override Editor

A browser-based editor for `overrides.json` lives in [`docs/override-editor/`](docs/override-editor/README.md) and is served via GitHub Pages.

## Resources

- [NeoForged Docs](https://docs.neoforged.net/)
- [NeoForged Discord](https://discord.neoforged.net/)

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
