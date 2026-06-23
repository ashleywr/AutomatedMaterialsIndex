package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnProviderSourceTest {
    @Test
    void spawnProviderRecordsEntityToBiomeEdges() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/providers/SpawnProvider.java"));
        String platform = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/platform/IPlatformHelper.java"));
        String neoForge = Files.readString(Path.of("../neoforge/src/main/java/com/sanhiruzu/ami/neoforge/NeoForgePlatformHelper.java"));
        String forge = Files.readString(Path.of("../forge/src/main/java/com/sanhiruzu/ami/forge/ForgePlatformHelper.java"));
        String fabric = Files.readString(Path.of("../fabric/src/main/java/com/sanhiruzu/ami/fabric/FabricPlatformHelper.java"));

        assertTrue(source.contains("EdgeType.SPAWNS_IN"));
        assertTrue(source.contains("Services.PLATFORM.getBiomeMobSpawnSettings"),
                "SpawnProvider should read effective platform biome spawn settings, not raw registry biome settings.");
        assertTrue(source.contains("getSingleplayerServer"),
                "SpawnProvider should retry the integrated server biome registry when the client registry has no spawn entries.");
        assertTrue(source.contains("server biome registry"),
                "SpawnProvider should log when it falls back to server biome data.");
        assertTrue(source.contains("addUnresolvedEdge"));
        assertTrue(source.contains("addResolvedEdge(EdgeType.SPAWNS_IN"),
                "SpawnProvider should attach resolved biome nodes immediately for Sources.");
        assertTrue(source.contains("entity->biome spawn edges"),
                "SpawnProvider should log normal-run counts so missing biome chips are diagnosable from latest.log.");
        assertTrue(source.contains("getMobs"));
        assertFalse(source.contains("getMethod(\"spawners\")"), "Spawn provider should use the local 1.21.1 API, not reflection");

        assertTrue(platform.contains("getBiomeMobSpawnSettings"));
        assertTrue(neoForge.contains("modifiableBiomeInfo().get().mobSpawnSettings()"),
                "NeoForge must use modified biome info; raw getMobSettings() was observed empty at runtime.");
        assertTrue(forge.contains("getBiomeMobSpawnSettings"));
        assertTrue(fabric.contains("getBiomeMobSpawnSettings"));
    }

    @Test
    void cacheRestorePathRebuildsSpawnBiomeEdgesForSources() throws Exception {
        String service = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java"));
        String registry = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/ProviderRegistry.java"));

        int cacheLoad = service.indexOf("Services.PLATFORM.tryLoadGlobalIndexCache()");
        int restoreStacks = service.indexOf("ProviderRegistry.rehydrateSubtypeStacks(level)", cacheLoad);
        int restoreSpawns = service.indexOf("ProviderRegistry.indexSpawnGraphDeferred(level)", restoreStacks);
        assertTrue(cacheLoad >= 0);
        assertTrue(restoreSpawns > restoreStacks,
                "Cached index restores must rebuild entity->biome spawn edges for Sources.");

        assertTrue(registry.contains("indexSpawnGraphDeferred"));
        assertTrue(registry.contains("new SpawnProvider().populate"));
    }

    @Test
    void sourceDemandRetriesSpawnGraphWhenInitialRebuildHadNoLevel() throws Exception {
        String service = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java"));

        int ensureSources = service.indexOf("boolean ensureSourcesForItem");
        int ensureSpawn = service.indexOf("ensureSpawnGraphBuilt", ensureSources);
        assertTrue(ensureSources >= 0);
        assertTrue(ensureSpawn > ensureSources,
                "Opening Sources must retry mob spawn biome indexing if the initial rebuild ran before a client level existed.");

        int pending = service.indexOf("boolean isSourceIndexingPending");
        int spawnPending = service.indexOf("isSpawnGraphIndexing", pending);
        assertTrue(spawnPending > pending,
                "Sources loading state must include pending spawn graph indexing so biome chips can appear after it finishes.");
    }

    @Test
    void biomeLocateUsesCommandPermissionInsteadOfCheatToggle() throws Exception {
        String panel = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java"));
        String menu = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/results/ResultContextMenuActionBuilder.java"));

        int locateSourceBiome = panel.indexOf("private boolean locateSourceBiome");
        int sourceAllowed = panel.indexOf("AMICheatMode.isAllowed()", locateSourceBiome);
        int sourceLocate = panel.indexOf("AMICheatMode.locateBiome", locateSourceBiome);
        assertTrue(sourceAllowed > locateSourceBiome && sourceAllowed < sourceLocate,
                "Source biome chip locate should require command permission, not the AMI cheat-mode toggle.");

        int biomeMenu = menu.indexOf("node.type() == NodeType.BIOME");
        int menuAllowed = menu.indexOf("AMICheatMode.isAllowed()", biomeMenu);
        int menuLocate = menu.indexOf("AMICheatMode.locateBiome", biomeMenu);
        assertTrue(menuAllowed > biomeMenu && menuAllowed < menuLocate,
                "Biome context-menu locate should require command permission, not the AMI cheat-mode toggle.");
    }
}
