package com.sanhiruzu.ami.client.results;

import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks nodes with pending soft deletion (visual fade-out before removal).
 * Shows visual feedback while preserving UI state, then removes from results
 * after a short delay.
 */
public final class SoftDeleteTracker {
    private static final long FADE_DURATION_MS = 1_000L;
    private static final CopyOnWriteArrayList<SoftDeleteEntry> softDeletedNodes = new CopyOnWriteArrayList<>();

    private SoftDeleteTracker() {
    }

    /**
     * Mark a node for soft deletion (visual fade, then removal).
     * Returns the fade progress (0.0 = full opacity, 1.0 = fully faded).
     */
    public static void markSoftDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return;
        cleanExpiredEntries();
        softDeletedNodes.removeIf(entry -> entry.nodeId.equals(nodeId));
        softDeletedNodes.add(new SoftDeleteEntry(nodeId, System.currentTimeMillis()));
    }

    /**
     * Get fade progress for a node (0.0 = visible, 1.0 = fully faded).
     * Returns 0.0 if node is not being soft-deleted.
     */
    public static float getFadeProgress(ResourceLocation nodeId) {
        if (nodeId == null) return 0.0f;
        cleanExpiredEntries();
        for (SoftDeleteEntry entry : softDeletedNodes) {
            if (entry.nodeId.equals(nodeId)) {
                long elapsed = System.currentTimeMillis() - entry.createdAtMs;
                return Math.min(1.0f, elapsed / (float) FADE_DURATION_MS);
            }
        }
        return 0.0f;
    }

    /**
     * Check if a node is currently soft-deleted and fully faded out.
     */
    public static boolean isFullyFaded(ResourceLocation nodeId) {
        return getFadeProgress(nodeId) >= 1.0f;
    }

    /**
     * Check if a node has any soft-delete fade in progress.
     */
    public static boolean isSoftDeleting(ResourceLocation nodeId) {
        if (nodeId == null) return false;
        cleanExpiredEntries();
        return softDeletedNodes.stream().anyMatch(entry -> entry.nodeId.equals(nodeId));
    }

    /**
     * Clear all soft-delete entries.
     */
    public static void clear() {
        softDeletedNodes.clear();
    }

    private static void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        softDeletedNodes.removeIf(entry -> now - entry.createdAtMs > FADE_DURATION_MS);
    }

    private static class SoftDeleteEntry {
        final ResourceLocation nodeId;
        final long createdAtMs;

        SoftDeleteEntry(ResourceLocation nodeId, long createdAtMs) {
            this.nodeId = nodeId;
            this.createdAtMs = createdAtMs;
        }
    }
}
