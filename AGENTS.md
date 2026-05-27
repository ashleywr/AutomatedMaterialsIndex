# Agent Notes

This is a NeoForge Minecraft mod targeting Minecraft 1.21.1.

Before changing Minecraft/NeoForge API usage, try to refresh the local reference sources first:

```powershell
.\gradlew.bat syncReferenceSources
```

If that task is unavailable in this repo, use the checked-in reference sources already present under:

```text
internal/reference-sources/
```

Before changing EMI/JEI integration behavior, refresh local recipe viewer sources:

```powershell
.\gradlew.bat syncRecipeViewerSources
```

Resolved dependency source jars are extracted under:

```text
vendor-sources/resolved/
```

Full local vendor checkouts, when present, live under:

```text
vendor-sources/emi/
vendor-sources/JustEnoughItems-1.21.1/
```

Inspect the exact local reference sources there instead of guessing from older tutorials or memory. Check superclass contracts for registrations, events, attributes, renderers, data components, recipe viewer integration, and runtime-only Minecraft behavior.

Compilation is not enough for Minecraft runtime contracts. Prefer a smoke test or GameTest for code paths involving registries, entity construction, resource reloads, client setup, or generated data.
