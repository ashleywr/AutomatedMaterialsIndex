package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.results.DeletedSearchNodesTracker;
import com.sanhiruzu.ami.client.results.SoftDeleteTracker;
import net.minecraft.resources.ResourceLocation;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal handler for item deletion notifications from plugins.
 * Handles the full flow: mark deleted, save UI state, refresh, restore state.
 */
final class ItemDeletionHandler {
    private static final Logger LOGGER = Logger.getLogger(ItemDeletionHandler.class.getName());

    private ItemDeletionHandler() {
    }

    /**
     * Handle a deleted item. Called by AmiApi.notifyItemDeleted().
     * Marks the item as deleted, refreshes results while preserving UI state.
     */
    static void handleItemDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return;

        try {
            // Mark for soft and hard deletion
            SoftDeleteTracker.markSoftDeleted(nodeId);
            DeletedSearchNodesTracker.markDeleted(nodeId);

            // Refresh with state preservation
            InventoryOverlayHandler.refreshOverlayResults();

            LOGGER.log(Level.FINE, "AMI: Item deleted: " + nodeId);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to handle item deletion", e);
        }
    }
}
