# Pack Override Editor Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a browser-based override editor (served from `/docs` via GitHub Pages) that lets modpack authors load a registry dump from their instance, fine-tune item classification + custom tooltip lines through a filterable bulk-edit grid, and emit an `overrides.json` they drop into `config/ami/` — applied as the highest-priority layer that survives reindex.

**Architecture:** Three coordinated pieces:
1. **Mod-side dump command** (`/ami dump-registry`) extends the existing `AmiClientCommands` pattern to emit a per-item JSON dump (id, mod, class name, display name, creative-tab labels, current category/subcategory/facets).
2. **Mod-side pack-config loader** extends `ClassificationOverrides` to load `<gamedir>/config/ami/overrides.json` *after* bundled defaults, merging it on top so pack edits win over mod-shipped defaults. The override record gains a `tooltipLines` field; a tooltip hook injects those lines.
3. **Static web tool** at `docs/override-editor/` — vanilla ES modules, no build step, plain HTML/CSS. Drag-drop two JSON files in, edit in a virtualized grid, click Download to save the sparse-patch result. Pure-logic modules are unit-tested with Node's built-in test runner.

**Tech Stack:**
- Java 21, NeoForge 1.21.x (mod side; pattern mirrored to Forge + Fabric loader classes)
- Gson (already a project dep, used throughout `ClassificationOverrides`)
- JUnit 5 (existing test infrastructure in `neoforge/src/test/`)
- Vanilla ES2022 modules + plain HTML/CSS for `docs/override-editor/`
- Node 20+ built-in test runner (`node --test`) for tool unit tests — Node is on the dev box but is NOT required to use the deployed tool

## Global Constraints

- Schema version: every override file carries `"schemaVersion": 1` at top level. Tool refuses to load mismatched major versions; mod tolerates unknown fields (forward-compat).
- Backward compat: existing bundled `xplat/src/main/resources/assets/ami/classification_overrides.json` must continue to load with no migration. The `schemaVersion` field is optional in v1 — missing == v1.
- All file paths in this plan are repo-relative to `C:/WorkDir/AutomatedMaterialsIndex/.dev/worktree/wise-garden/` unless absolute.
- Override loader fail-safe: malformed JSON or read errors must never break indexing — log and proceed with what loaded successfully. Mirror the existing `loadBundledDefaults()` try/catch pattern.
- Web tool runs from `file://` AND from GitHub Pages — use only universal browser APIs (drag-drop file input + `<a download>` Blob URL). Do NOT use the File System Access API.
- Tooltip rendering must respect AMI's existing tooltip-policy gate (`ItemTooltipEventPolicyTest`) — if tooltips are disabled in AMI settings, custom lines are also suppressed.
- Commit attribution: never add Co-Authored-By or AI attribution lines to commit messages.

---

## File Structure

**Mod (Java) — new files:**
- `xplat/src/main/java/com/sanhiruzu/ami/index/PackOverrideLoader.java` — reads `<gamedir>/config/ami/overrides.json` and merges into `ClassificationOverrides`
- `xplat/src/test/java/com/sanhiruzu/ami/index/PackOverrideLoaderTest.java` — pure-Java unit tests for merge precedence + malformed-input tolerance
- `xplat/src/main/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipAppender.java` — pulls `tooltipLines` off the override and appends to the item tooltip
- `neoforge/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideTooltipTest.java` — verifies tooltip lines surface in the tooltip pipeline
- `neoforge/src/test/java/com/sanhiruzu/ami/index/PackOverrideIntegrationTest.java` — bundled-then-pack precedence over a sample fixture

**Mod (Java) — modified files:**
- `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java` — add `List<String> tooltipLines` field (compact constructor for back-compat)
- `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java` — parse `tooltipLines`; add `mergeAndInstall(...)` (does NOT replace bundled — layers on top)
- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java` — call `PackOverrideLoader.load()` after `ClassificationOverrides.loadBundledDefaults()` during populate
- `neoforge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java` — register `dump-registry` literal
- `forge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java` — same registration (mirror)
- `xplat/src/main/java/com/sanhiruzu/ami/index/RegistryDumpWriter.java` — **new** — pure xplat dump-builder so both loader-specific command classes call shared logic

**Web tool — new files under `docs/override-editor/`:**
- `index.html` — single-page UI shell
- `styles.css` — minimal styling (dark theme to match repo aesthetic)
- `app.js` — entry module; wires UI events to logic modules
- `lib/load.js` — file-drop / file-picker → text → parsed objects
- `lib/merge.js` — joins registry dump rows with existing override entries into a unified item model
- `lib/grid.js` — virtualized filterable grid renderer
- `lib/edit.js` — bulk-edit panel logic (category/subcategory/facets/tooltip lines)
- `lib/diff.js` — produces the sparse-patch override JSON from the edited model vs. the loaded baseline
- `lib/validate.js` — schema-version + stale-ID + known-facet checks
- `lib/constants.js` — known facet/category enum values exported from the mod (generated by Task 11)
- `tests/load.test.mjs`, `tests/merge.test.mjs`, `tests/diff.test.mjs`, `tests/validate.test.mjs` — Node `--test` files
- `tests/fixtures/registry-dump-small.json`, `tests/fixtures/overrides-baseline.json`
- `.nojekyll` (at `docs/.nojekyll`) — disables Jekyll processing so `docs/override-editor/lib/*.js` is served as-is

**Generator (new):**
- `scripts/export-tool-constants.mjs` — reads `ItemFacet.java` / `AmiOntologyKinds.java` etc., emits `docs/override-editor/lib/constants.js`. Run manually; re-run when enums change.

---

## Task 1: Add the `dump-registry` command (NeoForge slice)

**Files:**
- Create: `xplat/src/main/java/com/sanhiruzu/ami/index/RegistryDumpWriter.java`
- Create: `xplat/src/test/java/com/sanhiruzu/ami/index/RegistryDumpWriterTest.java`
- Modify: `neoforge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java` (add `then(Commands.literal("dump-registry"))` branch)

**Interfaces:**
- Consumes: existing `GlobalIndex.getInstance().getNodes(NodeType.ITEM)` to enumerate indexed items; existing `ClassificationOverrides.forItem(id)` to read current override state; `BuiltInRegistries.ITEM` for class/mod metadata.
- Produces: `RegistryDumpWriter.writeJson(Path out, List<RegistryDumpWriter.Row> rows): int` — returns row count. `RegistryDumpWriter.Row` record: `(String id, String mod, String className, String displayName, List<String> creativeTabs, String currentCategory, String currentSubcategory, List<String> currentFacets)`. The collecting helper `RegistryDumpWriter.collectFromRuntime(Level level): List<Row>` lives in the same file and is the entry point for the command.

- [ ] **Step 1: Write the failing test for the dump writer**

Create `xplat/src/test/java/com/sanhiruzu/ami/index/RegistryDumpWriterTest.java`:

```java
package com.sanhiruzu.ami.index;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistryDumpWriterTest {

    @Test
    void writeJson_emitsVersionedDocumentWithExpectedRows(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("registry-dump.json");
        var rows = List.of(
            new RegistryDumpWriter.Row(
                "minecraft:diamond_sword", "minecraft",
                "net.minecraft.world.item.SwordItem", "Diamond Sword",
                List.of("Combat"), "weapons", "melee",
                List.of("melee_weapon"))
        );

        int count = RegistryDumpWriter.writeJson(out, rows);

        assertEquals(1, count);
        JsonObject doc = JsonParser.parseString(Files.readString(out)).getAsJsonObject();
        assertEquals(1, doc.get("schemaVersion").getAsInt());
        assertEquals(1, doc.getAsJsonArray("items").size());
        JsonObject item = doc.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("minecraft:diamond_sword", item.get("id").getAsString());
        assertEquals("minecraft", item.get("mod").getAsString());
        assertEquals("net.minecraft.world.item.SwordItem", item.get("className").getAsString());
        assertEquals("Diamond Sword", item.get("displayName").getAsString());
        assertEquals("Combat", item.getAsJsonArray("creativeTabs").get(0).getAsString());
        assertEquals("weapons", item.get("currentCategory").getAsString());
        assertEquals("melee", item.get("currentSubcategory").getAsString());
        assertEquals("melee_weapon", item.getAsJsonArray("currentFacets").get(0).getAsString());
    }

    @Test
    void writeJson_emptyRows_writesEmptyArrayNotNull(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("registry-dump.json");
        int count = RegistryDumpWriter.writeJson(out, List.of());
        assertEquals(0, count);
        JsonObject doc = JsonParser.parseString(Files.readString(out)).getAsJsonObject();
        assertTrue(doc.has("items"));
        assertEquals(0, doc.getAsJsonArray("items").size());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.RegistryDumpWriterTest"`
Expected: FAIL — `RegistryDumpWriter` class does not exist.

- [ ] **Step 3: Implement `RegistryDumpWriter`**

Create `xplat/src/main/java/com/sanhiruzu/ami/index/RegistryDumpWriter.java`:

```java
package com.sanhiruzu.ami.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Builds and writes the registry-dump JSON consumed by the override editor tool. */
public final class RegistryDumpWriter {

    public static final int SCHEMA_VERSION = 1;

    public record Row(String id, String mod, String className, String displayName,
                      List<String> creativeTabs,
                      String currentCategory, String currentSubcategory,
                      List<String> currentFacets) {}

    private RegistryDumpWriter() {}

    public static int writeJson(Path out, List<Row> rows) throws IOException {
        JsonObject doc = new JsonObject();
        doc.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray items = new JsonArray();
        for (Row r : rows) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.id());
            o.addProperty("mod", r.mod());
            o.addProperty("className", r.className());
            o.addProperty("displayName", r.displayName());
            o.add("creativeTabs", strings(r.creativeTabs()));
            if (r.currentCategory() != null) o.addProperty("currentCategory", r.currentCategory());
            if (r.currentSubcategory() != null) o.addProperty("currentSubcategory", r.currentSubcategory());
            o.add("currentFacets", strings(r.currentFacets()));
            items.add(o);
        }
        doc.add("items", items);
        Files.writeString(out, doc.toString(), StandardCharsets.UTF_8);
        return rows.size();
    }

    private static JsonArray strings(List<String> values) {
        JsonArray a = new JsonArray();
        if (values != null) {
            for (String s : values) a.add(new JsonPrimitive(s));
        }
        return a;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.RegistryDumpWriterTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Add the runtime collector**

`collectFromRuntime` cannot be unit-tested without a live client (it walks live registries + GlobalIndex), so it's added without a TDD step and verified manually in-game in Step 7. Append to `RegistryDumpWriter.java`:

```java
    public static List<Row> collectFromRuntime(net.minecraft.world.level.Level level) {
        java.util.List<Row> rows = new java.util.ArrayList<>();
        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ITEM)) {
            net.minecraft.world.item.ItemStack stack = node.getDisplayStack();
            if (stack == null || stack.isEmpty()) continue;
            net.minecraft.world.item.Item item = stack.getItem();
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;

            String displayName = stack.getHoverName().getString();
            String className = item.getClass().getName();
            String mod = id.getNamespace();

            String category = node.getAttribute(SearchNodeKeys.CATEGORY);
            String subcategory = node.getAttribute(SearchNodeKeys.SUBCATEGORY);
            java.util.List<String> facets = node.getListAttribute(SearchNodeKeys.FACETS);
            java.util.List<String> tabs = node.getListAttribute(SearchNodeKeys.CREATIVE_TABS);

            rows.add(new Row(id.toString(), mod, className, displayName,
                tabs == null ? java.util.List.of() : tabs,
                category, subcategory,
                facets == null ? java.util.List.of() : facets));
        }
        return rows;
    }
```

**NOTE for the engineer:** The exact `SearchNodeKeys` attribute names (`CATEGORY`, `SUBCATEGORY`, `FACETS`, `CREATIVE_TABS`) and the `SearchNode` accessor methods (`getAttribute`, `getListAttribute`, `getDisplayStack`) are the assumed shape from the existing dump-search-nodes command. **Open `xplat/src/main/java/com/sanhiruzu/ami/index/SearchNodeKeys.java` and `SearchNode.java` and adjust the names above to match.** If `getDisplayStack` does not exist, look at how `SearchNodeMirrorDump` (referenced in `AmiClientCommands.exportSearchNodes`) reconstructs item info from a node and mirror that. The test in Step 1 does not exercise this method, so adapt freely.

- [ ] **Step 6: Wire the command into `AmiClientCommands`**

In `neoforge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java`, add inside the chain of `.then(Commands.literal(...))` calls in `onClientCommandsRegister` (after `dump-recipe-viewer-recipes`, before `reindex`):

```java
                .then(Commands.literal("dump-registry")
                        .executes(context -> {
                            exportRegistryDump(context.getSource());
                            return 1;
                        })
                )
```

Add the method body next to the other `export*` methods:

```java
    private static void exportRegistryDump(CommandSourceStack source) {
        Path dumpDir = dumpDir("registry");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("registry-dump.json");
            int count = RegistryDumpWriter.writeJson(out,
                    RegistryDumpWriter.collectFromRuntime(net.minecraft.client.Minecraft.getInstance().level));
            source.sendSystemMessage(Component.literal(
                    "AMI registry dump written to " + out.toAbsolutePath() + " (" + count + " items)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export AMI registry dump", e);
            source.sendSystemMessage(Component.literal("Failed to export AMI registry dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }
```

- [ ] **Step 7: Compile and verify**

Run: `./gradlew :neoforge:compileJava`
Expected: BUILD SUCCESSFUL. If `SearchNode`/`SearchNodeKeys` accessor names differ, fix the compile errors in `RegistryDumpWriter.collectFromRuntime` to match the real API. **Do NOT skip this — guessed API is the most likely failure point.**

- [ ] **Step 8: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/RegistryDumpWriter.java \
        xplat/src/test/java/com/sanhiruzu/ami/index/RegistryDumpWriterTest.java \
        neoforge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java
git commit -m "feat(ami): add /ami dump-registry command emitting versioned registry-dump.json"
```

---

## Task 2: Mirror `dump-registry` to the Forge loader

**Files:**
- Modify: `forge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java`

**Interfaces:**
- Consumes: `RegistryDumpWriter` (from Task 1) — already on xplat classpath.
- Produces: nothing new (parity).

- [ ] **Step 1: Apply the same `dump-registry` branch + helper to the Forge `AmiClientCommands`**

The Forge `AmiClientCommands` follows the same shape as the NeoForge one (verify by reading it first). Add the identical `.then(Commands.literal("dump-registry").executes(...))` and the matching `exportRegistryDump` helper, substituting Forge equivalents:
- `FMLPaths.GAMEDIR` is identical
- Event class name may differ (`RegisterClientCommandsEvent` on Forge is in `net.minecraftforge.client.event.RegisterClientCommandsEvent`)

- [ ] **Step 2: Compile**

Run: `./gradlew :forge:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add forge/src/main/java/com/sanhiruzu/ami/client/AmiClientCommands.java
git commit -m "feat(ami): mirror dump-registry command to Forge loader"
```

---

## Task 3: Extend `ClassificationOverride` with `tooltipLines`

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java` (parser only)
- Create: `xplat/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideTooltipParseTest.java`

**Interfaces:**
- Consumes: existing `ClassificationOverrides.parseAndInstall(String)`.
- Produces: `ClassificationOverride.tooltipLines() -> List<String>` (never null, empty list when absent). Existing `ClassificationOverride` constructors keep their current signature — a new full constructor adds tooltip lines, and the compact constructors default it to `List.of()`.

- [ ] **Step 1: Write failing parser test**

Create `xplat/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideTooltipParseTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverrideTooltipParseTest {

    @AfterEach
    void cleanUp() {
        ClassificationOverrides.clear();
    }

    @Test
    void parses_tooltipLines_onPerItemEntry() {
        String json = """
        { "items": {
            "modid:thing": {
              "tooltipLines": ["Custom note", "Second line"]
            }
        }}""";
        ClassificationOverrides.parseAndInstall(json);
        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:thing"));
        assertTrue(ov.isPresent());
        assertEquals(List.of("Custom note", "Second line"), ov.get().tooltipLines());
    }

    @Test
    void absent_tooltipLines_defaultsToEmpty() {
        String json = """{ "items": { "modid:thing": { "category": "x" } } }""";
        ClassificationOverrides.parseAndInstall(json);
        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:thing"));
        assertTrue(ov.isPresent());
        assertEquals(List.of(), ov.get().tooltipLines());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideTooltipParseTest"`
Expected: FAIL — `tooltipLines()` method does not exist on record.

- [ ] **Step 3: Add `tooltipLines` to the record**

Edit `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java` to:

```java
package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.List;

/**
 * Per-item classification override loaded from data. {@code forceCategory}/{@code forceSubcategory}
 * may be null when the override only adjusts facets. {@code tooltipLines} is never null; absent ==
 * empty list.
 */
public record ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                     EnumSet<ItemFacet> removeFacets,
                                     EnumSet<SemanticVerb> addVerbs,
                                     EnumSet<SemanticVerb> removeVerbs,
                                     String forceCategory,
                                     String forceSubcategory,
                                     List<String> tooltipLines) {
    public ClassificationOverride {
        tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
    }

    public ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                  EnumSet<ItemFacet> removeFacets,
                                  EnumSet<SemanticVerb> addVerbs,
                                  EnumSet<SemanticVerb> removeVerbs,
                                  String forceCategory,
                                  String forceSubcategory) {
        this(addFacets, removeFacets, addVerbs, removeVerbs, forceCategory, forceSubcategory, List.of());
    }

    public ClassificationOverride(EnumSet<ItemFacet> addFacets,
                                  EnumSet<ItemFacet> removeFacets,
                                  String forceCategory,
                                  String forceSubcategory) {
        this(addFacets, removeFacets, EnumSet.noneOf(SemanticVerb.class), EnumSet.noneOf(SemanticVerb.class),
                forceCategory, forceSubcategory, List.of());
    }

    public boolean hasForcedCategory() {
        return forceCategory != null && !forceCategory.isBlank();
    }

    public String subcategoryOrEmpty() {
        return forceSubcategory == null ? "" : forceSubcategory;
    }
}
```

- [ ] **Step 4: Add the parser branch in `ClassificationOverrides.parseItems`**

In `ClassificationOverrides.java`, change the per-item `out.put(...)` block in `parseItems` to:

```java
            out.put(id.toLowerCase(Locale.ROOT), new ClassificationOverride(
                    parseFacets(entry, "addFacets"),
                    parseFacets(entry, "removeFacets"),
                    parseVerbs(entry, "addVerbs"),
                    parseVerbs(entry, "removeVerbs"),
                    category,
                    subcategory,
                    parseStringList(entry, "tooltipLines")));
```

And add the helper next to `parseFacets`:

```java
    private static java.util.List<String> parseStringList(JsonObject entry, String key) {
        if (!entry.has(key) || !entry.get(key).isJsonArray()) {
            return java.util.List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (JsonElement el : entry.getAsJsonArray(key)) {
            if (el.isJsonPrimitive()) {
                out.add(el.getAsString());
            }
        }
        return java.util.List.copyOf(out);
    }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideTooltipParseTest"`
Expected: PASS (2 tests). Also run the broader suite to confirm no regressions in callers of the old constructors:
Run: `./gradlew :xplat:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverride.java \
        xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java \
        xplat/src/test/java/com/sanhiruzu/ami/index/ClassificationOverrideTooltipParseTest.java
git commit -m "feat(index): add tooltipLines field to per-item classification overrides"
```

---

## Task 4: Add `ClassificationOverrides.mergeAndInstall` (layered loading)

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java`
- Create: `xplat/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesMergeTest.java`

**Interfaces:**
- Consumes: existing `parseAndInstall(String)`, `install(items, patterns)`.
- Produces: `ClassificationOverrides.mergeAndInstall(String json)` — parses the JSON and **adds** entries on top of the currently-installed maps. Per-item entries from the new JSON replace existing entries with the same id (last write wins). Per-mod patterns from the new JSON are *appended* to the existing rule list for that mod — patterns are matched in registered order, so appended rules act as a tier checked **first** if the loader sets `prependPatterns=true`. v1 uses `prependPatterns=true` so pack rules take precedence over bundled rules.

- [ ] **Step 1: Write failing merge test**

Create `xplat/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesMergeTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverridesMergeTest {

    @AfterEach
    void cleanUp() {
        ClassificationOverrides.clear();
    }

    @Test
    void packEntryOverridesBundledEntryForSameItem() {
        String bundled = """
        { "items": { "modid:thing": { "category": "old" } } }""";
        String pack = """
        { "items": { "modid:thing": { "category": "new" } } }""";

        ClassificationOverrides.parseAndInstall(bundled);
        ClassificationOverrides.mergeAndInstall(pack);

        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:thing"));
        assertTrue(ov.isPresent());
        assertEquals("new", ov.get().forceCategory());
    }

    @Test
    void packPatternMatchesBeforeBundledPattern() {
        String bundled = """
        { "modPatterns": [
            { "mod": "m", "pathTokens": ["x"], "category": "bundled_cat" }
        ]}""";
        String pack = """
        { "modPatterns": [
            { "mod": "m", "pathTokens": ["x"], "category": "pack_cat" }
        ]}""";
        ClassificationOverrides.parseAndInstall(bundled);
        ClassificationOverrides.mergeAndInstall(pack);

        Optional<ModPatternRule> rule = ClassificationOverrides.patternFor("m", "x");
        assertTrue(rule.isPresent());
        assertEquals("pack_cat", rule.get().category());
    }

    @Test
    void bundledItemSurvivesWhenPackOverridesDifferentItem() {
        ClassificationOverrides.parseAndInstall("""{ "items": { "modid:a": { "category": "A" } } }""");
        ClassificationOverrides.mergeAndInstall("""{ "items": { "modid:b": { "category": "B" } } }""");

        assertEquals("A", ClassificationOverrides.forItem(ResourceLocation.parse("modid:a")).orElseThrow().forceCategory());
        assertEquals("B", ClassificationOverrides.forItem(ResourceLocation.parse("modid:b")).orElseThrow().forceCategory());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesMergeTest"`
Expected: FAIL — `mergeAndInstall` does not exist.

- [ ] **Step 3: Implement `mergeAndInstall`**

In `ClassificationOverrides.java`, add (next to `parseAndInstall`):

```java
    public static void mergeAndInstall(String json) {
        Map<String, ClassificationOverride> items = new LinkedHashMap<>(itemOverrides);
        Map<String, List<ModPatternRule>> patterns = new LinkedHashMap<>();
        for (Map.Entry<String, List<ModPatternRule>> e : modPatternRules.entrySet()) {
            patterns.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                Map<String, ClassificationOverride> newItems = new LinkedHashMap<>();
                Map<String, List<ModPatternRule>> newPatterns = new LinkedHashMap<>();
                parseItems(root, newItems);
                parsePatterns(root, newPatterns);
                items.putAll(newItems);
                for (Map.Entry<String, List<ModPatternRule>> e : newPatterns.entrySet()) {
                    List<ModPatternRule> existing = patterns.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
                    existing.addAll(0, e.getValue());  // prepend: pack patterns win
                }
            }
        } catch (RuntimeException ignored) {
            // Malformed pack override JSON must never break indexing; bundled state is preserved.
        }
        install(items, patterns);
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.ClassificationOverridesMergeTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/ClassificationOverrides.java \
        xplat/src/test/java/com/sanhiruzu/ami/index/ClassificationOverridesMergeTest.java
git commit -m "feat(index): add mergeAndInstall for layering pack overrides over bundled defaults"
```

---

## Task 5: `PackOverrideLoader` reads `<gamedir>/config/ami/overrides.json`

**Files:**
- Create: `xplat/src/main/java/com/sanhiruzu/ami/index/PackOverrideLoader.java`
- Create: `xplat/src/test/java/com/sanhiruzu/ami/index/PackOverrideLoaderTest.java`

**Interfaces:**
- Consumes: `ClassificationOverrides.mergeAndInstall(String)` (Task 4).
- Produces: `PackOverrideLoader.loadFrom(Path configDir): LoadResult` — `LoadResult` is a record `(boolean fileFound, boolean parseOk, int bytesRead, String errorMessage)`. Reads `configDir.resolve("ami/overrides.json")`. Missing file is NOT an error (returns `fileFound=false, parseOk=true`). The mod entry point is a no-arg `PackOverrideLoader.load()` that resolves `FMLPaths.GAMEDIR.get().resolve("config")` — but the testable core is `loadFrom(Path)` so JUnit can supply a `@TempDir`.

- [ ] **Step 1: Write failing test**

Create `xplat/src/test/java/com/sanhiruzu/ami/index/PackOverrideLoaderTest.java`:

```java
package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PackOverrideLoaderTest {

    @AfterEach
    void cleanUp() { ClassificationOverrides.clear(); }

    @Test
    void missingFile_returnsFileFoundFalse_noError(@TempDir Path tmp) throws Exception {
        PackOverrideLoader.LoadResult r = PackOverrideLoader.loadFrom(tmp);
        assertFalse(r.fileFound());
        assertTrue(r.parseOk());
        assertNull(r.errorMessage());
    }

    @Test
    void validFile_installedOnTopOfBundled(@TempDir Path tmp) throws Exception {
        ClassificationOverrides.parseAndInstall(
                """{ "items": { "modid:keep": { "category": "kept" } } }""");
        Path amiDir = tmp.resolve("ami");
        Files.createDirectories(amiDir);
        Files.writeString(amiDir.resolve("overrides.json"),
                """{ "items": { "modid:new": { "category": "added" } } }""");

        PackOverrideLoader.LoadResult r = PackOverrideLoader.loadFrom(tmp);
        assertTrue(r.fileFound());
        assertTrue(r.parseOk());
        assertEquals("kept",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:keep")).orElseThrow().forceCategory());
        assertEquals("added",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:new")).orElseThrow().forceCategory());
    }

    @Test
    void malformedJson_doesNotThrow_preservesBundled(@TempDir Path tmp) throws Exception {
        ClassificationOverrides.parseAndInstall(
                """{ "items": { "modid:keep": { "category": "kept" } } }""");
        Path amiDir = tmp.resolve("ami");
        Files.createDirectories(amiDir);
        Files.writeString(amiDir.resolve("overrides.json"), "{ not valid json");

        PackOverrideLoader.LoadResult r = PackOverrideLoader.loadFrom(tmp);
        assertTrue(r.fileFound());
        // Bundled entry must still be present even though pack JSON was junk.
        assertEquals("kept",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:keep")).orElseThrow().forceCategory());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.PackOverrideLoaderTest"`
Expected: FAIL — `PackOverrideLoader` does not exist.

- [ ] **Step 3: Implement `PackOverrideLoader`**

Create `xplat/src/main/java/com/sanhiruzu/ami/index/PackOverrideLoader.java`:

```java
package com.sanhiruzu.ami.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads an optional modpack-author override file from {@code <gamedir>/config/ami/overrides.json}
 * and merges it on top of the bundled mod defaults. Never throws — the index must keep running
 * even if the pack file is missing or malformed.
 */
public final class PackOverrideLoader {

    public static final String RELATIVE_PATH = "ami/overrides.json";

    public record LoadResult(boolean fileFound, boolean parseOk, int bytesRead, String errorMessage) {}

    private PackOverrideLoader() {}

    public static LoadResult loadFrom(Path configDir) {
        Path file = configDir.resolve(RELATIVE_PATH);
        if (!Files.exists(file)) {
            return new LoadResult(false, true, 0, null);
        }
        String body;
        try {
            body = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new LoadResult(true, false, 0, e.getMessage());
        }
        try {
            ClassificationOverrides.mergeAndInstall(body);
            return new LoadResult(true, true, body.length(), null);
        } catch (RuntimeException e) {
            // mergeAndInstall already swallows parse errors, but guard the call site too.
            return new LoadResult(true, false, body.length(), e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :xplat:test --tests "com.sanhiruzu.ami.index.PackOverrideLoaderTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/PackOverrideLoader.java \
        xplat/src/test/java/com/sanhiruzu/ami/index/PackOverrideLoaderTest.java
git commit -m "feat(index): add PackOverrideLoader for config/ami/overrides.json"
```

---

## Task 6: Wire `PackOverrideLoader.load()` into `ItemProvider.populateItems`

**Files:**
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`
- Modify: `xplat/src/main/java/com/sanhiruzu/ami/index/PackOverrideLoader.java` (add no-arg `load()` entry point)

**Interfaces:**
- Consumes: `FMLPaths.GAMEDIR` — but xplat code can't depend on loader classes. The existing `IPlatformHelper` already abstracts this kind of thing; reuse it. If `IPlatformHelper` lacks a `getGameDir()` accessor, add one.
- Produces: `PackOverrideLoader.load(): LoadResult` — uses `IPlatformHelper.getGameDir().resolve("config")`.

- [ ] **Step 1: Check whether `IPlatformHelper` exposes a game-dir accessor**

Run: `grep -n "GameDir\|gameDir\|GAMEDIR\|getRootDir\|gameDirectory" xplat/src/main/java/com/sanhiruzu/ami/platform/IPlatformHelper.java`
If a game-dir method exists, use it. If not, add:

```java
    java.nio.file.Path getGameDir();
```

Implement in each loader's helper (`neoforge`/`forge`/`fabric` `*PlatformHelper` classes). For NeoForge:

```java
    @Override
    public java.nio.file.Path getGameDir() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
    }
```

Same shape for Forge. For Fabric:

```java
    @Override
    public java.nio.file.Path getGameDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
    }
```

- [ ] **Step 2: Add no-arg `PackOverrideLoader.load()`**

Append to `PackOverrideLoader.java`:

```java
    public static LoadResult load() {
        return loadFrom(com.sanhiruzu.ami.platform.Services.PLATFORM.getGameDir().resolve("config"));
    }
```

(`Services.PLATFORM` is the existing xplat accessor — verify the actual import path by grepping for an existing `Services.PLATFORM` use, e.g. in `IPlatformHelper` callers, and match it. If the project uses a different access pattern, adapt.)

- [ ] **Step 3: Wire it into `ItemProvider.populateItems`**

Grep for the existing `ClassificationOverrides.loadBundledDefaults()` call in `ItemProvider.java`:

Run: `grep -n "loadBundledDefaults" xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java`

Immediately after that call, insert:

```java
        PackOverrideLoader.LoadResult packResult = PackOverrideLoader.load();
        if (packResult.fileFound() && !packResult.parseOk()) {
            // Surface bad pack-override JSON in the log without breaking indexing.
            org.slf4j.LoggerFactory.getLogger("ami").warn(
                "Pack override file at config/ami/overrides.json failed to parse: {}", packResult.errorMessage());
        }
```

- [ ] **Step 4: Compile and run the full xplat test suite**

Run: `./gradlew :xplat:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java \
        xplat/src/main/java/com/sanhiruzu/ami/index/PackOverrideLoader.java \
        xplat/src/main/java/com/sanhiruzu/ami/platform/IPlatformHelper.java \
        neoforge/src/main/java/com/sanhiruzu/ami/neoforge/NeoForgePlatformHelper.java \
        forge/src/main/java/com/sanhiruzu/ami/forge/ForgePlatformHelper.java \
        fabric/src/main/java/com/sanhiruzu/ami/fabric/FabricPlatformHelper.java
git commit -m "feat(index): load pack overrides from config/ami/overrides.json during index populate"
```

(Adjust the staged paths to match the actual helper class file names in each loader — grep `class.*PlatformHelper` if unsure.)

---

## Task 7: Tooltip appender for `tooltipLines`

**Files:**
- Create: `xplat/src/main/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipAppender.java`
- Create: `neoforge/src/test/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipTest.java`
- Modify: NeoForge tooltip event hook (location discovered in Step 1)

**Interfaces:**
- Consumes: `ClassificationOverrides.forItem(ResourceLocation)` (already shipped); `BuiltInRegistries.ITEM.getKey(item)` to derive the id from a stack.
- Produces: `ClassificationOverrideTooltipAppender.appendTo(ItemStack stack, List<Component> tooltipLines): void` — looks up the override, appends each custom line as a non-italic gray `Component.literal`. Returns nothing.

- [ ] **Step 1: Discover the existing tooltip hook**

Run: `grep -rn "ItemTooltipEvent\|onItemTooltip" neoforge/src/main/java`

This will show you where AMI currently subscribes to tooltip events (likely in `AmiDevModeHandler` or a similar client-side class). The new appender will be invoked from that same hook, after AMI's existing dev-info lines.

- [ ] **Step 2: Write failing test for the appender**

Create `neoforge/src/test/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipTest.java`:

```java
package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.ClassificationOverride;
import com.sanhiruzu.ami.index.ClassificationOverrides;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverrideTooltipTest {

    @AfterEach
    void cleanUp() { ClassificationOverrides.clear(); }

    @Test
    void appendsLinesWhenOverridePresent() {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:stone")));
        ClassificationOverrides.install(
                Map.of("minecraft:stone",
                    new ClassificationOverride(
                        EnumSet.noneOf(com.sanhiruzu.ami.index.ItemFacet.class),
                        EnumSet.noneOf(com.sanhiruzu.ami.index.ItemFacet.class),
                        EnumSet.noneOf(com.sanhiruzu.ami.index.SemanticVerb.class),
                        EnumSet.noneOf(com.sanhiruzu.ami.index.SemanticVerb.class),
                        null, null, List.of("Hello", "World"))),
                Map.of());

        List<Component> tooltip = new ArrayList<>();
        ClassificationOverrideTooltipAppender.appendTo(stack, tooltip);

        assertEquals(2, tooltip.size());
        assertEquals("Hello", tooltip.get(0).getString());
        assertEquals("World", tooltip.get(1).getString());
    }

    @Test
    void noOpWhenOverrideAbsent() {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:stone")));
        List<Component> tooltip = new ArrayList<>();
        ClassificationOverrideTooltipAppender.appendTo(stack, tooltip);
        assertEquals(0, tooltip.size());
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.client.ClassificationOverrideTooltipTest"`
Expected: FAIL — `ClassificationOverrideTooltipAppender` does not exist.

- [ ] **Step 4: Implement the appender**

Create `xplat/src/main/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipAppender.java`:

```java
package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.ClassificationOverride;
import com.sanhiruzu.ami.index.ClassificationOverrides;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClassificationOverrideTooltipAppender {

    private ClassificationOverrideTooltipAppender() {}

    public static void appendTo(ItemStack stack, List<Component> tooltip) {
        if (stack == null || stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return;
        ClassificationOverride ov = ClassificationOverrides.forItem(id).orElse(null);
        if (ov == null) return;
        for (String line : ov.tooltipLines()) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :neoforge:test --tests "com.sanhiruzu.ami.client.ClassificationOverrideTooltipTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Wire the appender into the existing tooltip event hook**

In the file identified in Step 1, add a single line at the end of the tooltip event handler (after AMI's existing tooltip-policy gate so this also respects user settings):

```java
        ClassificationOverrideTooltipAppender.appendTo(event.getItemStack(), event.getToolTip());
```

Run: `./gradlew :neoforge:compileJava :forge:compileJava`
Expected: BUILD SUCCESSFUL. If Forge has its own tooltip event class with a different `getToolTip()` accessor, mirror appropriately.

- [ ] **Step 7: Commit**

```bash
git add xplat/src/main/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipAppender.java \
        neoforge/src/test/java/com/sanhiruzu/ami/client/ClassificationOverrideTooltipTest.java \
        neoforge/src/main/java/<tooltip-hook-file-from-step-1> \
        forge/src/main/java/<tooltip-hook-file-from-step-1-equivalent>
git commit -m "feat(client): inject classification-override tooltip lines into item tooltips"
```

---

## Task 8: Generate `docs/override-editor/lib/constants.js` from Java enums

**Files:**
- Create: `scripts/export-tool-constants.mjs`
- Create: `docs/override-editor/lib/constants.js` (generated output, committed)

**Interfaces:**
- Consumes: `xplat/src/main/java/com/sanhiruzu/ami/index/ItemFacet.java` (and other enum sources discovered in Step 1).
- Produces: a JS module exporting `KNOWN_FACETS: string[]`, `KNOWN_VERBS: string[]`, `SCHEMA_VERSION: number = 1`.

- [ ] **Step 1: Identify the canonical enum source files**

Run:
```
grep -rn "enum ItemFacet" xplat/src/main/java
grep -rn "enum SemanticVerb" xplat/src/main/java
```

Note the file paths for Step 2's parser.

- [ ] **Step 2: Write the generator script**

Create `scripts/export-tool-constants.mjs`:

```javascript
// Usage: node scripts/export-tool-constants.mjs
// Reads ItemFacet.java + SemanticVerb.java, emits docs/override-editor/lib/constants.js.

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, "..");

const FACET_SRC = resolve(repoRoot, "xplat/src/main/java/com/sanhiruzu/ami/index/ItemFacet.java");
const VERB_SRC  = resolve(repoRoot, "xplat/src/main/java/com/sanhiruzu/ami/index/SemanticVerb.java");
const OUT       = resolve(repoRoot, "docs/override-editor/lib/constants.js");

/** Parse Java enum constants by matching the `byId("...")` argument used in each enum entry. */
function parseEnumIds(javaSource) {
  const ids = new Set();
  // Matches e.g.  FOO("foo_id"),    or    FOO("foo_id", ... ),
  const re = /[A-Z_][A-Z0-9_]*\s*\(\s*"([a-z0-9_]+)"/g;
  for (const m of javaSource.matchAll(re)) ids.add(m[1]);
  return [...ids].sort();
}

const facets = parseEnumIds(readFileSync(FACET_SRC, "utf8"));
const verbs  = parseEnumIds(readFileSync(VERB_SRC,  "utf8"));

const body = `// AUTO-GENERATED by scripts/export-tool-constants.mjs — do not hand-edit.
export const SCHEMA_VERSION = 1;
export const KNOWN_FACETS = ${JSON.stringify(facets, null, 2)};
export const KNOWN_VERBS  = ${JSON.stringify(verbs,  null, 2)};
`;

writeFileSync(OUT, body);
console.log(`Wrote ${OUT} (${facets.length} facets, ${verbs.length} verbs)`);
```

- [ ] **Step 3: Run the generator and inspect the output**

Run: `node scripts/export-tool-constants.mjs`
Expected: console line like `Wrote .../docs/override-editor/lib/constants.js (N facets, M verbs)` with non-zero counts.

Open `docs/override-editor/lib/constants.js` and confirm the arrays look right (matches at least a sample of the values used in `xplat/src/main/resources/assets/ami/classification_overrides.json` — e.g. `ingredient_organic`, `melee_weapon`, `magic_artifact`).

- [ ] **Step 4: Commit**

```bash
git add scripts/export-tool-constants.mjs docs/override-editor/lib/constants.js
git commit -m "feat(tools): export ItemFacet/SemanticVerb ids to docs/override-editor/lib/constants.js"
```

---

## Task 9: Web tool — pure-logic modules with Node tests

**Files:**
- Create: `docs/override-editor/lib/load.js`
- Create: `docs/override-editor/lib/merge.js`
- Create: `docs/override-editor/lib/diff.js`
- Create: `docs/override-editor/lib/validate.js`
- Create: `docs/override-editor/tests/load.test.mjs`
- Create: `docs/override-editor/tests/merge.test.mjs`
- Create: `docs/override-editor/tests/diff.test.mjs`
- Create: `docs/override-editor/tests/validate.test.mjs`
- Create: `docs/override-editor/tests/fixtures/registry-dump-small.json`
- Create: `docs/override-editor/tests/fixtures/overrides-baseline.json`

**Interfaces:**
- Consumes: file text (strings) from the browser file-picker; `KNOWN_FACETS`/`SCHEMA_VERSION` from `constants.js`.
- Produces:
  - `parseRegistryDump(text): { schemaVersion, items: Array<RegistryItem> }`  — throws on schema mismatch.
  - `parseOverrides(text): { schemaVersion: number, items: Map<string, ItemOverride>, modPatterns: Array<PatternOverride> }` — `schemaVersion` defaults to 1 when absent; throws on >1.
  - `mergeForEditing(dump, overrides): Array<EditableItem>` — joins; each `EditableItem` carries `{ id, mod, className, displayName, creativeTabs, runtimeFacets: string[], baseline: {category, subcategory, facets, tooltipLines}, edited: {...same fields...}, dirty: boolean, missingFromDump: boolean }`. `runtimeFacets` is the dump's `currentFacets` verbatim (what the mod would produce with NO override). `baseline.facets` is `runtimeFacets ∪ override.addFacets \ override.removeFacets` — the merged final set the user sees in the grid. `edited` starts equal to `baseline` and is mutated by the UI.
  - `computeSparsePatch(editableItems, originalOverrides): { schemaVersion, items: object, modPatterns: array }` — emits only items whose `edited` differs from `baseline`. Facet diff is computed against `runtimeFacets` (NOT against `baseline.facets`) so that `addFacets`/`removeFacets` always faithfully reconstructs the override layer from the runtime baseline — this matters on round-trips, where re-saving a previously-overridden item must preserve facets the override added. Includes ALL `modPatterns` from the loaded overrides verbatim (v1 does not edit patterns — only per-item entries).
  - `validate(overridesJsonObject): Array<{ severity: "error"|"warn", message: string, itemId?: string }>` — schema version check, unknown-facet warnings, stale-id warnings (relative to a dump if provided).

- [ ] **Step 1: Write fixture files**

Create `docs/override-editor/tests/fixtures/registry-dump-small.json`:

```json
{
  "schemaVersion": 1,
  "items": [
    {
      "id": "minecraft:diamond_sword",
      "mod": "minecraft",
      "className": "net.minecraft.world.item.SwordItem",
      "displayName": "Diamond Sword",
      "creativeTabs": ["Combat"],
      "currentCategory": "weapons",
      "currentSubcategory": "melee",
      "currentFacets": ["melee_weapon"]
    },
    {
      "id": "modid:gizmo",
      "mod": "modid",
      "className": "modid.GizmoItem",
      "displayName": "Gizmo",
      "creativeTabs": ["Tools"],
      "currentCategory": "tools",
      "currentSubcategory": "misc",
      "currentFacets": []
    }
  ]
}
```

Create `docs/override-editor/tests/fixtures/overrides-baseline.json`:

```json
{
  "schemaVersion": 1,
  "items": {
    "modid:gizmo": { "category": "tools", "subcategory": "fancy", "tooltipLines": ["Crafted with love"] }
  },
  "modPatterns": []
}
```

- [ ] **Step 2: Write failing tests for `parseRegistryDump` and `parseOverrides`**

Create `docs/override-editor/tests/load.test.mjs`:

```javascript
import { test } from "node:test";
import { strict as assert } from "node:assert";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseRegistryDump, parseOverrides } from "../lib/load.js";

const here = dirname(fileURLToPath(import.meta.url));
const dumpText = readFileSync(resolve(here, "fixtures/registry-dump-small.json"), "utf8");
const ovText   = readFileSync(resolve(here, "fixtures/overrides-baseline.json"), "utf8");

test("parseRegistryDump parses fixture", () => {
  const r = parseRegistryDump(dumpText);
  assert.equal(r.schemaVersion, 1);
  assert.equal(r.items.length, 2);
  assert.equal(r.items[0].id, "minecraft:diamond_sword");
});

test("parseRegistryDump rejects future schema version", () => {
  const bad = JSON.stringify({ schemaVersion: 2, items: [] });
  assert.throws(() => parseRegistryDump(bad), /schema/i);
});

test("parseOverrides parses fixture", () => {
  const r = parseOverrides(ovText);
  assert.equal(r.schemaVersion, 1);
  assert.equal(r.items.size, 1);
  assert.equal(r.items.get("modid:gizmo").subcategory, "fancy");
});

test("parseOverrides treats missing schemaVersion as 1", () => {
  const r = parseOverrides(JSON.stringify({ items: {} }));
  assert.equal(r.schemaVersion, 1);
});
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `node --test docs/override-editor/tests/load.test.mjs`
Expected: FAIL — `lib/load.js` does not exist.

- [ ] **Step 4: Implement `lib/load.js`**

Create `docs/override-editor/lib/load.js`:

```javascript
import { SCHEMA_VERSION } from "./constants.js";

export function parseRegistryDump(text) {
  const doc = JSON.parse(text);
  const version = doc.schemaVersion ?? 1;
  if (version > SCHEMA_VERSION) {
    throw new Error(`registry-dump schemaVersion ${version} is newer than tool (${SCHEMA_VERSION})`);
  }
  return { schemaVersion: version, items: Array.isArray(doc.items) ? doc.items : [] };
}

export function parseOverrides(text) {
  const doc = JSON.parse(text);
  const version = doc.schemaVersion ?? 1;
  if (version > SCHEMA_VERSION) {
    throw new Error(`overrides schemaVersion ${version} is newer than tool (${SCHEMA_VERSION})`);
  }
  const items = new Map();
  if (doc.items && typeof doc.items === "object") {
    for (const [id, entry] of Object.entries(doc.items)) {
      items.set(id, {
        category: entry.category ?? null,
        subcategory: entry.subcategory ?? null,
        addFacets: entry.addFacets ?? [],
        removeFacets: entry.removeFacets ?? [],
        tooltipLines: entry.tooltipLines ?? [],
      });
    }
  }
  const modPatterns = Array.isArray(doc.modPatterns) ? doc.modPatterns : [];
  return { schemaVersion: version, items, modPatterns };
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `node --test docs/override-editor/tests/load.test.mjs`
Expected: PASS (4 tests).

- [ ] **Step 6: Write failing tests for `mergeForEditing`**

Create `docs/override-editor/tests/merge.test.mjs`:

```javascript
import { test } from "node:test";
import { strict as assert } from "node:assert";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseRegistryDump, parseOverrides } from "../lib/load.js";
import { mergeForEditing } from "../lib/merge.js";

const here = dirname(fileURLToPath(import.meta.url));
const dump = parseRegistryDump(readFileSync(resolve(here, "fixtures/registry-dump-small.json"), "utf8"));
const ov   = parseOverrides(readFileSync(resolve(here, "fixtures/overrides-baseline.json"), "utf8"));

test("merges dump row with override for same id, baseline reflects override", () => {
  const merged = mergeForEditing(dump, ov);
  const gizmo = merged.find(i => i.id === "modid:gizmo");
  assert.equal(gizmo.baseline.subcategory, "fancy");
  assert.deepEqual(gizmo.baseline.tooltipLines, ["Crafted with love"]);
  assert.equal(gizmo.dirty, false);
  assert.equal(gizmo.missingFromDump, false);
});

test("runtimeFacets reflects the dump verbatim, independent of overrides", () => {
  const dumpWithFacets = {
    schemaVersion: 1,
    items: [{
      id: "m:x", mod: "m", className: "X", displayName: "X",
      creativeTabs: [], currentCategory: null, currentSubcategory: null,
      currentFacets: ["runtime_one"],
    }],
  };
  const ovWithFacetAdd = {
    schemaVersion: 1,
    items: new Map([["m:x", {
      category: null, subcategory: null,
      addFacets: ["override_added"], removeFacets: [], tooltipLines: [],
    }]]),
    modPatterns: [],
  };
  const merged = mergeForEditing(dumpWithFacets, ovWithFacetAdd);
  const x = merged[0];
  assert.deepEqual(x.runtimeFacets, ["runtime_one"]);
  assert.deepEqual(x.baseline.facets.sort(), ["override_added", "runtime_one"]);
});

test("dump-only items appear with no override applied", () => {
  const merged = mergeForEditing(dump, ov);
  const sword = merged.find(i => i.id === "minecraft:diamond_sword");
  assert.equal(sword.baseline.category, "weapons");
  assert.deepEqual(sword.baseline.tooltipLines, []);
});

test("override-only items get missingFromDump=true", () => {
  const ovOnly = { ...ov, items: new Map([
    ...ov.items,
    ["modid:ghost", { category: "x", subcategory: null, addFacets: [], removeFacets: [], tooltipLines: [] }]
  ])};
  const merged = mergeForEditing(dump, ovOnly);
  const ghost = merged.find(i => i.id === "modid:ghost");
  assert.equal(ghost.missingFromDump, true);
});
```

- [ ] **Step 7: Run, see fail, implement `lib/merge.js`, run, see pass**

Run: `node --test docs/override-editor/tests/merge.test.mjs`
Expected: FAIL.

Create `docs/override-editor/lib/merge.js`:

```javascript
export function mergeForEditing(dump, overrides) {
  const merged = [];
  const seen = new Set();

  for (const row of dump.items) {
    seen.add(row.id);
    const ov = overrides.items.get(row.id);
    const runtimeFacets = [...(row.currentFacets ?? [])];
    const baseline = {
      category:     ov?.category    ?? row.currentCategory    ?? null,
      subcategory:  ov?.subcategory ?? row.currentSubcategory ?? null,
      facets:       Array.from(new Set([...runtimeFacets, ...(ov?.addFacets ?? [])]))
                       .filter(f => !(ov?.removeFacets ?? []).includes(f)),
      tooltipLines: ov?.tooltipLines ?? [],
    };
    merged.push({
      id: row.id, mod: row.mod, className: row.className,
      displayName: row.displayName, creativeTabs: row.creativeTabs ?? [],
      runtimeFacets,
      baseline, edited: structuredClone(baseline),
      dirty: false, missingFromDump: false,
    });
  }

  for (const [id, ov] of overrides.items) {
    if (seen.has(id)) continue;
    const baseline = {
      category: ov.category ?? null, subcategory: ov.subcategory ?? null,
      facets: [...(ov.addFacets ?? [])], tooltipLines: ov.tooltipLines ?? [],
    };
    merged.push({
      id, mod: id.split(":")[0], className: "", displayName: id, creativeTabs: [],
      runtimeFacets: [],
      baseline, edited: structuredClone(baseline), dirty: false, missingFromDump: true,
    });
  }

  return merged;
}
```

Run: `node --test docs/override-editor/tests/merge.test.mjs`
Expected: PASS (3 tests).

- [ ] **Step 8: Write failing tests for `computeSparsePatch`**

Create `docs/override-editor/tests/diff.test.mjs`:

```javascript
import { test } from "node:test";
import { strict as assert } from "node:assert";
import { computeSparsePatch } from "../lib/diff.js";

function editable(id, runtimeFacets, baseline, edited) {
  return { id, mod: id.split(":")[0], className: "", displayName: id,
           creativeTabs: [], runtimeFacets, baseline, edited, dirty: false, missingFromDump: false };
}

test("emits only changed fields per item", () => {
  const items = [
    editable("a:x", [],
      { category: "c", subcategory: "s", facets: [], tooltipLines: [] },
      { category: "c", subcategory: "S2", facets: [], tooltipLines: [] }),
    editable("a:y", [],
      { category: "c", subcategory: "s", facets: [], tooltipLines: [] },
      { category: "c", subcategory: "s", facets: [], tooltipLines: [] }),  // unchanged
  ];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.equal(patch.schemaVersion, 1);
  assert.deepEqual(Object.keys(patch.items), ["a:x"]);
  assert.equal(patch.items["a:x"].subcategory, "S2");
  assert.equal(patch.items["a:x"].category, undefined);
});

test("emits tooltipLines when edited", () => {
  const items = [editable("a:x", [],
    { category: "c", subcategory: "s", facets: [], tooltipLines: [] },
    { category: "c", subcategory: "s", facets: [], tooltipLines: ["hi"] })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.deepEqual(patch.items["a:x"].tooltipLines, ["hi"]);
});

test("computes addFacets / removeFacets against runtime baseline, not edited baseline", () => {
  // Runtime emits ["runtime_only"]. Override previously added "kept_add". User edits to drop
  // "runtime_only" and keep "kept_add" + introduces "new_add".
  const items = [editable("a:x",
    ["runtime_only"],
    { category: null, subcategory: null, facets: ["kept_add"],            tooltipLines: [] },
    { category: null, subcategory: null, facets: ["kept_add", "new_add"], tooltipLines: [] })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  // Diff is computed against runtimeFacets, so the saved override layer reconstructs
  // the user's intent from the bare runtime: add both "kept_add" and "new_add", remove "runtime_only".
  assert.deepEqual(patch.items["a:x"].addFacets.sort(), ["kept_add", "new_add"]);
  assert.deepEqual(patch.items["a:x"].removeFacets, ["runtime_only"]);
});

test("no-op edits against runtime emit no facet diff", () => {
  const items = [editable("a:x",
    ["r"],
    { category: null, subcategory: null, facets: ["r"], tooltipLines: [] },
    { category: null, subcategory: null, facets: ["r"], tooltipLines: [] })];
  const patch = computeSparsePatch(items, { modPatterns: [] });
  assert.equal(patch.items["a:x"], undefined);
});

test("passes modPatterns through unchanged", () => {
  const patch = computeSparsePatch([], { modPatterns: [{ mod: "m", pathTokens: ["x"] }] });
  assert.equal(patch.modPatterns.length, 1);
  assert.equal(patch.modPatterns[0].mod, "m");
});
```

- [ ] **Step 9: Run, see fail, implement `lib/diff.js`, run, see pass**

Run: `node --test docs/override-editor/tests/diff.test.mjs`
Expected: FAIL.

Create `docs/override-editor/lib/diff.js`:

```javascript
import { SCHEMA_VERSION } from "./constants.js";

export function computeSparsePatch(editableItems, originalOverrides) {
  const items = {};
  for (const it of editableItems) {
    const delta = {};
    const b = it.baseline, e = it.edited;
    if ((b.category ?? null) !== (e.category ?? null) && e.category != null) delta.category = e.category;
    if ((b.subcategory ?? null) !== (e.subcategory ?? null) && e.subcategory != null) delta.subcategory = e.subcategory;

    // Facet diff is computed against runtimeFacets, NOT against baseline.facets, so the saved
    // override layer reconstructs the desired final set from the bare runtime. This is what
    // makes round-tripping (load → re-save) preserve previously-added override facets.
    const runtime = new Set(it.runtimeFacets ?? []);
    const edFacets = new Set(e.facets ?? []);
    const added   = [...edFacets].filter(f => !runtime.has(f));
    const removed = [...runtime].filter(f => !edFacets.has(f));
    if (added.length)   delta.addFacets    = added;
    if (removed.length) delta.removeFacets = removed;

    const tb = b.tooltipLines ?? [], te = e.tooltipLines ?? [];
    if (JSON.stringify(tb) !== JSON.stringify(te)) delta.tooltipLines = te;

    if (Object.keys(delta).length > 0) items[it.id] = delta;
  }
  return {
    schemaVersion: SCHEMA_VERSION,
    items,
    modPatterns: originalOverrides.modPatterns ?? [],
  };
}
```

Run: `node --test docs/override-editor/tests/diff.test.mjs`
Expected: PASS (4 tests).

- [ ] **Step 10: Write failing tests for `validate`**

Create `docs/override-editor/tests/validate.test.mjs`:

```javascript
import { test } from "node:test";
import { strict as assert } from "node:assert";
import { validate } from "../lib/validate.js";
import { KNOWN_FACETS } from "../lib/constants.js";

test("warns on unknown facet ids", () => {
  const sample = { schemaVersion: 1, items: { "m:x": { addFacets: ["totally_made_up"] } }, modPatterns: [] };
  const issues = validate(sample, /* dump */ null);
  assert.ok(issues.some(i => i.severity === "warn" && i.message.includes("totally_made_up")));
});

test("warns on item ids missing from dump", () => {
  const sample = { schemaVersion: 1, items: { "m:ghost": { category: "x" } }, modPatterns: [] };
  const dump = { items: [{ id: "m:alive" }] };
  const issues = validate(sample, dump);
  assert.ok(issues.some(i => i.severity === "warn" && i.itemId === "m:ghost"));
});

test("errors on future schemaVersion", () => {
  const sample = { schemaVersion: 999, items: {}, modPatterns: [] };
  assert.ok(validate(sample, null).some(i => i.severity === "error"));
});

test("accepts a clean fixture without warnings", () => {
  const goodFacet = KNOWN_FACETS[0];
  const sample = { schemaVersion: 1, items: { "m:x": { addFacets: [goodFacet] } }, modPatterns: [] };
  const dump = { items: [{ id: "m:x" }] };
  const issues = validate(sample, dump);
  assert.equal(issues.length, 0);
});
```

- [ ] **Step 11: Implement and verify `lib/validate.js`**

Create `docs/override-editor/lib/validate.js`:

```javascript
import { KNOWN_FACETS, SCHEMA_VERSION } from "./constants.js";

export function validate(overrides, dump) {
  const issues = [];
  if ((overrides.schemaVersion ?? 1) > SCHEMA_VERSION) {
    issues.push({ severity: "error",
      message: `overrides schemaVersion ${overrides.schemaVersion} > supported ${SCHEMA_VERSION}` });
  }
  const dumpIds = dump ? new Set((dump.items ?? []).map(r => r.id)) : null;
  const known = new Set(KNOWN_FACETS);
  for (const [id, entry] of Object.entries(overrides.items ?? {})) {
    if (dumpIds && !dumpIds.has(id)) {
      issues.push({ severity: "warn", itemId: id, message: `item ${id} not present in loaded registry dump` });
    }
    for (const f of [...(entry.addFacets ?? []), ...(entry.removeFacets ?? [])]) {
      if (!known.has(f)) {
        issues.push({ severity: "warn", itemId: id, message: `unknown facet "${f}"` });
      }
    }
  }
  return issues;
}
```

Run: `node --test docs/override-editor/tests/`
Expected: PASS — all four test files green.

- [ ] **Step 12: Commit**

```bash
git add docs/override-editor/lib/load.js \
        docs/override-editor/lib/merge.js \
        docs/override-editor/lib/diff.js \
        docs/override-editor/lib/validate.js \
        docs/override-editor/tests/
git commit -m "feat(tools): pure-logic modules + node tests for override editor"
```

---

## Task 10: Web tool — UI shell + grid + edit panel

**Files:**
- Create: `docs/override-editor/index.html`
- Create: `docs/override-editor/styles.css`
- Create: `docs/override-editor/app.js`
- Create: `docs/override-editor/lib/grid.js`
- Create: `docs/override-editor/lib/edit.js`
- Create: `docs/.nojekyll` (empty file)

**Interfaces:**
- Consumes: all of Task 9's modules.
- Produces: UI behavior only — no module signatures consumed by later tasks.

UI scope is purely manual-test (no headless browser); the deliverable is verified by opening the page and exercising it. Pure-logic helpers exposed for testing are folded into `lib/edit.js` (e.g. `applyBulkEdit(items, ids, edits)`), but UI binding code is not unit-tested.

- [ ] **Step 1: Write `index.html` shell**

Create `docs/override-editor/index.html`:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>AMI Override Editor</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <header>
    <h1>AMI Override Editor</h1>
    <div id="status">No files loaded.</div>
  </header>

  <section id="load">
    <label>Registry dump (registry-dump.json):
      <input type="file" id="dump-input" accept="application/json">
    </label>
    <label>Existing overrides (optional, overrides.json):
      <input type="file" id="overrides-input" accept="application/json">
    </label>
  </section>

  <section id="filters">
    <input id="filter-text" type="search" placeholder="Filter by id / display name / mod">
    <select id="filter-mod"><option value="">(all mods)</option></select>
    <select id="filter-category"><option value="">(all categories)</option></select>
    <label><input type="checkbox" id="filter-dirty-only"> Show edited only</label>
  </section>

  <section id="bulk">
    <strong>Bulk edit selected:</strong>
    <input id="bulk-category" placeholder="Set category">
    <input id="bulk-subcategory" placeholder="Set subcategory">
    <input id="bulk-add-facet" placeholder="Add facet">
    <input id="bulk-remove-facet" placeholder="Remove facet">
    <button id="bulk-apply">Apply to selected</button>
  </section>

  <main id="grid-host"></main>

  <aside id="single-edit" hidden>
    <h2>Edit item</h2>
    <div id="single-edit-body"></div>
  </aside>

  <footer>
    <button id="download" disabled>Download overrides.json</button>
    <div id="issues"></div>
  </footer>

  <script type="module" src="app.js"></script>
</body>
</html>
```

Create `docs/.nojekyll` (empty file). This is critical — without it, GitHub Pages will hide files starting with `_` and may otherwise mangle module imports.

Run: `touch docs/.nojekyll` (on Windows PowerShell: `New-Item -ItemType File docs/.nojekyll`)

- [ ] **Step 2: Write `styles.css` (minimal)**

Create `docs/override-editor/styles.css`:

```css
:root { --bg:#1e1e1e; --fg:#ddd; --accent:#7ec; --dirty:#fc7; --error:#f88; }
* { box-sizing: border-box; }
body { margin:0; padding:0.75rem; background:var(--bg); color:var(--fg);
       font: 13px/1.4 system-ui, sans-serif; }
header { display:flex; justify-content:space-between; align-items:baseline; }
h1 { margin:0; font-size:1.1rem; }
section, footer { margin:0.5rem 0; padding:0.5rem; border:1px solid #333; }
input, select, button { background:#2a2a2a; color:var(--fg); border:1px solid #444; padding:0.25rem; }
#grid-host { height:60vh; overflow:auto; border:1px solid #333; }
table { width:100%; border-collapse:collapse; }
th, td { padding:0.2rem 0.4rem; border-bottom:1px solid #2a2a2a; text-align:left; white-space:nowrap; }
tr.dirty { background: #3a2a10; }
tr.missing-dump { color: var(--error); }
.issue-warn { color: var(--dirty); }
.issue-error { color: var(--error); }
```

- [ ] **Step 3: Write `lib/grid.js` (virtualized list — start non-virtualized, note for future)**

Create `docs/override-editor/lib/grid.js`:

```javascript
// Initial implementation renders all rows; if performance is poor with 20k items,
// swap renderAll for windowed rendering (track scrollTop, only render visible slice).
export function renderGrid(host, items, { onSelect }) {
  const tbody = document.createElement("tbody");
  for (const it of items) {
    const tr = document.createElement("tr");
    tr.dataset.id = it.id;
    if (it.dirty) tr.classList.add("dirty");
    if (it.missingFromDump) tr.classList.add("missing-dump");
    tr.innerHTML = `
      <td><input type="checkbox" class="select" ${it._selected ? "checked" : ""}></td>
      <td>${escapeHtml(it.id)}</td>
      <td>${escapeHtml(it.displayName)}</td>
      <td>${escapeHtml(it.mod)}</td>
      <td>${escapeHtml(it.edited.category ?? "")}</td>
      <td>${escapeHtml(it.edited.subcategory ?? "")}</td>
      <td>${escapeHtml((it.edited.facets ?? []).join(", "))}</td>
      <td>${(it.edited.tooltipLines ?? []).length}</td>`;
    tr.querySelector(".select").addEventListener("change", e => {
      it._selected = e.target.checked;
    });
    tr.addEventListener("click", e => {
      if (e.target.tagName !== "INPUT") onSelect(it);
    });
    tbody.appendChild(tr);
  }
  host.innerHTML = `<table>
    <thead><tr><th></th><th>id</th><th>name</th><th>mod</th>
      <th>category</th><th>subcategory</th><th>facets</th><th>tooltipLines</th></tr></thead></table>`;
  host.querySelector("table").appendChild(tbody);
}

function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"]/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}
```

- [ ] **Step 4: Write `lib/edit.js`**

Create `docs/override-editor/lib/edit.js`:

```javascript
export function applyBulkEdit(items, ids, edits) {
  const idSet = new Set(ids);
  for (const it of items) {
    if (!idSet.has(it.id)) continue;
    if (edits.category != null && edits.category !== "") it.edited.category = edits.category;
    if (edits.subcategory != null && edits.subcategory !== "") it.edited.subcategory = edits.subcategory;
    if (edits.addFacet) {
      const set = new Set(it.edited.facets ?? []);
      set.add(edits.addFacet);
      it.edited.facets = [...set];
    }
    if (edits.removeFacet) {
      it.edited.facets = (it.edited.facets ?? []).filter(f => f !== edits.removeFacet);
    }
    it.dirty = !isEqual(it.baseline, it.edited);
  }
}

export function setTooltipLines(item, linesText) {
  item.edited.tooltipLines = linesText.split("\n").map(l => l.trim()).filter(l => l.length > 0);
  item.dirty = !isEqual(item.baseline, item.edited);
}

function isEqual(a, b) {
  return JSON.stringify(a) === JSON.stringify(b);
}
```

- [ ] **Step 5: Write `app.js` — wires everything together**

Create `docs/override-editor/app.js`:

```javascript
import { parseRegistryDump, parseOverrides } from "./lib/load.js";
import { mergeForEditing } from "./lib/merge.js";
import { computeSparsePatch } from "./lib/diff.js";
import { validate } from "./lib/validate.js";
import { renderGrid } from "./lib/grid.js";
import { applyBulkEdit, setTooltipLines } from "./lib/edit.js";

const state = {
  dump: null,
  overrides: { schemaVersion: 1, items: new Map(), modPatterns: [] },
  items: [],
  filter: { text: "", mod: "", category: "", dirtyOnly: false },
};

const el = id => document.getElementById(id);
const setStatus = m => el("status").textContent = m;

async function readFile(input) {
  const f = input.files[0];
  if (!f) return null;
  return await f.text();
}

el("dump-input").addEventListener("change", async e => {
  const text = await readFile(e.target);
  if (!text) return;
  try {
    state.dump = parseRegistryDump(text);
    setStatus(`Dump: ${state.dump.items.length} items loaded.`);
    rebuild();
  } catch (err) {
    setStatus(`Error loading dump: ${err.message}`);
  }
});

el("overrides-input").addEventListener("change", async e => {
  const text = await readFile(e.target);
  if (!text) return;
  try {
    state.overrides = parseOverrides(text);
    setStatus(`Overrides: ${state.overrides.items.size} items + ${state.overrides.modPatterns.length} patterns.`);
    rebuild();
  } catch (err) {
    setStatus(`Error loading overrides: ${err.message}`);
  }
});

function rebuild() {
  if (!state.dump) return;
  state.items = mergeForEditing(state.dump, state.overrides);
  populateFilterOptions();
  refreshGrid();
  refreshIssues();
  el("download").disabled = false;
}

function populateFilterOptions() {
  const mods = new Set(state.items.map(i => i.mod));
  const cats = new Set(state.items.map(i => i.edited.category).filter(Boolean));
  el("filter-mod").innerHTML = `<option value="">(all mods)</option>` +
    [...mods].sort().map(m => `<option>${m}</option>`).join("");
  el("filter-category").innerHTML = `<option value="">(all categories)</option>` +
    [...cats].sort().map(c => `<option>${c}</option>`).join("");
}

function applyFilters() {
  const { text, mod, category, dirtyOnly } = state.filter;
  const lower = text.toLowerCase();
  return state.items.filter(i =>
    (!mod || i.mod === mod) &&
    (!category || i.edited.category === category) &&
    (!dirtyOnly || i.dirty) &&
    (!text ||
      i.id.toLowerCase().includes(lower) ||
      i.displayName.toLowerCase().includes(lower) ||
      i.mod.toLowerCase().includes(lower))
  );
}

function refreshGrid() {
  renderGrid(el("grid-host"), applyFilters(), { onSelect: openSingleEdit });
}

function refreshIssues() {
  const patch = computeSparsePatch(state.items, state.overrides);
  const issues = validate({ ...patch, items: patch.items }, state.dump);
  el("issues").innerHTML = issues.map(i =>
    `<div class="issue-${i.severity}">[${i.severity}] ${escapeHtml(i.itemId ?? "")} ${escapeHtml(i.message)}</div>`
  ).join("");
}

function openSingleEdit(item) {
  const host = el("single-edit");
  host.hidden = false;
  el("single-edit-body").innerHTML = `
    <div><strong>${escapeHtml(item.id)}</strong> (${escapeHtml(item.displayName)})</div>
    <label>Category <input id="se-cat" value="${escapeHtml(item.edited.category ?? "")}"></label>
    <label>Subcategory <input id="se-sub" value="${escapeHtml(item.edited.subcategory ?? "")}"></label>
    <label>Tooltip lines (one per line):<br>
      <textarea id="se-tooltip" rows="4" cols="60">${escapeHtml((item.edited.tooltipLines ?? []).join("\n"))}</textarea></label>
    <button id="se-apply">Apply</button>`;
  el("se-apply").addEventListener("click", () => {
    item.edited.category = el("se-cat").value || null;
    item.edited.subcategory = el("se-sub").value || null;
    setTooltipLines(item, el("se-tooltip").value);
    item.dirty = JSON.stringify(item.baseline) !== JSON.stringify(item.edited);
    refreshGrid();
    refreshIssues();
  });
}

el("filter-text").addEventListener("input", e => { state.filter.text = e.target.value; refreshGrid(); });
el("filter-mod").addEventListener("change", e => { state.filter.mod = e.target.value; refreshGrid(); });
el("filter-category").addEventListener("change", e => { state.filter.category = e.target.value; refreshGrid(); });
el("filter-dirty-only").addEventListener("change", e => { state.filter.dirtyOnly = e.target.checked; refreshGrid(); });

el("bulk-apply").addEventListener("click", () => {
  const selected = state.items.filter(i => i._selected).map(i => i.id);
  applyBulkEdit(state.items, selected, {
    category: el("bulk-category").value,
    subcategory: el("bulk-subcategory").value,
    addFacet: el("bulk-add-facet").value,
    removeFacet: el("bulk-remove-facet").value,
  });
  refreshGrid();
  refreshIssues();
});

el("download").addEventListener("click", () => {
  const patch = computeSparsePatch(state.items, state.overrides);
  const blob = new Blob([JSON.stringify(patch, null, 2)], { type: "application/json" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "overrides.json";
  a.click();
  URL.revokeObjectURL(a.href);
});

function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"]/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}
```

- [ ] **Step 6: Manual verification**

Open `docs/override-editor/index.html` in a browser (double-click or `file:///...`). Verify each of:

1. Loading `docs/override-editor/tests/fixtures/registry-dump-small.json` populates the grid (2 rows).
2. Loading `docs/override-editor/tests/fixtures/overrides-baseline.json` updates the gizmo row's subcategory to `fancy`.
3. Filtering by mod / text narrows the grid.
4. Clicking a row opens the single-edit panel; setting a category + tooltip lines + Apply marks the row dirty (`bg becomes orange-ish`).
5. Selecting checkboxes + bulk-apply applies edits to all selected rows.
6. Clicking Download produces a JSON file with only changed items.

If anything fails, fix it before committing. Note manual-only verification in the commit message.

- [ ] **Step 7: Commit**

```bash
git add docs/override-editor/ docs/.nojekyll
git commit -m "feat(tools): web UI for override editor (manual-verified, no headless test)"
```

---

## Task 11: GitHub Pages config + README

**Files:**
- Modify: `README.md` (add Override Editor section pointing at the Pages URL once known)
- Create: `docs/override-editor/README.md` (user-facing usage notes)

**Interfaces:**
- Produces: documentation only.

- [ ] **Step 1: Write `docs/override-editor/README.md`**

```markdown
# AMI Override Editor

Browser-based editor for the `overrides.json` file that AMI loads from a modpack's `config/ami/` directory.

## Use it

1. In-game: run `/ami dump-registry`. Look in `<instance>/ami_dumps/registry/registry-dump.json`.
2. Open https://<your-github-username>.github.io/<repo>/override-editor/.
3. Drop in `registry-dump.json`. Optionally drop in an existing `overrides.json` to keep editing.
4. Filter / select / bulk-edit. Per-item edits via row click.
5. Click **Download overrides.json**, place it in `<instance>/config/ami/overrides.json`.
6. In-game: `/ami reindex` (or restart) to apply.

## Notes

- Pack overrides win over mod-shipped defaults — your file is the highest-priority layer.
- Tool refuses to load files with a `schemaVersion` newer than it supports.
- Stale-id warnings show which override entries reference items not in your current dump.
- v1 lets you edit per-item category / subcategory / facets / custom tooltip lines. `modPatterns` are passed through unchanged.
```

- [ ] **Step 2: Append a section to root `README.md`**

Find an existing section heading (e.g. "Tools" or "For developers") and append:

```markdown
## Pack Override Editor

A browser-based editor for `overrides.json` lives in [`docs/override-editor/`](docs/override-editor/README.md) and is served via GitHub Pages.
```

- [ ] **Step 3: Enable GitHub Pages**

This is a one-time manual step in the GitHub web UI (not scriptable here):

1. Repo Settings → Pages
2. Source: `Deploy from a branch`, Branch: `main`, Folder: `/docs`
3. Save. The site appears at `https://<user>.github.io/<repo>/override-editor/` after ~1 minute.

Note this step in the commit message as a follow-up the user needs to perform.

- [ ] **Step 4: Commit**

```bash
git add README.md docs/override-editor/README.md
git commit -m "docs: document override editor + GitHub Pages serving"
```

---

## Task 12: End-to-end smoke test (manual)

**Files:**
- None (manual verification).

**Interfaces:**
- Consumes: all prior tasks.
- Produces: confidence that the full loop works.

- [ ] **Step 1: Build and run the mod in a dev client**

Run: `./gradlew :neoforge:runClient` (or the project's existing dev-client entry point)
Wait for the title screen, open a singleplayer world.

- [ ] **Step 2: Generate a dump**

In-game: type `/ami dump-registry`.
Expected: green chat message naming the output path. Open the file — verify `schemaVersion`, `items` array with realistic item counts (thousands).

- [ ] **Step 3: Hand-craft a tiny pack override**

Create `<instance>/config/ami/overrides.json`:

```json
{
  "schemaVersion": 1,
  "items": {
    "minecraft:stone": {
      "category": "decoration",
      "tooltipLines": ["Pack override test", "Second line"]
    }
  }
}
```

In-game: `/ami reindex`.

- [ ] **Step 4: Verify in-game**

- Hover Stone in inventory — confirm the two custom tooltip lines appear in gray.
- Open AMI — confirm Stone now appears under `decoration` instead of its prior category.

- [ ] **Step 5: Round-trip via the web tool**

- Open `docs/override-editor/index.html` in the browser.
- Drop in the dump from Step 2.
- Drop in the `overrides.json` from Step 3.
- Make a small edit (change Stone's subcategory).
- Click Download. Inspect — confirm the resulting file contains only the changed entries (sparse).
- Replace `<instance>/config/ami/overrides.json` with the downloaded file, `/ami reindex`, verify the edit took effect.

- [ ] **Step 6: Commit a CHANGELOG entry**

Append to `CHANGELOG.md`:

```markdown
## Unreleased
- Added `/ami dump-registry` command emitting `registry-dump.json` for tooling.
- Added pack override layer: drop `config/ami/overrides.json` into a modpack; its edits win over mod-shipped defaults and survive reindex.
- Added custom tooltip lines to the classification override schema.
- Added browser-based override editor served via GitHub Pages (`docs/override-editor/`).
```

```bash
git add CHANGELOG.md
git commit -m "docs: changelog entry for override editor + pack override layer"
```

---

## Self-Review

**Spec coverage:**
- `/ami dump` command — Task 1 (NeoForge) + Task 2 (Forge).
- HTML tool in `/docs` via GitHub Pages — Tasks 9–11.
- Drag-drop registry dump + existing overrides — `app.js` file inputs in Task 10.
- Filterable grid + multiselect + bulk-assign category/subcategory + per-item tooltip lines — Tasks 9 (`merge`, `diff`, `edit`) + 10 (`grid`, `app`).
- Sparse-patch output — `computeSparsePatch` in Task 9.
- Stale-ID warnings — `validate` in Task 9, surfaced in `app.js` issues panel.
- Schema version stamped — `SCHEMA_VERSION` constant flows through dump writer (Task 1), JS validator (Task 9), output JSON (Task 9 diff).
- Pack overrides survive reindex as highest-priority layer — Tasks 4 (merge), 5 (loader), 6 (wired into populate).
- Tooltip injection — Tasks 3 (data field), 7 (renderer).

**Placeholder scan:**
- Task 1 Step 5 calls out `SearchNodeKeys` accessor names as engineer-verified — this is acknowledged uncertainty about an external API, not a placeholder for unwritten plan content.
- Task 6 Step 1 / Step 5 reference platform helper paths and `Services.PLATFORM` patterns that the engineer must grep to confirm — again, intentional pointers to existing code, not "TBD".
- Task 7 Step 1 explicitly tasks the engineer to discover the tooltip hook location — required because the discovery is genuinely the work, and predicting the file path here is more brittle than running grep.

**Type consistency:**
- `ClassificationOverride.tooltipLines()` introduced in Task 3, consumed in Task 7. Signature stable: `List<String>`.
- `RegistryDumpWriter.Row` shape stable between Task 1 Step 1 (test fixture), Task 1 Step 3 (impl), Task 9 fixture, and Task 9 `parseRegistryDump`.
- `EditableItem` shape stable between Task 9 merge (`{ id, mod, ..., baseline, edited, dirty, missingFromDump }`) and consumers in Tasks 9 diff + 10 grid/edit/app.
- `ClassificationOverrides.mergeAndInstall(String)` introduced in Task 4 and consumed in Task 5 `PackOverrideLoader`. Signature stable.

**Risk callouts (not gaps, but worth keeping in mind during execution):**
- The biggest unknown is the `SearchNode` API shape (Task 1 Step 5). If the actual API can't fluently produce `(category, subcategory, facets, creativeTabs, displayName, className)` for every node, the dump may need a different collection path — e.g. iterate `BuiltInRegistries.ITEM` directly and look up classification via the existing `PrimaryCategoryResolver` rather than via search-node attributes. Flag back to the user if that's the case.
- v1 explicitly does not edit `modPatterns` from the web tool — the tool passes them through unchanged. If users immediately ask for pattern-rule editing, that's v2.

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-06-26-pack-override-editor-tool.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
