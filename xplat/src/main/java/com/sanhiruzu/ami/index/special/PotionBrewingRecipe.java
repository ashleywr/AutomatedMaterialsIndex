package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class PotionBrewingRecipe implements Recipe<net.minecraft.world.Container> {
    private final ResourceLocation id;
    private final ItemStack input;
    private final Ingredient ingredient;
    private final ItemStack output;
    private final RecipeType<?> type;

    public PotionBrewingRecipe(ResourceLocation id, ItemStack input, Ingredient ingredient, ItemStack output, RecipeType<?> type) {
        this.id = id;
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
        this.type = type;
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
        return output;
    }

    @Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(input));
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }
}

