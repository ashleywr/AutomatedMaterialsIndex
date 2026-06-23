package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AmiSidebarSyncHandler {

    public static List<SearchNode> getNodesForContent(AmiConfig.PanelContent content) {
        return switch (content) {
            case FAVORITES -> AmiFavoritesHandler.getInstance().getFavorites();
            case LOOKUP_HISTORY -> toNodes(RecipeViewerBridge.getLookupHistory());
            case CRAFTING_HISTORY -> toNodes(RecipeViewerBridge.getCraftHistory());
            case CRAFTABLE -> craftableNodesForStacks(RecipeViewerBridge.getCraftables());
            default -> List.of();
        };
    }

    static List<SearchNode> craftableNodesForStacks(List<ItemStack> stacks) {
        Map<String, SearchNode> nodesById = new LinkedHashMap<>();
        for (SearchNode node : toNodes(stacks)) {
            if (isSurvivalVisibleCraftable(node)) {
                nodesById.putIfAbsent(node.id().toString(), node);
            }
        }
        List<SearchNode> nodes = new ArrayList<>(nodesById.values());
        nodes.sort(Comparator
                .comparing(SearchNode::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(node -> node.id().toString()));
        return nodes;
    }

    private static boolean isSurvivalVisibleCraftable(SearchNode node) {
        if (node == null) return false;
        String accessLevel = node.meta(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        if (!accessLevel.isBlank() && !ItemFilter.ACCESS_SURVIVAL.equals(accessLevel)) {
            return false;
        }
        return !"hidden".equals(node.meta(SearchNodeKeys.VISIBILITY, ""));
    }

    private static List<SearchNode> toNodes(List<ItemStack> stacks) {
        List<SearchNode> nodes = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            GlobalIndex.getInstance().getNode(id).ifPresent(nodes::add);
        }
        return nodes;
    }
}
