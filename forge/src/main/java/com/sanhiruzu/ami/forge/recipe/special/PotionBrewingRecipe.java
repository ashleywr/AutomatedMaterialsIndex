package com.sanhiruzu.ami.forge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.PotionBrewingRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class PotionBrewingRecipe extends AmiSpecialRecipe implements Recipe<net.minecraft.world.Container>, PotionBrewingRecipeView {
    private final ResourceLocation id;
    private final ItemStack input;
    private final Ingredient ingredient;

    public PotionBrewingRecipe(ResourceLocation id, ItemStack input, Ingredient ingredient, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(input), ingredient));
        this.id = id;
        this.input = input;
        this.ingredient = ingredient;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getInput() {
        return input;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getOutput() {
        return result();
    }

    @Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return resultCopy();
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return result();
    }
}
