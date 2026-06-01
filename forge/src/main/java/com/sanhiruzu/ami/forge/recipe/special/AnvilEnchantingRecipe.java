package com.sanhiruzu.ami.forge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AnvilEnchantingRecipeView;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class AnvilEnchantingRecipe implements Recipe<net.minecraft.world.Container>, AnvilEnchantingRecipeView {
    private final ForgeSpecialRecipeData special;
    private final ResourceLocation id;
    private final ItemStack tool;
    private final ItemStack book;

    public AnvilEnchantingRecipe(ResourceLocation id, ItemStack tool, ItemStack book, ItemStack enchantedTool, RecipeType<?> type) {
        this.special = new ForgeSpecialRecipeData(enchantedTool, type, List.of(Ingredient.of(tool), Ingredient.of(book)));
        this.id = id;
        this.tool = tool;
        this.book = book;
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
        return special.result();
    }

    @Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return special.resultCopy();
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return special.result();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return special.ingredientsCopy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return special.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return special.type();
    }
}
