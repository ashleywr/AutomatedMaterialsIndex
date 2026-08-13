package com.sanhiruzu.ami.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Direct EMI API calls — only referenced behind a Services.PLATFORM.isModLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
final class EmiRecipeDumpBridge {
    private EmiRecipeDumpBridge() {
    }

    static List<RecipeDumpWriters.ViewerDatasetOutput> writeRecipes(Path dumpDir, Level level) throws IOException {
        List<RecipeDumpWriters.ViewerRecipeSnapshot> snapshots = new ArrayList<>();
        for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes()) {
            snapshots.add(recipeSnapshot(recipe, level));
        }
        snapshots.sort(Comparator.comparing(RecipeDumpWriters.ViewerRecipeSnapshot::categoryId)
                .thenComparing(RecipeDumpWriters.ViewerRecipeSnapshot::recipeId)
                .thenComparing(RecipeDumpWriters.ViewerRecipeSnapshot::recipeClass));

        Path out = dumpDir.resolve(RecipeDumpWriters.viewerDumpFileName("emi", "all"));
        RecipeDumpWriters.writeJsonl(out, snapshots);
        return List.of(new RecipeDumpWriters.ViewerDatasetOutput("emi", "all", out.getFileName().toString(), snapshots.size()));
    }

    private static RecipeDumpWriters.ViewerRecipeSnapshot recipeSnapshot(EmiRecipe recipe, Level level) {
        EmiRecipeCategory category = recipe.getCategory();
        String categoryId = category == null ? "" : category.getId().toString();
        String categoryTitle = category == null ? "" : RecipeDumpWriters.safeComponentString(category.getName());
        String backingRecipeId = "";
        try {
            RecipeHolder<?> backing = recipe.getBackingRecipe();
            if (backing != null) {
                backingRecipeId = backing.id().toString();
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        return new RecipeDumpWriters.ViewerRecipeSnapshot(
                "emi",
                "all",
                RecipeDumpWriters.resourceLocationString(recipe.getId()),
                categoryId,
                categoryTitle,
                recipe.getClass().getName(),
                backingRecipeId,
                ingredients(recipe.getInputs(), level),
                ingredients(recipe.getCatalysts(), level),
                stacks(recipe.getOutputs(), level),
                recipe.getDisplayWidth(),
                recipe.getDisplayHeight()
        );
    }

    private static List<RecipeDumpWriters.IngredientSnapshot> ingredients(List<EmiIngredient> ingredients, Level level) {
        List<RecipeDumpWriters.IngredientSnapshot> snapshots = new ArrayList<>();
        for (EmiIngredient ingredient : ingredients) {
            snapshots.add(new RecipeDumpWriters.IngredientSnapshot(
                    ingredient.getClass().getName(),
                    "",
                    stackSnapshots(ingredient.getEmiStacks(), level)
            ));
        }
        return snapshots;
    }

    private static List<RecipeDumpWriters.IngredientSnapshot> stacks(List<EmiStack> stacks, Level level) {
        return stacks.stream()
                .map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> new RecipeDumpWriters.IngredientSnapshot("item_stack", stack.getHoverName().getString(),
                        List.of(RecipeDumpWriters.stackSnapshot(stack, level))))
                .toList();
    }

    private static List<RecipeDumpWriters.StackSnapshot> stackSnapshots(List<EmiStack> stacks, Level level) {
        List<RecipeDumpWriters.StackSnapshot> snapshots = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (EmiStack emiStack : stacks) {
            RecipeDumpWriters.StackSnapshot snapshot = RecipeDumpWriters.stackSnapshot(emiStack.getItemStack(), level);
            if (!snapshot.itemId().isBlank() && seen.add(snapshot.exactKey())) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }
}
