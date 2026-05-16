# Release Checklist

Use this checklist when preparing a public AMI release.

## 1. Decide Scope

- Pick the release version, for example `0.1.0`.
- Write a short release goal for what this version is meant to prove.
- Freeze features for the version. After this point, only fixes should land.

## 2. Clean Local Verification

Run:

```powershell
.\gradlew.bat clean check
```

This verifies Java compilation, JUnit tests, and the AMI GameTest benchmark suite.

For a longer benchmark sample:

```powershell
.\gradlew.bat check -Pami_benchmark_iterations=500
```

## 3. Capture Benchmark Summary

Use the newest line from:

```text
run/config/ami_benchmark_history.jsonl
```

Include these values in release notes:

- `indexed_items`
- `query_executions`
- `average_search_latency_ms`
- `p99_search_latency_ms`
- `skipped_anomalies`

Example:

```text
Benchmark: 1,332 indexed items, 960 benchmark queries, 0.84 ms average search latency, 4.21 ms P99, 0 skipped anomalies.
```

Pause the release and investigate if P99 latency jumps sharply or skipped anomalies is nonzero.

## 4. Manual Smoke Test

Run:

```powershell
.\gradlew.bat runClient
```

Check:

- Game starts cleanly.
- Inventory opens.
- Search bar accepts typing.
- Queries like `iron`, `oak`, `sword`, and `#minecraft:planks` work.
- EMI/JEI integration behaves as expected.
- Config screens open where applicable.
- Logs do not show unexpected errors or repeated warnings.

## 5. Version And Metadata

Update:

- `gradle.properties` `mod_version`
- `src/main/templates/META-INF/neoforge.mods.toml` dependency ranges, URLs, and description if needed
- `README.md` supported Minecraft and NeoForge versions
- `CHANGELOG.md`

## 6. Build Artifact

Run:

```powershell
.\gradlew.bat build
```

Upload the normal mod jar from:

```text
build/libs/
```

Do not upload source, dev, or generated helper jars as the main release artifact.

## 7. Release Notes Template

```md
## AMI VERSION

Short summary for Minecraft VERSION / NeoForge VERSION.

### Added
- 

### Changed
- 

### Fixed
- 

### Performance
- Indexed items:
- Benchmark queries:
- Average search latency:
- P99 search latency:
- Skipped anomalies:

### Known Issues
- 
```

## 8. Publish

Publish to the chosen channels:

- GitHub Release
- Modrinth
- CurseForge, if desired

Include:

- Minecraft version
- NeoForge version range
- Required and optional dependencies
- Known incompatibilities
- Built jar
- Release notes

## 9. Post-Release

- Create a Git tag, for example `v0.1.0`.
- Keep the exact released jar.
- Watch crash reports and issue reports.
- Start a new `Unreleased` section in `CHANGELOG.md`.

