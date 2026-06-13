package com.sanhiruzu.ami.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sanhiruzu.ami.client.AmiClientTelemetry;
import com.sanhiruzu.ami.client.EntityIconCache;
import com.sanhiruzu.ami.client.EntityIconWarmupMetrics;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.*;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Reflection-friendly runtime hooks for local smoke automation.
 */
public final class AmiRuntimeDebugApi {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;
    private static final int MAX_TREE_DEPTH = 8;

    private AmiRuntimeDebugApi() {
    }

    public static boolean isReady() {
        return AmiIndexerService.getInstance().isReady();
    }

    public static String statusJson() {
        JsonObject out = baseStatus();
        return GSON.toJson(out);
    }

    public static String requestReindexJson(boolean forceProviderRebuild) {
        boolean accepted = AmiIndexerService.getInstance().rebuild(forceProviderRebuild);
        JsonObject out = baseStatus();
        out.addProperty("accepted", accepted);
        out.addProperty("forceProviderRebuild", forceProviderRebuild);
        return GSON.toJson(out);
    }

    public static String searchJson(String query, int limit, String viewModeName, String lensName, boolean compact) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT));
        SearchState state = new SearchState();
        state.setQuery(query == null ? "" : query.trim());
        state.setViewMode(parseViewMode(viewModeName));
        state.setListLens(parseLens(lensName));

        List<SearchNode> source = atlasNodes();
        SearchService searchService = AmiIndexerService.getInstance().getOrBuildSearchService();
        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                source,
                state,
                searchService,
                AmiConfig.searchIncludeGuides ? AmiIndexerService.getInstance().getGuideSearchIndex() : null,
                AmiConfig.searchIncludeQuests ? AmiQuestsApi.getQuestSearchIndex() : null,
                AmiConfig.searchIncludeAdvancements ? AdvancementRuntimeDocuments.searchIndex() : null,
                compact,
                false
        );
        List<SearchNode> displayedLeaves = flattenLeaves(projection.roots(), Integer.MAX_VALUE);

        JsonObject out = baseStatus();
        out.addProperty("query", state.getQuery());
        out.addProperty("limit", boundedLimit);
        out.addProperty("viewMode", state.getViewMode().name());
        out.addProperty("listLens", state.getListLens().name());
        out.addProperty("compact", compact);
        out.addProperty("sourceCount", projection.sourceCount());
        out.addProperty("projectedItemCount", projection.displayedItemCount());
        out.addProperty("displayedItemCount", displayedLeaves.size());
        out.addProperty("guideResultCount", projection.guideRows().size());
        out.addProperty("questResultCount", projection.questRows().size());
        out.addProperty("advancementResultCount", projection.advancementRows().size());
        out.addProperty("summary", projection.summary());
        out.add("sourceTypeCounts", typeCounts(source));
        out.add("displayedTypeCounts", typeCounts(displayedLeaves));
        out.add("results", leafSummaries(displayedLeaves, boundedLimit));
        out.add("roots", treeSummaries(projection.roots(), boundedLimit));
        out.add("guides", guideSummaries(projection.guideRows(), boundedLimit));
        out.add("quests", questSummaries(projection.questRows(), boundedLimit));
        out.add("advancements", advancementSummaries(projection.advancementRows(), boundedLimit));
        out.addProperty("treeDump", ResultsTreeShapeDump.dumpTree(projection.roots()));
        return GSON.toJson(out);
    }

    private static JsonObject baseStatus() {
        AmiIndexerService indexer = AmiIndexerService.getInstance();
        GlobalIndex index = GlobalIndex.getInstance();
        JsonObject out = new JsonObject();
        out.addProperty("ready", indexer.isReady());
        out.addProperty("busy", indexer.isBusy());
        out.addProperty("indexReady", index.isIndexReady());
        out.addProperty("revision", index.revision());
        out.addProperty("indexBuildTimeMs", index.getIndexBuildTimeMs());
        out.addProperty("indexedItemCount", indexer.indexedItemCount());
        out.add("nodeTypeCounts", indexTypeCounts(index));
        out.add("memory", memoryStatus());
        out.add("clientTick", clientTickStatus(AmiClientTelemetry.snapshot()));
        out.add("entityIcons", entityIconStatus());
        AmiIndexProgress progress = indexer.progress();
        JsonObject progressJson = new JsonObject();
        progressJson.addProperty("active", progress.active());
        progressJson.addProperty("phase", progress.phase());
        progressJson.addProperty("detail", progress.detail());
        progressJson.addProperty("current", progress.current());
        progressJson.addProperty("total", progress.total());
        progressJson.addProperty("percent", progress.percent());
        progressJson.addProperty("elapsedMs", progress.elapsedMs());
        progressJson.addProperty("message", progress.message());
        out.add("progress", progressJson);
        Throwable failure = indexer.getLastRebuildFailure();
        if (failure != null) {
            out.addProperty("lastRebuildFailure", failure.getClass().getName() + ": " + failure.getMessage());
        }
        return out;
    }

    private static JsonObject memoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        JsonObject out = new JsonObject();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        out.addProperty("heapUsedBytes", total - free);
        out.addProperty("heapCommittedBytes", total);
        out.addProperty("heapMaxBytes", max);
        out.addProperty("availableProcessors", runtime.availableProcessors());
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            out.addProperty("processCpuLoad", processCpuLoad(bean));
            out.addProperty("systemCpuLoad", systemCpuLoad(bean));
        } catch (RuntimeException ignored) {
        }
        return out;
    }

    private static double processCpuLoad(java.lang.management.OperatingSystemMXBean bean) {
        if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            return sunBean.getProcessCpuLoad();
        }
        return -1.0D;
    }

    private static double systemCpuLoad(java.lang.management.OperatingSystemMXBean bean) {
        if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            return sunBean.getCpuLoad();
        }
        return -1.0D;
    }

    private static JsonObject clientTickStatus(AmiClientTelemetry.Snapshot snapshot) {
        JsonObject out = new JsonObject();
        out.addProperty("tickSamples", snapshot.tickSamples());
        out.addProperty("tickIntervalSamples", snapshot.tickIntervalSamples());
        out.addProperty("frameSamples", snapshot.frameSamples());
        out.addProperty("averageTickMs", nanosToMs(snapshot.averageTickNanos()));
        out.addProperty("maxTickMs", nanosToMs(snapshot.maxTickNanos()));
        out.addProperty("averageTickIntervalMs", nanosToMs(snapshot.averageTickIntervalNanos()));
        out.addProperty("maxTickIntervalMs", nanosToMs(snapshot.maxTickIntervalNanos()));
        out.addProperty("averageFrameIntervalMs", nanosToMs(snapshot.averageFrameIntervalNanos()));
        out.addProperty("maxFrameIntervalMs", nanosToMs(snapshot.maxFrameIntervalNanos()));
        out.addProperty("estimatedFps", snapshot.estimatedFps());
        return out;
    }

    private static JsonObject indexTypeCounts(GlobalIndex index) {
        JsonObject counts = new JsonObject();
        for (NodeType type : NodeType.values()) {
            counts.addProperty(type.name(), index.getNodes(type).size());
        }
        return counts;
    }

    private static JsonObject entityIconStatus() {
        EntityIconCache.Stats stats = EntityIconCache.stats();
        EntityIconWarmupMetrics.Snapshot warmup = EntityIconWarmupMetrics.snapshot();
        JsonObject out = new JsonObject();
        out.addProperty("atlasCount", stats.atlasCount());
        out.addProperty("pendingBakeCount", stats.pendingBakeCount());
        out.addProperty("failedKeyCount", stats.failedKeyCount());
        out.addProperty("queuedBakeRequests", stats.queuedBakeRequests());
        out.addProperty("droppedBakeRequests", stats.droppedBakeRequests());
        out.addProperty("renderedBakeCount", stats.renderedBakeCount());
        out.addProperty("persistentLoadCount", stats.persistentLoadCount());
        out.addProperty("failedBakeCount", stats.failedBakeCount());
        out.addProperty("pendingPersistentWrites", stats.pendingPersistentWrites());
        out.addProperty("droppedPersistentWrites", stats.droppedPersistentWrites());
        JsonObject warmupJson = new JsonObject();
        warmupJson.addProperty("revision", warmup.revision());
        warmupJson.addProperty("total", warmup.total());
        warmupJson.addProperty("visited", warmup.visited());
        warmupJson.addProperty("remaining", warmup.remaining());
        warmupJson.addProperty("queuedOrCached", warmup.queuedOrCached());
        warmupJson.addProperty("skipped", warmup.skipped());
        warmupJson.addProperty("renderFailures", warmup.renderFailures());
        warmupJson.addProperty("done", warmup.done());
        out.add("warmup", warmupJson);
        JsonArray atlases = new JsonArray();
        for (EntityIconCache.AtlasStats atlas : stats.atlases().values()) {
            JsonObject atlasJson = new JsonObject();
            atlasJson.addProperty("size", atlas.size());
            atlasJson.addProperty("entryCount", atlas.entryCount());
            atlasJson.addProperty("maxSlots", atlas.maxSlots());
            atlases.add(atlasJson);
        }
        out.add("atlases", atlases);
        return out;
    }

    private static double nanosToMs(long nanos) {
        return nanos / 1_000_000.0D;
    }

    private static List<SearchNode> atlasNodes() {
        List<SearchNode> all = new ArrayList<>();
        for (NodeType type : NodeType.atlasValues()) {
            all.addAll(GlobalIndex.getInstance().getNodes(type));
        }
        return all;
    }

    private static ResultsToolbar.ViewMode parseViewMode(String value) {
        if (value == null || value.isBlank()) {
            return ResultsToolbar.ViewMode.LIST;
        }
        try {
            return ResultsToolbar.ViewMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ResultsToolbar.ViewMode.LIST;
        }
    }

    private static ListLens parseLens(String value) {
        if (value == null || value.isBlank()) {
            return ListLens.ALL;
        }
        try {
            return ListLens.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ListLens.ALL;
        }
    }

    private static JsonObject typeCounts(List<SearchNode> nodes) {
        JsonObject counts = new JsonObject();
        for (SearchNode node : nodes) {
            String key = node.type().name();
            counts.addProperty(key, counts.has(key) ? counts.get(key).getAsInt() + 1 : 1);
        }
        return counts;
    }

    private static JsonArray leafSummaries(List<SearchNode> leaves, int limit) {
        JsonArray out = new JsonArray();
        int count = 0;
        for (SearchNode node : leaves) {
            if (count++ >= limit) break;
            out.add(nodeSummary(node));
        }
        return out;
    }

    private static List<SearchNode> flattenLeaves(List<TreeNode> roots, int limit) {
        List<SearchNode> out = new ArrayList<>();
        for (TreeNode root : roots) {
            collectLeaves(root, out, limit);
            if (out.size() >= limit) break;
        }
        return out;
    }

    private static void collectLeaves(TreeNode node, List<SearchNode> out, int limit) {
        if (out.size() >= limit) return;
        if (node.isLeaf()) {
            out.add(node.getEntry());
            return;
        }
        for (TreeNode child : node.getChildren()) {
            collectLeaves(child, out, limit);
            if (out.size() >= limit) return;
        }
    }

    private static JsonObject nodeSummary(SearchNode node) {
        JsonObject out = new JsonObject();
        out.addProperty("id", node.id().toString());
        out.addProperty("type", node.type().name());
        out.addProperty("name", node.displayName());
        out.addProperty("mod", node.meta(SearchNodeKeys.MOD_ID, ""));
        out.addProperty("category", node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""));
        out.addProperty("subcategory", node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
        out.addProperty("family", node.meta(SearchNodeKeys.COMPAT_FAMILY, ""));
        out.addProperty("accessLevel", node.meta(SearchNodeKeys.ACCESS_LEVEL, ""));
        out.add("metadata", metadataSummary(node.metadata()));
        return out;
    }

    private static JsonObject metadataSummary(Map<String, String> metadata) {
        JsonObject out = new JsonObject();
        for (var entry : new TreeMap<>(metadata).entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                out.addProperty(entry.getKey(), value);
            }
        }
        return out;
    }

    private static JsonArray treeSummaries(List<TreeNode> roots, int limit) {
        JsonArray out = new JsonArray();
        TreeBudget budget = new TreeBudget(limit);
        for (TreeNode root : roots) {
            if (!budget.hasRemaining()) break;
            out.add(treeSummary(root, 0, budget));
        }
        return out;
    }

    private static JsonObject treeSummary(TreeNode node, int depth, TreeBudget budget) {
        budget.use();
        JsonObject out = new JsonObject();
        out.addProperty("label", node.getLabel().getString());
        out.addProperty("leaf", node.isLeaf());
        if (node.getKey() != null) {
            out.addProperty("key", node.getKey());
        }
        if (node.isLeaf()) {
            out.add("node", nodeSummary(node.getEntry()));
        } else {
            out.addProperty("expanded", node.isExpanded());
            out.addProperty("highCardinality", node.isHighCardinality());
            out.addProperty("childCount", node.getChildren().size());
            JsonArray children = new JsonArray();
            if (depth < MAX_TREE_DEPTH) {
                for (TreeNode child : node.getChildren()) {
                    if (!budget.hasRemaining()) break;
                    children.add(treeSummary(child, depth + 1, budget));
                }
            }
            out.add("children", children);
        }
        return out;
    }

    private static JsonArray guideSummaries(List<GuideResultRow> rows, int limit) {
        JsonArray out = new JsonArray();
        for (GuideResultRow row : rows.stream().limit(limit).toList()) {
            JsonObject json = new JsonObject();
            json.addProperty("title", row.title());
            json.addProperty("sourceLine", row.sourceLine());
            json.addProperty("provenanceLine", row.provenanceLine());
            json.addProperty("referencedItemCount", row.referencedItemCount());
            out.add(json);
        }
        return out;
    }

    private static JsonArray questSummaries(List<QuestResultRow> rows, int limit) {
        JsonArray out = new JsonArray();
        for (QuestResultRow row : rows.stream().limit(limit).toList()) {
            JsonObject json = new JsonObject();
            json.addProperty("title", row.title());
            json.addProperty("sourceLine", row.sourceLine());
            json.addProperty("provenanceLine", row.provenanceLine());
            json.addProperty("requirementCount", row.requirementCount());
            json.addProperty("rewardCount", row.rewardCount());
            out.add(json);
        }
        return out;
    }

    private static JsonArray advancementSummaries(List<AdvancementResultRow> rows, int limit) {
        JsonArray out = new JsonArray();
        for (AdvancementResultRow row : rows.stream().limit(limit).toList()) {
            JsonObject json = new JsonObject();
            json.addProperty("title", row.title());
            json.addProperty("sourceLine", row.sourceLine());
            json.addProperty("provenanceLine", row.provenanceLine());
            out.add(json);
        }
        return out;
    }

    private static final class TreeBudget {
        private int remaining;

        private TreeBudget(int remaining) {
            this.remaining = Math.max(1, remaining);
        }

        private boolean hasRemaining() {
            return remaining > 0;
        }

        private void use() {
            remaining--;
        }
    }
}
