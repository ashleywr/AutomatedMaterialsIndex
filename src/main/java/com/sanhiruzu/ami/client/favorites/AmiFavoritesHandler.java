package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiFavorites;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles favorite synchronization between AMI and other recipe viewers (primarily EMI).
 */
public class AmiFavoritesHandler {
    private static final AmiFavoritesHandler INSTANCE = new AmiFavoritesHandler();
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
        if (ModList.get().isLoaded("emi")) {
            return isEmiFavorite(node);
        }
        return false;
    }

    private boolean isEmiFavorite(SearchNode node) {
        ResourceLocation id = node.id();
        for (EmiFavorite fav : EmiFavorites.favorites) {
            EmiIngredient stack = fav.getStack();
            if (stack instanceof EmiStack es) {
                if (es.getId().equals(id)) return true;
            }
        }
        return false;
    }

    public void addFavorite(SearchNode node) {
        if (ModList.get().isLoaded("emi")) {
            ItemStack stack = com.sanhiruzu.ami.client.icon.ItemIconRenderer.resolveStack(node.id());
            addFavorite(stack);
        }
    }

    public void addFavorite(ItemStack stack) {
        if (!stack.isEmpty() && ModList.get().isLoaded("emi")) {
            EmiFavorites.addFavorite(EmiStack.of(stack));
        }
    }

    public void addFavoriteAt(ItemStack stack, int index) {
        if (!stack.isEmpty() && ModList.get().isLoaded("emi")) {
            // If already favorited, remove it first to "move" it
            EmiFavorites.removeFavorite(EmiStack.of(stack));
            EmiFavorites.addFavoriteAt(EmiStack.of(stack), index);
            notifyChange();
        }
    }

    public void removeFavorite(SearchNode node) {
        if (ModList.get().isLoaded("emi")) {
            ItemStack stack = com.sanhiruzu.ami.client.icon.ItemIconRenderer.resolveStack(node.id());
            removeFavorite(stack);
        }
    }

    public void removeFavorite(ItemStack stack) {
        if (!stack.isEmpty() && ModList.get().isLoaded("emi")) {
            EmiFavorites.removeFavorite(EmiStack.of(stack));
        }
    }

    public List<SearchNode> getFavorites() {
        List<SearchNode> result = new ArrayList<>();
        if (ModList.get().isLoaded("emi")) {
            for (EmiFavorite fav : EmiFavorites.favorites) {
                EmiIngredient stack = fav.getStack();
                if (stack instanceof EmiStack es) {
                    GlobalIndex.getInstance().getNode(es.getId()).ifPresent(result::add);
                }
            }
        }
        return result;
    }
}
