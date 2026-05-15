package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.*;

/**
 * Mutable SearchNode that supports lazy, non-blocking edge resolution.
 */
public class SearchNode {
    private final ResourceLocation id;
    private final NodeType type; // ITEM, BIOME, MOB
    private final String displayName;
    private final int color;
    private final int searchWeight;
    private final Map<String, String> metadata;

    // Unresolved edges are stored as target ResourceLocation ids. Thread-safe collections.
    private final ConcurrentMap<EdgeType, CopyOnWriteArrayList<ResourceLocation>> unresolvedEdges = new ConcurrentHashMap<>();
    // Resolved edges cache
    private final ConcurrentMap<EdgeType, CopyOnWriteArrayList<SearchNode>> resolvedEdges = new ConcurrentHashMap<>();

    public SearchNode(ResourceLocation id, NodeType type, String displayName, int color, int searchWeight, Map<String, String> metadata) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.color = color;
        this.searchWeight = searchWeight;
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }

    public ResourceLocation id() { return id; }
    public NodeType type() { return type; }
    public String displayName() { return displayName; }
    public int color() { return color; }
    public int searchWeight() { return searchWeight; }
    public Map<String, String> metadata() { return Collections.unmodifiableMap(metadata); }

    /**
     * Convenience: read a metadata key with a default.
     */
    public String meta(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    public String meta(String key) {
        return meta(key, "");
    }

    // Edge API
    public void addUnresolvedEdge(EdgeType type, ResourceLocation target) {
        unresolvedEdges.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(target);
    }

    public List<ResourceLocation> getUnresolvedEdgeIds(EdgeType type) {
        var list = unresolvedEdges.get(type);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public void addResolvedEdge(EdgeType type, SearchNode node) {
        resolvedEdges.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(node);
    }

    /**
     * Get resolved edges for a given type. Attempts a quick synchronous resolution using GlobalIndex,
     * and if nothing is found schedules an asynchronous resolution so subsequent calls won't block.
     * This method never blocks the caller waiting on I/O.
     */
    public List<SearchNode> getEdges(EdgeType type) {
        CopyOnWriteArrayList<SearchNode> cached = resolvedEdges.get(type);
        if (cached != null && !cached.isEmpty()) return Collections.unmodifiableList(cached);

        var ids = unresolvedEdges.get(type);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        GlobalIndex gi = GlobalIndex.getInstance();
        List<SearchNode> resolvedNow = new ArrayList<>();
        for (ResourceLocation rid : ids) {
            gi.getNode(rid).ifPresent(resolvedNow::add);
        }

        if (!resolvedNow.isEmpty()) {
            resolvedEdges.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).addAll(resolvedNow);
            return Collections.unmodifiableList(resolvedEdges.get(type));
        }

        // Schedule asynchronous resolution for later (non-blocking)
        CompletableFuture.runAsync(() -> {
            List<SearchNode> batch = new ArrayList<>();
            for (ResourceLocation rid : ids) {
                gi.getNode(rid).ifPresent(batch::add);
            }
            if (!batch.isEmpty()) {
                resolvedEdges.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).addAll(batch);
            }
        });

        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchNode)) return false;
        SearchNode that = (SearchNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
