package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeLookupStackResolverTest {
    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
        ItemIconRenderer.clearPersistent();
    }

    @Test
    void includesSubtypeStacksForBaseItemLookups() {
        Identifier baseId = new Identifier("minecraft", "diamond_pickaxe");
        Identifier variantId = new Identifier("minecraft", "diamond_pickaxe/variant/sun_0");

        SearchNode baseNode = new SearchNode(baseId, NodeType.ITEM, "Etching", 0, 0, Map.of());
        SearchNode variantNode = new SearchNode(
                variantId,
                NodeType.ITEM,
                "Sun Etching",
                0,
                0,
                Map.of(
                        SearchNodeKeys.SUBTYPE_OF, baseId.toString(),
                        SearchNodeKeys.VARIANT_SOURCE, "creative_tab"
                )
        );
        GlobalIndex.getInstance().addNode(baseNode);
        GlobalIndex.getInstance().addNode(variantNode);

        ItemStack baseStack = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack variantStack = new ItemStack(Items.DIAMOND_PICKAXE)
                .withComponentSignature("sun")
                .withHoverName("Sun Etching");

        ItemIconRenderer.registerStack(baseId, baseStack);
        ItemIconRenderer.registerStack(variantId, variantStack);

        List<ItemStack> candidates = RecipeLookupStackResolver.candidates(baseStack);

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().anyMatch(stack -> "Sun Etching".equals(stack.getHoverName().getString())));
    }
}
