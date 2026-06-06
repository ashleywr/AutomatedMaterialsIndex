package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitySpawnEggAliasSearchTest {
    private static final String RUSSIAN_BLUE_SWET_SPAWN_EGG =
            "\u042f\u0439\u0446\u043e \u043f\u0440\u0438\u0437\u044b\u0432\u0430 "
                    + "\u0441\u0438\u043d\u0435\u0433\u043e \u0441\u043b\u0430\u0434\u043d\u044f";

    @BeforeEach
    void setup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void entityPlainSearchCanUseLocalizedSpawnEggNameAlias() {
        GlobalIndex index = GlobalIndex.getInstance();
        SearchNode entity = new SearchNode(
                new ResourceLocation("example", "blue_swet"),
                NodeType.ENTITY,
                "Blue Swet",
                0,
                0,
                Map.of(SearchNodeKeys.PLAIN_SEARCH_TOKENS, RUSSIAN_BLUE_SWET_SPAWN_EGG)
        );
        index.addNode(entity);

        SearchService service = SearchService.buildFrom(index, false);

        assertTrue(service.query(RUSSIAN_BLUE_SWET_SPAWN_EGG)
                .getOrDefault(NodeType.ENTITY, List.of())
                .contains(entity));
    }
}
