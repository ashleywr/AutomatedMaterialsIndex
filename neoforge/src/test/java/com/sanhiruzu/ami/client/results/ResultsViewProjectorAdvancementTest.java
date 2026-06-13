package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiAdvancementSearchIndex;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsViewProjectorAdvancementTest {
    @BeforeEach
    void setUp() {
        AmiConfig.resetToDefaults();
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @AfterEach
    void tearDown() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void projectionIncludesAdvancementRowsWithoutChangingItemTree() {
        SearchNode item = new SearchNode(
                new ResourceLocation("minecraft", "stone"),
                NodeType.ITEM,
                "Stone",
                0,
                0,
                Map.of()
        );
        AmiAdvancementSearchIndex advancementIndex = new AmiAdvancementSearchIndex(List.of(advancement()));
        SearchState state = new SearchState();
        state.setQuery("stone age");
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                List.of(item),
                state,
                null,
                null,
                null,
                advancementIndex,
                false,
                false
        );

        assertEquals(1, projection.roots().size());
        assertEquals("Stone", projection.roots().getFirst().getLabel().getString());
        assertEquals(1, projection.advancementRows().size());
        assertEquals("Stone Age", projection.advancementRows().getFirst().title());
        assertTrue(projection.summary().contains("advancements=1"));
    }

    @Test
    void compactAndFavoritesProjectionsSuppressAdvancementRows() {
        AmiAdvancementSearchIndex advancementIndex = new AmiAdvancementSearchIndex(List.of(advancement()));
        SearchState state = new SearchState();
        state.setQuery("stone");

        assertTrue(ResultsViewProjector.project(List.of(), state, null, null, null, advancementIndex, true, false).advancementRows().isEmpty());
        assertTrue(ResultsViewProjector.project(List.of(), state, null, null, null, advancementIndex, false, true).advancementRows().isEmpty());
    }

    @Test
    void blankSearchSuppressesAdvancementRows() {
        AmiAdvancementSearchIndex advancementIndex = new AmiAdvancementSearchIndex(List.of(advancement()));
        SearchState state = new SearchState();
        state.setQuery("");

        assertTrue(ResultsViewProjector.project(List.of(), state, null, null, null, advancementIndex, false, false).advancementRows().isEmpty());
    }

    @Test
    void configCanSuppressAdvancementRows() {
        AmiAdvancementSearchIndex advancementIndex = new AmiAdvancementSearchIndex(List.of(advancement()));
        SearchState state = new SearchState();
        state.setQuery("stone");
        AmiConfig.searchIncludeAdvancements = false;

        assertTrue(ResultsViewProjector.project(List.of(), state, null, null, null, advancementIndex, false, false).advancementRows().isEmpty());
    }

    @Test
    void configCanSuppressEntityPlayerAndWaypointNodes() {
        SearchState state = new SearchState();
        state.setQuery("");
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);
        SearchNode entity = node(NodeType.ENTITY, "minecraft", "zombie", "Zombie");
        SearchNode player = node(NodeType.PLAYER, "ami", "player/ash", "ash");
        SearchNode waypoint = node(NodeType.WAYPOINT, "ami", "waypoint/home", "Home");
        AmiConfig.searchIncludeEntities = false;
        AmiConfig.searchIncludePlayers = false;
        AmiConfig.searchIncludeWaypoints = false;

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                List.of(entity, player, waypoint),
                state,
                null,
                null,
                null,
                null,
                false,
                false
        );

        assertTrue(projection.roots().isEmpty());
    }

    private static AmiAdvancementDocument advancement() {
        return AmiAdvancementDocument.builder(new ResourceLocation("minecraft", "story/mine_stone"), "Stone Age")
                .sourceId("minecraft")
                .tabTitle("Minecraft")
                .description("Mine stone with your new pickaxe")
                .type("task")
                .iconItemId(new ResourceLocation("minecraft", "stone"))
                .build();
    }

    private static SearchNode node(NodeType type, String namespace, String path, String name) {
        return new SearchNode(new ResourceLocation(namespace, path), type, name, 0, 0, Map.of());
    }
}
