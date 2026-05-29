package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.AmiRecipeIndex;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Populates the AmiRecipeIndex from vanilla RecipeManager, then
 * annotates item SearchNodes with recipe availability metadata.
 * Must run before ItemProvider so it can use recipe data.
 */
public class RecipeProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        AmiRecipeIndex recipeIndex = AmiRecipeIndex.getInstance();
        recipeIndex.rebuild(level);
        AMI.LOGGER.info("AmiRecipeIndex rebuilt: {} recipes indexed", recipeIndex.recipeCount());
    }

    /**
     * Computes obtainability and recipe category strings for a given item.
     * Called from ItemProvider during node construction.
     */
    public static String computeObtainability(Item item, AmiRecipeIndex recipeIndex) {
        if (!recipeIndex.hasRecipe(item)) return "no_recipe";

        Set<String> categories = new LinkedHashSet<>();
        for (com.sanhiruzu.ami.util.AmiRecipeHolder<?> entry : recipeIndex.getRecipesFor(new ItemStack(item))) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return categories.isEmpty() ? "no_recipe" : String.join(",", categories);
    }

    public static String computeRecipeCategories(Item item, AmiRecipeIndex recipeIndex) {
        if (!recipeIndex.hasRecipe(item)) return "";

        Set<String> categories = new LinkedHashSet<>();
        for (com.sanhiruzu.ami.util.AmiRecipeHolder<?> entry : recipeIndex.getRecipesFor(new ItemStack(item))) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return String.join(",", categories);
    }

    public static String computeRecipeUseCategories(Item item, AmiRecipeIndex recipeIndex) {
        Set<String> categories = new LinkedHashSet<>();
        for (com.sanhiruzu.ami.util.AmiRecipeHolder<?> entry : recipeIndex.getUsesFor(new ItemStack(item))) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return String.join(",", categories);
    }

    public static int computeRecipeOutputCount(Item item, AmiRecipeIndex recipeIndex) {
        return recipeIndex.getRecipesFor(new ItemStack(item)).size();
    }

    public static int computeRecipeUseCount(Item item, AmiRecipeIndex recipeIndex) {
        return recipeIndex.getUsesFor(new ItemStack(item)).size();
    }

    private static String getCategoryName(RecipeType<?> type) {
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) return type.toString().toLowerCase();
        return key.getPath();
    }
}

