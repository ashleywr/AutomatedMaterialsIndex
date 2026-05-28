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

    @Test
    void collapsedGroupsPackInlineWithoutLeavingSparseRows() throws Exception {
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

    private static int itemCount(Object row) throws Exception {
        Method items = row.getClass().getDeclaredMethod("items");
        items.setAccessible(true);
        return ((List<?>) items.invoke(row)).size();
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
}
