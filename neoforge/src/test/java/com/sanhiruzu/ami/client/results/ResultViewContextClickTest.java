package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultViewContextClickTest {
    @Test
    void treeViewRightClickLeafDispatchesItemCallback() {
        ResultsTreeView treeView = new ResultsTreeView(0, 0, 200, 200);
        SearchNode item = item("stone", "Stone");
        AtomicReference<SearchNode> clicked = new AtomicReference<>();
        AtomicReference<Integer> button = new AtomicReference<>();
        treeView.setItemClickCallback((node, b) -> {
            clicked.set(node);
            button.set(b);
        });
        treeView.setRootNodes(List.of(new TreeNode(Component.literal("Stone"), item)));

        assertTrue(treeView.mouseClicked(10, 5, 1));

        assertEquals(item, clicked.get());
        assertEquals(1, button.get());
    }

    @Test
    void treeViewRightClickGroupDispatchesGroupCallbackWithoutToggling() {
        ResultsTreeView treeView = new ResultsTreeView(0, 0, 200, 200);
        TreeNode group = new TreeNode("building", Component.literal("Building"));
        AtomicReference<TreeNode> clicked = new AtomicReference<>();
        treeView.setGroupClickCallback((node, button) -> clicked.set(node));
        treeView.setRootNodes(List.of(group));

        assertTrue(treeView.mouseClicked(10, 5, 1));

        assertEquals(group, clicked.get());
        assertFalse(group.isExpanded());
    }

    @Test
    void gridViewRightClickHighCardinalityCardDispatchesGroupCallbackWithoutToggling() {
        ItemGridView gridView = new ItemGridView(0, 0, 61, 100);
        TreeNode group = new TreeNode("cardinality:minecraft:disc", Component.literal("Discs"));
        group.setHighCardinality(true);
        group.addChild(new TreeNode(Component.literal("Disc"), item("music_disc_13", "Disc")));
        AtomicReference<TreeNode> clicked = new AtomicReference<>();
        gridView.setGroupClickCallback((node, button) -> clicked.set(node));
        gridView.setRootNodes(List.of(group));

        assertTrue(gridView.mouseClicked(5, 5, 1));

        assertEquals("Discs", clicked.get().getLabel().getString());
        assertFalse(clicked.get().isExpanded());
    }

    @Test
    void gridViewExpandedHighCardinalityHeaderTogglesLikeAGroupRow() {
        ItemGridView gridView = new ItemGridView(0, 0, 61, 100);
        TreeNode group = new TreeNode("cardinality:minecraft:disc", Component.literal("Discs"));
        group.setHighCardinality(true);
        group.addChild(new TreeNode(Component.literal("Disc"), item("music_disc_13", "Disc")));
        gridView.setRootNodes(List.of(group));
        gridView.expandAll();

        assertTrue(gridView.getRootNodes().getFirst().isExpanded());
        assertTrue(gridView.mouseClicked(5, 5, 0));

        assertFalse(gridView.getRootNodes().getFirst().isExpanded());
    }

    private static SearchNode item(String path, String name) {
        return new SearchNode(
                new Identifier("minecraft:" + path),
                NodeType.ITEM,
                name,
                0,
                0,
                Map.of()
        );
    }
}
