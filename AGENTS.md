# Agent Notes

This is a NeoForge Minecraft mod targeting Minecraft 1.21.1.

Before changing Minecraft/NeoForge API usage, run:

```powershell
.\gradlew.bat syncReferenceSources
```

Then inspect the exact local reference sources under:

```text
internal/reference-sources/
```

Use these sources instead of guessing from older tutorials or memory. Check superclass contracts for registrations, events, attributes, renderers, data components, recipe viewer integration, and runtime-only Minecraft behavior.

Compilation is not enough for Minecraft runtime contracts. Prefer a smoke test or GameTest for code paths involving registries, entity construction, resource reloads, client setup, or generated data.
