# Automated Materials Index (AMI)

AMI is an optimized search mod for NeoForge 1.21.1 (with Forge 1.20.1 support in this repository) designed to manage the
massive item registries of large-scale Minecraft modpacks. It operates entirely client-side to organize inventory
clutter into a clean, mathematically searchable data structure without requiring server installation.

* **Material Grouping**: Collapses endless variants of building blocks, such as stairs, slabs, and walls, under a single
  material node to streamline the inventory interface.
* **Universal Query Language**: Replaces basic text matching with relational database filtering, allowing players to
  execute mathematical queries like "energy>50000" or "damage>15" directly in the search bar.
* **Deep Component Simulation**: Evaluates Data Components to calculate and display simulated DPS, durability, and trait
  synergies for modular weapons and tools.
* **Normalized Metrics**: Assigns standardized integer values to diverse modded storage systems, fluid tanks, and energy
  generators to enable direct cross-mod utility comparisons.
* **Entity Behavioral Indexing**: Scans entity registries to allow filtering mobs by live game mechanics, such as
  mountability, base health, or specific taming requirements.
* **Spatial Awareness**: Bridges inventory management with world navigation by integrating natively with map mods to
  support waypoint, biome, and structure searching.
* **Advanced Filtering**: Allows players to pivot search results by source mod, tech tier, or color by scanning item
  textures to match specific aesthetic requirements.
* **Built for Scale**: Engineered for modpacks containing hundreds of mods, providing rapid search queries with minimal
  memory overhead.
* **Seamless Integration**: Functions as a standalone search tool or integrates directly alongside existing recipe
  viewers like JEI and EMI.

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
