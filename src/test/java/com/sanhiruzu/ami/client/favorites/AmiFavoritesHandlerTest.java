package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class AmiFavoritesHandlerTest {

    @Test
    void testLocalFavoritesPersistence() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        
        // Mock a biome node (not supported by EMI)
        SearchNode biome = new SearchNode(
            ResourceLocation.parse("minecraft:plains"),
            NodeType.BIOME,
            "Plains",
            0, 0, Map.of()
        );
        com.sanhiruzu.ami.index.GlobalIndex.getInstance().addNode(biome);

        assertFalse(handler.isFavorite(biome));
        handler.addFavorite(biome);
        assertTrue(handler.isFavorite(biome));

        // Check if it appears in the list
        List<SearchNode> favorites = handler.getFavorites();
        assertTrue(favorites.stream().anyMatch(n -> n.id().equals(biome.id())));

        handler.removeFavorite(biome);
        assertFalse(handler.isFavorite(biome));
    }
}
