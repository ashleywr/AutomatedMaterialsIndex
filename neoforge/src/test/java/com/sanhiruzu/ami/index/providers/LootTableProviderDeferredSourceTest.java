package com.sanhiruzu.ami.index.providers;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootTableProviderDeferredSourceTest {
    @Test
    void lootTableResourceScanningIsDeferredAfterPrimarySearchPublish() throws Exception {
        String provider = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/providers/LootTableProvider.java"));
        String registry = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/ProviderRegistry.java"));
        String service = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java"));

        int populateStart = provider.indexOf("public void populate");
        int deferredStart = provider.indexOf("indexDeferredDrops");
        assertTrue(deferredStart > populateStart, "Loot table resource scanning should live behind a deferred method.");
        String populateBody = provider.substring(populateStart, deferredStart);
        assertFalse(populateBody.contains("listResources"), "Primary provider pass must not scan loot-table JSON resources.");
        assertFalse(populateBody.contains("LootTableDropIndexer"), "Primary provider pass must not parse loot tables.");

        assertTrue(provider.contains("indexRoot(index, resourceManager.get(), \"loot_table\")"));
        assertTrue(provider.contains("indexRoot(index, resourceManager.get(), \"loot_tables\")"));
        assertTrue(provider.contains("resourceManager.listResources(root"));
        assertTrue(provider.contains("LootTableDropIndexer.indexEntityLootTables"));
        assertTrue(provider.contains("getSingleplayerServer()"),
                "Loot tables are server/datapack resources; the scanner must not use the client asset manager.");
        assertTrue(provider.contains("server.getResourceManager()"),
                "Integrated-server worlds should scan the loaded server datapack resource manager.");
        assertTrue(provider.contains("SERVER_DATA_UNAVAILABLE"),
                "The source view needs to explain when real loot data is unavailable, such as multiplayer clients.");
        assertFalse(provider.contains("Minecraft.getInstance().getResourceManager()"),
                "Client resources are assets, so scanning them for loot tables produces the observed zero-resource pass.");
        assertTrue(registry.contains("indexLootTablesDeferred"));
        assertTrue(registry.contains("new LootTableProvider().indexDeferredDrops"));

        int publishSearchService = service.indexOf("publishSearchService(index, SearchService.buildFrom(index, true))");
        int scheduleDeferredLootIndex = service.indexOf("scheduleDeferredLootIndex();", publishSearchService);
        assertTrue(publishSearchService >= 0);
        assertTrue(scheduleDeferredLootIndex > publishSearchService,
                "Passive loot drop indexing should start only after the main search service is published.");
        int demandSources = service.indexOf("ensureSourcesForItem");
        assertTrue(service.indexOf("scheduleDeferredLootIndex();", demandSources) > demandSources,
                "A source route may demand-start deferred loot indexing before the passive tail task runs.");
        assertTrue(service.contains("isDeferredLootIndexing"));
        assertTrue(service.contains("ProviderRegistry.indexLootTablesDeferred"));
    }
}
