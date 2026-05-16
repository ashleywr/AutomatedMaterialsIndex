package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.providers.*;
import net.minecraft.client.multiplayer.ClientLevel;
import java.util.List;

/**
 * Registry of all data providers.
 * Orchestrates the population of GlobalIndex.
 */
public final class ProviderRegistry {
    private ProviderRegistry() {}

    /**
     * All providers run on first inventory open, except StructureProvider (deferred).
     */
    private static final List<IAmiDataProvider> PROVIDERS = List.of(
            new ItemProvider(),
            new BiomeProvider(),
            new EntityProvider(),
            new DimensionProvider(),
            new LootTableProvider(),
            new SpawnProvider()
    );

    /**
     * Index all data types except STRUCTURE and DIMENSION (which are deferred).
     */
    public static void indexAll(ClientLevel level) {
        AMI.LOGGER.info("Starting GlobalIndex population...");
        long start = System.currentTimeMillis();
        GlobalIndex index = GlobalIndex.getInstance();
        index.clear();
        ItemIconRenderer.clearPersistent();

        // Mark deferred types as loading
        index.setLoading(NodeType.STRUCTURE, true);
        index.setLoading(NodeType.DIMENSION, true);

        for (IAmiDataProvider provider : PROVIDERS) {
            try {
                provider.populate(index, level);
            } catch (Exception e) {
                AMI.LOGGER.error("Provider {} failed", provider.getClass().getSimpleName(), e);
            }
        }

        index.setIndexBuildTime(System.currentTimeMillis() - start);
        AMI.LOGGER.info("GlobalIndex populated in {}ms", index.getIndexBuildTimeMs());
    }

    /**
     * Deferred retry for STRUCTURE and DIMENSION data.
     * Matches WorldAtlasIndexer.indexStructuresFromConnection() contract.
     */
    public static void indexStructuresDeferred(ClientLevel level) {
        try {
            new StructureProvider().populate(GlobalIndex.getInstance(), level);
        } catch (Exception e) {
            AMI.LOGGER.error("Deferred StructureProvider failed", e);
        }
        try {
            new DimensionProvider().populate(GlobalIndex.getInstance(), level);
        } catch (Exception e) {
            AMI.LOGGER.error("Deferred DimensionProvider failed", e);
        }
    }
}
