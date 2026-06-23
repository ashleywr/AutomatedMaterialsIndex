package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityDetailsQueryTest {
    @BeforeEach
    void clearIndex() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void parsesEntityAndMobRouteTargets() {
        assertEquals("cow", EntityDetailsQuery.parseTarget("?entity:cow").orElseThrow());
        assertEquals("minecraft:cow", EntityDetailsQuery.parseTarget("?entity:minecraft:cow").orElseThrow());
        assertEquals("cow", EntityDetailsQuery.parseTarget("?mob:cow").orElseThrow());
        assertTrue(EntityDetailsQuery.parseTarget("cow").isEmpty());
        assertTrue(EntityDetailsQuery.parseTarget("entity:cow").isEmpty(),
                "entity: should remain normal search text so entity names cannot collide with detail routes");
    }

    @Test
    void formatsCanonicalEntityRoutes() {
        SearchNode cow = entity("cow", "Cow");

        assertEquals("?entity:minecraft:cow", EntityDetailsQuery.queryFor(cow));
    }

    @Test
    void resolvesShortPathAgainstIndexedEntities() {
        SearchNode cow = entity("cow", "Cow");
        GlobalIndex.getInstance().addNode(cow);

        assertEquals(cow, EntityDetailsQuery.resolveTarget("?entity:cow", null).orElseThrow());
        assertEquals(cow, EntityDetailsQuery.resolveTarget("?mob:cow", null).orElseThrow());
    }

    @Test
    void doesNotGuessShortPathWhenMultipleEntitiesMatch() {
        GlobalIndex.getInstance().addNode(entity("cow", "Cow"));
        GlobalIndex.getInstance().addNode(new SearchNode(new ResourceLocation("examplemod:cow"), NodeType.ENTITY,
                "Cow", 0, 0, Map.of()));

        assertTrue(EntityDetailsQuery.resolveTarget("?entity:cow", null).isEmpty());
    }

    private static SearchNode entity(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ENTITY, name, 0, 0, Map.of());
    }
}
