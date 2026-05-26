package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class AnvilRepairRecipe implements Recipe<net.minecraft.world.Container> {
    private final ResourceLocation id;
    private final ItemStack tool;
    private final Ingredient material;
    private final ItemStack output;
    private final RecipeType<?> type;

    public AnvilRepairRecipe(ResourceLocation id, ItemStack tool, Ingredient material, ItemStack output, RecipeType<?> type) {
        this.id = id;
        this.tool = tool;
        this.material = material;
        this.output = output;
        this.type = type;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getTool() {
        return tool;
    }

    public Ingredient getMaterial() {
        return material;
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
        list.add(Ingredient.of(tool));
        list.add(material);
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

