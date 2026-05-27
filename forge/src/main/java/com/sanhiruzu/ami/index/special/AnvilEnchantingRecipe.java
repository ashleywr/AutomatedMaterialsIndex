package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class AnvilEnchantingRecipe implements Recipe<net.minecraft.world.Container> {
    private final ResourceLocation id;
    private final ItemStack tool;
    private final ItemStack book;
    private final ItemStack enchantedTool;
    private final RecipeType<?> type;

    public AnvilEnchantingRecipe(ResourceLocation id, ItemStack tool, ItemStack book, ItemStack enchantedTool, RecipeType<?> type) {
        this.id = id;
        this.tool = tool;
        this.book = book;
        this.enchantedTool = enchantedTool;
        this.type = type;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getTool() {
        return tool;
    }

    public ItemStack getBook() {
        return book;
    }

    public ItemStack getEnchantedTool() {
        return enchantedTool;
    }

    @Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return enchantedTool.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return enchantedTool;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(tool));
        list.add(Ingredient.of(book));
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

