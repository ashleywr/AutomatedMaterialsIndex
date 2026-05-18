package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.results.SearchScope;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FavoritesSearchScopeTest {

    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void favoritesQueriesStayScopedToFavoriteSourceNodes() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode ironIngot = new SearchNode(
                ResourceLocation.parse("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of()
        );
        SearchNode goldIngot = new SearchNode(
                ResourceLocation.parse("minecraft:gold_ingot"),
                NodeType.ITEM,
                "Gold Ingot",
                0,
                0,
                Map.of()
        );

        index.addNode(ironIngot);
        index.addNode(goldIngot);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> favorites = List.of(ironIngot);

        List<SearchNode> scoped = SearchScope.resolveQueriedSource(service, favorites, "ingot", true);

        assertEquals(1, scoped.size());
        assertTrue(scoped.contains(ironIngot));
    }

    @Test
    void nonFavoriteQueriesStillReturnGlobalResults() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode ironIngot = new SearchNode(
                ResourceLocation.parse("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of()
        );
        SearchNode goldIngot = new SearchNode(
                ResourceLocation.parse("minecraft:gold_ingot"),
                NodeType.ITEM,
                "Gold Ingot",
                0,
                0,
                Map.of()
        );

        index.addNode(ironIngot);
        index.addNode(goldIngot);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> source = List.of(ironIngot);

        List<SearchNode> global = SearchScope.resolveQueriedSource(service, source, "ingot", false);

        assertEquals(2, global.size());
        assertTrue(global.contains(ironIngot));
        assertTrue(global.contains(goldIngot));
    }
}
