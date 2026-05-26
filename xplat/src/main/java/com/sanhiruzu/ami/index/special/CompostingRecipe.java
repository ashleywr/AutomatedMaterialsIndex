package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class CompostingRecipe implements Recipe<net.minecraft.world.Container> {
    private final ResourceLocation id;
    private final ItemStack stack;
    private final float chance;
    private final RecipeType<?> type;

    public CompostingRecipe(ResourceLocation id, ItemStack stack, float chance, RecipeType<?> type) {
        this.id = id;
        this.stack = stack;
        this.chance = chance;
        this.type = type;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getStack() {
        return stack;
    }

    public float getChance() {
        return chance;
    }

    @Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return new ItemStack(Items.BONE_MEAL);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return new ItemStack(Items.BONE_MEAL);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(stack));
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

