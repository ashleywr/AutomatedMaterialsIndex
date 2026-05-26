package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class GrindstoneRepairRecipe implements Recipe<net.minecraft.world.Container> {
    private final ResourceLocation id;
    private final ItemStack tool1;
    private final ItemStack tool2;
    private final ItemStack output;
    private final RecipeType<?> type;

    public GrindstoneRepairRecipe(ResourceLocation id, ItemStack tool1, ItemStack tool2, ItemStack output, RecipeType<?> type) {
        this.id = id;
        this.tool1 = tool1;
        this.tool2 = tool2;
        this.output = output;
        this.type = type;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getTool1() {
        return tool1;
    }

    public ItemStack getTool2() {
        return tool2;
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
        list.add(Ingredient.of(tool1));
        list.add(Ingredient.of(tool2));
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

