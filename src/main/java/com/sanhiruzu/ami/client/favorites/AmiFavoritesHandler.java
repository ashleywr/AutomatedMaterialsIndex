package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.EmiFavoritesBridge;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles favorite synchronization between AMI and other recipe viewers (primarily EMI).
 * Maintains a local list of favorite IDs to ensure AMI-specific nodes (like entities/biomes)
 * can be favorited even if the external viewer doesn't support them.
 */
public class AmiFavoritesHandler {
    private static final AmiFavoritesHandler INSTANCE = new AmiFavoritesHandler();
    
    private final Set<ResourceLocation> localFavorites = new HashSet<>();
    private Runnable onChange;

    public static AmiFavoritesHandler getInstance() {
        return INSTANCE;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private void notifyChange() {
        if (onChange != null) onChange.run();
    }

    public void toggleFavorite(SearchNode node) {
        if (node == null) return;
        if (isFavorite(node)) {
            removeFavorite(node);
        } else {
            addFavorite(node);
        }
        notifyChange();
    }

    public boolean isFavorite(SearchNode node) {
        if (localFavorites.contains(node.id())) return true;
        
        if (ModList.get().isLoaded("emi")) {
            return isEmiFavorite(node);
        }
        return false;
    }

    private boolean isEmiFavorite(SearchNode node) {
        return EmiFavoritesBridge.isFavorite(node.id());
    }

    public static ItemStack resolveStack(SearchNode node) {
        ItemStack stack = ItemIconRenderer.resolveStack(node.id());
        if (stack.isEmpty() && node.type() == NodeType.ENTITY) {
            ResourceLocation eggId = ResourceLocation.withDefaultNamespace(node.id().getPath() + "_spawn_egg");
            stack = BuiltInRegistries.ITEM.getOptional(eggId).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        return stack;
    }

    public void addFavorite(SearchNode node) {
        localFavorites.add(node.id());
        
        if (ModList.get().isLoaded("emi") && node.type() == NodeType.ITEM) {
            addFavorite(resolveStack(node));
        }
        notifyChange();
    }

    public void addFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.add(id);

        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.addFavorite(stack);
        }
        notifyChange();
    }

    public void addFavoriteAt(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.add(id);

        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.addFavoriteAt(stack, index);
        }
        notifyChange();
    }

    public void removeFavorite(SearchNode node) {
        localFavorites.remove(node.id());
        
        if (ModList.get().isLoaded("emi")) {
            removeFavorite(resolveStack(node));
        }
        notifyChange();
    }

    public void removeFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.remove(id);

        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.removeFavorite(stack);
        }
        notifyChange();
    }

    public List<SearchNode> getFavorites() {
        List<SearchNode> result = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();

        // 1. Add EMI favorites first to maintain their order
        if (ModList.get().isLoaded("emi")) {
            for (ResourceLocation id : EmiFavoritesBridge.getFavoriteIds()) {
                GlobalIndex.getInstance().getNode(id).ifPresent(node -> {
                    result.add(node);
                    seen.add(id);
                });
            }
        }

        // 2. Add local favorites that weren't in EMI (e.g. entities, biomes)
        for (ResourceLocation id : localFavorites) {
            if (!seen.contains(id)) {
                GlobalIndex.getInstance().getNode(id).ifPresent(result::add);
            }
        }

        return result;
    }
}
