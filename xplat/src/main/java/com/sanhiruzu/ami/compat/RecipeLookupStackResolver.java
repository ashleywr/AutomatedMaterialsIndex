package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class RecipeLookupStackResolver {
    private static final int MAX_CANDIDATES = 32;

    private RecipeLookupStackResolver() {
    }

    static List<ItemStack> candidates(ItemStack requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }

        List<ItemStack> candidates = new ArrayList<>();
        addUnique(candidates, requested);

        Identifier baseItemId = BuiltInRegistries.ITEM.getKey(requested.getItem());
        if (baseItemId == null) {
            return List.copyOf(candidates);
        }

        String baseItemKey = baseItemId.toString();
        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ITEM)) {
            if (candidates.size() >= MAX_CANDIDATES) {
                break;
            }
            if (node == null || node.id() == null) {
                continue;
            }
            if (!baseItemId.equals(node.id()) && !baseItemKey.equals(node.meta(SearchNodeKeys.SUBTYPE_OF, ""))) {
                continue;
            }

            ItemStack candidate = ItemIconRenderer.resolveStack(node.id());
            if (candidate.isEmpty() || candidate.getItem() != requested.getItem()) {
                continue;
            }
            addUnique(candidates, candidate);
        }

        return List.copyOf(candidates);
    }

    private static void addUnique(List<ItemStack> candidates, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        for (ItemStack existing : candidates) {
            if (Services.PLATFORM.sameItemSameComponents(existing, normalized)) {
                return;
            }
        }
        candidates.add(normalized);
    }
}
