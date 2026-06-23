package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSourceQueryTest {
    @BeforeEach
    void clearIndex() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void parsesSourcesRouteTargets() {
        assertEquals("leather", ItemSourceQuery.parseTarget("?sources=leather").orElseThrow());
        assertEquals("minecraft:leather", ItemSourceQuery.parseTarget("?sources=minecraft:leather").orElseThrow());
        assertTrue(ItemSourceQuery.parseTarget("leather").isEmpty());
        assertTrue(ItemSourceQuery.parseTarget("sources:leather").isEmpty(),
                "sources: should remain normal search text so item names cannot collide with source routes");
    }

    @Test
    void formatsCanonicalItemRoutes() {
        SearchNode leather = item("leather", "Leather");

        assertEquals("?sources=minecraft:leather", ItemSourceQuery.queryFor(leather));
    }

    @Test
    void resolvesShortPathAgainstIndexedItems() {
        SearchNode leather = item("leather", "Leather");
        GlobalIndex.getInstance().addNode(leather);

        assertEquals(leather, ItemSourceQuery.resolveTarget("?sources=leather", null).orElseThrow());
    }

    private static SearchNode item(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ITEM, name, 0, 0, Map.of());
    }
}
