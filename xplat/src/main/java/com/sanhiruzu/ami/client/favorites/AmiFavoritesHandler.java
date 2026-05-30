package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.EmiFavoritesBridge;
import com.sanhiruzu.ami.compat.JeiFavoritesBridge;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

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

    public static ItemStack resolveStack(SearchNode node) {
        ItemStack stack = ItemIconRenderer.resolveStack(node.id());
        if (stack.isEmpty() && node.type() == NodeType.ENTITY) {
            ResourceLocation eggId = Services.PLATFORM.rl(node.id().getNamespace(), node.id().getPath() + "_spawn_egg");
            stack = BuiltInRegistries.ITEM.getOptional(eggId).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        return stack;
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
        return Services.PLATFORM.isModLoaded("emi") || Services.PLATFORM.isModLoaded("jei");
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private void notifyChange() {
        if (onChange != null) onChange.run();
    }

    public void externalFavoritesChanged() {
        notifyChange();
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

    public void toggleFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (isFavorite(stack)) {
            removeFavorite(stack);
        } else {
            addFavorite(stack);
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
        if (Services.PLATFORM.isModLoaded("emi") && externalStackFavorite(EmiFavoritesBridge.getFavoriteEntries(), key))
            return true;
        return Services.PLATFORM.isModLoaded("jei") && externalStackFavorite(JeiFavoritesBridge.getFavoriteEntries(), key);
    }

    public boolean isRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        if (recipeId == null || stack == null || stack.isEmpty()) return false;
        String key = "recipe|" + recipeId + "|" + FavoriteEntry.stackKey(stack);
        if (localItemFavorites.containsKey(key)) return true;
        if (Services.PLATFORM.isModLoaded("emi") && containsFavoriteEntry(EmiFavoritesBridge.getFavoriteEntries(), key))
            return true;
        return Services.PLATFORM.isModLoaded("jei") && containsFavoriteEntry(JeiFavoritesBridge.getFavoriteEntries(), key);
    }

    public void addFavorite(SearchNode node) {
        if (node.type() == NodeType.ITEM) {
            ResourceLocation recipeId = parseRecipeId(node);
            if (recipeId != null) {
                addRecipeFavorite(recipeId, resolveStack(node));
                return;
            }
            if (hasExternalFavoriteStore()) {
                ItemStack stack = resolveStack(node);
                if (!stack.isEmpty()) {
                    addFavorite(stack);
                    return;
                }
            }
        }
        localFavorites.add(node.id());
        notifyChange();
    }

    public void addFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        FavoriteEntry entry = FavoriteEntry.localItem(stack);

        boolean externalAdded = false;
        if (Services.PLATFORM.isModLoaded("emi")) {
            externalAdded |= EmiFavoritesBridge.addFavorite(stack);
        }
        if (Services.PLATFORM.isModLoaded("jei")) {
            JeiFavoritesBridge.addFavorite(stack);
            externalAdded = true;
        }
        if (entry != null && !externalAdded) {
            localItemFavorites.put(entry.key(), entry);
        }
        notifyChange();
    }

    public void addFavoriteAt(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) return;

        FavoriteEntry entry = FavoriteEntry.localItem(stack);

        boolean externalAdded = false;
        if (Services.PLATFORM.isModLoaded("emi")) {
            externalAdded |= EmiFavoritesBridge.addFavoriteAt(stack, index);
        }
        if (Services.PLATFORM.isModLoaded("jei")) {
            JeiFavoritesBridge.addFavoriteAt(stack, index);
            externalAdded = true;
        }
        if (entry != null && !externalAdded) {
            localItemFavorites.put(entry.key(), entry);
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

        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiFavoritesBridge.removeFavorite(stack);
        }
        if (Services.PLATFORM.isModLoaded("jei")) {
            JeiFavoritesBridge.removeFavorite(stack);
        }
        notifyChange();
    }

    public void addRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        if (recipeId == null || stack == null || stack.isEmpty()) return;
        FavoriteEntry entry = FavoriteEntry.localRecipe(stack, recipeId);
        boolean externalAdded = false;
        if (Services.PLATFORM.isModLoaded("emi")) {
            externalAdded = EmiFavoritesBridge.addRecipeFavorite(stack, recipeId);
        }
        if (entry != null && !externalAdded) {
            localItemFavorites.put(entry.key(), entry);
        }
        notifyChange();
    }

    public void removeRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        if (recipeId == null || stack == null || stack.isEmpty()) return;
        localItemFavorites.remove("recipe|" + recipeId + "|" + FavoriteEntry.stackKey(stack));
        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiFavoritesBridge.removeRecipeFavorite(stack, recipeId);
        }
        notifyChange();
    }

    public List<SearchNode> getFavorites() {
        List<SearchNode> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Add external viewer favorites first to maintain their order.
        if (Services.PLATFORM.isModLoaded("emi")) {
            for (FavoriteEntry entry : EmiFavoritesBridge.getFavoriteEntries()) addEntry(result, seen, entry);
        }
        if (Services.PLATFORM.isModLoaded("jei")) {
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
}
