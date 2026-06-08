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
public final class ItemDeletionHandler {
    private static final Logger LOGGER = Logger.getLogger(ItemDeletionHandler.class.getName());

    private ItemDeletionHandler() {
    }

    /**
     * Handle a deleted item. Called by AmiApi.notifyItemDeleted().
     * Marks the item as deleted, refreshes results while preserving UI state.
     */
    public static void handleItemDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return;

        LOGGER.log(Level.INFO, "ItemDeletionHandler.handleItemDeleted: " + nodeId);
        try {
            // Mark for soft and hard deletion
            LOGGER.log(Level.INFO, "  - Marking soft deleted");
            SoftDeleteTracker.markSoftDeleted(nodeId);
            LOGGER.log(Level.INFO, "  - Marking hard deleted");
            DeletedSearchNodesTracker.markDeleted(nodeId);
            LOGGER.log(Level.INFO, "  - Calling refreshOverlayResults");

            // Refresh with state preservation
            InventoryOverlayHandler.refreshOverlayResults();

            LOGGER.log(Level.INFO, "ItemDeletionHandler: Item deleted successfully: " + nodeId);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "ItemDeletionHandler: Failed to handle item deletion: " + nodeId, e);
        }
    }
}
