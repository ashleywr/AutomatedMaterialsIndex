package com.sanhiruzu.ami.fabric.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * Fabric intermediate base for AMI's synthetic recipes.
 *
 * <p>{@link AmiSpecialRecipe} (xplat) intentionally does not implement {@link Recipe} because
 * it is shared with the 1.20.1 Forge module, where {@code Recipe}'s type argument is
 * {@code Container} and {@link RecipeInput} does not exist. As a result, the vanilla Recipe
 * methods it defines ({@code getType}, {@code getSerializer}, {@code canCraftInDimensions},
 * {@code getIngredients}, {@code isSpecial}) are not seen by Loom's remapper as implementations
 * of the vanilla interface, so on Fabric they keep their Mojang names and never get renamed to
 * the intermediary names the interface demands — yielding {@code AbstractMethodError} at runtime.
 *
 * <p>This class implements {@code Recipe<RecipeInput>} and re-declares those methods (delegating
 * to the xplat base) so the remapper renames them here. Concrete Fabric recipes extend this and
 * supply {@code matches}/{@code assemble}/{@code getResultItem}.
 */
public abstract class FabricSpecialRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput> {

    protected FabricSpecialRecipe(ItemStack result, RecipeType<?> type, List<Ingredient> ingredients) {
        super(result, type, ingredients);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return super.canCraftInDimensions(width, height);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return super.getIngredients();
    }

    @Override
    public boolean isSpecial() {
        return super.isSpecial();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return super.getSerializer();
    }

    @Override
    public RecipeType<?> getType() {
        return super.getType();
    }
}
