package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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

    @Test
    void craftableNodesOnlyIncludeSurvivalVisibleItems() {
        SearchNode redstone = item("redstone", "Redstone", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL
        ));
        SearchNode shulkerBox = item("shulker_box", "Shulker Box", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CREATIVE
        ));
        SearchNode rail = item("rail", "Rail", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT
        ));
        SearchNode hiddenApple = item("apple", "Apple", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL,
                SearchNodeKeys.VISIBILITY, "hidden"
        ));
        GlobalIndex.getInstance().addNode(redstone);
        GlobalIndex.getInstance().addNode(shulkerBox);
        GlobalIndex.getInstance().addNode(rail);
        GlobalIndex.getInstance().addNode(hiddenApple);

        List<SearchNode> result = AmiSidebarSyncHandler.craftableNodesForStacks(List.of(
                new ItemStack(Items.SHULKER_BOX),
                new ItemStack(Items.RAIL),
                new ItemStack(Items.APPLE),
                new ItemStack(Items.REDSTONE)
        ));

        assertEquals(List.of("minecraft:redstone"), result.stream().map(node -> node.id().toString()).toList());
    }

    @Test
    void craftableNodesSortAlphabeticallyForStableSidebarOrder() {
        GlobalIndex.getInstance().addNode(item("rail", "Rail", Map.of()));
        GlobalIndex.getInstance().addNode(item("redstone", "Redstone", Map.of()));
        GlobalIndex.getInstance().addNode(item("apple", "Apple", Map.of()));

        List<SearchNode> result = AmiSidebarSyncHandler.craftableNodesForStacks(List.of(
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.RAIL),
                new ItemStack(Items.APPLE)
        ));

        assertEquals(List.of("Apple", "Rail", "Redstone"),
                result.stream().map(SearchNode::displayName).toList());
    }

    private static SearchNode item(String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("minecraft", path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                metadata
        );
    }
}
