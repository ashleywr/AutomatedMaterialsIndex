package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SearchNodeEdgeResolutionTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void unresolvedEdgesResolveSyncWhenNodePresent() {
        GlobalIndex gi = GlobalIndex.getInstance();

        var itemId = ResourceLocation.parse("minecraft:gunpowder");
        var entityId = ResourceLocation.parse("minecraft:creeper");

        var itemNode = new SearchNode(itemId, NodeType.ITEM, "Gunpowder", 0, 0, new HashMap<>());
        var entityNode = new SearchNode(entityId, NodeType.ENTITY, "Creeper", 0, 0, new HashMap<>());

        gi.addNode(itemNode);
        gi.addNode(entityNode);

        entityNode.addUnresolvedEdge(EdgeType.DROPS, itemId);

        List<SearchNode> edges = entityNode.getEdges(EdgeType.DROPS);
        assertNotNull(edges);
        assertTrue(edges.stream().anyMatch(n -> n.id().equals(itemId)));
    }
}
