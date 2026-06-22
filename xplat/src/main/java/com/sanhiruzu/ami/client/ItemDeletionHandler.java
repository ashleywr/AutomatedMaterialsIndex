package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.client.results.DeletedSearchNodesTracker;
import com.sanhiruzu.ami.client.results.SoftDeleteTracker;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal handler for item deletion notifications from plugins.
 * Handles seamless deletion: mark deleted, remove from results, refresh display.
 */
public final class ItemDeletionHandler {
    private static final Logger LOGGER = Logger.getLogger(ItemDeletionHandler.class.getName());

    private ItemDeletionHandler() {
    }

    /**
     * Handle a deleted item. Called by AmiApi.notifyItemDeleted().
     * Seamlessly removes the item from all visible displays.
     */
    public static void handleItemDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return;

        LOGGER.log(Level.INFO, "ItemDeletionHandler.handleItemDeleted: " + nodeId);
        try {
            // Mark for soft deletion (instant hiding via filter)
            LOGGER.log(Level.INFO, "  - Marking soft/hard deleted");
            SoftDeleteTracker.markSoftDeleted(nodeId);
            DeletedSearchNodesTracker.markDeleted(nodeId);

            // Remove from global index permanently
            LOGGER.log(Level.INFO, "  - Removing from global index");
            GlobalIndex index = GlobalIndex.getInstance();
            for (NodeType type : NodeType.values()) {
                index.removeNode(nodeId, type);
            }

            // Seamlessly remove from visible result panels
            LOGGER.log(Level.INFO, "  - Removing from visible result panels");
            removeFromVisiblePanels(nodeId);

            LOGGER.log(Level.INFO, "ItemDeletionHandler: Item deleted successfully: " + nodeId);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "ItemDeletionHandler: Failed to handle item deletion: " + nodeId, e);
        }
    }

    /**
     * Remove the deleted item from all visible result panel displays.
     * This is seamless - just removes the item without a full refresh.
     */
    private static void removeFromVisiblePanels(ResourceLocation nodeId) {
        try {
            OverlayWidgetManager manager = InventoryOverlayHandler.getManager();
            if (manager == null) return;

            // Get all visible result panels (main, sidebar, favorites, etc)
            List<UniversalResultsPanel> panels = manager.getDebugVisibleResultPanels();
            for (UniversalResultsPanel panel : panels) {
                removeFromPanel(panel, nodeId);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "Failed to remove from visible panels", e);
        }
    }

    /**
     * Remove the deleted SearchNode from a single result panel's display.
     */
    private static void removeFromPanel(UniversalResultsPanel panel, ResourceLocation nodeId) {
        try {
            // Use reflection to access currentResults
            var currentResultsField = UniversalResultsPanel.class.getDeclaredField("currentResults");
            currentResultsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<SearchNode> currentResults = (List<SearchNode>) currentResultsField.get(panel);

            // Remove matching entries by ID
            boolean removed = currentResults.removeIf(n -> n.id().equals(nodeId));
            if (removed) {
                LOGGER.log(Level.INFO, "  - Removed from panel results");
                // Refresh the tree display from the updated currentResults
                var refreshTreeMethod = UniversalResultsPanel.class.getDeclaredMethod("refreshTree");
                refreshTreeMethod.setAccessible(true);
                refreshTreeMethod.invoke(panel);
                LOGGER.log(Level.INFO, "  - Refreshed tree display");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "Failed to remove from panel", e);
        }
    }
}
