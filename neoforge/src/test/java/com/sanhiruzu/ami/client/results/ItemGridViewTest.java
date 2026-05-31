package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemGridViewTest {

    private static int itemCount(Object row) throws Exception {
        Method items = row.getClass().getDeclaredMethod("items");
        items.setAccessible(true);
        return ((List<?>) items.invoke(row)).size();
    }

    private static String headerLabel(Object row) throws Exception {
        Method node = row.getClass().getDeclaredMethod("node");
        node.setAccessible(true);
        return ((TreeNode) node.invoke(row)).getLabel().getString();
    }

    private static int itemRowDepth(Object row) throws Exception {
        Method depth = row.getClass().getDeclaredMethod("depth");
        depth.setAccessible(true);
        return (int) depth.invoke(row);
    }

    private static TreeNode leaf(String path, String displayName) {
        SearchNode node = new SearchNode(
                new ResourceLocation("minecraft:" + path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                Map.of()
        );
        return new TreeNode(Component.literal(displayName), node);
    }

    @Test
    void collapsedGroupsStayAsInlineGridCards() throws Exception {
        ItemGridView gridView = new ItemGridView(0, 0, 61, 100);

        TreeNode leafA = leaf("stone", "Stone");
        TreeNode leafB = leaf("dirt", "Dirt");
        TreeNode leafC = leaf("sand", "Sand");

        TreeNode collapsed = new TreeNode("cardinality:family:music_discs", Component.literal("Music Discs"));
        collapsed.setHighCardinality(true);
        collapsed.addChild(leaf("music_disc_13", "Music Disc 13"));

        gridView.setRootNodes(List.of(leafA, leafB, collapsed, leafC));

        Method buildVirtualRows = ItemGridView.class.getDeclaredMethod("buildVirtualRows", int.class);
        buildVirtualRows.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) buildVirtualRows.invoke(gridView, 3);

        assertEquals(2, rows.size());
        assertEquals(3, itemCount(rows.get(0)));
        assertEquals(1, itemCount(rows.get(1)));
    }

    @Test
    void expandAllExpandsHighCardinalityCardsInline() throws Exception {
        ItemGridView gridView = new ItemGridView(0, 0, 61, 100);

        TreeNode collapsed = new TreeNode("cardinality:family:music_discs", Component.literal("Music Discs"));
        collapsed.setHighCardinality(true);
        collapsed.addChild(leaf("music_disc_13", "Music Disc 13"));
        collapsed.addChild(leaf("music_disc_cat", "Music Disc Cat"));

        gridView.setRootNodes(List.of(collapsed));
        gridView.expandAll();

        Method buildVirtualRows = ItemGridView.class.getDeclaredMethod("buildVirtualRows", int.class);
        buildVirtualRows.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) buildVirtualRows.invoke(gridView, 3);

        assertEquals(1, rows.size());
        assertEquals(3, itemCount(rows.get(0)));
    }

    @Test
    void expandedGroupItemsExpandInPlaceInTheGrid() throws Exception {
        ItemGridView gridView = new ItemGridView(0, 0, 61, 100);

        TreeNode group = new TreeNode("cardinality:minecraft:mushroom", Component.literal("Mushrooms"));
        group.setHighCardinality(true);
        group.setExpanded(true);
        group.addChild(leaf("red_mushroom", "Red Mushroom"));
        group.addChild(leaf("brown_mushroom", "Brown Mushroom"));

        gridView.setRootNodes(List.of(group, leaf("apple", "Apple")));
        gridView.expandAll();

        Method buildVirtualRows = ItemGridView.class.getDeclaredMethod("buildVirtualRows", int.class);
        buildVirtualRows.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) buildVirtualRows.invoke(gridView, 3);

        assertEquals(2, rows.size());
        assertEquals(3, itemCount(rows.get(0)));
        assertEquals(1, itemCount(rows.get(1)));
    }

    @Test
    void mixedGroupChildrenRenderLooseItemsBeforeSubheaders() throws Exception {
        ItemGridView gridView = new ItemGridView(0, 0, 61, 100);

        TreeNode furniture = new TreeNode("decoration/furniture", Component.literal("Furniture"));
        furniture.setExpanded(true);
        furniture.addChild(leaf("loose_tuff", "Loose Tuff"));

        TreeNode oak = new TreeNode("cardinality:minecraft:oak", Component.literal("Oak"));
        oak.setExpanded(false);
        oak.addChild(leaf("oak_table", "Oak Table"));
        furniture.addChild(oak);
        furniture.addChild(leaf("loose_bamboo", "Loose Bamboo"));

        TreeNode spruce = new TreeNode("cardinality:minecraft:spruce", Component.literal("Spruce"));
        spruce.setExpanded(false);
        spruce.addChild(leaf("spruce_table", "Spruce Table"));
        furniture.addChild(spruce);

        gridView.setRootNodes(List.of(furniture));

        Method buildVirtualRows = ItemGridView.class.getDeclaredMethod("buildVirtualRows", int.class);
        buildVirtualRows.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) buildVirtualRows.invoke(gridView, 3);

        assertEquals("Furniture", headerLabel(rows.get(0)));
        assertEquals(2, itemCount(rows.get(1)));
        assertEquals(1, itemRowDepth(rows.get(1)));
        assertEquals("Oak", headerLabel(rows.get(2)));
        assertEquals("Spruce", headerLabel(rows.get(3)));
    }
}
