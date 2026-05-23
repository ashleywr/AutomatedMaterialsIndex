package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class AnvilEnchantingRecipe implements Recipe<RecipeInput> {
    private final ItemStack tool;
    private final ItemStack book;
    private final ItemStack enchantedTool;
    private final RecipeType<?> type;

    public AnvilEnchantingRecipe(ItemStack tool, ItemStack book, ItemStack enchantedTool, RecipeType<?> type) {
        this.tool = tool;
        this.book = book;
        this.enchantedTool = enchantedTool;
        this.type = type;
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
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return enchantedTool.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
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
