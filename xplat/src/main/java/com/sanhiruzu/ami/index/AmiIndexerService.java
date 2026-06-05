package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiGuideRegistry;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.ApotheosisGuideSource;
import com.sanhiruzu.ami.compat.SilentGearMaterialTraitIndex;
import com.sanhiruzu.ami.compat.SilentGearTraitGuideSource;
import com.sanhiruzu.ami.config.AmiCustomTaxonomy;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.index.query.SearchSuggestions;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.Util;
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
    private final AtomicBoolean isDeferredIndexing = new AtomicBoolean(false);
    private volatile SearchService searchService;
    private volatile long searchServiceRevision = -1L;
    private volatile AmiGuideSearchIndex guideSearchIndex = new AmiGuideSearchIndex(null, AmiGuideSearchIndex.GuideIndexingMode.OFF);
    private volatile int indexedItemCount;
    private volatile Throwable lastRebuildFailure;
    private volatile AmiIndexProgress progress = AmiIndexProgress.idle();
    private volatile String indexedLanguageCode = "";
    private volatile String pendingLanguageRebuildCode = "";

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

    public AmiIndexProgress progress() {
        return progress;
    }

    public AmiGuideSearchIndex getGuideSearchIndex() {
        return guideSearchIndex;
    }

    public boolean rebuild() {
        return rebuild(com.sanhiruzu.ami.util.DistUtils.getClientLevel());
    }

    public boolean rebuild(Level level) {
        return rebuild(level, false);
    }

    public boolean rebuild(boolean forceProviderRebuild) {
        return rebuild(com.sanhiruzu.ami.util.DistUtils.getClientLevel(), forceProviderRebuild);
    }

    public boolean rebuild(Level level, boolean forceProviderRebuild) {
        if (!isRebuilding.compareAndSet(false, true)) return false;
        lastRebuildFailure = null;

        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            try {
                performRebuild(level, forceProviderRebuild);
            } catch (Throwable t) {
                lastRebuildFailure = t;
                pendingLanguageRebuildCode = "";
                beginProgress("Indexing failed");
                AmiCore.LOGGER.error("AMI: Index rebuild failed", t);
            } finally {
                isRebuilding.set(false);
            }
        }), Util.backgroundExecutor());
        return true;
    }

    public boolean ensureCurrentLanguageIndex(Level level) {
        if (level == null || searchService == null || isRebuilding.get()) return false;

        String currentLanguageCode = GlobalIndexCache.currentClientLanguageCacheKey();
        if (currentLanguageCode.equals(indexedLanguageCode)
                || currentLanguageCode.equals(pendingLanguageRebuildCode)) {
            return false;
        }

        if (rebuild(level, false)) {
            pendingLanguageRebuildCode = currentLanguageCode;
            AmiCore.LOGGER.info("AMI: Client language changed to {}; loading or rebuilding localized index.",
                    currentLanguageCode);
            return true;
        }
        return false;
    }

    private static Runnable withIndexerClassLoader(Runnable task) {
        ClassLoader indexerClassLoader = AmiIndexerService.class.getClassLoader();
        return () -> {
            Thread thread = Thread.currentThread();
            ClassLoader previousClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(indexerClassLoader);
            try {
                task.run();
            } finally {
                thread.setContextClassLoader(previousClassLoader);
            }
        };
    }

    private void performRebuild(Level level, boolean forceProviderRebuild) {
        long started = System.currentTimeMillis();
        String buildLanguageCode = GlobalIndexCache.currentClientLanguageCacheKey();
        GlobalIndex index = GlobalIndex.getInstance();

        // 1. Core indexing of all standard types
        beginProgress("Preparing index");
        GroupingEngine.initialize(level);
        AmiCustomTaxonomy.reload();
        AmiDataFixes.reload();
        beginProgress("Checking index cache");
        boolean deferredNamespaceMode = IndexingHotItemPolicy.hasDeferredIndexNamespaces();
        if (deferredNamespaceMode) {
            AmiCore.LOGGER.info(
                    "AMI: Deferred namespace indexing enabled for {}; bypassing index cache for this experimental run.",
                    IndexingHotItemPolicy.deferredIndexNamespacesForLog());
        }
        if (!deferredNamespaceMode && !forceProviderRebuild && Services.PLATFORM.tryLoadGlobalIndexCache()) {
            beginProgress("Restoring cached item icons", "Rebuilding runtime stacks", estimatedItemTotal());
            ProviderRegistry.rehydrateSubtypeStacks(level);
        } else {
            beginProgress("Building item index");
            ProviderRegistry.indexAll(level);
            if (deferredNamespaceMode) {
                AmiCore.LOGGER.info("AMI: Deferred namespace indexing active; skipping index cache save until the experiment is disabled.");
            } else {
                beginProgress("Saving index cache");
                Services.PLATFORM.saveGlobalIndexCache();
            }
        }

        // 2. Populate world/datapack-backed atlas types before we snapshot the search service.
        beginProgress("Indexing world data");
        ProviderRegistry.indexStructuresDeferred(level);

        // 3. Post-indexing tasks
        beginProgress("Applying custom taxonomy");
        AmiCustomTaxonomy.applyToIndex(index);
        beginProgress("Applying data fixes");
        AmiDataFixes.applyToIndex(index);
        beginProgress("Applying compatibility metadata");
        SilentGearMaterialTraitIndex.applyToIndex(index);

        beginProgress("Indexing guides");
        AmiGuideRegistry.clear();
        ApotheosisGuideSource.registerGuideDocuments(AmiGuideRegistry::register);
        SilentGearTraitGuideSource.registerGuideDocuments(AmiGuideRegistry::register);
        AmiGuideRegistry.registerPluginGuides();
        guideSearchIndex = AmiGuideSearchIndex.fromConfig(AmiGuideRegistry.getDocuments());

        index.markIndexReady();
        indexedLanguageCode = buildLanguageCode;
        pendingLanguageRebuildCode = "";
        indexedItemCount = index.getNodes(NodeType.ITEM).size();

        // Build search service from the new index
        long searchServiceStart = System.currentTimeMillis();
        beginProgress("Building search cache");
        publishSearchService(index, SearchService.buildFrom(index, true));
        long searchServiceMs = System.currentTimeMillis() - searchServiceStart;
        if (!deferredNamespaceMode) {
            warmSuggestionsAsync(index);
        }

        index.setIndexBuildTime(System.currentTimeMillis() - started);
        progress = AmiIndexProgress.idle();
        AmiCore.LOGGER.info("AMI: Index rebuild complete in {}ms (search service: {}ms). Indexed {} items and {} guide docs.",
                index.getIndexBuildTimeMs(), searchServiceMs, indexedItemCount, guideSearchIndex.allDocuments().size());
        if (deferredNamespaceMode) {
            scheduleDeferredNamespaceIndex(level);
        } else {
            scheduleIconAuditIfEnabled();
        }
    }

    private void scheduleDeferredNamespaceIndex(Level level) {
        if (!isDeferredIndexing.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            long started = System.currentTimeMillis();
            long searchServiceMs = 0L;
            int added = 0;
            try {
                GlobalIndex index = GlobalIndex.getInstance();
                AmiCore.LOGGER.info("AMI: Deferred namespace indexing started for {}.",
                        IndexingHotItemPolicy.deferredIndexNamespacesForLog());
                added = ProviderRegistry.indexDeferredItems(level);
                if (added > 0) {
                    beginProgress("Applying deferred taxonomy");
                    AmiCustomTaxonomy.applyToIndex(index);
                    beginProgress("Applying deferred data fixes");
                    AmiDataFixes.applyToIndex(index);
                    beginProgress("Applying deferred compatibility metadata");
                    SilentGearMaterialTraitIndex.applyToIndex(index);
                    indexedItemCount = index.getNodes(NodeType.ITEM).size();

                    long searchServiceStart = System.currentTimeMillis();
                    beginProgress("Building deferred search cache");
                    publishSearchService(index, SearchService.buildFrom(index, true));
                    searchServiceMs = System.currentTimeMillis() - searchServiceStart;
                }
                warmSuggestionsAsync(index);
                scheduleIconAuditIfEnabled();
                AmiCore.LOGGER.info(
                        "AMI: Deferred namespace indexing complete in {}ms (added {} item nodes, search service: {}ms). Indexed {} items.",
                        System.currentTimeMillis() - started,
                        added,
                        searchServiceMs,
                        indexedItemCount);
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI: Deferred namespace indexing failed: {}", t.getMessage(), t);
            } finally {
                isDeferredIndexing.set(false);
                progress = AmiIndexProgress.idle();
            }
        }), Util.backgroundExecutor());
    }

    private void publishSearchService(GlobalIndex index, SearchService service) {
        searchService = service;
        searchServiceRevision = index.revision();
    }

    private void scheduleIconAuditIfEnabled() {
        if (!IndexingHotItemPolicy.shouldAuditIcons()) {
            return;
        }
        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            long started = System.currentTimeMillis();
            try {
                ItemIconRenderer.auditMissingIcons();
                AmiCore.LOGGER.info("AMI IconAudit: completed in {}ms.", System.currentTimeMillis() - started);
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI IconAudit: failed: {}", t.getMessage(), t);
            }
        }), Util.backgroundExecutor());
    }

    private void warmSuggestionsAsync(GlobalIndex index) {
        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            long started = System.currentTimeMillis();
            try {
                SearchSuggestions.warm(index);
                AmiCore.LOGGER.info("AMI: Suggestion vocabulary warmed in {}ms.", System.currentTimeMillis() - started);
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI: Suggestion vocabulary warm-up failed: {}", t.getMessage(), t);
            }
        }), Util.backgroundExecutor());
    }

    public void beginProgress(String phase) {
        progress = AmiIndexProgress.start(phase);
    }

    public void beginProgress(String phase, String detail, int total) {
        progress = AmiIndexProgress.start(phase, detail, total);
    }

    public void updateProgress(int current) {
        progress = progress.withProgress(current);
    }

    public void updateProgressDetail(String detail) {
        progress = progress.withDetail(detail);
    }

    private static int estimatedItemTotal() {
        try {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.size();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public int indexedItemCount() {
        return indexedItemCount;
    }

    public long searchServiceRevision() {
        return searchServiceRevision;
    }

    public boolean isDeferredIndexing() {
        return isDeferredIndexing.get();
    }
}
