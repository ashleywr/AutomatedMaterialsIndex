package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootTableDropIndexerTest {
    private final GlobalIndex index = GlobalIndex.getInstance();

    @BeforeEach
    void clearIndex() {
        index.clear();
    }

    @Test
    void entityLootTableAddsDropEdgesForKnownItemRefs() {
        SearchNode cow = entity("cow", "Cow");
        SearchNode leather = item("leather", "Leather");
        SearchNode beef = item("beef", "Beef");
        index.addNode(cow);
        index.addNode(leather);
        index.addNode(beef);

        LootTableDropIndexer.IndexingResult result = LootTableDropIndexer.indexEntityLootTables(index, List.of(
                new LootTableDropIndexer.LootTableResource(
                        new ResourceLocation("minecraft:entities/cow"),
                        """
                                {
                                  "pools": [
                                    {"entries": [{"type": "minecraft:item", "name": "minecraft:leather"}]},
                                    {"entries": [{"type": "minecraft:item", "name": "minecraft:beef"}]}
                                  ]
                                }
                                """
                )
        ));

        assertEquals(1, result.entityTables());
        assertEquals(2, result.edgesAdded());
        assertEquals(List.of(leather, beef), cow.getEdges(EdgeType.DROPS));
    }

    @Test
    void ignoresNonEntityTablesAndRefsWithoutIndexedItems() {
        SearchNode cow = entity("cow", "Cow");
        SearchNode leather = item("leather", "Leather");
        index.addNode(cow);
        index.addNode(leather);

        LootTableDropIndexer.IndexingResult result = LootTableDropIndexer.indexEntityLootTables(index, List.of(
                new LootTableDropIndexer.LootTableResource(
                        new ResourceLocation("minecraft:blocks/oak_log"),
                        "{\"pools\":[{\"entries\":[{\"name\":\"minecraft:leather\"}]}]}"
                ),
                new LootTableDropIndexer.LootTableResource(
                        new ResourceLocation("minecraft:entities/cow"),
                        "{\"pools\":[{\"entries\":[{\"name\":\"example:missing_hide\"}]}]}"
                )
        ));

        assertEquals(1, result.entityTables());
        assertEquals(0, result.edgesAdded());
        assertTrue(cow.getEdges(EdgeType.DROPS).isEmpty());
    }

    @Test
    void deduplicatesRepeatedItemRefsForOneEntity() {
        SearchNode cow = entity("cow", "Cow");
        SearchNode leather = item("leather", "Leather");
        index.addNode(cow);
        index.addNode(leather);

        LootTableDropIndexer.IndexingResult result = LootTableDropIndexer.indexEntityLootTables(index, List.of(
                new LootTableDropIndexer.LootTableResource(
                        new ResourceLocation("minecraft:entities/cow"),
                        "{\"pools\":[{\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:leather\"},{\"type\":\"minecraft:item\",\"name\":\"minecraft:leather\"}]}]}"
                )
        ));

        assertEquals(1, result.edgesAdded());
        assertEquals(List.of(leather), cow.getEdges(EdgeType.DROPS));
    }

    private static SearchNode item(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ITEM, name, 0, 0, Map.of());
    }

    private static SearchNode entity(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ENTITY, name, 0, 0, Map.of());
    }
}
