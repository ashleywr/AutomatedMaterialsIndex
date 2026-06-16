package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Populates the AmiRecipeIndex from vanilla RecipeManager, then
 * annotates item SearchNodes with recipe availability metadata.
 * Must run before ItemProvider so it can use recipe data.
 */
public class RecipeProvider implements IAmiDataProvider {

    /**
     * Computes obtainability and recipe category strings for a given item.
     * Called from ItemProvider during node construction.
     */
    public static String computeObtainability(Item item, AmiRecipeIndex recipeIndex) {
        if (!recipeIndex.hasRecipe(item)) return "no_recipe";

        Set<String> categories = new LinkedHashSet<>();
        for (AmiRecipeHolder<?> entry : Services.PLATFORM.getRecipesFor(new ItemStack(item))) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return categories.isEmpty() ? "no_recipe" : String.join(",", categories);
    }

    public static String computeRecipeCategories(Item item, AmiRecipeIndex recipeIndex) {
        if (!recipeIndex.hasRecipe(item)) return "";

        Set<String> categories = new LinkedHashSet<>();
        for (AmiRecipeHolder<?> entry : Services.PLATFORM.getRecipesFor(new ItemStack(item))) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return String.join(",", categories);
    }

    public static String computeRecipeUseCategories(Item item, AmiRecipeIndex recipeIndex) {
        Set<String> categories = new LinkedHashSet<>();
        for (AmiRecipeHolder<?> entry : Services.PLATFORM.getUsesFor(new ItemStack(item))) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return String.join(",", categories);
    }

    public static int computeRecipeOutputCount(Item item, AmiRecipeIndex recipeIndex) {
        return Services.PLATFORM.getRecipesFor(new ItemStack(item)).size();
    }

    public static int computeRecipeUseCount(Item item, AmiRecipeIndex recipeIndex) {
        return Services.PLATFORM.getUsesFor(new ItemStack(item)).size();
    }

    public static RecipeMetadata computeRecipeMetadata(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RecipeMetadata.EMPTY;
        }
        List<AmiRecipeHolder<?>> recipes = Services.PLATFORM.getRecipesFor(stack);
        List<AmiRecipeHolder<?>> uses = Services.PLATFORM.getUsesFor(stack);
        String recipeCategories = categoryNames(recipes);
        return new RecipeMetadata(
                recipes.isEmpty() ? "no_recipe" : recipeCategories.isEmpty() ? "no_recipe" : recipeCategories,
                recipeCategories,
                categoryNames(uses),
                recipes.size(),
                uses.size()
        );
    }

    private static String categoryNames(List<AmiRecipeHolder<?>> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return "";
        }
        Set<String> categories = new LinkedHashSet<>();
        for (AmiRecipeHolder<?> entry : recipes) {
            String name = getCategoryName(entry.value().getType());
            if (!name.isEmpty()) categories.add(name);
        }
        return String.join(",", categories);
    }

    private static String getCategoryName(RecipeType<?> type) {
        Identifier key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) return type.toString().toLowerCase();
        return key.getPath();
    }

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        AmiRecipeIndex recipeIndex = AmiRecipeIndex.getInstance();
        recipeIndex.rebuild(level);
        AmiCore.LOGGER.debug("AmiRecipeIndex rebuilt: {} recipes indexed", recipeIndex.recipeCount());
    }

    public record RecipeMetadata(
            String obtainability,
            String recipeCategories,
            String recipeUseCategories,
            int recipeOutputCount,
            int recipeUseCount
    ) {
        public static final RecipeMetadata EMPTY = new RecipeMetadata("no_recipe", "", "", 0, 0);
    }
}
