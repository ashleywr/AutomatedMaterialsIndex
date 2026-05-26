package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.neoforge.AMI;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Side-safe service for Constructing AMI's searchable item index.
 * Orchestrates all data providers via ProviderRegistry.
 */
public final class AmiIndexerService {
    private static final AmiIndexerService INSTANCE = new AmiIndexerService();

    private volatile SearchService searchService;
    private volatile int indexedItemCount;
    private final AtomicBoolean isRebuilding = new AtomicBoolean(false);

    private AmiIndexerService() {
    }

    public static AmiIndexerService getInstance() {
        return INSTANCE;
    }

    public SearchService getOrBuildSearchService() {
        if (searchService == null) {
            rebuild();
            // Return an empty service for the very first frame to avoid null
            return SearchService.buildFrom(GlobalIndex.getInstance(), false);
        }
        return searchService;
    }

    public boolean isReady() {
        return searchService != null && !isRebuilding.get();
    }

    public void rebuild() {
        rebuild(com.sanhiruzu.ami.util.DistUtils.getClientLevel());
    }

    public void rebuild(Level level) {
        if (!isRebuilding.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                performRebuild(level);
            } finally {
                isRebuilding.set(false);
            }
        });
    }

    private void performRebuild(Level level) {
        long started = System.currentTimeMillis();
        GlobalIndex index = GlobalIndex.getInstance();

        // 1. Core indexing of all standard types
        ProviderRegistry.indexAll(level);

        // 2. Populate world/datapack-backed atlas types before we snapshot the search service.
        ProviderRegistry.indexStructuresDeferred(level);

        // 3. Post-indexing tasks
        if (AmiConfig.devMode) {
            ItemIconRenderer.auditMissingIcons();
        }

        index.markIndexReady();
        index.setIndexBuildTime(System.currentTimeMillis() - started);
        indexedItemCount = index.getNodes(NodeType.ITEM).size();

        // Build search service from the new index
        searchService = SearchService.buildFrom(index, true);

        AMI.LOGGER.info("AMI: Index rebuild complete in {}ms. Indexed {} items.",
                index.getIndexBuildTimeMs(), indexedItemCount);
    }

    public int indexedItemCount() {
        return indexedItemCount;
    }
}
