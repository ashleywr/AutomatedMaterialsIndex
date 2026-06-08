package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmiSidebarSyncHandlerTest {

    @BeforeEach
    void setup() {
        GlobalIndex.getInstance().clear();
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.disablePersistenceForTests();
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.clearForTests();
    }

    @Test
    void testGetNodesForFavorites() {
        ItemStack stack = new ItemStack(Items.APPLE);
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().addFavorite(stack);

        List<SearchNode> result = AmiSidebarSyncHandler.getNodesForContent(AmiConfig.PanelContent.FAVORITES);
        assertEquals(1, result.size());
        assertEquals("item", result.get(0).meta(FavoriteEntry.META_KIND, ""));
        assertEquals("minecraft:apple", result.get(0).meta(FavoriteEntry.META_BASE_ID, ""));
    }
}
