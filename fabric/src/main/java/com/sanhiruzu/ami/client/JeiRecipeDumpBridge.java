package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.compat.JeiRuntimeAccessor;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Direct JEI API calls — only referenced behind a Services.PLATFORM.isModLoaded("jei") guard
 * so this class is never loaded when JEI is absent.
 */
final class JeiRecipeDumpBridge {
    private JeiRecipeDumpBridge() {
    }

    static List<RecipeDumpWriters.ViewerDatasetOutput> writeRecipes(Path dumpDir, Level level) throws IOException {
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            try {
                IRecipeManager recipeManager = runtime.getRecipeManager();
                List<RecipeDumpWriters.ViewerDatasetOutput> outputs = new ArrayList<>();

                List<RecipeDumpWriters.ViewerRecipeSnapshot> visible = collectRecipes(recipeManager, level, false);
                Path visibleOut = dumpDir.resolve(RecipeDumpWriters.viewerDumpFileName("jei", "visible"));
                RecipeDumpWriters.writeJsonl(visibleOut, visible);
                outputs.add(new RecipeDumpWriters.ViewerDatasetOutput("jei", "visible", visibleOut.getFileName().toString(), visible.size()));

                List<RecipeDumpWriters.ViewerRecipeSnapshot> all = collectRecipes(recipeManager, level, true);
                Path allOut = dumpDir.resolve(RecipeDumpWriters.viewerDumpFileName("jei", "all"));
                RecipeDumpWriters.writeJsonl(allOut, all);
                outputs.add(new RecipeDumpWriters.ViewerDatasetOutput("jei", "all", allOut.getFileName().toString(), all.size()));

                return outputs;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, List.of());
    }

    private static List<RecipeDumpWriters.ViewerRecipeSnapshot> collectRecipes(IRecipeManager recipeManager, Level level,
                                                                                boolean includeHidden) {
        var categoryLookup = recipeManager.createRecipeCategoryLookup();
        if (includeHidden) {
            categoryLookup.includeHidden();
        }

        List<RecipeDumpWriters.ViewerRecipeSnapshot> snapshots = new ArrayList<>();
        for (IRecipeCategory<?> category : categoryLookup.get().toList()) {
            snapshots.addAll(collectCategoryRecipes(recipeManager, category, level, includeHidden));
        }
        snapshots.sort(Comparator.comparing(RecipeDumpWriters.ViewerRecipeSnapshot::categoryId)
                .thenComparing(RecipeDumpWriters.ViewerRecipeSnapshot::recipeId)
                .thenComparing(RecipeDumpWriters.ViewerRecipeSnapshot::recipeClass));
        return snapshots;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<RecipeDumpWriters.ViewerRecipeSnapshot> collectCategoryRecipes(IRecipeManager recipeManager,
                                                                                        IRecipeCategory category,
                                                                                        Level level,
                                                                                        boolean includeHidden) {
        var lookup = recipeManager.createRecipeLookup(category.getRecipeType());
        if (includeHidden) {
            lookup.includeHidden();
        }

        List<RecipeDumpWriters.ViewerRecipeSnapshot> snapshots = new ArrayList<>();
        for (Object recipe : lookup.get().toList()) {
            snapshots.add(recipeSnapshot(recipeManager, category, recipe, level, includeHidden));
        }
        return snapshots;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RecipeDumpWriters.ViewerRecipeSnapshot recipeSnapshot(IRecipeManager recipeManager,
                                                                          IRecipeCategory category,
                                                                          Object recipe,
                                                                          Level level,
                                                                          boolean includeHidden) {
        String categoryId = category.getRecipeType().getUid().toString();
        String categoryTitle = RecipeDumpWriters.safeComponentString(category.getTitle());
        String recipeId = RecipeDumpWriters.resourceLocationString(category.getRegistryName(recipe));
        List<RecipeDumpWriters.IngredientSnapshot> inputs = List.of();
        List<RecipeDumpWriters.IngredientSnapshot> catalysts = List.of();
        List<RecipeDumpWriters.IngredientSnapshot> outputs = List.of();
        String backingRecipeId = "";

        if (recipe instanceof RecipeHolder<?> holder) {
            backingRecipeId = holder.id().toString();
            if (recipeId.isBlank()) {
                recipeId = backingRecipeId;
            }
            try {
                inputs = holder.value().getIngredients().stream()
                        .map(ingredient -> new RecipeDumpWriters.IngredientSnapshot(
                                "minecraft_ingredient",
                                "",
                                RecipeDumpWriters.stackSnapshots(ingredient.getItems(), level)
                        ))
                        .toList();
                ItemStack output = holder.value().getResultItem(level.registryAccess());
                outputs = output.isEmpty()
                        ? List.of()
                        : List.of(new RecipeDumpWriters.IngredientSnapshot("item_stack", output.getHoverName().getString(),
                                List.of(RecipeDumpWriters.stackSnapshot(output, level))));
            } catch (RuntimeException | LinkageError ignored) {
            }
        }

        return new RecipeDumpWriters.ViewerRecipeSnapshot(
                "jei",
                includeHidden ? "all" : "visible",
                recipeId,
                categoryId,
                categoryTitle,
                recipe.getClass().getName(),
                backingRecipeId,
                inputs,
                catalysts,
                outputs,
                0,
                0
        );
    }
}
