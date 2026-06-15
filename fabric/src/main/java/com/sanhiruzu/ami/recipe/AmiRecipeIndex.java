package com.sanhiruzu.ami.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric stub for AmiRecipeIndex.
 * The full recipe-index implementation is deferred to Milestone E.
 * Provides the public API surface required by xplat so the project compiles.
 */
public final class AmiRecipeIndex extends AmiRecipeIndexBase {

    private static final AmiRecipeIndex INSTANCE = new AmiRecipeIndex();

    private AmiRecipeIndex() {
    }

    public static AmiRecipeIndex getInstance() {
        return INSTANCE;
    }

    // TODO(Milestone E): implement Fabric recipe-type registration and rebuild logic
    public void rebuild(@Nullable Level level) {
        // Stub: no-op until Milestone E
    }

    // -------------------------------------------------------------------------
    // EMI category registry — forwarded to the shared base registry
    // -------------------------------------------------------------------------

    public static RecipeType<?> getEmiCategoryType(net.minecraft.resources.ResourceLocation categoryId, String categoryName) {
        return AmiRecipeCategoryRegistry.getEmiCategoryType(
                categoryId,
                categoryName,
                id -> "emi:" + id.getNamespace() + "/" + id.getPath());
    }

    public static void setEmiCategoryIcon(RecipeType<?> type, ItemStack icon) {
        AmiRecipeCategoryRegistry.setEmiCategoryIcon(type, icon);
    }

    public static boolean isEmiCategoryType(RecipeType<?> type) {
        return AmiRecipeCategoryRegistry.isEmiCategoryType(type);
    }

    public static String getEmiCategoryName(RecipeType<?> type) {
        return AmiRecipeCategoryRegistry.getEmiCategoryName(type);
    }

    public static ItemStack getEmiCategoryIcon(RecipeType<?> type) {
        return AmiRecipeCategoryRegistry.getEmiCategoryIcon(type);
    }
}
