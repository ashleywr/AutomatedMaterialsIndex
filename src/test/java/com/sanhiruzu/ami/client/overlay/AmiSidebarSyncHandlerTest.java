package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmiSidebarSyncHandlerTest {

    @BeforeEach
    void setup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void testGetNodesForFavorites() {
        SearchNode node = new SearchNode(
                ResourceLocation.parse("minecraft:apple"),
                NodeType.ITEM,
                "Apple",
                0, 0, Map.of()
        );
        GlobalIndex.getInstance().addNode(node);
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().addFavorite(node);

        List<SearchNode> result = AmiSidebarSyncHandler.getNodesForContent(AmiConfig.PanelContent.FAVORITES);
        assertEquals(1, result.size());
        assertEquals("minecraft:apple", result.get(0).id().toString());
    }
}
