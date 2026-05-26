package com.sanhiruzu.ami.compat;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import java.util.ArrayList;
import java.util.List;

/**
 * Determines craftable items using vanilla Minecraft's {@link ClientRecipeBook}
 * and {@link StackedContents}, with no dependency on EMI or JEI.
 */
public class VanillaCraftablesService {
    private static final int CRAFTING_GRID_WIDTH = 3;
    private static final int CRAFTING_GRID_HEIGHT = 3;

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

        StackedContents stackedContents = new StackedContents();
        mc.player.getInventory().fillStackedContents(stackedContents);

        var registryAccess = mc.level.registryAccess();
        List<ItemStack> result = new ArrayList<>();

        for (RecipeCollection collection : collections) {
            collection.canCraft(stackedContents, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT, recipeBook);
            if (!collection.hasCraftable()) continue;

            List<Recipe<?>> craftable = collection.getRecipes(true);
            if (craftable.isEmpty()) continue;

            ItemStack output = craftable.get(0).getResultItem(registryAccess);
            if (output.isEmpty()) continue;

            result.add(output.copy());
        }

        cached = result;
        return result;
    }
}

