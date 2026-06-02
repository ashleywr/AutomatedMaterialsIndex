package com.sanhiruzu.ami.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sanhiruzu.ami.client.results.GuideResultRow;
import com.sanhiruzu.ami.client.results.ListLens;
import com.sanhiruzu.ami.client.results.QuestResultRow;
import com.sanhiruzu.ami.client.results.ResultsToolbar;
import com.sanhiruzu.ami.client.results.ResultsTreeShapeDump;
import com.sanhiruzu.ami.client.results.ResultsViewProjector;
import com.sanhiruzu.ami.client.results.SearchState;
import com.sanhiruzu.ami.client.results.TreeNode;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                AmiIndexerService.getInstance().getGuideSearchIndex(),
                AmiQuestsApi.getQuestSearchIndex(),
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
        out.addProperty("summary", projection.summary());
        out.add("sourceTypeCounts", typeCounts(source));
        out.add("displayedTypeCounts", typeCounts(displayedLeaves));
        out.add("results", leafSummaries(displayedLeaves, boundedLimit));
        out.add("roots", treeSummaries(projection.roots(), boundedLimit));
        out.add("guides", guideSummaries(projection.guideRows(), boundedLimit));
        out.add("quests", questSummaries(projection.questRows(), boundedLimit));
        out.addProperty("treeDump", ResultsTreeShapeDump.dumpTree(projection.roots()));
        return GSON.toJson(out);
    }

    private static JsonObject baseStatus() {
        AmiIndexerService indexer = AmiIndexerService.getInstance();
        GlobalIndex index = GlobalIndex.getInstance();
        JsonObject out = new JsonObject();
        out.addProperty("ready", indexer.isReady());
        out.addProperty("indexReady", index.isIndexReady());
        out.addProperty("revision", index.revision());
        out.addProperty("indexBuildTimeMs", index.getIndexBuildTimeMs());
        out.addProperty("indexedItemCount", indexer.indexedItemCount());
        Throwable failure = indexer.getLastRebuildFailure();
        if (failure != null) {
            out.addProperty("lastRebuildFailure", failure.getClass().getName() + ": " + failure.getMessage());
        }
        return out;
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
        out.addProperty("modularGearKind", node.meta(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        out.addProperty("modularGearMaterial", node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL, ""));
        out.addProperty("modularGearMaterialTraits", node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, ""));
        out.addProperty("modularGearRuntimeTraits", node.meta(SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, ""));
        out.addProperty("accessLevel", node.meta(SearchNodeKeys.ACCESS_LEVEL, ""));
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
