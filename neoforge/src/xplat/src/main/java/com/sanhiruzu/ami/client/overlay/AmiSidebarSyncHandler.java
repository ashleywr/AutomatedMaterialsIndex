package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AmiSidebarSyncHandler {

    public static List<SearchNode> getNodesForContent(AmiConfig.PanelContent content) {
        return switch (content) {
            case FAVORITES -> AmiFavoritesHandler.getInstance().getFavorites();
            case LOOKUP_HISTORY -> toNodes(RecipeViewerBridge.getLookupHistory());
            case CRAFTING_HISTORY -> toNodes(RecipeViewerBridge.getCraftHistory());
            case CRAFTABLE -> toNodes(RecipeViewerBridge.getCraftables());
            default -> List.of();
        };
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
