package com.sanhiruzu.ami.index;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton index replacing both AMIIndex and WorldAtlasIndex.
 * Holds a unified list of SearchNode objects, organized by NodeType.
 */
public class GlobalIndex {
    private static final GlobalIndex INSTANCE = new GlobalIndex();

    private final Map<NodeType, List<SearchNode>> nodes = new EnumMap<>(NodeType.class);
    private final Set<NodeType> loadingTypes = EnumSet.noneOf(NodeType.class);
    private volatile boolean indexReady = false;
    private long indexBuildTimeMs;

    private GlobalIndex() {
        for (NodeType t : NodeType.values()) {
            nodes.put(t, new ArrayList<>());
        }
    }

    public static GlobalIndex getInstance() {
        return INSTANCE;
    }

    public void addNode(SearchNode node) {
        nodes.get(node.type()).add(node);
    }

    public List<SearchNode> getNodes(NodeType type) {
        return Collections.unmodifiableList(nodes.get(type));
    }

    /**
     * Replace all nodes of a given type. Used for deferred/retry population.
     */
    public void replaceNodes(NodeType type, List<SearchNode> newNodes) {
        List<SearchNode> list = nodes.get(type);
        list.clear();
        list.addAll(newNodes);
    }

    public void clear() {
        nodes.values().forEach(List::clear);
        loadingTypes.clear();
        indexReady = false;
        indexBuildTimeMs = 0;
    }

    public void markIndexReady() {
        this.indexReady = true;
    }

    public boolean isIndexReady() {
        return indexReady;
    }

    public void setLoading(NodeType type, boolean loading) {
        if (loading) {
            loadingTypes.add(type);
        } else {
            loadingTypes.remove(type);
        }
    }

    public boolean isLoading(NodeType type) {
        return loadingTypes.contains(type);
    }

    public void setIndexBuildTime(long ms) {
        indexBuildTimeMs = ms;
    }

    public long getIndexBuildTimeMs() {
        return indexBuildTimeMs;
    }

    /**
     * Returns items grouped by a metadata key — replaces AMIIndex.getCategoryIndex().
     * Example: getGrouped(NodeType.ITEM, SearchNodeKeys.MOD_ID) → map from modId to entries.
     */
    public Map<String, List<SearchNode>> getGrouped(NodeType type, String metadataKey) {
        return getNodes(type).stream()
                .collect(Collectors.groupingBy(
                        n -> n.meta(metadataKey, "unknown"),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }
}
