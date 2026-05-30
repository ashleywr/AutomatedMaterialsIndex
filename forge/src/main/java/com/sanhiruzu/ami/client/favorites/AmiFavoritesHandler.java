package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.EmiFavoritesBridge;
import com.sanhiruzu.ami.compat.JeiFavoritesBridge;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles favorite synchronization between AMI and other recipe viewers.
 * Maintains a local list of favorite IDs to ensure AMI-specific nodes (like entities/biomes)
 * can be favorited even if the external viewer doesn't support them.
 */
public class AmiFavoritesHandler {
    private static final AmiFavoritesHandler INSTANCE = new AmiFavoritesHandler();

    private final Set<ResourceLocation> localFavorites = new HashSet<>();
    private final Map<String, FavoriteEntry> localItemFavorites = new LinkedHashMap<>();
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
        if (node.type() == NodeType.ITEM) {
            ResourceLocation recipeId = parseRecipeId(node);
            ItemStack stack = resolveStack(node);
            if (recipeId != null) {
                return isRecipeFavorite(recipeId, stack);
            }
            return !stack.isEmpty() && isFavorite(stack);
        }
        return false;
    }

    public boolean isFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String key = "item|" + FavoriteEntry.stackKey(stack);
        if (localItemFavorites.containsKey(key)) return true;
        if (localFavorites.contains(id)) return true;
        if (ModList.get().isLoaded("emi") && externalStackFavorite(EmiFavoritesBridge.getFavoriteEntries(), key)) return true;
        return ModList.get().isLoaded("jei") && externalStackFavorite(JeiFavoritesBridge.getFavoriteEntries(), key);
    }

    public boolean isRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        if (recipeId == null || stack == null || stack.isEmpty()) return false;
        String key = "recipe|" + recipeId + "|" + FavoriteEntry.stackKey(stack);
        if (localItemFavorites.containsKey(key)) return true;
        if (ModList.get().isLoaded("emi") && containsFavoriteEntry(EmiFavoritesBridge.getFavoriteEntries(), key)) return true;
        return ModList.get().isLoaded("jei") && containsFavoriteEntry(JeiFavoritesBridge.getFavoriteEntries(), key);
    }

    public static ItemStack resolveStack(SearchNode node) {
        ItemStack stack = ItemIconRenderer.resolveStack(node.id());
        if (stack.isEmpty() && node.type() == NodeType.ENTITY) {
            ResourceLocation eggId = new ResourceLocation(node.id().getNamespace(), node.id().getPath() + "_spawn_egg");
            stack = BuiltInRegistries.ITEM.getOptional(eggId).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        return stack;
    }

    public void addFavorite(SearchNode node) {
        if (node.type() == NodeType.ITEM) {
            ResourceLocation recipeId = parseRecipeId(node);
            if (recipeId != null) {
                addRecipeFavorite(recipeId, resolveStack(node));
                return;
            }
        }
        localFavorites.add(node.id());
        if (node.type() == NodeType.ITEM) {
            if (hasExternalFavoriteStore()) {
                addFavorite(resolveStack(node));
            }
        }
        notifyChange();
    }

    public void addFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        FavoriteEntry entry = FavoriteEntry.localItem(stack);
        if (entry != null) {
            localItemFavorites.put(entry.key(), entry);
        }

        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.addFavorite(stack);
        }
        if (ModList.get().isLoaded("jei")) {
            JeiFavoritesBridge.addFavorite(stack);
        }
        notifyChange();
    }

    public void addFavoriteAt(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) return;

        FavoriteEntry entry = FavoriteEntry.localItem(stack);
        if (entry != null) {
            localItemFavorites.put(entry.key(), entry);
        }

        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.addFavoriteAt(stack, index);
        }
        if (ModList.get().isLoaded("jei")) {
            JeiFavoritesBridge.addFavoriteAt(stack, index);
        }
        notifyChange();
    }

    public void removeFavorite(SearchNode node) {
        localFavorites.remove(node.id());

        if (node.type() == NodeType.ITEM) {
            ResourceLocation recipeId = parseRecipeId(node);
            ItemStack stack = resolveStack(node);
            if (recipeId != null) {
                removeRecipeFavorite(recipeId, stack);
                return;
            }
            if (!stack.isEmpty()) {
                removeFavorite(stack);
                return;
            }
        }
        notifyChange();
    }

    public void removeFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.remove(id);
        localItemFavorites.remove("item|" + FavoriteEntry.stackKey(stack));

        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.removeFavorite(stack);
        }
        if (ModList.get().isLoaded("jei")) {
            JeiFavoritesBridge.removeFavorite(stack);
        }
        notifyChange();
    }

    public void addRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        if (recipeId == null || stack == null || stack.isEmpty()) return;
        FavoriteEntry entry = FavoriteEntry.localRecipe(stack, recipeId);
        if (entry != null) {
            localItemFavorites.put(entry.key(), entry);
        }
        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.addRecipeFavorite(stack, recipeId);
        }
        notifyChange();
    }

    public void removeRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        if (recipeId == null || stack == null || stack.isEmpty()) return;
        localItemFavorites.remove("recipe|" + recipeId + "|" + FavoriteEntry.stackKey(stack));
        if (ModList.get().isLoaded("emi")) {
            EmiFavoritesBridge.removeRecipeFavorite(stack, recipeId);
        }
        notifyChange();
    }

    public List<SearchNode> getFavorites() {
        List<SearchNode> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Add external viewer favorites first to maintain their order.
        if (ModList.get().isLoaded("emi")) {
            for (FavoriteEntry entry : EmiFavoritesBridge.getFavoriteEntries()) addEntry(result, seen, entry);
        }
        if (ModList.get().isLoaded("jei")) {
            for (FavoriteEntry entry : JeiFavoritesBridge.getFavoriteEntries()) addEntry(result, seen, entry);
        }
        for (FavoriteEntry entry : localItemFavorites.values()) {
            addEntry(result, seen, entry);
        }

        // 2. Add local favorites that weren't in a recipe viewer (e.g. entities, biomes).
        for (ResourceLocation id : localFavorites) {
            if (seen.add("node|" + id)) {
                GlobalIndex.getInstance().getNode(id).ifPresent(result::add);
            }
        }

        return result;
    }

    private static void addEntry(List<SearchNode> result, Set<String> seen, FavoriteEntry entry) {
        if (entry != null && seen.add(entry.key())) {
            result.add(entry.toNode());
            if (!entry.isRecipeFavorite()) {
                seen.add("node|" + entry.itemId());
            }
        }
    }

    private static boolean externalStackFavorite(List<FavoriteEntry> entries, String key) {
        for (FavoriteEntry entry : entries) {
            if (!entry.isRecipeFavorite() && entry.key().equals(key)) return true;
        }
        return false;
    }

    private static boolean containsFavoriteEntry(List<FavoriteEntry> entries, String key) {
        for (FavoriteEntry entry : entries) {
            if (entry.key().equals(key)) return true;
        }
        return false;
    }

    private static ResourceLocation parseRecipeId(SearchNode node) {
        String recipeId = node.meta(FavoriteEntry.META_RECIPE_ID);
        return recipeId == null || recipeId.isBlank() ? null : ResourceLocation.tryParse(recipeId);
    }

    private static boolean hasExternalFavoriteStore() {
        return ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");
    }
}
