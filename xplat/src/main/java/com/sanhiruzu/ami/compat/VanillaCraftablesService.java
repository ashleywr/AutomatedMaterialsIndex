package com.sanhiruzu.ami.compat;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines craftable items using vanilla Minecraft's {@link ClientRecipeBook}
 * and {@link StackedItemContents}, with no dependency on EMI or JEI.
 */
public class VanillaCraftablesService {
    private static List<ItemStack> cached = List.of();
    private static int lastInventoryVersion = -1;

    private VanillaCraftablesService() {
    }

    public static List<ItemStack> getCraftables() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return List.of();

        int version = mc.player.getInventory().getTimesChanged();
        if (version == lastInventoryVersion) return cached;
        lastInventoryVersion = version;

        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        List<RecipeCollection> collections = recipeBook.getCollections();
        if (collections.isEmpty()) return cached = List.of();

        StackedItemContents stackedContents = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(stackedContents);

        List<ItemStack> result = new ArrayList<>();

        for (RecipeCollection collection : collections) {
            collection.selectRecipes(stackedContents, display -> true);
            if (!collection.hasCraftable()) continue;

            var craftable = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
            if (craftable.isEmpty()) continue;

            ItemStack output = craftable.get(0).display().result().resolveForFirstStack(ContextMap.EMPTY);
            if (output.isEmpty()) continue;

            result.add(output.copy());
        }

        cached = result;
        return result;
    }
}
