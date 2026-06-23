package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiIndexerServiceSourceDemandSourceTest {
    @Test
    void sourcesRouteCanDemandStartDeferredSourceIndexes() throws Exception {
        String service = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java"));
        String panel = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java"));

        int ensureSources = service.indexOf("ensureSourcesForItem");
        assertTrue(ensureSources >= 0, "AmiIndexerService should expose a demand path for item source routes.");
        assertTrue(service.indexOf("ensurePendingRecipeIndexBuild()", ensureSources) > ensureSources,
                "Demand source resolving should start pending recipe graph work when a level is available.");
        assertTrue(service.indexOf("scheduleDeferredLootIndex()", ensureSources) > ensureSources,
                "Demand source resolving should start deferred loot source indexing immediately when needed.");
        assertTrue(service.contains("isSourceIndexingPending"),
                "The source view needs a cheap loading predicate while deferred source indexes are still filling.");
        assertTrue(service.contains("sourceDiagnostics"),
                "The source view should expose diagnostics when source indexes complete but still yield no rows.");
        assertTrue(service.contains("lastDeferredLootIndexResult"),
                "Empty source diagnostics need the last deferred loot scan result.");

        int openRoute = panel.indexOf("private boolean openSourceRoute");
        int demandCall = panel.indexOf("ensureSourcesForItem", openRoute);
        assertTrue(demandCall > openRoute,
                "Opening a source route should demand-start source indexing for the target item.");
        assertTrue(panel.indexOf("isSourceIndexingPending", openRoute) > openRoute,
                "Opening a source route should pass loading state into the source report.");
        assertTrue(panel.indexOf("sourceDiagnostics", openRoute) > openRoute,
                "Opening a source route should pass empty-result diagnostics into the source report.");
        assertTrue(panel.indexOf("sourceViewDiagnostics", openRoute) > openRoute,
                "Opening a source route should include diagnostics for partial rows, such as mob drops missing biome data.");
    }

    @Test
    void sourcesRouteExplainsMobDropsWithoutBiomeEdges() throws Exception {
        String panel = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java"));
        String lang = Files.readString(Path.of("../xplat/src/main/resources/assets/ami/lang/en_us.json"));

        assertTrue(panel.contains("ami.sources.diagnostic.spawn.no_biomes"),
                "Sources should explain when mob drops are present but no spawn biome edges are indexed.");
        assertTrue(panel.contains("ItemSourceType.MOB_DROP"),
                "The diagnostic should be tied to mob-drop rows, not all source rows.");
        assertTrue(lang.contains("\"ami.sources.diagnostic.spawn.no_biomes\""),
                "Missing-biome diagnostic needs a translated string.");
    }

    @Test
    void sourceIndexingConfigGatesExpensiveLootAndSpawnWork() throws Exception {
        String config = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/config/AmiConfig.java"));
        String service = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java"));
        String registry = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/ProviderRegistry.java"));
        String panel = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java"));
        String lang = Files.readString(Path.of("../xplat/src/main/resources/assets/ami/lang/en_us.json"));

        assertTrue(config.contains("sourceIndexLootDrops"));
        assertTrue(config.contains("sourceIndexSpawnBiomes"));
        assertTrue(config.contains("@ConfigValue(\"sources.index-loot-drops\")"));
        assertTrue(config.contains("@ConfigValue(\"sources.index-spawn-biomes\")"));

        int scheduleLoot = service.indexOf("private void scheduleDeferredLootIndex()");
        assertTrue(service.indexOf("!AmiConfig.sourceIndexLootDrops", scheduleLoot) > scheduleLoot,
                "Disabled loot indexing should not schedule the deferred loot scanner.");
        assertTrue(service.indexOf("deferredLootIndexComplete.set(true)", scheduleLoot) > scheduleLoot,
                "Disabled loot indexing should mark the source phase complete so Sources does not spin forever.");

        int ensureSpawn = service.indexOf("private void ensureSpawnGraphBuilt()");
        assertTrue(service.indexOf("!AmiConfig.sourceIndexSpawnBiomes", ensureSpawn) > ensureSpawn,
                "Disabled spawn indexing should skip demand-started spawn graph work.");
        assertTrue(service.indexOf("spawnGraphComplete.set(true)", ensureSpawn) > ensureSpawn,
                "Disabled spawn indexing should mark the source phase complete so Sources can explain the missing enrichment.");

        assertTrue(registry.contains("!AmiConfig.sourceIndexSpawnBiomes"),
                "ProviderRegistry should guard direct spawn graph indexing entry points too.");
        assertTrue(service.contains("ami.sources.diagnostic.loot.disabled"));
        assertTrue(panel.contains("ami.sources.diagnostic.spawn.disabled"));
        assertTrue(lang.contains("\"ami.sources.diagnostic.loot.disabled\""));
        assertTrue(lang.contains("\"ami.sources.diagnostic.spawn.disabled\""));
    }
}
