package com.sanhiruzu.ami.client.results;

import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks search nodes that have been marked for deletion/hiding, allowing instant
 * removal from search results without waiting for index updates. Entries automatically
 * expire after 10 seconds to prevent memory leaks.
 */
public final class DeletedSearchNodesTracker {
    private static final long EXPIRY_MS = 10_000L;
    private static final CopyOnWriteArrayList<DeletedNodeEntry> deletedNodes = new CopyOnWriteArrayList<>();

    private DeletedSearchNodesTracker() {
    }

    /**
     * Mark a search node as deleted, removing it from results instantly.
     * The deletion is temporary and expires after 10 seconds.
     */
    public static void markDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return;
        cleanExpiredEntries();
        deletedNodes.removeIf(entry -> entry.nodeId.equals(nodeId));
        deletedNodes.add(new DeletedNodeEntry(nodeId, System.currentTimeMillis()));
    }

    /**
     * Check if a node is currently marked as deleted.
     */
    public static boolean isDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return false;
        cleanExpiredEntries();
        return deletedNodes.stream().anyMatch(entry -> entry.nodeId.equals(nodeId));
    }

    /**
     * Clear all deleted node entries.
     */
    public static void clear() {
        deletedNodes.clear();
    }

    private static void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        deletedNodes.removeIf(entry -> now - entry.createdAtMs > EXPIRY_MS);
    }

    private static class DeletedNodeEntry {
        final ResourceLocation nodeId;
        final long createdAtMs;

        DeletedNodeEntry(ResourceLocation nodeId, long createdAtMs) {
            this.nodeId = nodeId;
            this.createdAtMs = createdAtMs;
        }
    }
}
