package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Singleton index replacing both AMIIndex and WorldAtlasIndex.
 * Holds a unified list of SearchNode objects, organized by NodeType.
 */
public class GlobalIndex {
    private static final GlobalIndex INSTANCE = new GlobalIndex();

    private final Map<NodeType, List<SearchNode>> nodes = new EnumMap<>(NodeType.class);
    private final Set<NodeType> loadingTypes = EnumSet.noneOf(NodeType.class);
    private final ConcurrentMap<NodeKey, SearchNode> idIndex = new ConcurrentHashMap<>();
    private final AtomicLong revision = new AtomicLong();
    // Category index for fast dashboard lookups
    private final Map<String, List<SearchNode>> categoryIndex = new ConcurrentHashMap<>();
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

    public synchronized void addNode(SearchNode node) {
        NodeKey key = new NodeKey(node.id(), node.type());
        SearchNode existing = idIndex.get(key);
        if (existing != null) {
            removeNodeFromCollections(existing);
        }

        nodes.get(node.type()).add(node);
        idIndex.put(key, node);
        addNodeToCategoryIndex(node);
        revision.incrementAndGet();
    }

    private void addNodeToCategoryIndex(SearchNode node) {
        // Index by ontology category
        String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        if (category.isEmpty()) {
            category = AmiOntology.classifyNode(node).id;
        }
        categoryIndex.computeIfAbsent(category, k -> Collections.synchronizedList(new ArrayList<>())).add(node);
    }

    private void removeNodeFromCollections(SearchNode node) {
        nodes.get(node.type()).remove(node);
        String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        if (category.isEmpty()) category = AmiOntology.classifyNode(node).id;
        List<SearchNode> categoryNodes = categoryIndex.get(category);
        if (categoryNodes != null) {
            categoryNodes.remove(node);
            if (categoryNodes.isEmpty()) {
                categoryIndex.remove(category);
            }
        }
    }

    public Optional<SearchNode> getNode(ResourceLocation id) {
        // Fallback to searching all types if type is unknown, prioritizing ITEM then ENTITY
        SearchNode match = idIndex.get(new NodeKey(id, NodeType.ITEM));
        if (match != null) return Optional.of(match);
        match = idIndex.get(new NodeKey(id, NodeType.ENTITY));
        if (match != null) return Optional.of(match);

        for (NodeType type : NodeType.values()) {
            match = idIndex.get(new NodeKey(id, type));
            if (match != null) return Optional.of(match);
        }
        return Optional.empty();
    }

    public Optional<SearchNode> getNode(ResourceLocation id, NodeType type) {
        return Optional.ofNullable(idIndex.get(new NodeKey(id, type)));
    }

    public synchronized List<SearchNode> getNodes(NodeType type) {
        return List.copyOf(nodes.get(type));
    }

    public synchronized List<SearchNode> getNodesByCategory(String categoryId) {
        return List.copyOf(categoryIndex.getOrDefault(categoryId, List.of()));
    }

    /**
     * Replace all nodes of a given type. Used for deferred/retry population.
     */
    public synchronized void replaceNodes(NodeType type, List<SearchNode> newNodes) {
        List<SearchNode> list = nodes.get(type);
        // Remove old entries from id index and category index
        for (SearchNode n : list) {
            idIndex.remove(new NodeKey(n.id(), n.type()));
            String cat = n.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
            if (cat.isEmpty()) cat = AmiOntology.classifyNode(n).id;
            List<SearchNode> catList = categoryIndex.get(cat);
            if (catList != null) catList.remove(n);
        }
        list.clear();
        list.addAll(newNodes);
        for (SearchNode n : newNodes) addNodeToIndices(n);
        revision.incrementAndGet();
    }

    /**
     * Replace a single node by id. If no existing node matches, adds the new one.
     */
    public synchronized void replaceNode(ResourceLocation id, NodeType type, SearchNode updated) {
        SearchNode old = idIndex.get(new NodeKey(id, type));
        if (old != null) {
            List<SearchNode> typeList = nodes.get(type);
            typeList.remove(old);
            idIndex.remove(new NodeKey(id, type));
            String cat = old.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
            if (cat.isEmpty()) cat = AmiOntology.classifyNode(old).id;
            List<SearchNode> catList = categoryIndex.get(cat);
            if (catList != null) catList.remove(old);
        }
        List<SearchNode> typeList = nodes.get(updated.type());
        typeList.add(updated);
        addNodeToIndices(updated);
        revision.incrementAndGet();
    }

    private void addNodeToIndices(SearchNode n) {
        idIndex.put(new NodeKey(n.id(), n.type()), n);
        String category = n.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        if (category.isEmpty()) {
            category = AmiOntology.classifyNode(n).id;
        }
        categoryIndex.computeIfAbsent(category, k -> Collections.synchronizedList(new ArrayList<>())).add(n);
    }

    public synchronized void clear() {
        nodes.values().forEach(List::clear);
        idIndex.clear();
        categoryIndex.clear();
        loadingTypes.clear();
        indexReady = false;
        indexBuildTimeMs = 0;
        revision.incrementAndGet();
    }

    public long revision() {
        return revision.get();
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

    // Fast lookup by ID + Type
    private record NodeKey(ResourceLocation id, NodeType type) {
    }
}
