package com.sanhiruzu.ami.index.runtime;

import com.sanhiruzu.ami.client.favorites.AmiHistoryHandler;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LookupHistorySearchProvider implements RuntimeSearchProvider {
    private static final int MAX_LOOKUP_HISTORY = 12;
    private static final String LOOKUP_HISTORY_CATEGORY_ID = "lookup_history";
    private static final String LOOKUP_HISTORY_SUBCATEGORY = "items";

    @Override
    public String id() {
        return "ami:lookup_history";
    }

    @Override
    public long revision() {
        return AmiHistoryHandler.getInstance().revision();
    }

    @Override
    public List<SearchNode> nodes() {
        List<ItemStack> history = AmiHistoryHandler.getInstance().getLookupHistory();
        if (history.isEmpty()) {
            return List.of();
        }

        List<SearchNode> nodes = new ArrayList<>();
        int limit = Math.min(MAX_LOOKUP_HISTORY, history.size());
        for (int i = 0; i < limit; i++) {
            ItemStack stack = history.get(i);
            if (stack.isEmpty()) {
                continue;
            }

            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) {
                continue;
            }
            GlobalIndex.getInstance().getNode(itemId).ifPresent(node -> {
                Map<String, String> metadata = new HashMap<>(node.metadata());
                metadata.put(SearchNodeKeys.ONTOLOGY_CATEGORY, LOOKUP_HISTORY_CATEGORY_ID);
                metadata.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, LOOKUP_HISTORY_SUBCATEGORY);
                nodes.add(node.withMetadata(metadata));
            });
        }
        return List.copyOf(nodes);
    }
}
