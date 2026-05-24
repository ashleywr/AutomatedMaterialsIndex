package com.sanhiruzu.ami.client.results;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that clicking a group header in the list view correctly
 * toggles expansion state.
 */
public class ResultsTreeViewDebounceTest {

    @Test
    void leftClickGroupHeaderTogglesExpansion() {
        var treeView = new ResultsTreeView(0, 0, 200, 200);

        TreeNode group = new TreeNode("test_group", Component.literal("Test Group"));
        group.setExpanded(false);
        treeView.setRootNodes(List.of(group));

        // Click row 0 (group header): mouseY=5 maps to row 0
        boolean handled = treeView.mouseClicked(10, 5, 0);
        assertTrue(handled, "click on group row should be handled");
        assertTrue(group.isExpanded(), "group should be expanded after click");

        // Click again: should collapse
        handled = treeView.mouseClicked(10, 5, 0);
        assertTrue(handled, "second click on group row should be handled");
        assertFalse(group.isExpanded(), "group should be collapsed after second click");
    }

    @Test
    void rightClickGroupHeaderDoesNotToggleExpansion() {
        var treeView = new ResultsTreeView(0, 0, 200, 200);

        TreeNode group = new TreeNode("test_group", Component.literal("Test Group"));
        group.setExpanded(false);
        treeView.setRootNodes(List.of(group));

        // Right click group header: should not toggle expand (only left click does)
        boolean handled = treeView.mouseClicked(10, 5, 1);
        assertTrue(handled, "right click on group row should be handled");
        assertFalse(group.isExpanded(), "group should remain collapsed after right click");
    }

    @Test
    void clickOutsideVisibleRowsReturnsFalse() {
        var treeView = new ResultsTreeView(0, 0, 200, 200);

        TreeNode group = new TreeNode("test_group", Component.literal("Test Group"));
        group.setExpanded(false);
        treeView.setRootNodes(List.of(group));

        // Click far outside the visible area
        boolean handled = treeView.mouseClicked(10, -100, 0);
        assertFalse(handled, "click outside visible rows should return false");
        assertFalse(group.isExpanded(), "group should remain collapsed");
    }
}
