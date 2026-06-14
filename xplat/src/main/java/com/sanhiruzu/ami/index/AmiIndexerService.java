package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiGuideRegistry;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.CompatIndexRegistry;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiCustomTaxonomy;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.index.query.SearchSuggestions;
import com.sanhiruzu.ami.index.runtime.RuntimeSearchProviders;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.index.providers.AmiRegistryDocumentBuilders;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
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
    private final AtomicBoolean isDeferredGuideIndexing = new AtomicBoolean(false);
    private final AtomicBoolean isDeferredRegistryDocumentIndexing = new AtomicBoolean(false);
    private final AtomicBoolean pendingRecipeIndexRebuild = new AtomicBoolean(false);
    private final AtomicBoolean isRecipeIndexRebuilding = new AtomicBoolean(false);
    private volatile SearchService searchService;
    private volatile long searchServiceRevision = -1L;
    private volatile AmiGuideSearchIndex guideSearchIndex = new AmiGuideSearchIndex(null, AmiGuideSearchIndex.GuideIndexingMode.OFF);
    private volatile AmiRegistryDocumentIndex registryDocumentIndex = AmiRegistryDocumentIndex.EMPTY;
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

    public boolean isBusy() {
        return isRebuilding.get()
                || isDeferredIndexing.get()
                || isDeferredGuideIndexing.get()
                || isDeferredRegistryDocumentIndexing.get()
                || isRecipeIndexRebuilding.get()
                || pendingRecipeIndexRebuild.get();
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

    public AmiRegistryDocumentIndex getRegistryDocumentIndex() {
        return registryDocumentIndex;
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

        // Capture creative tab data on the calling (main) thread before dispatching background work.
        // tab.buildContents() fires NeoForge events; calling it off-thread is unsupported by mod authors.
        ItemFilter.captureCreativeTabSnapshot(level);

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

        // Clear deletion tracker since we're rebuilding the index
        com.sanhiruzu.ami.client.results.DeletedSearchNodesTracker.clear();

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
            ensureRecipeIndexBuilt(level);
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

        // 3. Add runtime providers (waypoints, players) to index so they're available to all resolvers
        beginProgress("Loading runtime data");
        for (SearchNode node : RuntimeSearchProviders.nodes()) {
            index.addNode(node);
        }

        // 4. Post-indexing tasks
        beginProgress("Applying custom taxonomy");
        AmiCustomTaxonomy.applyToIndex(index);
        beginProgress("Applying data fixes");
        AmiDataFixes.applyToIndex(index);
        beginProgress("Applying compatibility metadata");
        CompatIndexRegistry.applyAll(index);

        beginProgress("Preparing guide index");
        AmiGuideRegistry.clear();
        guideSearchIndex = AmiGuideSearchIndex.fromConfig(java.util.List.of());
        registryDocumentIndex = AmiRegistryDocumentIndex.EMPTY;

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
        scheduleDeferredGuideIndex();
        scheduleDeferredRegistryDocumentIndex(level != null ? level.registryAccess() : null);
    }

    private void ensureRecipeIndexBuilt(Level level) {
        if (level == null) {
            if (pendingRecipeIndexRebuild.compareAndSet(false, true)) {
                AmiCore.LOGGER.debug("AMI: Recipe index rebuild deferred; waiting for client level.");
            }
            return;
        }
        pendingRecipeIndexRebuild.set(false);

        AmiRecipeIndex recipeIndex = AmiRecipeIndex.getInstance();
        if (recipeIndex.isBuilt()) {
            return;
        }
        rebuildRecipeIndexNow(level, recipeIndex);
    }

    public void ensurePendingRecipeIndexBuild() {
        if (!pendingRecipeIndexRebuild.get()) return;

        Level level = com.sanhiruzu.ami.util.DistUtils.getClientLevel();
        if (level == null) return;

        if (pendingRecipeIndexRebuild.compareAndSet(true, false)) {
            scheduleRecipeIndexRebuild(level);
        }
    }

    public void scheduleRecipeIndexRebuild(Level level) {
        if (level == null) return;
        if (!isRecipeIndexRebuilding.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            try {
                rebuildRecipeIndexNow(level, AmiRecipeIndex.getInstance());
            } finally {
                isRecipeIndexRebuilding.set(false);
            }
        }), Util.backgroundExecutor());
    }

    private void rebuildRecipeIndexNow(Level level, AmiRecipeIndex recipeIndex) {
        beginProgress("Rebuilding recipe index");
        try {
            recipeIndex.rebuild(level);
            AmiCore.LOGGER.debug("AMI: Recipe index rebuilt after cache restore ({} recipes)", recipeIndex.recipeCount());
        } catch (RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Recipe index rebuild after cache restore failed: {}", e.getMessage(), e);
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
                    CompatIndexRegistry.applyAll(index);
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

    private void scheduleDeferredGuideIndex() {
        if (!AmiConfig.searchIncludeGuides || AmiConfig.guideIndexingMode == AmiConfig.GuideIndexingMode.OFF) {
            return;
        }
        if (!isDeferredGuideIndexing.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            long started = System.currentTimeMillis();
            try {
                beginProgress("Indexing guide books");
                AmiGuideRegistry.clear();
                CompatIndexRegistry.registerAllGuideDocuments(AmiGuideRegistry::register);
                AmiGuideRegistry.registerPluginGuides();
                AmiGuideRegistry.registerSearchableGuideProviders();
                guideSearchIndex = AmiGuideSearchIndex.fromConfig(AmiGuideRegistry.getDocuments());
                AmiCore.LOGGER.info("AMI: Deferred guide indexing complete in {}ms ({} guide docs).",
                        System.currentTimeMillis() - started,
                        guideSearchIndex.allDocuments().size());
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI: Deferred guide indexing failed: {}", t.getMessage(), t);
            } finally {
                isDeferredGuideIndexing.set(false);
                progress = AmiIndexProgress.idle();
            }
        }), Util.backgroundExecutor());
    }

    private void scheduleDeferredRegistryDocumentIndex(RegistryAccess registryAccess) {
        if (!isDeferredRegistryDocumentIndexing.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(withIndexerClassLoader(() -> {
            long started = System.currentTimeMillis();
            try {
                beginProgress("Indexing registry documents");
                List<RegistryDocument> docs = new ArrayList<>();
                if (registryAccess != null) {
                    if (AmiConfig.searchIncludeEnchantments) {
                        docs.addAll(AmiRegistryDocumentBuilders.buildEnchantmentDocuments(registryAccess));
                    }
                    if (AmiConfig.searchIncludePaintings) {
                        docs.addAll(AmiRegistryDocumentBuilders.buildPaintingDocuments(registryAccess));
                    }
                    if (AmiConfig.searchIncludeTags) {
                        docs.addAll(AmiRegistryDocumentBuilders.buildTagDocuments(registryAccess));
                    }
                }
                if (AmiConfig.searchIncludeMobEffects) {
                    docs.addAll(AmiRegistryDocumentBuilders.buildMobEffectDocuments());
                }
                if (AmiConfig.searchIncludeGameRules) {
                    docs.addAll(AmiRegistryDocumentBuilders.buildGameRuleDocuments());
                }
                registryDocumentIndex = new AmiRegistryDocumentIndex(docs);
                SearchSuggestions.warmRegistryDocuments(registryDocumentIndex);
                AmiCore.LOGGER.info("AMI: Registry document indexing complete in {}ms ({} docs).",
                        System.currentTimeMillis() - started, docs.size());
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI: Registry document indexing failed: {}", t.getMessage(), t);
            } finally {
                isDeferredRegistryDocumentIndexing.set(false);
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

    public long runtimeSearchRevision() {
        return RuntimeSearchProviders.revision();
    }

    public boolean isDeferredIndexing() {
        return isDeferredIndexing.get();
    }
}
