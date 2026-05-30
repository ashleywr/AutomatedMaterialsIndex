# Automated Materials Index (AMI)

  AMI is an index and search mod for NeoForge 1.21.1 and Forge 1.20.1 designed for large Minecraft modpacks. It builds a client-side searchable index of items, entities, biomes, structures, and recipe data, with recommended integration for JEI and EMI for the recipe viewer support.

  * **Material Grouping**: Groups related block variants such as stairs, slabs, walls, and other material families to make large item lists easier to browse.
  * **Structured Query Search**: Supports text search plus filters for tags, mods, categories, properties, and numeric metadata such as `>energy:50000`, `>damage:15`, or `>dps:8`.
  * **Item Metadata Indexing**: Detects useful item facts such as durability, tool stats, armor stats, food values, storage capacity, energy capacity, and fluid capacity where available.
  * **Entity Indexing**: Adds searchable entity entries with category, health, attack damage, spawn egg support, and metadata hints such as mountable or tamable where detectable.
  * **Biome and Structure Search**: Includes indexed biome and structure entries alongside item and entity results.
  * **Recipe Viewer Integration**: Works standalone or alongside JEI/EMI, including lookup history, favorites, cheat-mode actions, and recipe transfer from AMI panels.
  * **Built for Large Modpacks**: Designed to keep searching and browsing responsive across large registries.

## Performance & Benchmarks

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

If dependencies get weird or something isn't loading right, you can try:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

### Mappings

We use Mojang's official mappings. You can find the license for those here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Resources

- [NeoForged Docs](https://docs.neoforged.net/)
- [NeoForged Discord](https://discord.neoforged.net/)

## License

This project is open-source and licensed under the MIT License. See the `LICENSE` file for details.
