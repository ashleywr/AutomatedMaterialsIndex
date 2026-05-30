package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Side-safe service for Constructing AMI's searchable item index.
 * Orchestrates all data providers via ProviderRegistry.
 */
public final class AmiIndexerService {
    private static final AmiIndexerService INSTANCE = new AmiIndexerService();
    private final AtomicBoolean isRebuilding = new AtomicBoolean(false);
    private volatile SearchService searchService;
    private volatile int indexedItemCount;
    private volatile Throwable lastRebuildFailure;

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

    public Throwable getLastRebuildFailure() {
        return lastRebuildFailure;
    }

    public void rebuild() {
        rebuild(com.sanhiruzu.ami.util.DistUtils.getClientLevel());
    }

    public void rebuild(Level level) {
        if (!isRebuilding.compareAndSet(false, true)) return;
        lastRebuildFailure = null;

        CompletableFuture.runAsync(() -> {
            try {
                performRebuild(level);
            } catch (Throwable t) {
                lastRebuildFailure = t;
                AmiCore.LOGGER.error("AMI: Index rebuild failed", t);
            } finally {
                isRebuilding.set(false);
            }
        });
    }

    private void performRebuild(Level level) {
        long started = System.currentTimeMillis();
        GlobalIndex index = GlobalIndex.getInstance();

        // 1. Core indexing of all standard types
        GroupingEngine.initialize(level);
        if (Services.PLATFORM.tryLoadGlobalIndexCache()) {
            ProviderRegistry.rehydrateSubtypeStacks(level);
        } else {
            ProviderRegistry.indexAll(level);
            Services.PLATFORM.saveGlobalIndexCache();
        }

        // 2. Populate world/datapack-backed atlas types before we snapshot the search service.
        ProviderRegistry.indexStructuresDeferred(level);

        // 3. Post-indexing tasks
        if (AmiConfig.devMode) {
            ItemIconRenderer.auditMissingIcons();
        }

        index.markIndexReady();
        indexedItemCount = index.getNodes(NodeType.ITEM).size();

        // Build search service from the new index
        long searchServiceStart = System.currentTimeMillis();
        searchService = SearchService.buildFrom(index, true);
        long searchServiceMs = System.currentTimeMillis() - searchServiceStart;

        index.setIndexBuildTime(System.currentTimeMillis() - started);
        AmiCore.LOGGER.info("AMI: Index rebuild complete in {}ms (search service: {}ms). Indexed {} items.",
                index.getIndexBuildTimeMs(), searchServiceMs, indexedItemCount);
    }

    public int indexedItemCount() {
        return indexedItemCount;
    }
}
