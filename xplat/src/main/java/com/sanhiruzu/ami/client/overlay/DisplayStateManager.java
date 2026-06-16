package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.client.results.SearchState;
import com.sanhiruzu.ami.client.results.TreeNode;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages saving and restoring overlay display state (expansion, selection, scroll, mode, filters).
 * Used to preserve UX during operations like deletion or refresh.
 */
public final class DisplayStateManager {
    private static DisplayState cachedState = null;

    private DisplayStateManager() {
    }

    /**
     * Save the current display state from the overlay manager.
     * Should be called before operations that will refresh the results.
     */
    public static void saveState(OverlayWidgetManager manager) {
        if (manager == null) {
            cachedState = null;
            return;
        }

        // Get the visible result panels
        List<UniversalResultsPanel> panels = manager.getDebugVisibleResultPanels();
        if (panels.isEmpty()) {
            cachedState = null;
            return;
        }

        // Capture state from the first visible panel (primary results)
        UniversalResultsPanel panel = panels.get(0);
        SearchState searchState = panel.getState();

        // Capture expanded nodes
        Set<String> expandedNodeIds = captureExpandedNodes(panel);

        // Capture scroll position (get from tree view via reflection/public method if available)
        double scrollY = 0; // TODO: Get actual scroll position from panel's tree view

        // Create and cache the state
        cachedState = new DisplayState(
                expandedNodeIds,
                null, // TODO: Get selected node if available
                scrollY,
                searchState.getViewMode().toString(),
                searchState.getActiveFacets()
        );
    }

    /**
     * Walk the tree and collect all expanded node keys.
     */
    private static Set<String> captureExpandedNodes(UniversalResultsPanel panel) {
        Set<String> expanded = new HashSet<>();
        try {
            // Use reflection to access the tree view and walk the nodes
            var treeViewField = UniversalResultsPanel.class.getDeclaredField("treeView");
            treeViewField.setAccessible(true);
            Object treeView = treeViewField.get(panel);

            if (treeView != null) {
                var getRootNodesMethod = treeView.getClass().getMethod("getRootNodes");
                @SuppressWarnings("unchecked")
                List<TreeNode> rootNodes = (List<TreeNode>) getRootNodesMethod.invoke(treeView);
                collectExpandedKeys(rootNodes, expanded);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Silently fail - state capture is not critical
        }
        return expanded;
    }

    /**
     * Recursively collect keys of all expanded nodes.
     */
    private static void collectExpandedKeys(List<TreeNode> nodes, Set<String> expanded) {
        for (TreeNode node : nodes) {
            if (node.isExpanded() && node.getKey() != null) {
                expanded.add(node.getKey());
            }
            if (!node.isLeaf() && !node.getChildren().isEmpty()) {
                collectExpandedKeys(node.getChildren(), expanded);
            }
        }
    }

    /**
     * Restore the previously saved display state to the overlay manager.
     * Should be called after operations that refreshed the results.
     */
    public static void restoreState(OverlayWidgetManager manager) {
        if (manager == null || cachedState == null) {
            cachedState = null;
            return;
        }

        // Get the visible result panels
        List<UniversalResultsPanel> panels = manager.getDebugVisibleResultPanels();
        if (panels.isEmpty()) {
            cachedState = null;
            return;
        }

        // Restore to the first visible panel
        UniversalResultsPanel panel = panels.get(0);
        SearchState searchState = panel.getState();

        // Restore active facets
        Set<String> currentFacets = searchState.getActiveFacets();
        for (String facet : cachedState.activeFacets()) {
            if (!currentFacets.contains(facet)) {
                searchState.toggleFacet(facet);
            }
        }
        for (String facet : currentFacets) {
            if (!cachedState.activeFacets().contains(facet)) {
                searchState.toggleFacet(facet);
            }
        }

        // Restore tree expansion state
        restoreExpandedNodes(panel, cachedState.expandedNodeIds());

        // TODO: Restore scroll position if we capture it

        cachedState = null;
    }

    /**
     * Restore expansion state to match the saved set of expanded node keys.
     */
    private static void restoreExpandedNodes(UniversalResultsPanel panel, Set<String> expandedKeys) {
        try {
            // Use reflection to access the tree view
            var treeViewField = UniversalResultsPanel.class.getDeclaredField("treeView");
            treeViewField.setAccessible(true);
            Object treeView = treeViewField.get(panel);

            if (treeView != null) {
                var getRootNodesMethod = treeView.getClass().getMethod("getRootNodes");
                @SuppressWarnings("unchecked")
                List<TreeNode> rootNodes = (List<TreeNode>) getRootNodesMethod.invoke(treeView);
                restoreNodeExpansion(rootNodes, expandedKeys);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Silently fail - state restoration is not critical
        }
    }

    /**
     * Recursively restore expansion state to match the saved keys.
     */
    private static void restoreNodeExpansion(List<TreeNode> nodes, Set<String> expandedKeys) {
        for (TreeNode node : nodes) {
            if (node.getKey() != null) {
                boolean shouldExpand = expandedKeys.contains(node.getKey());
                node.setExpanded(shouldExpand);
            }
            if (!node.isLeaf() && !node.getChildren().isEmpty()) {
                restoreNodeExpansion(node.getChildren(), expandedKeys);
            }
        }
    }

    /**
     * Clear the cached state.
     */
    public static void clear() {
        cachedState = null;
    }
}
