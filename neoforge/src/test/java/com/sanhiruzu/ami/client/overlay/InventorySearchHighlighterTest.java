package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySearchHighlighterTest {
    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void resolvesMatchingItemIdsWithAmiSearchSyntax() {
        GlobalIndex index = GlobalIndex.getInstance();
        ResourceLocation mixerId = new ResourceLocation("create", "mechanical_mixer");
        ResourceLocation pressId = new ResourceLocation("create", "mechanical_press");
        ResourceLocation cableId = new ResourceLocation("ae2", "fluix_glass_cable");
        ResourceLocation entityId = new ResourceLocation("minecraft", "zombie");

        index.addNode(item(mixerId, "Mechanical Mixer", Map.of(
                SearchNodeKeys.CREATE_FACTS, "kinetic,mixing",
                SearchNodeKeys.COMPAT_FAMILIES, "create"
        )));
        index.addNode(item(pressId, "Mechanical Press", Map.of(
                SearchNodeKeys.CREATE_FACTS, "kinetic,pressing",
                SearchNodeKeys.COMPAT_FAMILIES, "create"
        )));
        index.addNode(item(cableId, "Fluix Glass Cable", Map.of(
                SearchNodeKeys.AE2_FACTS, "network,cable",
                SearchNodeKeys.COMPAT_FAMILIES, "ae2"
        )));
        index.addNode(new SearchNode(entityId, NodeType.ENTITY, "Zombie", 0, 0, Map.of()));

        assertEquals(Set.of(mixerId), InventorySearchHighlighter.matchingItems(index, "mixer"));
        assertEquals(Set.of(mixerId, pressId), InventorySearchHighlighter.matchingItems(index, "@create"));
        assertEquals(Set.of(cableId), InventorySearchHighlighter.matchingItems(index, "?fact:network"));
        assertEquals(Set.of(mixerId), InventorySearchHighlighter.matchingItems(index, "@create -press"));
    }

    @Test
    void blankSearchCanToggleModeWithoutRenderingFilter() {
        InventorySearchHighlighter highlighter = new InventorySearchHighlighter();

        assertTrue(highlighter.toggle("   "));
        assertTrue(highlighter.isActive());

        assertFalse(highlighter.toggle("stone"));
        assertFalse(highlighter.isActive());
        assertTrue(highlighter.toggle("stone"));
        highlighter.updateQuery("");
        assertTrue(highlighter.isActive());
    }

    private static SearchNode item(ResourceLocation id, String displayName, Map<String, String> metadata) {
        return new SearchNode(id, NodeType.ITEM, displayName, 0, 0, metadata);
    }
}
